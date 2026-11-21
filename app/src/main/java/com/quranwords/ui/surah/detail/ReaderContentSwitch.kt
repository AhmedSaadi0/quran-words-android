package com.quranwords.ui.surah.detail

import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.layout.BoxScope
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyListState
import androidx.compose.foundation.LocalOverscrollConfiguration
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.quranwords.core.util.AyahRef
import com.quranwords.ui.surah.detail.components.MushafFitCache
import com.quranwords.ui.surah.detail.components.SurahAyatList
import com.quranwords.ui.surah.detail.components.SurahDbMissingState
import com.quranwords.ui.surah.detail.components.SurahPagesPager
import com.quranwords.ui.surah.detail.components.SurahSwitchHints
import com.quranwords.ui.surah.detail.components.WordActionTarget
import com.quranwords.ui.surah.detail.components.WordInteractionCallbacks

/** Pager/list content selection isolated from the reader shell (P3-2). */
@OptIn(ExperimentalFoundationApi::class)
@Composable
internal fun BoxScope.ReaderContentSwitch(
    uiState: SurahDetailUiState,
    surahId: Int,
    targetAyah: Int,
    isPagesMode: Boolean,
    pagesReady: Boolean,
    awaitingPlacement: Boolean,
    bookmarkedRefs: Set<AyahRef>,
    fitCache: MushafFitCache,
    listState: LazyListState,
    chromeTopPadding: Dp,
    hasBasmalah: Boolean,
    wordInteraction: WordInteractionCallbacks,
    activeWordAction: WordActionTarget?,
    hintDirection: Int,
    activeSurahId: Int,
    onEvent: (SurahDetailEvent) -> Unit
) {
    val retry = { onEvent(SurahDetailEvent.Load(surahId, targetAyah, immediate = true)) }
    if (isPagesMode) {
        when {
            !pagesReady || uiState.isLoading -> Loading()
            uiState.error != null && uiState.pageAyahs.isEmpty() -> SurahErrorState(uiState.error.orEmpty(), retry)
            uiState.pageAyahs.isEmpty() -> SurahDbMissingState()
            else -> CompositionLocalProvider(LocalOverscrollConfiguration provides null) {
                SurahPagesPager(
                    totalPages = uiState.totalPages,
                    entryPage = uiState.entryPage,
                    pagesByNumber = uiState.pagesByNumber,
                    failedPages = uiState.failedPages,
                    fitCache = fitCache,
                    quranFont = uiState.quranFont,
                    activeSurah = uiState.activeSurah,
                    isSelectionMode = uiState.isSelectionMode,
                    selectedAyahs = uiState.selectedAyahs,
                    bookmarkedAyahs = bookmarkedRefs,
                    pulseAyah = uiState.pulseAyah,
                    onPulseDone = { onEvent(SurahDetailEvent.SetPulseAyah(null)) },
                    wordInteraction = wordInteraction,
                    activeWordAction = activeWordAction,
                    pendingPage = uiState.pendingPage,
                    onPageChanged = { onEvent(SurahDetailEvent.PageChanged(it)) },
                    onRetryPage = { onEvent(SurahDetailEvent.RetryPage(it)) },
                    onToggleSelection = { onEvent(SurahDetailEvent.ToggleAyahSelection(it)) },
                    onEnterSelection = {
                        if (uiState.isSelectionMode) onEvent(SurahDetailEvent.RangeSelect(it))
                        else onEvent(SurahDetailEvent.EnterSelection(it))
                    }
                )
            }
        }
    } else {
        when {
            (uiState.isLoading && uiState.ayat.isEmpty()) || awaitingPlacement -> Loading()
            uiState.error != null && uiState.ayat.isEmpty() -> SurahErrorState(uiState.error.orEmpty(), retry)
            uiState.ayat.isEmpty() -> SurahDbMissingState()
            else -> CompositionLocalProvider(LocalOverscrollConfiguration provides null) {
                SurahAyatList(
                    ayat = uiState.ayat,
                    contentPadding = PaddingValues(start = 16.dp, end = 16.dp, top = chromeTopPadding, bottom = 24.dp),
                    fontSize = uiState.fontSize,
                    quranFont = uiState.quranFont,
                    hasBasmalah = hasBasmalah,
                    surah = uiState.surah,
                    isSelectionMode = uiState.isSelectionMode,
                    selectedAyahs = uiState.selectedAyahs,
                    bookmarkedAyahs = bookmarkedRefs,
                    pulseAyah = uiState.pulseAyah,
                    onPulseDone = { onEvent(SurahDetailEvent.SetPulseAyah(null)) },
                    listState = listState,
                    wordInteraction = wordInteraction,
                    activeWordAction = activeWordAction,
                    onToggleSelection = { onEvent(SurahDetailEvent.ToggleAyahSelection(it)) },
                    onEnterSelection = {
                        if (uiState.isSelectionMode) onEvent(SurahDetailEvent.RangeSelect(it))
                        else onEvent(SurahDetailEvent.EnterSelection(it))
                    }
                )
            }
        }
    }
    SurahSwitchHints(
        direction = hintDirection,
        surahId = activeSurahId,
        horizontal = isPagesMode
    )
    if (uiState.error != null &&
        (if (isPagesMode) uiState.pageAyahs.isNotEmpty() else uiState.ayat.isNotEmpty())
    ) {
        Box(Modifier.align(Alignment.BottomCenter)) {
            SurahErrorBanner(uiState.error.orEmpty(), retry)
        }
    }
}

@Composable
private fun Loading() {
    Box(Modifier.fillMaxSize().padding(24.dp), contentAlignment = Alignment.Center) {
        CircularProgressIndicator(color = MaterialTheme.colorScheme.primary)
    }
}
