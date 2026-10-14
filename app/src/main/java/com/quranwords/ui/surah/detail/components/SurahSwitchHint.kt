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
import androidx.compose.material.icons.filled.KeyboardArrowUp
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import com.quranwords.R
import com.quranwords.core.util.SurahMetadata
import com.quranwords.ui.theme.AppMotion

/**
 * Both edge badges for the surah screen: `direction` -1 shows the next-surah
 * badge at the bottom edge, +1 the previous-surah badge at the top edge, 0
 * hides both. Neighbor names come from static Arabic reference data.
 */
@Composable
fun BoxScope.SurahSwitchHints(direction: Int, surahId: Int) {
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

/**
 * Edge badge shown while the user pulls at a surah boundary: the neighbor
 * surah name fades in at the edge the content is moving toward (bottom badge =
 * next surah, top badge = previous surah). Display-only overlay — it never
 * intercepts touches.
 */
@Composable
fun SurahSwitchHint(
    visible: Boolean,
    isNext: Boolean,
    name: String?,
    modifier: Modifier = Modifier
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
                    // Up/Down keyboard arrows have no AutoMirrored variant —
                    // vertical direction does not mirror under RTL.
                    imageVector = if (isNext) {
                        Icons.Filled.KeyboardArrowDown
                    } else {
                        Icons.Filled.KeyboardArrowUp
                    },
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.primary,
                    modifier = Modifier.size(18.dp)
                )
            }
        }
    }
}
