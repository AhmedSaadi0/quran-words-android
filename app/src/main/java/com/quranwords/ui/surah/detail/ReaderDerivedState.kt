package com.quranwords.ui.surah.detail

import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.statusBars
import androidx.compose.foundation.lazy.LazyListState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.unit.Density
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.LocalLayoutDirection
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.LayoutDirection
import androidx.compose.ui.unit.dp
import com.quranwords.core.util.ReadingProgress
import com.quranwords.core.util.adjacentSurahTarget
import com.quranwords.core.util.SurahMetadata
import com.quranwords.ui.surah.detail.components.AyahFlowGroup
import com.quranwords.ui.surah.detail.components.ReadingChromePillHeight
import com.quranwords.ui.surah.detail.components.SurahReadingScrollConnection
import com.quranwords.ui.surah.detail.components.currentPageFor
import com.quranwords.ui.surah.detail.components.readingProgressOf
import com.quranwords.ui.surah.detail.components.readingProgressOfMushafPage
import com.quranwords.ui.surah.detail.components.rememberSurahReadingScroll

/** Scroll/progress values derived once for the reader shell (P3-2). */
internal data class ReaderDerivedState(
    val scrollConnection: SurahReadingScrollConnection,
    val isRtlTurn: Boolean,
    val readingProgress: () -> ReadingProgress?,
    val currentPage: () -> Int?,
    val chromeTopPadding: Dp,
    val statusBarTopPx: Int,
    val density: Density
)

@Composable
internal fun rememberReaderDerivedState(
    uiState: SurahDetailUiState,
    surahId: Int,
    isPagesMode: Boolean,
    listState: LazyListState,
    flowGroups: List<AyahFlowGroup>,
    listOffset: Int,
    onNavigateToAdjacentSurah: (surahId: Int, ayah: Int, fromNext: Boolean, initialImmersive: Boolean) -> Unit
): ReaderDerivedState {
    val scrollConnection = rememberSurahReadingScroll(
        canScrollForward = {
            if (isPagesMode) uiState.currentPage < uiState.totalPages else listState.canScrollForward
        },
        canScrollBackward = {
            if (isPagesMode) uiState.currentPage > 1 else listState.canScrollBackward
        },
        readingMode = uiState.readingMode,
        canSwitchDown = !uiState.isSelectionMode && uiState.selectedWord == null && !uiState.isLoading &&
            (if (isPagesMode) {
                uiState.pageAyahs.isNotEmpty() && uiState.activeSurahId in 1..113
            } else {
                uiState.ayat.isNotEmpty() && surahId < SurahMetadata.SURAHS.size
            }),
        canSwitchUp = !uiState.isSelectionMode && uiState.selectedWord == null && !uiState.isLoading &&
            (if (isPagesMode) {
                uiState.pageAyahs.isNotEmpty() && uiState.activeSurahId > 1
            } else {
                uiState.ayat.isNotEmpty() && surahId > 1
            }),
        onSwitch = { forward ->
            val current = if (isPagesMode) uiState.activeSurahId else surahId
            adjacentSurahTarget(current, forward)?.let { target ->
                onNavigateToAdjacentSurah(target.surahId, target.ayah, target.fromNext, uiState.isImmersive)
            }
        }
    )

    val readingProgressState = remember(
        listState, flowGroups, listOffset, isPagesMode,
        uiState.currentPage, uiState.activeSurah, uiState.surahPages, uiState.pageAyahs, uiState.surah
    ) {
        derivedStateOf {
            if (isPagesMode) {
                readingProgressOfMushafPage(
                    uiState.currentPage, uiState.activeSurah, uiState.surahPages, uiState.pageAyahs
                )
            } else {
                readingProgressOf(listState, flowGroups, listOffset, uiState.surah, uiState.surahPages)
            }
        }
    }
    val currentPageState = remember(listState, flowGroups, listOffset, isPagesMode, uiState.currentPage) {
        derivedStateOf {
            if (isPagesMode) uiState.currentPage
            else currentPageFor(flowGroups, listState, hasBasmalah = listOffset > 0)
        }
    }

    val density = LocalDensity.current
    val initialStatusTopPx = WindowInsets.statusBars.getTop(density) ?: 0
    val statusBarTopPx = remember { initialStatusTopPx }
    val chromeTopPadding = with(density) {
        (statusBarTopPx + ReadingChromePillHeight.roundToPx() + 8.dp.roundToPx()).toDp()
    }

    return ReaderDerivedState(
        scrollConnection = scrollConnection,
        isRtlTurn = LocalLayoutDirection.current == LayoutDirection.Rtl,
        readingProgress = { readingProgressState.value },
        currentPage = { currentPageState.value },
        chromeTopPadding = chromeTopPadding,
        statusBarTopPx = statusBarTopPx,
        density = density
    )
}
