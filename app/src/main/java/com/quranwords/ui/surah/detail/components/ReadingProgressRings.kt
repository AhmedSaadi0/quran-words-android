package com.quranwords.ui.surah.detail.components

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.size
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.clearAndSetSemantics
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.quranwords.R
import com.quranwords.core.util.ReadingProgress
import com.quranwords.core.util.toEasternArabicDigits
import kotlin.math.roundToInt

private val RingSize = 36.dp
private val RingStroke = 3.dp

/**
 * Pinned top-bar pair: the mushaf page at the viewport top with its position
 * within the surah (left), and the current juz with how far the reader has
 * advanced inside it (right). Both arcs GROW as the reader advances.
 *
 * [readingProgress] is invoked HERE — inside this node's own composition — so
 * per-frame scroll updates recompose only this row: arcs redraw in the draw
 * phase and the center texts re-lay out only when their displayed number
 * changes (AGENTS §17: no per-frame text reshaping).
 */
@Composable
fun ReadingProgressRings(
    readingProgress: () -> ReadingProgress?,
    modifier: Modifier = Modifier
) {
    val progress = readingProgress() ?: return
    Row(
        modifier = modifier,
        horizontalArrangement = Arrangement.spacedBy(6.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        ProgressRing(
            progress = progress.pageFraction,
            centerText = progress.currentPage.toEasternArabicDigits(),
            contentDescription = stringResource(
                R.string.cd_page_progress,
                progress.currentPage,
                progress.totalPages
            ),
            modifier = Modifier.testTag("page_progress_ring")
        )
        ProgressRing(
            progress = progress.juzFraction,
            centerText = progress.juz.toEasternArabicDigits(),
            contentDescription = stringResource(
                R.string.cd_juz_progress,
                progress.juz,
                (progress.juzFraction * 100).roundToInt()
            ),
            modifier = Modifier.testTag("juz_progress_ring")
        )
    }
}

/**
 * 36dp ring with an arc drawn in the draw phase ([progress] is read by the
 * draw lambda only through the recomposing caller) and a static center label.
 * Semantics are cleared and replaced by a single TalkBack description.
 */
@Composable
private fun ProgressRing(
    progress: Float,
    centerText: String,
    contentDescription: String,
    modifier: Modifier = Modifier
) {
    val trackColor = MaterialTheme.colorScheme.surfaceVariant
    val arcColor = MaterialTheme.colorScheme.primary
    Box(
        modifier = modifier
            .size(RingSize)
            .clearAndSetSemantics { this.contentDescription = contentDescription },
        contentAlignment = Alignment.Center
    ) {
        Canvas(modifier = Modifier.size(RingSize)) {
            val stroke = RingStroke.toPx()
            val inset = stroke / 2f
            val arcSize = Size(size.width - stroke, size.height - stroke)
            drawArc(
                color = trackColor,
                startAngle = 0f,
                sweepAngle = 360f,
                useCenter = false,
                topLeft = Offset(inset, inset),
                size = arcSize,
                style = Stroke(width = stroke, cap = StrokeCap.Round)
            )
            val sweep = 360f * progress.coerceIn(0f, 1f)
            if (sweep > 0f) {
                drawArc(
                    color = arcColor,
                    startAngle = -90f,
                    sweepAngle = sweep,
                    useCenter = false,
                    topLeft = Offset(inset, inset),
                    size = arcSize,
                    style = Stroke(width = stroke, cap = StrokeCap.Round)
                )
            }
        }
        Text(
            text = centerText,
            style = MaterialTheme.typography.labelSmall,
            fontWeight = FontWeight.SemiBold,
            color = MaterialTheme.colorScheme.onSurface
        )
    }
}
