package io.github.ahmedsaadi0.quranwords.ui.surah.detail.components

import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.compose.animation.togetherWith
import androidx.compose.animation.core.tween
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.statusBars
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.BookmarkBorder
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.ContentCopy
import androidx.compose.material.icons.filled.Share
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.pluralStringResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import io.github.ahmedsaadi0.quranwords.R
import io.github.ahmedsaadi0.quranwords.ui.theme.AppMotion

/**
 * Contextual selection TopAppBar (multi-ayah bookmark/copy/share). Decision 13:
 * ✓/📋/↗ emoji replaced with vectors; platform execution lives in the Route.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SelectionTopBar(
    selectedCount: Int,
    onDismiss: () -> Unit,
    onBookmark: () -> Unit,
    onCopy: () -> Unit,
    onShare: () -> Unit
) {
    TopAppBar(
        title = {
            // Direction-aware count tick: slides up on increment, down on
            // decrement. Single tiny node, 150ms — negligible cost.
            AnimatedContent(
                targetState = selectedCount,
                transitionSpec = {
                    val down = targetState < initialState
                    val enterSlide = if (down) 1 else -1
                    val exitSlide = if (down) -1 else 1
                    (fadeIn(tween(150, easing = AppMotion.EasingStandard)) +
                        slideInVertically(tween(150, easing = AppMotion.EasingStandard)) { enterSlide * it / 3 }) togetherWith
                        (fadeOut(tween(150, easing = AppMotion.EasingExit)) +
                            slideOutVertically(tween(150, easing = AppMotion.EasingExit)) { exitSlide * it / 3 })
                },
                label = "selectedCountTick"
            ) { count ->
                Text(
                    text = pluralStringResource(R.plurals.selected_ayat_count, count, count),
                    fontWeight = FontWeight.Bold
                )
            }
        },
        navigationIcon = {
            IconButton(
                onClick = onDismiss,
                modifier = Modifier.testTag("dismiss_selection")
            ) {
                Icon(
                    imageVector = Icons.Filled.Close,
                    contentDescription = stringResource(R.string.cd_cancel)
                )
            }
        },
        actions = {
            IconButton(
                onClick = onBookmark,
                modifier = Modifier.testTag("bookmark_selected_btn")
            ) {
                Icon(
                    imageVector = Icons.Filled.BookmarkBorder,
                    contentDescription = stringResource(R.string.cd_bookmark_selection)
                )
            }
            IconButton(
                onClick = onCopy,
                modifier = Modifier.testTag("copy_selected_btn")
            ) {
                Icon(
                    imageVector = Icons.Filled.ContentCopy,
                    contentDescription = stringResource(R.string.cd_copy_selection)
                )
            }
            IconButton(
                onClick = onShare,
                modifier = Modifier.testTag("share_selected_btn")
            ) {
                Icon(
                    imageVector = Icons.Filled.Share,
                    contentDescription = stringResource(R.string.share_ayat)
                )
            }
        },
        colors = TopAppBarDefaults.topAppBarColors(
            containerColor = MaterialTheme.colorScheme.primaryContainer,
            titleContentColor = MaterialTheme.colorScheme.onPrimaryContainer
        ),
        modifier = Modifier
            .fillMaxWidth()
            .windowInsetsPadding(WindowInsets.statusBars)
    )
}