package com.quranwords.ui.surah.components

import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Box
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Outline
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.Density
import androidx.compose.ui.unit.LayoutDirection
import androidx.compose.ui.unit.dp
import androidx.compose.ui.geometry.Size
import kotlin.math.cos
import kotlin.math.min
import kotlin.math.sin

/**
 * Ratio between the inner vertices and the outer ones of a khatam (Islamic
 * eight-pointed star): the outline of two squares rotated 45° on top of each
 * other. The outer vertices sit at 45° steps (one straight up); the inner
 * ones fall between them at 22.5°, where a square's edge (x + y = R) crosses
 * that ray: r = R / (cos22.5° + sin22.5°) = 0.7654·R.
 */
private const val STAR_INNER_RATIO = 0.7654f

/**
 * The khatam octagram used by the index's number medallions — 8 outer points
 * with a point straight up, exactly like the quran.com Quran app badge.
 */
class OctagramShape : Shape {
    override fun createOutline(
        size: Size,
        layoutDirection: LayoutDirection,
        density: Density
    ): Outline {
        val radius = min(size.width, size.height) / 2f
        val cx = size.width / 2f
        val cy = size.height / 2f
        val step = Math.PI / 4
        val path = Path()
        for (i in 0 until 8) {
            val outerAngle = -Math.PI / 2 + i * step
            val innerAngle = outerAngle + step / 2
            val outerX = cx + radius * cos(outerAngle).toFloat()
            val outerY = cy + radius * sin(outerAngle).toFloat()
            val innerX = cx + radius * STAR_INNER_RATIO * cos(innerAngle).toFloat()
            val innerY = cy + radius * STAR_INNER_RATIO * sin(innerAngle).toFloat()
            if (i == 0) path.moveTo(outerX, outerY) else path.lineTo(outerX, outerY)
            path.lineTo(innerX, innerY)
        }
        path.close()
        return Outline.Generic(path)
    }
}

/**
 * Index medallion: a thin octagram outline holding a localized number —
 * the flat quran.com-style row badge replacing the filled circles with
 * borders. [contentColor] drives both the outline and the digits.
 */
@Composable
fun OctagramNumberBadge(
    number: String,
    modifier: Modifier = Modifier,
    contentColor: Color = MaterialTheme.colorScheme.primary
) {
    val shape = remember { OctagramShape() }
    Box(
        modifier = modifier.border(1.dp, contentColor, shape),
        contentAlignment = Alignment.Center
    ) {
        Text(
            text = number,
            style = MaterialTheme.typography.titleSmall,
            fontWeight = FontWeight.SemiBold,
            color = contentColor
        )
    }
}

/** Convenience size for index medallions (kept in one place). */
val OctagramBadgeSize = 40.dp
