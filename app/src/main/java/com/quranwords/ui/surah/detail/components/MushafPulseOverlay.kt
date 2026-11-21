package com.quranwords.ui.surah.detail.components

import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.clipRect
import androidx.compose.ui.graphics.drawscope.translate
import androidx.compose.ui.unit.dp

/**
 * Draws the GPU-animated "here you are" bands behind static mushaf text.
 * Caller owns the alpha animation; this function never changes the text.
 */
internal fun DrawScope.drawMushafPulseOverlay(
    geometry: PulseGeometry?,
    alpha: Float,
    color: Color,
    justification: LastLineJustification?
) {
    val geo = geometry ?: return
    if (geo.isEmpty || alpha <= 0.01f) return
    val bandColor = color.copy(alpha = alpha)
    val radius = CornerRadius(6.dp.toPx())
    val expandX = 3.dp.toPx()

    fun drawBand(rect: Rect) = drawRoundRect(
        color = bandColor,
        topLeft = Offset(rect.left - expandX, rect.top),
        size = Size(rect.width + expandX * 2f, rect.height),
        cornerRadius = radius
    )

    if (justification != null) {
        clipRect(top = 0f, bottom = justification.lineTop) {
            geo.above.forEach(::drawBand)
        }
        geo.lastLine.forEach(::drawBand)
    } else if (geo.centered) {
        clipRect(top = 0f, bottom = geo.lastLineTop) {
            geo.above.forEach(::drawBand)
        }
        clipRect(top = geo.lastLineTop, bottom = size.height) {
            translate(left = -geo.emptySpace / 2f) {
                geo.lastLine.forEach(::drawBand)
            }
        }
    } else {
        geo.above.forEach(::drawBand)
        geo.lastLine.forEach(::drawBand)
    }
}
