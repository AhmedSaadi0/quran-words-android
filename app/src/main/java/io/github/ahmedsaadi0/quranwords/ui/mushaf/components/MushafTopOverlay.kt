package io.github.ahmedsaadi0.quranwords.ui.mushaf.components

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import io.github.ahmedsaadi0.quranwords.R
import io.github.ahmedsaadi0.quranwords.core.util.MushafConstants
import io.github.ahmedsaadi0.quranwords.ui.theme.AppMotion
import io.github.ahmedsaadi0.quranwords.ui.theme.MushafTokens
import io.github.ahmedsaadi0.quranwords.ui.theme.rememberMushafPalette

/**
 * Floating reader chrome: a glass-paper bar (translucent, shadowless, closed
 * by a hairline) that slides away so the 15-line grid owns the viewport.
 * Toggled by background taps on the page; auto-hide is owned by the screen.
 */
@Composable
fun MushafTopOverlay(
    visible: Boolean,
    currentPage: Int,
    onNavigateBack: () -> Unit,
    modifier: Modifier = Modifier
) {
    val palette = rememberMushafPalette()
    AnimatedVisibility(
        visible = visible,
        modifier = modifier.testTag("mushaf_topbar"),
        enter = slideInVertically(
            initialOffsetY = { -it },
            animationSpec = tween(AppMotion.DurationMedium, easing = AppMotion.EasingStandard)
        ) + fadeIn(animationSpec = tween(AppMotion.DurationMedium)),
        exit = slideOutVertically(
            targetOffsetY = { -it },
            animationSpec = tween(AppMotion.DurationMedium, easing = AppMotion.EasingStandard)
        ) + fadeOut(animationSpec = tween(AppMotion.DurationShort))
    ) {
        Column {
            Surface(
                color = palette.paper.copy(alpha = MushafTokens.CHROME_GLASS_ALPHA),
                contentColor = palette.ink,
                shadowElevation = 0.dp
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .statusBarsPadding()
                        .height(64.dp)
                        .padding(horizontal = 4.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    IconButton(
                        onClick = onNavigateBack,
                        modifier = Modifier.testTag("mushaf_back")
                    ) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                            contentDescription = stringResource(R.string.cd_back),
                            tint = palette.ink
                        )
                    }
                    Text(
                        text = stringResource(R.string.mushaf_preview_title),
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.SemiBold,
                        color = palette.ink,
                        modifier = Modifier
                            .weight(1f)
                            .testTag("mushaf_title")
                    )
                    Text(
                        text = stringResource(
                            R.string.mushaf_page_label,
                            MushafConstants.toEasternArabic(currentPage)
                        ),
                        style = MaterialTheme.typography.titleMedium,
                        color = palette.inkFaded,
                        modifier = Modifier
                            .padding(end = 12.dp)
                            .testTag("mushaf_page_label")
                    )
                }
            }
            HorizontalDivider(color = palette.divider, thickness = 1.dp)
        }
    }
}
