package com.quranwords.ui.surah.detail.components

import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.clipRect
import androidx.compose.ui.graphics.drawscope.translate
import androidx.compose.ui.text.TextLayoutResult
import androidx.compose.ui.text.drawText

/**
 * Last-line rectangle/centered drawing. Text stays static; only the clipped
 * passes move, so per-frame pulses and taps never re-shape Arabic glyphs.
 */
internal fun DrawScope.drawMushafText(
    result: TextLayoutResult?,
    justification: LastLineJustification?,
    drawContent: () -> Unit
) {
    if (result == null || result.lineCount == 0) {
        drawContent()
        return
    }
    val just = justification
    if (just != null) {
        clipRect(top = 0f, bottom = just.lineTop) { drawContent() }
        just.tokens.forEach { token ->
            drawText(token.layout, topLeft = Offset(token.finalLeft, just.lineTop))
        }
        return
    }
    val lastLineIndex = result.lineCount - 1
    val emptySpace = result.lastLineEmptySpace()
    if (emptySpace > LAST_LINE_EPS_PX) {
        val lastLineTop = result.getLineTop(lastLineIndex)
        clipRect(top = 0f, bottom = lastLineTop) { drawContent() }
        clipRect(top = lastLineTop, bottom = size.height) {
            translate(left = -emptySpace / 2f) { drawContent() }
        }
    } else {
        drawContent()
    }
}
