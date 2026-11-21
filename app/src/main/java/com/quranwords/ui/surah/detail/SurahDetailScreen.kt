package com.quranwords.ui.surah.detail

import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyListState
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.input.nestedscroll.nestedScroll
import com.quranwords.core.util.AyahRef
import com.quranwords.core.util.ReadingModePreference
import com.quranwords.ui.navigation.SurahEnter
import com.quranwords.ui.surah.detail.components.MushafFitCache
import com.quranwords.ui.surah.detail.components.buildBlockIndexOfAyah
import com.quranwords.ui.surah.detail.components.groupAyatByPage

/**
 * Reader shell. Scroll mode is surah-scoped; Pages mode is a unified 1..N
 * mushaf pager owned by the VM's lazy page window. Selection and bookmarks
 * are keyed by [AyahRef] so shared pages never collide.
 */
@OptIn(ExperimentalMaterial3Api::class, ExperimentalFoundationApi::class)
@Composable
fun SurahDetailScreen(
    uiState: SurahDetailUiState,
    surahId: Int,
    targetAyah: Int,
    enter: SurahEnter,
    animationsEnabled: Boolean,
    onEvent: (SurahDetailEvent) -> Unit,
    onNavigateBack: () -> Unit,
    onNavigateToAdjacentSurah: (surahId: Int, ayah: Int, fromNext: Boolean, initialImmersive: Boolean) -> Unit,
    onNavigateToRootDetail: (Int) -> Unit,
    snackbarHostState: SnackbarHostState
) {
    val listState = rememberSaveable(surahId, saver = LazyListState.Saver) { LazyListState() }
    val hasBasmalah = surahId != 9 && surahId != 1
    val listOffset = 1
    val flowGroups = remember(uiState.ayat) { groupAyatByPage(uiState.ayat) }
    val blockIndexOfAyah = remember(flowGroups, listOffset) { buildBlockIndexOfAyah(flowGroups, listOffset) }

    val isPagesMode = uiState.readingMode == ReadingModePreference.PAGES
    // Never unmount the pager because the LRU evicted the entry page.
    val pagesReady = isPagesMode && uiState.isPagesReady &&
        uiState.totalPages > 0 && uiState.entryPage in 1..uiState.totalPages

    val hasHandledInitialScroll = rememberSaveable(surahId, targetAyah) { mutableStateOf(false) }
    val isInitialPositionSettled = rememberSaveable(surahId, targetAyah) { mutableStateOf(targetAyah <= 1) }
    val entryDidScroll = rememberSaveable(surahId, targetAyah) { mutableStateOf(false) }
    val pulseDone = rememberSaveable(surahId, targetAyah) { mutableStateOf(false) }
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
    val fitCache = remember { MushafFitCache() }
    val bookmarkedRefs = remember(uiState.bookmarkedAyat) {
        uiState.bookmarkedAyat.mapNotNull { AyahRef.parse(it) }.toSet()
    }

    val derived = rememberReaderDerivedState(
        uiState, surahId, isPagesMode, listState, flowGroups, listOffset, onNavigateToAdjacentSurah
    )
    val immersiveState = rememberUpdatedState(uiState.isImmersive)
    val wordInteraction = rememberReaderWordInteraction(
        onEvent, uiState.isImmersive, uiState.wordAction != null
    )
    ReaderBackHandlers(uiState, onEvent)

    ReaderPlacementEffects(
        uiState = uiState,
        surahId = surahId,
        targetAyah = targetAyah,
        enter = enter,
        animationsEnabled = animationsEnabled,
        isPagesMode = isPagesMode,
        pagesReady = pagesReady,
        listState = listState,
        flowGroups = flowGroups,
        listOffset = listOffset,
        blockIndexOfAyah = blockIndexOfAyah,
        hasHandledInitialScroll = hasHandledInitialScroll,
        isInitialPositionSettled = isInitialPositionSettled,
        entryDidScroll = entryDidScroll,
        pulseDone = pulseDone,
        immersiveState = immersiveState,
        immersiveInteractionLocked = uiState.isSelectionMode || uiState.selectedWord != null,
        onEvent = onEvent,
        onNavigateToAdjacentSurah = onNavigateToAdjacentSurah
    )

    val awaitingPlacement = !isPagesMode && targetAyah > 1 && !isInitialPositionSettled.value &&
        uiState.ayat.isNotEmpty() && blockIndexOfAyah[AyahRef(surahId, targetAyah)] != null

    Scaffold(
        contentWindowInsets = WindowInsets(0, 0, 0, 0),
        containerColor = MaterialTheme.colorScheme.background,
        snackbarHost = { SnackbarHost(snackbarHostState) }
    ) { innerPadding ->
        Box(
            modifier = Modifier.fillMaxSize().padding(innerPadding)
                .nestedScroll(derived.scrollConnection)
        ) {
            Box(
                modifier = Modifier.fillMaxSize().graphicsLayer {
                    if (isPagesMode) {
                        translationX = if (derived.isRtlTurn) {
                            -derived.scrollConnection.pullPx
                        } else {
                            derived.scrollConnection.pullPx
                        }
                    } else {
                        translationY = derived.scrollConnection.pullPx
                    }
                }
            ) {
                ReaderContentSwitch(
                    uiState = uiState,
                    surahId = surahId,
                    targetAyah = targetAyah,
                    isPagesMode = isPagesMode,
                    pagesReady = pagesReady,
                    awaitingPlacement = awaitingPlacement,
                    bookmarkedRefs = bookmarkedRefs,
                    fitCache = fitCache,
                    listState = listState,
                    chromeTopPadding = derived.chromeTopPadding,
                    hasBasmalah = hasBasmalah,
                    wordInteraction = wordInteraction,
                    activeWordAction = uiState.wordAction,
                    hintDirection = derived.scrollConnection.hintDirection,
                    activeSurahId = if (isPagesMode) uiState.activeSurahId else surahId,
                    onEvent = onEvent
                )
            }

            ReaderChromeHost(
                isSelectionMode = uiState.isSelectionMode,
                isPagesMode = isPagesMode,
                isImmersive = uiState.isImmersive,
                selectedCount = uiState.selectedAyahs.size,
                readingProgress = derived.readingProgress,
                pages = uiState.surahPages,
                currentPage = derived.currentPage,
                pillTransparency = uiState.pillTransparency,
                statusBarTopPx = derived.statusBarTopPx,
                density = derived.density,
                onEvent = onEvent,
                onNavigateBack = onNavigateBack
            )
        }

        ReaderSheets(uiState, sheetState, onEvent, onNavigateToRootDetail)
    }
}
