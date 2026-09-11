package io.github.ahmedsaadi0.quranwords.ui.mushaf.components

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.navigationBars
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Slider
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import io.github.ahmedsaadi0.quranwords.R
import io.github.ahmedsaadi0.quranwords.core.util.toEasternArabicDigits
import kotlin.math.roundToInt

/**
 * Reader bottom bar: discrete page slider with live preview badge.
 * D4: the pager jumps only on release ([onJumpToPage]); while dragging, the
 * badge previews the candidate page ("صفحة ٤٥ / ٦٠٤") with no data loading.
 */
@Composable
fun MushafBottomBar(
    currentPage: Int,
    totalPages: Int,
    onJumpToPage: (Int) -> Unit,
    modifier: Modifier = Modifier
) {
    var dragPage by remember { mutableIntStateOf(0) }
    val shownPage = if (dragPage > 0) dragPage else currentPage

    Column(
        modifier = modifier
            .fillMaxWidth()
            .background(
                Brush.verticalGradient(
                    colors = listOf(
                        Color.Transparent,
                        Color.Black.copy(alpha = 0.55f)
                    )
                )
            )
            .windowInsetsPadding(WindowInsets.navigationBars)
            .padding(horizontal = 16.dp, vertical = 8.dp)
            .testTag("mushaf_bottombar"),
        verticalArrangement = Arrangement.spacedBy(2.dp)
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.Center,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = stringResource(
                    R.string.mushaf_page_counter,
                    shownPage.toEasternArabicDigits(),
                    totalPages.toEasternArabicDigits()
                ),
                style = MaterialTheme.typography.labelLarge,
                fontWeight = FontWeight.Bold,
                color = Color.White,
                modifier = Modifier.testTag("mushaf_page_label")
            )
        }
        Slider(
            value = shownPage.toFloat(),
            onValueChange = { dragPage = it.roundToInt().coerceIn(1, totalPages) },
            onValueChangeFinished = {
                val target = dragPage
                dragPage = 0
                if (target in 1..totalPages && target != currentPage) {
                    onJumpToPage(target)
                }
            },
            valueRange = 1f..totalPages.toFloat(),
            steps = (totalPages - 2).coerceAtLeast(0),
            modifier = Modifier
                .fillMaxWidth()
                .testTag("mushaf_slider")
        )
    }
}
