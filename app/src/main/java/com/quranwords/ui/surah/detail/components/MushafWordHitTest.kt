package com.quranwords.ui.surah.detail.components

import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.TextLayoutResult
import com.quranwords.domain.model.Ayah
import com.quranwords.domain.model.WordToken

/**
 * Tag wrapping ONLY the ayah-end marker body (`﴿n﴾`) — not the `\u00A0` gluing
 * it to the ayah's final word. Taps landing on the marker are inert; taps on
 * the gluing NBSP fall back to nearest-word resolution (the ayah's final word).
 */
internal const val FLOW_MARKER_TAG = "ayah_marker"

/** Winner of a word hit test: the resolved pair [onWordClick] needs. */
internal data class WordHit(val word: WordToken, val ayah: Ayah)

/**
 * Picks the index of the rect winning the tap at [x], or null when there is
 * nothing reasonable to hit.
 *
 * Rules, in order:
 * 1. `null` when [rects] is empty.
 * 2. Exact containment wins — `x in rect.left..rect.right` selects that rect
 *    (first match in input order if rects overlap; they never do in practice).
 * 3. Taps further than [outerMarginSlopPx] outside the whole band
 *    `[minOf{it.left}, maxOf{it.right}]` select nothing (blank margins on short
 *    centered last lines must not trigger edge words).
 * 4. Otherwise the rect with the smallest horizontal distance from [x] to its
 *    `[left, right]` interval wins — every inter-word gap, every trailing
 *    half, every wide justified gap maps to a word. On an exactly equidistant
 *    gap (mid-gap), the rect appearing EARLIER in [rects] (reading order)
 *    wins, deterministically.
 *
 * Note: callers pass [rects] in reading (annotation) order; the containment and
 * distance rules themselves are direction-agnostic (RTL-safe).
 */
internal fun closestWordIndex(x: Float, rects: List<Rect>, outerMarginSlopPx: Float): Int? {
    if (rects.isEmpty()) return null

    // Exact containment first.
    for (i in rects.indices) {
        if (x >= rects[i].left && x <= rects[i].right) return i
    }

    val lineMinLeft = rects.minOf { it.left }
    val lineMaxRight = rects.maxOf { it.right }
    if (x < lineMinLeft - outerMarginSlopPx || x > lineMaxRight + outerMarginSlopPx) {
        return null
    }

    var bestIndex = -1
    var bestDistance = Float.POSITIVE_INFINITY
    for (i in rects.indices) {
        val rect = rects[i]
        val distance = when {
            x < rect.left -> rect.left - x
            x > rect.right -> x - rect.right
            else -> 0f
        }
        // Strict inequality keeps the earlier rect on an exact tie.
        if (distance < bestDistance) {
            bestDistance = distance
            bestIndex = i
        }
    }
    return if (bestIndex >= 0) bestIndex else null
}

/**
 * Forgiving word hit test for the continuous-flow mushaf block. Unlike a plain
 * `getOffsetForPosition` + annotation query (which returns null inside stretched
 * justified gaps and requires the tap to land on an exact annotated offset),
 * this resolves a tap to the word it is visually pointing at:
 *
 * - The line is located by the tap's vertical band; taps outside the block's
 *   vertical extent select nothing.
 * - The ayah-end marker ([FLOW_MARKER_TAG]) is inert even though the gluing
 *   NBSP is not.
 * - Every other horizontal position on the line maps to the containing or
 *   nearest word (see [closestWordIndex]); the marker check runs before the
 *   nearest-word fallback so `﴿n﴾` can never open a sheet.
 * - Arabic tashkeel/combining marks measure as zero-width boxes — bounds are
 *   the union over ALL glyph boxes of the span with `width > 0f` (the same
 *   pattern as the pulse overlay), never first/last char.
 * - Last-line rendering is accounted for per span: justified tokens are drawn
 *   shifted left by `token.shift` (so their visual band moves with them), or —
 *   under the centered fallback — the whole line is drawn shifted left by half
 *   its empty space.
 *
 * [lastLineJustification] and [lastLineEmptySpace] are produced by the caller
 * (it owns the layout); pass 0f / null to disable the last-line handling.
 * Direction-safe throughout: spans merge min/max box edges.
 */
internal fun TextLayoutResult.wordAtPoint(
    text: AnnotatedString,
    ayahByNumber: Map<Int, Ayah>,
    position: Offset,
    lastLineJustification: LastLineJustification?,
    lastLineEmptySpace: Float,
    outerMarginSlopPx: Float
): WordHit? {
    if (lineCount == 0 || text.isEmpty()) return null

    // Vertical line check: first line whose band contains the tap; the whole
    // block vertically must contain it too.
    if (position.y < getLineTop(0) || position.y > getLineBottom(lineCount - 1)) return null
    var lineIndex = -1
    for (i in 0 until lineCount) {
        if (position.y >= getLineTop(i) && position.y <= getLineBottom(i)) {
            lineIndex = i
            break
        }
    }
    if (lineIndex < 0) return null

    val lineStart = getLineStart(lineIndex)
    val lineEnd = getLineEnd(lineIndex)

    // Horizontal shift (px) bringing a layout span into its DRAWN position on
    // the last line; 0f elsewhere. Keeps the transform symmetric with the
    // draw-time offsets in drawWithContent.
    fun shiftOf(spanStart: Int, spanEnd: Int): Float {
        if (lineIndex < lineCount - 1) return 0f
        val justification = lastLineJustification
        if (justification != null) {
            val token = justification.tokens.firstOrNull { token ->
                token.charStart <= spanStart && spanEnd <= token.charEnd
            }
            return if (token != null) -token.shift else 0f
        }
        return if (lastLineEmptySpace > LAST_LINE_EPS_PX) -lastLineEmptySpace / 2f else 0f
    }

    // Visual bounds of [start, end): union of all positive-width glyph boxes,
    // re-positioned into drawn coordinates. Null when nothing inked (a pure
    // zero-width span).
    fun spanVisualRect(start: Int, end: Int): Rect? {
        var left = Float.POSITIVE_INFINITY
        var right = Float.NEGATIVE_INFINITY
        for (i in start until end) {
            val box = getBoundingBox(i)
            if (box.width <= 0f) continue
            if (box.left < left) left = box.left
            if (box.right > right) right = box.right
        }
        if (left == Float.POSITIVE_INFINITY) return null
        val shift = shiftOf(start, end)
        return Rect(
            left = left + shift,
            top = getLineTop(lineIndex),
            right = right + shift,
            bottom = getLineBottom(lineIndex)
        )
    }

    // Inert ayah-end marker — checked before any nearest-word fallback.
    for (marker in text.getStringAnnotations(FLOW_MARKER_TAG, lineStart, lineEnd)) {
        val bounds = spanVisualRect(marker.start, marker.end) ?: continue
        if (position.x in bounds.left..bounds.right) return null
    }

    val wordAnnotations = text.getStringAnnotations(FLOW_WORD_TAG, lineStart, lineEnd)
    if (wordAnnotations.isEmpty()) return null

    val rects = ArrayList<Rect>(wordAnnotations.size)
    val spans = ArrayList<AnnotatedString.Range<String>>(wordAnnotations.size)
    for (annotation in wordAnnotations) {
        val bounds = spanVisualRect(annotation.start, annotation.end) ?: continue
        rects.add(bounds)
        spans.add(annotation)
    }

    val winnerIndex = closestWordIndex(position.x, rects, outerMarginSlopPx) ?: return null
    val winner = spans[winnerIndex]
    val wordId = winner.item.toIntOrNull() ?: return null
    val ayahNum = text.getStringAnnotations(FLOW_AYAH_TAG, winner.start, winner.end)
        .firstOrNull()?.item?.toIntOrNull() ?: return null
    val ayah = ayahByNumber[ayahNum] ?: return null
    val word = ayah.words.find { it.wordAyahId == wordId }
        ?: ayah.words.find { it.wordId == wordId }
        ?: return null
    return WordHit(word, ayah)
}
