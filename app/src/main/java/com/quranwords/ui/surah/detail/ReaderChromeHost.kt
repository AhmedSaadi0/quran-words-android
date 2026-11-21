package com.quranwords.ui.surah.detail

import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.unit.Density
import androidx.compose.ui.unit.dp
import com.quranwords.core.util.ReadingProgress
import com.quranwords.ui.surah.detail.components.ReadingChromePillHeight
import com.quranwords.ui.surah.detail.components.ReadingTopBarOverlay
import com.quranwords.ui.surah.detail.components.SelectionTopBar
import com.quranwords.ui.theme.AppMotion

/** Selection bar / reading bar swap isolated from the reader shell (P3-2). */
@Composable
internal fun ReaderChromeHost(
    isSelectionMode: Boolean,
    isPagesMode: Boolean,
    isImmersive: Boolean,
    selectedCount: Int,
    readingProgress: () -> ReadingProgress?,
    pages: List<Int>,
    currentPage: () -> Int?,
    pillTransparency: Float,
    statusBarTopPx: Int,
    density: Density,
    onEvent: (SurahDetailEvent) -> Unit,
    onNavigateBack: () -> Unit
) {
    val barSlidePx = with(density) { 8.dp.roundToPx() }
    AnimatedContent(
        targetState = isSelectionMode,
        transitionSpec = {
            (fadeIn(tween(AppMotion.DurationShort, easing = AppMotion.EasingStandard)) +
                slideInVertically(tween(AppMotion.DurationShort, easing = AppMotion.EasingStandard)) { -barSlidePx }) togetherWith
                (fadeOut(tween(AppMotion.DurationShort, easing = AppMotion.EasingExit)) +
                    slideOutVertically(tween(AppMotion.DurationShort, easing = AppMotion.EasingExit)) { -barSlidePx })
        },
        label = "readingChromeSwap"
    ) { inSelection ->
        if (inSelection) {
            SelectionTopBar(
                selectedCount = selectedCount,
                onDismiss = { onEvent(SurahDetailEvent.ClearSelection) },
                onBookmark = { onEvent(SurahDetailEvent.BookmarkSelection) },
                onCopy = { onEvent(SurahDetailEvent.CopySelection) },
                onShare = { onEvent(SurahDetailEvent.ShareSelection) }
            )
        } else {
            Box(Modifier.fillMaxSize()) {
                if (isPagesMode && !isImmersive) {
                    val scrimHeight = with(density) {
                        (statusBarTopPx + ReadingChromePillHeight.roundToPx() + 24.dp.roundToPx()).toDp()
                    }
                    Box(
                        modifier = Modifier
                            .align(Alignment.TopCenter)
                            .fillMaxWidth()
                            .height(scrimHeight)
                            .background(
                                Brush.verticalGradient(
                                    listOf(
                                        MaterialTheme.colorScheme.background.copy(alpha = 0.96f),
                                        MaterialTheme.colorScheme.background.copy(alpha = 0f)
                                    )
                                )
                            )
                    )
                }
                ReadingTopBarOverlay(
                    showBar = !isImmersive,
                    readingProgress = readingProgress,
                    pages = pages,
                    currentPage = currentPage,
                    pillTransparency = pillTransparency,
                    onBack = onNavigateBack,
                    onOpenFontSize = { onEvent(SurahDetailEvent.SetFontSheetVisible(true)) },
                    onPageSelected = { page -> onEvent(SurahDetailEvent.SetPendingPage(page)) }
                )
            }
        }
    }
}
