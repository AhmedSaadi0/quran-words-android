package com.quranwords.ui.roots.detail.components.cards

import androidx.compose.material3.ColorScheme
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.BaselineShift
import androidx.compose.ui.text.withStyle
import com.quranwords.core.util.CitationMatcher
import com.quranwords.core.util.MdBlock
import com.quranwords.core.util.MdSpan


/**
 * Flattens a block's inline spans into one [AnnotatedString], overlaying the
 * citation clusters described by [fragments].
 *
 * Both [fragments] and the inline span ranges are offsets into the same string
 * ([MdBlock.text] — always marker-free), so a single left-to-right walk emits
 * them in order without any offset reconciliation. [fragments] must tile the
 * whole source contiguously (as [CitationMatcher.Session.citeRanges] returns);
 * every `Plain` range is emitted literally, every `Cite` range is replaced by
 * its superscript cluster.
 */
internal fun buildBlockString(
    block: MdBlock,
    fragments: List<CitationMatcher.Fragment>,
    style: TextStyle,
    colors: ColorScheme
): AnnotatedString {
    val source = block.text
    val ranges = block.spanRanges()
    val bold = SpanStyle(fontWeight = FontWeight.Bold, color = colors.primary)
    val code = SpanStyle(
        fontFamily = FontFamily.Monospace,
        fontStyle = FontStyle.Italic,
        color = colors.onSurfaceVariant
    )

    return buildAnnotatedString {
        var cursor = 0
        fragments.forEach { fragment ->
            when (fragment) {
                is CitationMatcher.Fragment.Plain -> {
                    // Literal text: emit the gap AND the fragment's own range.
                    // (Emitting only up to fragment.start drops the whole body —
                    // that range is correct only for Cite, where the marker is
                    // replaced by the cluster below.)
                    appendStyled(source, cursor, fragment.end, ranges, bold, code)
                    cursor = fragment.end
                }
                is CitationMatcher.Fragment.Cite -> {
                    appendStyled(source, cursor, fragment.start, ranges, bold, code)
                    val start = length
                    withStyle(
                        SpanStyle(
                            color = colors.primary,
                            fontWeight = FontWeight.Bold,
                            // Eastern Arabic digits are directionally neutral-safe
                            // inside RTL runs; the cluster stays in place.
                            baselineShift = BaselineShift.Superscript,
                            fontSize = style.fontSize * 0.8f
                        )
                    ) {
                        append(CitationMatcher.formatCluster(fragment.numbers))
                    }
                    addStringAnnotation(
                        tag = "cite",
                        annotation = fragment.numbers.first().toString(),
                        start = start,
                        end = length
                    )
                    cursor = fragment.end
                }
            }
        }
        appendStyled(source, cursor, source.length, ranges, bold, code)
    }
}

/**
 * Appends `source[from, to)` applying whichever [MdSpan] styles overlap that
 * range. Offsets are into [source], never into the builder being written, so
 * replacing a `[...]` marker with a shorter `[١]` cluster cannot desynchronise
 * the styling that follows it.
 */
private fun AnnotatedString.Builder.appendStyled(
    source: String,
    from: Int,
    to: Int,
    ranges: List<MdSpanRange>,
    bold: SpanStyle,
    code: SpanStyle
) {
    if (to <= from) return
    var cursor = from
    ranges.forEach { range ->
        val overlapStart = maxOf(range.start, from)
        val overlapEnd = minOf(range.end, to)
        if (overlapEnd <= overlapStart) return@forEach
        if (overlapStart > cursor) {
            append(source, cursor, overlapStart)
            cursor = overlapStart
        }
        when (range.span) {
            is MdSpan.Bold -> withStyle(bold) { append(source, cursor, overlapEnd) }
            is MdSpan.Code -> withStyle(code) { append(source, cursor, overlapEnd) }
            is MdSpan.Text -> append(source, cursor, overlapEnd)
        }
        cursor = overlapEnd
    }
    if (cursor < to) append(source, cursor, to)
}

/** An [MdSpan] positioned inside its block's flattened [MdBlock.text]. */
private data class MdSpanRange(val start: Int, val end: Int, val span: MdSpan)

private fun MdBlock.spanRanges(): List<MdSpanRange> {
    val ranges = mutableListOf<MdSpanRange>()
    var offset = 0
    inlines.forEach { span ->
        ranges += MdSpanRange(offset, offset + span.plain.length, span)
        offset += span.plain.length
    }
    return ranges
}