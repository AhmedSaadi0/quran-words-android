package com.quranwords.ui.surah.detail

import androidx.compose.foundation.lazy.LazyListState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.MutableState
import androidx.compose.runtime.State
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.runtime.snapshotFlow
import com.quranwords.core.util.AyahRef
import com.quranwords.ui.navigation.SurahEnter
import com.quranwords.ui.surah.detail.components.AyahFlowGroup
import kotlinx.coroutines.flow.distinctUntilChanged

/** Deep-link placement, last-read, pulse and mode-flip effects (P3-2). */
@Composable
internal fun ReaderPlacementEffects(
    uiState: SurahDetailUiState,
    surahId: Int,
    targetAyah: Int,
    enter: SurahEnter,
    animationsEnabled: Boolean,
    isPagesMode: Boolean,
    pagesReady: Boolean,
    listState: LazyListState,
    flowGroups: List<AyahFlowGroup>,
    listOffset: Int,
    blockIndexOfAyah: Map<AyahRef, Int>,
    hasHandledInitialScroll: MutableState<Boolean>,
    isInitialPositionSettled: MutableState<Boolean>,
    entryDidScroll: MutableState<Boolean>,
    pulseDone: MutableState<Boolean>,
    immersiveState: State<Boolean>,
    immersiveInteractionLocked: Boolean,
    onEvent: (SurahDetailEvent) -> Unit,
    onNavigateToAdjacentSurah: (surahId: Int, ayah: Int, fromNext: Boolean, initialImmersive: Boolean) -> Unit
) {
    // A page change / scroll retires the toolbar.
    LaunchedEffect(if (isPagesMode) uiState.currentPage else listState.firstVisibleItemIndex, isPagesMode) {
        onEvent(SurahDetailEvent.SetWordAction(null))
    }

    // Vertical-only auto-chrome. Pages mode is tap-only.
    LaunchedEffect(listState, isPagesMode, immersiveInteractionLocked) {
        if (isPagesMode) return@LaunchedEffect
        var lastScrollPx = 0
        snapshotFlow {
            listState.firstVisibleItemIndex * 10000 + listState.firstVisibleItemScrollOffset
        }.distinctUntilChanged().collect { posPx ->
            if (immersiveInteractionLocked) {
                if (immersiveState.value) onEvent(SurahDetailEvent.SetImmersive(false))
                lastScrollPx = posPx
                return@collect
            }
            val delta = posPx - lastScrollPx
            lastScrollPx = posPx
            if (posPx <= 5) {
                if (immersiveState.value) onEvent(SurahDetailEvent.SetImmersive(false))
            } else if (delta > 2) {
                if (!immersiveState.value) onEvent(SurahDetailEvent.SetImmersive(true))
            } else if (delta < -2) {
                if (immersiveState.value) onEvent(SurahDetailEvent.SetImmersive(false))
            }
        }
    }

    // Load once per route. The VM preloads the page window before emitting.
    LaunchedEffect(surahId) {
        if (uiState.surah?.id != surahId || uiState.ayat.isEmpty()) {
            onEvent(SurahDetailEvent.SetWordAction(null))
            hasHandledInitialScroll.value = false
            isInitialPositionSettled.value = targetAyah <= 1
            onEvent(
                SurahDetailEvent.Load(
                    surahId = surahId,
                    targetAyah = targetAyah,
                    immediate = enter != SurahEnter.Normal || targetAyah > 1
                )
            )
        }
    }

    LaunchedEffect(pagesReady) {
        if (pagesReady) {
            hasHandledInitialScroll.value = true
            isInitialPositionSettled.value = true
        }
    }

    // Vertical deep-link placement (instant, silent).
    LaunchedEffect(uiState.ayat, uiState.surah, hasHandledInitialScroll.value) {
        if (isPagesMode || hasHandledInitialScroll.value) return@LaunchedEffect
        val currentSurah = uiState.surah
        if (uiState.ayat.isEmpty() || currentSurah == null) return@LaunchedEffect
        val resolvedIndex = blockIndexOfAyah[AyahRef(surahId, targetAyah)]
        val scrollIndex = resolvedIndex ?: (flowGroups.size - 1 + listOffset).coerceAtLeast(0)
        if (resolvedIndex != null) {
            entryDidScroll.value = scrollIndex != listState.firstVisibleItemIndex ||
                listState.firstVisibleItemScrollOffset != 0
        }
        listState.requestScrollToItem(scrollIndex)
        isInitialPositionSettled.value = true
        hasHandledInitialScroll.value = true
        if (targetAyah in 1..currentSurah.ayahCount) {
            onEvent(SurahDetailEvent.AyahVisible(AyahRef(surahId, targetAyah)))
        }
    }

    // One-shot pulse: vertical deep link or a Pages-mode jump beyond the surah's first page.
    LaunchedEffect(hasHandledInitialScroll.value, uiState.ayat, pagesReady, uiState.entryPage) {
        if (!hasHandledInitialScroll.value || pulseDone.value || enter != SurahEnter.Normal || !animationsEnabled) return@LaunchedEffect
        val ref = AyahRef(surahId, targetAyah)
        val shouldPulse = if (isPagesMode) {
            pagesReady && uiState.pageAyahs.any { it.surah == ref.surahId && it.ayah == ref.ayah } &&
                uiState.entryPage > (uiState.surahPages.firstOrNull() ?: 1)
        } else {
            uiState.ayat.any { it.ayah == targetAyah } && entryDidScroll.value
        }
        pulseDone.value = true
        if (shouldPulse) onEvent(SurahDetailEvent.SetPulseAyah(ref))
    }

    // Last-read tracking, keyed by surah + ayah on both modes.
    LaunchedEffect(listState, isPagesMode, uiState.ayat, uiState.currentPage, hasHandledInitialScroll.value) {
        if (isPagesMode) {
            snapshotFlow { uiState.currentPage to uiState.pageAyahs }
                .distinctUntilChanged()
                .collect { (page, ayat) ->
                    if (!hasHandledInitialScroll.value || page < 1) return@collect
                    val active = ayat.firstOrNull { it.surah == uiState.activeSurahId }
                        ?: ayat.firstOrNull() ?: return@collect
                    onEvent(SurahDetailEvent.AyahVisible(AyahRef(active.surah, active.ayah)))
                }
        } else {
            snapshotFlow { listState.firstVisibleItemIndex }.distinctUntilChanged().collect { firstIdx ->
                if (!hasHandledInitialScroll.value) return@collect
                val group = flowGroups.getOrNull(firstIdx - listOffset) ?: return@collect
                val ayah = group.ayat.firstOrNull() ?: return@collect
                onEvent(SurahDetailEvent.AyahVisible(AyahRef(ayah.surah, ayah.ayah)))
            }
        }
    }

    // Page chip jump in scroll mode; the pager handles its own in Pages mode.
    LaunchedEffect(uiState.ayat, uiState.pendingPage, isPagesMode, flowGroups) {
        if (isPagesMode) return@LaunchedEffect
        val page = uiState.pendingPage ?: return@LaunchedEffect
        val index = flowGroups.indexOfFirst { it.pageNumber == page }
        if (index >= 0) {
            listState.animateScrollToItem(index + listOffset)
            onEvent(SurahDetailEvent.SetPendingPage(null))
        }
    }

    var previousReadingMode by rememberSaveable { mutableStateOf<Int?>(null) }
    LaunchedEffect(uiState.readingMode) {
        onEvent(SurahDetailEvent.SetWordAction(null))
        val previous = previousReadingMode
        previousReadingMode = uiState.readingMode
        if (previous == null || previous == uiState.readingMode || !hasHandledInitialScroll.value) return@LaunchedEffect
        if (isPagesMode) {
            val groupIndex = (listState.firstVisibleItemIndex - listOffset).coerceIn(0, flowGroups.size - 1)
            val page = flowGroups.getOrNull(groupIndex)?.pageNumber
            if (page != null) onEvent(SurahDetailEvent.SetEntryPage(page))
        } else if (uiState.activeSurahId != surahId) {
            val ayah = uiState.pageAyahs.firstOrNull { it.surah == uiState.activeSurahId }?.ayah ?: 1
            onNavigateToAdjacentSurah(
                uiState.activeSurahId, ayah, uiState.activeSurahId > surahId, uiState.isImmersive
            )
        }
    }
}
