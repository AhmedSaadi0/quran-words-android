package com.quranwords.ui.surah.detail.components

import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.TextLayoutResult

/**
 * Overlay bands covering one ayah's characters. In the centered fallback the
 * bands are pre-split for the two-pass clip/translate drawing; in the
 * justified rectangle the last-line bands are already re-positioned with
 * their tokens.
 */
internal data class PulseGeometry(
    val above: List<Rect>,
    val lastLine: List<Rect>,
    val centered: Boolean,
    val lastLineTop: Float,
    val emptySpace: Float
) {
    val isEmpty: Boolean get() = above.isEmpty() && lastLine.isEmpty()
}

/** Horizontal band of an ayah's glyphs on one layout line; null if no overlap. */
internal fun ayahLineBand(
    segStart: Int,
    segEnd: Int,
    lineStart: Int,
    lineEnd: Int,
    firstBox: Rect,
    lastBox: Rect,
    lineLeft: Float,
    lineRight: Float
): Rect? {
    if (segStart >= segEnd || segEnd <= lineStart || segStart >= lineEnd) return null
    val x1 = minOf(firstBox.left, lastBox.left)
    val x2 = maxOf(firstBox.right, lastBox.right)
    // A collapsed box would be invisible; widen to the full line instead.
    val collapsed = x2 - x1 < 1f
    return Rect(
        left = if (collapsed) lineLeft else x1.coerceIn(lineLeft, lineRight),
        top = firstBox.top,
        right = if (collapsed) lineRight else x2.coerceIn(lineLeft, lineRight),
        bottom = firstBox.bottom
    )
}

/**
 * Layout-space line rects intersecting the given ayah's annotated range.
 * Each band is clipped horizontally to the ayah's own characters on that
 * line (not the full line width), so verses sharing a line never bleed into
 * each other. Returns null when the layout is empty or the ayah is not in
 * this block.
 */
internal fun TextLayoutResult.pulseGeometry(
    text: AnnotatedString,
    ayahNum: Int,
    justification: LastLineJustification?
): PulseGeometry? {
    if (lineCount == 0 || text.isEmpty()) return null
    val span = text.getStringAnnotations(FLOW_AYAH_TAG, 0, text.length)
        .firstOrNull { it.item.toIntOrNull() == ayahNum } ?: return null
    val lastLineIndex = lineCount - 1
    val emptySpace = lastLineEmptySpace()
    val centered = justification == null && emptySpace > LAST_LINE_EPS_PX
    val lastLineTop = getLineTop(lastLineIndex)
    val above = mutableListOf<Rect>()
    val lastLine = mutableListOf<Rect>()
    val just = justification
    for (line in 0 until lineCount) {
        // Justified rectangle: the last line is owned by the re-positioned
        // tokens below.
        if (just != null && line == lastLineIndex) continue
        if (getLineEnd(line) <= span.start || getLineStart(line) >= span.end) continue
        val band = ayahLineBand(
            segStart = maxOf(getLineStart(line), span.start),
            segEnd = minOf(getLineEnd(line), span.end),
            lineStart = getLineStart(line),
            lineEnd = getLineEnd(line),
            firstBox = getBoundingBox(maxOf(getLineStart(line), span.start)),
            lastBox = getBoundingBox(minOf(getLineEnd(line), span.end) - 1),
            lineLeft = getLineLeft(line),
            lineRight = getLineRight(line)
        ) ?: continue
        val rect = band.copy(top = getLineTop(line), bottom = getLineBottom(line))
        if (centered && line == lastLineIndex) lastLine.add(rect) else above.add(rect)
    }
    // Justified rectangle: the target ayah's tokens re-positioned in place.
    if (just != null) {
        just.tokens.filter { it.ayah == ayahNum }.forEach { token ->
            lastLine.add(
                Rect(
                    left = token.finalLeft,
                    top = just.lineTop,
                    right = token.finalRight,
                    bottom = getLineBottom(lastLineIndex)
                )
            )
        }
    }
    return PulseGeometry(above, lastLine, centered, lastLineTop, emptySpace)
}

/**
 * Resolves the tapped ayah with a ±2 char window so taps landing on the
 * inter-ayah space still map to a verse instead of dropping silently.
 */
internal fun TextLayoutResult.ayahAt(text: AnnotatedString, position: Offset): Int? {
    val center = getOffsetForPosition(position).coerceIn(0, text.length)
    val start = (center - 2).coerceAtLeast(0)
    val end = (center + 2).coerceAtMost(text.length)
    return text.getStringAnnotations(FLOW_AYAH_TAG, start, end).firstOrNull()?.item?.toIntOrNull()
}
