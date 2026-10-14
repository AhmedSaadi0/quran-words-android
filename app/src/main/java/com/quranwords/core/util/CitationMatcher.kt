package com.quranwords.core.util

/**
 * Turns inline `[...]` book markers in AI texts into sequential citations.
 *
 * Numbering follows **first appearance in the body** (1, 2, 3…), not the raw
 * `refs` order — the refs list is reordered to match. Matching is
 * normalization-based ([ArabicNormalizer.normalizeAr]), so inflected /
 * diacritized markers like `[مَقَايِيسِ اللُّغَةِ]` match plain refs like
 * `مقاييس اللغة`. Unmatched brackets are kept literally so old-style data
 * renders exactly as before.
 *
 * Pure Kotlin, no Android deps — mandatory unit tests in CitationMatcherTest.
 */
object CitationMatcher {

    sealed interface Segment {
        data class Text(val value: String) : Segment
        data class Cluster(val numbers: List<Int>) : Segment
    }

    data class CitedBody(
        val segments: List<Segment>,
        /** Cited refs first (by appearance), never-cited refs appended. */
        val orderedRefs: List<String>
    )

    private val MARKER_REGEX = Regex("\\[([^\\[\\]\\n]+)\\]")
    private const val EASTERN_ZERO = '٠'
    private val ZERO_WIDTH_REGEX = Regex("[\u200B-\u200D\uFEFF]")
    private val EDGE_PUNCT_REGEX = Regex("^[\\s\\u00A0,;:.،؛؟!…\\-–—\"'«»()\\[\\]]+|[\\s\\u00A0,;:.،؛؟!…\\-–—\"'«»()\\[\\]]+$")
    private val WHITESPACE_RUN_REGEX = Regex("\\s+")

    /**
     * Matcher-local token cleaning (kept out of [ArabicNormalizer] so search
     * behavior is untouched): NBSP -> space, zero-width chars stripped, edge
     * punctuation trimmed, whitespace collapsed — then normalized. This is
     * what makes `[مَقَايِيسِ اللُّغَةِ]` strictly equal `"مقاييس اللغة"`.
     */
    fun cleanToken(raw: String): String {
        if (raw.all { it == '\u00A0' || it.isWhitespace() }) return ""
        var res = raw.replace('\u00A0', ' ')
        res = ZERO_WIDTH_REGEX.replace(res, "")
        res = EDGE_PUNCT_REGEX.replace(res, "")
        res = WHITESPACE_RUN_REGEX.replace(res, " ")
        return ArabicNormalizer.normalizeAr(res)
    }

    fun toEasternArabicDigits(value: Int): String =
        value.toString().map { c ->
            if (c in '0'..'9') EASTERN_ZERO + (c - '0') else c
        }.joinToString("")

    /** Renders a cluster as `[١، ٢]` (Eastern digits, Arabic comma). */
    fun formatCluster(numbers: List<Int>): String =
        "[" + numbers.joinToString("، ") { toEasternArabicDigits(it) } + "]"

    /**
     * Source-range aware citation output. [start]/[end] are offsets into the
     * scanned string, so a renderer that overlays its own span styling (see
     * `CitedBodyText`) stays in one coordinate system instead of having to
     * reconcile builder offsets against source offsets. A [Cite] may span the
     * whitespace between two merged markers; that gap is dropped, exactly as
     * the pre-markdown segment renderer did.
     */
    sealed interface Fragment {
        val start: Int
        val end: Int

        /** Literal text, including any bracket the matcher could not resolve. */
        data class Plain(override val start: Int, override val end: Int) : Fragment

        /** One or more resolved `[...]` markers rendered as a single cluster. */
        data class Cite(
            override val start: Int,
            override val end: Int,
            val numbers: List<Int>
        ) : Fragment
    }

    /**
     * Numbering state shared across the **whole body**, so a caller that splits
     * the body into blocks (see `MarkdownBlocks`) still numbers citations by
     * first appearance across all of them — never restarting per block.
     *
     * Usage: build one session, call [citeRanges] per block in document order,
     * then read [orderedRefs] once at the end.
     */
    class Session(private val refs: List<String>) {
        // Normalized ref -> first refs index (duplicate spellings: first wins).
        private val refIndexByNormalized = mutableMapOf<String, Int>().apply {
            refs.forEachIndexed { index, ref -> putIfAbsent(cleanToken(ref), index) }
        }
        private val numberByRefIndex = mutableMapOf<Int, Int>()

        /** Cited refs first (by appearance), never-cited refs appended. */
        val orderedRefs: List<String>
            get() {
                val citedRefIndices = numberByRefIndex.entries.sortedBy { it.value }.map { it.key }
                return citedRefIndices.map { refs[it] } +
                    refs.filterIndexed { index, _ -> index !in numberByRefIndex }
            }

        /** Range-based scan — preferred by renderers overlaying their own spans. */
        fun citeRanges(fragment: String): List<Fragment> =
            if (fragment.isBlank() || refs.isEmpty()) {
                listOf(Fragment.Plain(0, fragment.length))
            } else {
                scan(fragment)
            }

        /** Segment-based scan — preserves the original public API shape. */
        fun citeFragment(fragment: String): List<Segment> {
            if (fragment.isBlank() || refs.isEmpty()) return listOf(Segment.Text(fragment))
            val source = fragment
            return scan(fragment).map { fragment ->
                when (fragment) {
                    is Fragment.Plain -> Segment.Text(source.substring(fragment.start, fragment.end))
                    is Fragment.Cite -> Segment.Cluster(fragment.numbers)
                }
            }
        }

        /** Raw scan: text runs + per-marker ref hits (unmatched stays literal). */
        private fun scan(fragment: String): List<Fragment> {
            val tokens = mutableListOf<Fragment>()
            var cursor = 0
            for (match in MARKER_REGEX.findAll(fragment)) {
                if (match.range.first > cursor) {
                    tokens.add(Fragment.Plain(cursor, match.range.first))
                }
                val inner = match.groupValues[1].trim()
                val refIndex = if (inner.isEmpty()) null else refIndexByNormalized[cleanToken(inner)]
                tokens += if (refIndex == null) {
                    Fragment.Plain(match.range.first, match.range.last + 1)
                } else {
                    val number = numberByRefIndex.getOrPut(refIndex) { numberByRefIndex.size + 1 }
                    Fragment.Cite(match.range.first, match.range.last + 1, listOf(number))
                }
                cursor = match.range.last + 1
            }
            if (cursor < fragment.length) tokens.add(Fragment.Plain(cursor, fragment.length))
            return mergeAdjacent(mergeWhitespaceSplitClusters(tokens, fragment))
        }

        /** Merge clusters split only by whitespace: `[a] [b]` -> one `[١، ٢]`. */
        private fun mergeWhitespaceSplitClusters(
            tokens: List<Fragment>,
            source: String
        ): List<Fragment> {
            val merged = mutableListOf<Fragment>()
            var i = 0
            while (i < tokens.size) {
                val current = tokens[i]
                val gap = tokens.getOrNull(i + 1)
                val following = tokens.getOrNull(i + 2)
                if (current is Fragment.Cite &&
                    gap is Fragment.Plain &&
                    source.substring(gap.start, gap.end).isBlank() &&
                    following is Fragment.Cite
                ) {
                    val numbers = (current.numbers + following.numbers).distinct()
                    merged.add(Fragment.Cite(current.start, following.end, numbers))
                    i += 3
                } else {
                    merged.add(current)
                    i++
                }
            }
            return merged
        }

        /** Coalesce adjacent text runs (e.g. around unmatched literals). */
        private fun mergeAdjacent(tokens: List<Fragment>): List<Fragment> {
            val fragments = mutableListOf<Fragment>()
            for (token in tokens) {
                val last = fragments.lastOrNull()
                if (token is Fragment.Plain && last is Fragment.Plain && last.end == token.start) {
                    fragments[fragments.lastIndex] = Fragment.Plain(last.start, token.end)
                } else {
                    fragments.add(token)
                }
            }
            return fragments
        }
    }

    /** Single-fragment convenience wrapper around [Session] (unchanged behavior). */
    fun cite(body: String, refs: List<String>): CitedBody {
        val session = Session(refs)
        return CitedBody(session.citeFragment(body), session.orderedRefs)
    }

    /** Plain-text rendering for copy/share: clusters become `[١، ٢]`. */
    fun renderPlain(cited: CitedBody): String =
        cited.segments.joinToString("") { segment ->
            when (segment) {
                is Segment.Text -> segment.value
                is Segment.Cluster -> formatCluster(segment.numbers)
            }
        }
}
