package com.quranwords.ui.surah.detail.components

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.BoxScope
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.KeyboardArrowDown
import androidx.compose.material.icons.filled.KeyboardArrowLeft
import androidx.compose.material.icons.filled.KeyboardArrowRight
import androidx.compose.material.icons.filled.KeyboardArrowUp
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalLayoutDirection
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.LayoutDirection
import androidx.compose.ui.unit.dp
import com.quranwords.R
import com.quranwords.core.util.SurahMetadata
import com.quranwords.ui.theme.AppMotion

/**
 * Both edge badges for the surah screen: `direction` -1 shows the next-surah
 * badge, +1 the previous-surah badge, 0 hides both. Placement follows the
 * ACTIVE page-turn axis:
 *
 *  - vertical: next at the bottom edge (arrow down), previous at the top edge
 *    (arrow up) — up/down arrows never mirror under RTL.
 *  - horizontal: the badge sits at the physical edge the content moves
 *    toward (RTL mushaf: right = next, left = previous; mirrored for LTR),
 *    with a matching left/right arrow.
 *
 * Neighbor names come from static Arabic reference data.
 */
@Composable
fun BoxScope.SurahSwitchHints(direction: Int, surahId: Int, horizontal: Boolean = false) {
    val rtl = LocalLayoutDirection.current == LayoutDirection.Rtl
    if (horizontal) {
        SurahSwitchHint(
            visible = direction == -1,
            isNext = true,
            horizontal = true,
            rtl = rtl,
            name = SurahMetadata.SURAHS.getOrNull(surahId)?.nameAr,
            modifier = Modifier.align(if (rtl) Alignment.CenterStart else Alignment.CenterEnd)
        )
        SurahSwitchHint(
            visible = direction == 1,
            isNext = false,
            horizontal = true,
            rtl = rtl,
            name = SurahMetadata.SURAHS.getOrNull(surahId - 2)?.nameAr,
            modifier = Modifier.align(if (rtl) Alignment.CenterEnd else Alignment.CenterStart)
        )
    } else {
        SurahSwitchHint(
            visible = direction == -1,
            isNext = true,
            name = SurahMetadata.SURAHS.getOrNull(surahId)?.nameAr,
            modifier = Modifier.align(Alignment.BottomCenter)
        )
        SurahSwitchHint(
            visible = direction == 1,
            isNext = false,
            name = SurahMetadata.SURAHS.getOrNull(surahId - 2)?.nameAr,
            modifier = Modifier.align(Alignment.TopCenter)
        )
    }
}

/**
 * Edge badge shown while the user pulls at a surah boundary: the neighbor
 * surah name fades in near the boundary being crossed. Display-only overlay —
 * it never intercepts touches.
 */
@Composable
fun SurahSwitchHint(
    visible: Boolean,
    isNext: Boolean,
    name: String?,
    modifier: Modifier = Modifier,
    horizontal: Boolean = false,
    rtl: Boolean = false
) {
    AnimatedVisibility(
        visible = visible && name != null,
        enter = fadeIn(tween(AppMotion.DurationShort)),
        exit = fadeOut(tween(AppMotion.DurationShort)),
        modifier = modifier
    ) {
        Surface(
            shape = MaterialTheme.shapes.medium,
            color = MaterialTheme.colorScheme.surfaceVariant,
            tonalElevation = 2.dp,
            modifier = Modifier.testTag(if (isNext) "switch_hint_next" else "switch_hint_prev")
        ) {
            Row(
                modifier = Modifier.padding(horizontal = 14.dp, vertical = 8.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(6.dp)
            ) {
                Text(
                    text = stringResource(
                        if (isNext) R.string.surah_switch_next_hint else R.string.surah_switch_prev_hint,
                        name.orEmpty()
                    ),
                    style = MaterialTheme.typography.labelMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                Icon(
                    // Directional arrows have no AutoMirrored variant — the
                    // physical direction is chosen explicitly below.
                    imageVector = when {
                        !horizontal -> if (isNext) {
                            Icons.Filled.KeyboardArrowDown
                        } else {
                            Icons.Filled.KeyboardArrowUp
                        }
                        rtl -> if (isNext) {
                            Icons.Filled.KeyboardArrowRight
                        } else {
                            Icons.Filled.KeyboardArrowLeft
                        }
                        else -> if (isNext) {
                            Icons.Filled.KeyboardArrowLeft
                        } else {
                            Icons.Filled.KeyboardArrowRight
                        }
                    },
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.primary,
                    modifier = Modifier.size(18.dp)
                )
            }
        }
    }
}
