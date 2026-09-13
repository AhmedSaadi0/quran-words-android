package io.github.ahmedsaadi0.quranwords.ui.mushaf.components

import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.key
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.dp
import io.github.ahmedsaadi0.quranwords.R
import io.github.ahmedsaadi0.quranwords.core.util.MushafConstants
import io.github.ahmedsaadi0.quranwords.ui.theme.rememberMushafPalette

private const val SHIMMER_MIN = 0.35f
private const val SHIMMER_MAX = 0.70f
private const val SHIMMER_MS = 900

/** Every 5th bar renders short, mimicking a ragged short line. */
private const val SHORT_BAR_EVERY = 5
private const val SHORT_BAR_FRACTION = 0.55f
private const val FULL_BAR_FRACTION = 0.92f

/**
 * Loading twin of the real page: the same [MushafPageFrame] geometry with the
 * same 15 line slots, filled with shimmer bars instead of a centered spinner.
 * Layout never jumps when content arrives — the oldest polish trick in books.
 *
 * Root keeps the legacy `mushaf_loading_{page}` test tag.
 */
@Composable
fun MushafPageSkeleton(
    pageNumber: Int,
    footerLabel: String,
    modifier: Modifier = Modifier
) {
    val palette = rememberMushafPalette()
    val shimmer by rememberInfiniteTransition(label = "mushaf_shimmer").animateFloat(
        initialValue = SHIMMER_MIN,
        targetValue = SHIMMER_MAX,
        animationSpec = infiniteRepeatable(
            animation = tween(SHIMMER_MS),
            repeatMode = RepeatMode.Reverse
        ),
        label = "mushaf_shimmer_alpha"
    )
    val loadingDesc = stringResource(
        R.string.mushaf_loading,
        MushafConstants.toEasternArabic(pageNumber)
    )
    // Dedicated wrapper node: the legacy `mushaf_loading_{page}` tag lives
    // here, the frame keeps its own tag — both stay queryable.
    Box(
        modifier = modifier
            .fillMaxSize()
            .testTag("mushaf_loading_$pageNumber")
            .semantics { contentDescription = loadingDesc }
    ) {
        MushafPageFrame(
            pageNumber = pageNumber,
            footerLabel = footerLabel
        ) {
            BoxWithConstraints(modifier = Modifier.fillMaxSize()) {
                val slotHeight = maxHeight / MushafConstants.LINES_PER_PAGE
                Column(modifier = Modifier.fillMaxSize()) {
                    for (slot in MushafConstants.MIN_LINE..MushafConstants.MAX_LINE) {
                        key(slot) {
                            Box(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .height(slotHeight)
                                    .padding(horizontal = 4.dp, vertical = 6.dp),
                                contentAlignment = Alignment.Center
                            ) {
                                Box(
                                    modifier = Modifier
                                        .fillMaxWidth(
                                            fraction = if (slot % SHORT_BAR_EVERY == 0) {
                                                SHORT_BAR_FRACTION
                                            } else {
                                                FULL_BAR_FRACTION
                                            }
                                        )
                                        .height(16.dp)
                                        .alpha(shimmer)
                                        .clip(RoundedCornerShape(6.dp))
                                        .background(palette.inkFaded.copy(alpha = 0.30f))
                                )
                            }
                        }
                    }
                }
            }
        }
    }
}
