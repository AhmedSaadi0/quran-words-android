package com.quranwords.ui.surah.detail.components

import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.TextLayoutResult
import androidx.compose.ui.text.TextMeasurer
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.unit.Constraints
import androidx.compose.ui.unit.LayoutDirection

/** Sub-pixel tolerance (px) below which the last line counts as full. */
internal const val LAST_LINE_EPS_PX = 0.5f

/**
 * One drawable token of the justified last line: its character range, owning
 * ayah, paragraph-local ink rect and the LEFTWARD shift (px) applied when
 * drawing. Glyphs are never scaled or stretched — only the inter-token gaps
 * widen.
 */
internal data class JustifiedToken(
    val charStart: Int,
    val charEnd: Int,
    val ayah: Int,
    val origLeft: Float,
    val origRight: Float,
    /** Leftward shift in px: final x = original x − shift. */
    val shift: Float,
    /** Single-line layout of the token's styled substring, drawn at its final position. */
    val layout: TextLayoutResult
) {
    val finalLeft: Float get() = origLeft - shift
    val finalRight: Float get() = origRight - shift
}

/**
 * Word-gap justification plan for the block's last line, or null when the
 * line must keep the centered fallback (already full, a single token, or a
 * required gap that would read as stretched).
 */
internal data class LastLineJustification(
    val lineTop: Float,
    val tokens: List<JustifiedToken>
) {
    /** Maps a visual x (px) inside the justified band back to layout coordinates. */
    fun toLayoutX(x: Float): Float {
        val containing = tokens.firstOrNull { x >= it.finalLeft && x <= it.finalRight }
        if (containing != null) return x + containing.shift
        val nearest = tokens.minByOrNull {
            if (x < it.finalLeft) it.finalLeft - x else x - it.finalRight
        } ?: return x
        return x + nearest.shift
    }
}

private data class TokenRect(val range: IntRange, val left: Float, val right: Float, val ayah: Int)

/**
 * Computes the last-line justification plan: the line's unused width is
 * distributed EQUALLY into the inter-token gaps (spaces only), so the whole
 * block renders as a uniform rectangle with every letter and word at exactly
 * its normal size and shape. Each token is re-laid-out standalone (shaping
 * cannot cross spaces, so per-token layouts are visually identical) and is
 * drawn shifted left at draw time — zero text relayout per frame.
 *
 * Returns null when the line is already full, has fewer than two tokens, or
 * the per-gap width would exceed [maxGapFactor] × the space advance (a giant
 * gap reads as broken — the caller keeps the centered-last-line fallback).
 */
internal fun computeLastLineJustification(
    text: AnnotatedString,
    layout: TextLayoutResult,
    style: TextStyle,
    textMeasurer: TextMeasurer,
    maxGapFactor: Float = 2f
): LastLineJustification? {
    if (layout.lineCount == 0 || text.isEmpty()) return null
    val lastLine = layout.lineCount - 1
    val lineStart = layout.getLineStart(lastLine)
    val lineEnd = layout.getLineEnd(lastLine)
    val lineTop = layout.getLineTop(lastLine)
    val lineLeft = layout.getLineLeft(lastLine)

    // Tokens = maximal runs without a breaking space; the wrap space (when
    // the layout keeps it on the line) forms no token. The ﴿n﴾ marker is
    // glued to its ayah's last word by a non-breaking space, so they are one
    // token — exactly the unit that must never separate.
    val chars = text.text
    val ranges = mutableListOf<IntRange>()
    var start = -1
    for (i in lineStart until lineEnd) {
        if (chars[i] == ' ') {
            if (start >= 0) {
                ranges.add(start until i)
                start = -1
            }
        } else if (start < 0) {
            start = i
        }
    }
    if (start >= 0) ranges.add(start until lineEnd)
    if (ranges.size < 2) return null

    // Ink rect per token: union of the first/last glyph boxes — direction-
    // correct in RTL (same technique as the pulse bands).
    val rects = ranges.map { range ->
        val firstBox = layout.getBoundingBox(range.first)
        val lastBox = layout.getBoundingBox(range.last)
        TokenRect(
            range = range,
            left = minOf(firstBox.left, lastBox.left),
            right = maxOf(firstBox.right, lastBox.right),
            ayah = text.getStringAnnotations(FLOW_AYAH_TAG, range.first, range.last + 1)
                .firstOrNull()?.item?.toIntOrNull() ?: 0
        )
    }

    // Unused width sits at the LEFT end of an RTL line (the text is flush
    // against the right margin).
    val empty = rects.minOf { it.left } - lineLeft
    if (empty <= LAST_LINE_EPS_PX) return null

    val gapWidth = empty / (rects.size - 1)
    val spaceWidth = textMeasurer.measure(
        text = " ",
        style = style,
        constraints = Constraints(maxWidth = layout.size.width),
        layoutDirection = LayoutDirection.Rtl
    ).size.width.toFloat()
    if (gapWidth > spaceWidth * maxGapFactor) return null

    val tokens = rects.mapIndexed { index, rect ->
        val tokenLayout = textMeasurer.measure(
            text = text.subSequence(rect.range.first, rect.range.last + 1),
            style = style,
            constraints = Constraints(maxWidth = layout.size.width),
            layoutDirection = LayoutDirection.Rtl
        )
        // A token never wraps (it fit on the line); bail defensively otherwise.
        if (tokenLayout.lineCount != 1) return null
        JustifiedToken(
            charStart = rect.range.first,
            charEnd = rect.range.last + 1,
            ayah = rect.ayah,
            origLeft = rect.left,
            origRight = rect.right,
            shift = index * gapWidth,
            layout = tokenLayout
        )
    }
    return LastLineJustification(lineTop = lineTop, tokens = tokens)
}

/**
 * Unused width on the last line, in pixels. Uses the text layout width
 * ([TextLayoutResult.size]) rather than the draw-scope size so shifts stay
 * pixel-perfect regardless of the [Text] padding.
 */
internal fun TextLayoutResult.lastLineEmptySpace(): Float {
    if (lineCount == 0) return 0f
    // getLineWidth() exists only on newer Compose; this BOM (2024.09.00)
    // provides getLineLeft()/getLineRight() — equivalent width.
    val last = lineCount - 1
    return size.width - (getLineRight(last) - getLineLeft(last))
}

/**
 * Re-maps a visual tap/long-press position back to text-layout coordinates
 * for the CENTERED last-line fallback: the line is drawn shifted left by
 * `emptySpace / 2`, so taps on it shift back right before querying the
 * annotated ranges. Positions above the last line pass through untouched.
 */
internal fun TextLayoutResult.remapForCenteredLastLine(position: Offset): Offset {
    if (lineCount == 0) return position
    val lastLineIndex = lineCount - 1
    val emptySpace = lastLineEmptySpace()
    if (emptySpace <= LAST_LINE_EPS_PX) return position
    if (position.y < getLineTop(lastLineIndex)) return position
    return position.copy(x = position.x + emptySpace / 2f)
}

/**
 * Unified tap remap: justified last line maps through the per-token shifts;
 * otherwise the centered fallback applies. Lines above the last line pass
 * through in both cases.
 */
internal fun TextLayoutResult.remapForLastLine(
    justification: LastLineJustification?,
    position: Offset
): Offset {
    if (justification != null && position.y >= justification.lineTop) {
        return position.copy(x = justification.toLayoutX(position.x))
    }
    return remapForCenteredLastLine(position)
}
