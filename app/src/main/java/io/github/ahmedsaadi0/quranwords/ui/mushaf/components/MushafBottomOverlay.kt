package io.github.ahmedsaadi0.quranwords.ui.mushaf.components

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.key
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import io.github.ahmedsaadi0.quranwords.core.util.MushafConstants
import io.github.ahmedsaadi0.quranwords.ui.mushaf.MUSHAF_DEMO_PAGES
import io.github.ahmedsaadi0.quranwords.ui.theme.AppMotion
import io.github.ahmedsaadi0.quranwords.ui.theme.MushafTokens
import io.github.ahmedsaadi0.quranwords.ui.theme.rememberMushafPalette

/**
 * Floating page chips (preview scope: the 5 bundled pages). Slides away with
 * the top bar; the Phase-4 scrubber replaces this for the full 604 pages.
 */
@Composable
fun MushafBottomOverlay(
    visible: Boolean,
    currentPage: Int,
    onSelectPage: (index: Int) -> Unit,
    modifier: Modifier = Modifier
) {
    val palette = rememberMushafPalette()
    AnimatedVisibility(
        visible = visible,
        modifier = modifier.testTag("mushaf_bottombar"),
        enter = slideInVertically(
            initialOffsetY = { it },
            animationSpec = tween(AppMotion.DurationMedium, easing = AppMotion.EasingStandard)
        ) + fadeIn(animationSpec = tween(AppMotion.DurationMedium)),
        exit = slideOutVertically(
            targetOffsetY = { it },
            animationSpec = tween(AppMotion.DurationMedium, easing = AppMotion.EasingStandard)
        ) + fadeOut(animationSpec = tween(AppMotion.DurationShort))
    ) {
        Column {
            HorizontalDivider(color = palette.divider, thickness = 1.dp)
            Surface(
                color = palette.paper.copy(alpha = MushafTokens.CHROME_GLASS_ALPHA),
                shadowElevation = 0.dp
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .navigationBarsPadding()
                        .padding(vertical = 4.dp)
                        .testTag("mushaf_page_chips"),
                    horizontalArrangement = Arrangement.Center,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    MUSHAF_DEMO_PAGES.forEachIndexed { index, page ->
                        key(page) {
                            TextButton(
                                onClick = { onSelectPage(index) },
                                modifier = Modifier.testTag("mushaf_chip_$page")
                            ) {
                                Text(
                                    text = MushafConstants.toEasternArabic(page),
                                    color = if (page == currentPage) {
                                        palette.gold
                                    } else {
                                        palette.inkFaded
                                    },
                                    fontWeight = if (page == currentPage) {
                                        FontWeight.Bold
                                    } else {
                                        FontWeight.Normal
                                    }
                                )
                            }
                        }
                    }
                }
            }
        }
    }
}
