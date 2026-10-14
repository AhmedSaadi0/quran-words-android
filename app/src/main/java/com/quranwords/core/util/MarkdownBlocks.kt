package com.quranwords.core.util

/**
 * Minimal Markdown parser for the `root_ai_summary` corpus.
 *
 * The dialect is deliberately tiny and was measured against the shipped DB
 * (1,642 roots): only ~139 roots (~8.5%) contain any markup at all, the rest
 * is legacy plain prose. Supported constructs and their real-world counts:
 *
 * | Construct        | Observed                                                     |
 * |------------------|--------------------------------------------------------------|
 * | `- ` bullet      | 3,902 lines, one nesting level via 2-space indent (612)     |
 * | `**bold**`       | inline, always balanced, never spans a newline, never holds `[]` |
 * | `` `code` ``     | 25 lines — table names such as `masadir`                     |
 * | blank line       | paragraph break (`summary_ar`, `tatawwur`, `shabaka`, `furuq`) |
 *
 * **Not** supported on purpose: headings, ordered lists, tables, links,
 * fenced code, block quotes, horizontal rules, and **single-asterisk
 * italics** — the only `*` occurrences outside `**` are Arabic poetic
 * caesuras inside Quran quotations (`﴿فَوَيْلٌ * لِلْمُصَلِّينَ﴾`), so
 * treating them as emphasis would corrupt scripture. Quranic `{...}` braces
 * are likewise left verbatim (see Decision in AGENTS.md: they collide with
 * Lane's `{...}` in `root_meanings`).
 *
 * **Graceful degradation is the contract.** A body with zero markup parses to
 * exactly one [MdBlock.Paragraph] whose flattened text is byte-identical to
 * the input, so the 1,503 legacy roots render exactly as they did before.
 * Pure Kotlin, no Android deps — see `MarkdownBlocksTest`.
 */
object MarkdownBlocks {

    /** Max nesting honoured; the corpus never exceeds 1, extra depth is clamped. */
    private const val MAX_DEPTH = 1

    /** Bullet glyphs for depth 0 / depth 1 (already RTL-mirrored by layout, not by text). */
    private val BULLET_GLYPHS = listOf("•", "–")

    /** Matches a bullet line and captures its indentation and content. */
    private val BULLET_REGEX = Regex("^(\\s*)-\\s+(.*)$")

    /**
     * Same shape as [BULLET_REGEX] but line-anchored across the whole body, so
     * the guard also fires for bodies whose first line is *not* a bullet.
     */
    private val ANY_BULLET_REGEX = Regex("(?m)^\\s*-\\s+")

    /** `**bold**` — non-greedy, never crosses a newline (SP is deliberate). */
    private val BOLD_REGEX = Regex("\\*\\*(.+?)\\*\\*")

    /** `` `code` `` — non-greedy, never crosses a newline. */
    private val CODE_REGEX = Regex("`([^`\n]+)`")

    /** Cheap guard so legacy plain-text bodies skip parsing entirely. */
    fun hasMarkdown(text: String): Boolean =
        text.contains("**") || text.contains("`") || ANY_BULLET_REGEX.containsMatchIn(text)

    /**
     * Splits [raw] into render-ready blocks.
     *
     * Rules:
     * - `- `/`  - ` lines become [MdBlock.Bullet] with `depth = indent / 2`
     *   clamped to [MAX_DEPTH].
     * - Consecutive non-blank, non-bullet lines coalesce into a single
     *   [MdBlock.Paragraph], keeping internal `\n` (soft line breaks).
     * - A blank line closes the current paragraph/list.
     * - A non-bullet line immediately after a bullet list **closes the list**
     *   (the `shabaka` "witnesses then prose" shape, 138 roots).
     */
    fun parse(raw: String): List<MdBlock> {
        if (raw.isBlank()) return emptyList()
        // Graceful degradation: a body with no markup is returned whole — even
        // when it contains blank lines — so it renders through the exact same
        // single-`Text` path as before Markdown support existed. Blank-line
        // paragraph splitting only applies once there is real markup to honour.
        if (!hasMarkdown(raw)) {
            return listOf(MdBlock.Paragraph(inlinesOf(raw)))
        }
        val blocks = mutableListOf<MdBlock>()
        var pendingLines = mutableListOf<String>()

        fun flushParagraph() {
            if (pendingLines.isEmpty()) return
            blocks += MdBlock.Paragraph(inlinesOf(pendingLines.joinToString("\n")))
            pendingLines = mutableListOf()
        }

        for (line in raw.split('\n')) {
            val bullet = BULLET_REGEX.find(line)
            when {
                bullet != null -> {
                    flushParagraph()
                    val depth = (bullet.groupValues[1].length / 2).coerceIn(0, MAX_DEPTH)
                    blocks += MdBlock.Bullet(depth, inlinesOf(bullet.groupValues[2]))
                }
                // Soft break: keep accumulating into the open paragraph.
                line.isBlank() -> flushParagraph()
                else -> pendingLines += line
            }
        }
        flushParagraph()
        return blocks
    }

    /**
     * Plain-text projection used by copy/share, the 2-line root card preview and
     * the report dialog. Markup markers are removed while every visible
     * character is preserved; bullets become `▪ ` so the hierarchy survives.
     */
    fun renderPlain(raw: String): String =
        parse(raw).joinToString("\n") { block ->
            when (block) {
                is MdBlock.Bullet -> "  ".repeat(block.depth) + "▪ " + block.text
                is MdBlock.Paragraph -> block.text
            }
        }

    /** Bullet glyph for a given [MdBlock.Bullet.depth]. */
    fun glyphFor(depth: Int): String = BULLET_GLYPHS[depth.coerceIn(0, BULLET_GLYPHS.lastIndex)]

    private fun inlinesOf(line: String): List<MdSpan> {
        if (line.isEmpty()) return listOf(MdSpan.Text(""))
        // One pass over the line; whichever marker starts earliest wins, so
        // bold and code can interleave without a precedence problem.
        val spans = mutableListOf<MdSpan>()
        var cursor = 0
        while (cursor < line.length) {
            val bold = BOLD_REGEX.find(line, cursor)
            val code = CODE_REGEX.find(line, cursor)
            val next = when {
                bold == null && code == null -> null
                code == null -> bold
                bold == null -> code
                else -> if (bold.range.first <= code.range.first) bold else code
            } ?: break
            if (next.range.first > cursor) {
                spans += MdSpan.Text(line.substring(cursor, next.range.first))
            }
            val inner = next.groupValues[1]
            spans += if (next === bold) MdSpan.Bold(inner) else MdSpan.Code(inner)
            cursor = next.range.last + 1
        }
        if (cursor < line.length) spans += MdSpan.Text(line.substring(cursor))
        return spans
    }
}

/** One renderable unit of a parsed body. */
sealed interface MdBlock {
    /** The spans carry inline markup. */
    val inlines: List<MdSpan>

    /**
     * Marker-free text of this block — the string citations are matched
     * against. Keeping this flattened (never the raw `- **x** y` source line)
     * is what stops bold/code spans from drifting out of sync with the
     * citation offsets computed over the same string.
     */
    val text: String
        get() = inlines.joinToString("") { it.plain }

    /** Prose. Legacy single-block bodies land here, soft `\n` breaks preserved. */
    data class Paragraph(override val inlines: List<MdSpan>) : MdBlock

    /** List item; [depth] is `0` for top level, `1` for the single nested level. */
    data class Bullet(val depth: Int, override val inlines: List<MdSpan>) : MdBlock
}

/** An inline run inside an [MdBlock]. */
sealed interface MdSpan {
    val plain: String

    data class Text(override val plain: String) : MdSpan

    /** `**text**` — rendered bold in the section's primary colour. */
    data class Bold(override val plain: String) : MdSpan

    /** `` `text` `` — rendered monospaced and de-emphasised. */
    data class Code(override val plain: String) : MdSpan
}