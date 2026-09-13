package io.github.ahmedsaadi0.quranwords.ui.mushaf.components

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.widthIn
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import io.github.ahmedsaadi0.quranwords.ui.theme.MushafTokens
import io.github.ahmedsaadi0.quranwords.ui.theme.ShapeLarge
import io.github.ahmedsaadi0.quranwords.ui.theme.rememberMushafPalette

/**
 * The book itself: a centered paper card (max 520.dp for tablets/foldables).
 *
 * Paper depth is pure gradient, zero bitmaps: a center-weighted 3-stop
 * vertical wash (`paper → paper → paperEdge`) plus an ultra-soft radial
 * vignette (≤ [MushafTokens.VIGNETTE_MAX_ALPHA] at the corners) drawn behind
 * the content — warmth without dirt. A double gold rule (card border @ 18%
 * + inner hairline @ 10%) echoes print margins.
 *
 * Gesture-free by design: taps are owned by line-level handlers
 * ([MushafGlyphLine] forwards background taps), so this frame never competes
 * with the [HorizontalPager][androidx.compose.foundation.pager.HorizontalPager]
 * drag deltas. Footer is inert metadata (scrubber lands in Phase 4).
 */
@Composable
fun MushafPageFrame(
    pageNumber: Int,
    footerLabel: String,
    modifier: Modifier = Modifier,
    content: @Composable ColumnScope.() -> Unit
) {
    val palette = rememberMushafPalette()
    Box(
        modifier = modifier
            .fillMaxSize()
            .background(
                brush = Brush.verticalGradient(
                    colors = listOf(palette.outer, palette.outerScrim)
                )
            )
            .testTag("mushaf_page_frame_$pageNumber"),
        contentAlignment = Alignment.Center
    ) {
        Card(
            modifier = Modifier
                .fillMaxSize()
                .padding(horizontal = 12.dp, vertical = 8.dp)
                .widthIn(max = MushafTokens.PageMaxWidth),
            shape = ShapeLarge,
            colors = CardDefaults.cardColors(containerColor = Color.Transparent),
            elevation = CardDefaults.cardElevation(defaultElevation = MushafTokens.CardElevation),
            border = BorderStroke(1.dp, palette.gold.copy(alpha = 0.18f))
        ) {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .background(
                        brush = Brush.verticalGradient(
                            0.0f to palette.paper,
                            0.55f to palette.paper,
                            1.0f to palette.paperEdge
                        ),
                        shape = ShapeLarge
                    )
                    .drawBehind {
                        drawRect(
                            brush = Brush.radialGradient(
                                listOf(
                                    Color.Transparent,
                                    Color.Transparent,
                                    palette.outer.copy(
                                        alpha = MushafTokens.VIGNETTE_MAX_ALPHA
                                    )
                                ),
                                center = center,
                                radius = size.maxDimension * 0.65f
                            )
                        )
                    }
                    .padding(all = 4.dp)
                    .border(
                        width = 1.dp,
                        color = palette.gold.copy(alpha = 0.10f),
                        shape = ShapeLarge
                    )
                    .padding(
                        horizontal = MushafTokens.PagePaddingH - 4.dp,
                        vertical = MushafTokens.PagePaddingV - 4.dp
                    )
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .weight(1f),
                    content = content
                )
                HorizontalDivider(color = palette.divider)
                Text(
                    text = footerLabel,
                    style = MaterialTheme.typography.labelSmall,
                    color = palette.inkFaded,
                    textAlign = TextAlign.Center,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(top = 6.dp)
                        .testTag("mushaf_page_footer_$pageNumber")
                )
            }
        }
    }
}
