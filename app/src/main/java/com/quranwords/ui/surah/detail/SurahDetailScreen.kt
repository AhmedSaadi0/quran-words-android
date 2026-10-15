package com.quranwords.ui.surah.detail

import android.provider.Settings
import androidx.activity.compose.BackHandler
import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.compose.animation.togetherWith
import androidx.compose.animation.core.tween
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.LocalOverscrollConfiguration
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.runtime.snapshotFlow
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.input.nestedscroll.nestedScroll
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.unit.dp
import kotlinx.coroutines.flow.distinctUntilChanged
import com.quranwords.core.util.SurahMetadata
import com.quranwords.core.util.adjacentSurahTarget
import com.quranwords.ui.components.MorphologyBottomSheet
import com.quranwords.ui.navigation.SurahEnter
import com.quranwords.ui.surah.detail.components.SelectionTopBar
import com.quranwords.ui.surah.detail.components.SurahAyatList
import com.quranwords.ui.surah.detail.components.SurahDbMissingState
import com.quranwords.ui.surah.detail.components.SurahSwitchHints
import com.quranwords.ui.surah.detail.components.buildBlockIndexOfAyah
import com.quranwords.ui.surah.detail.components.groupAyatByPage
import com.quranwords.ui.surah.detail.components.SurahCollapsingHeaderState
import com.quranwords.ui.surah.detail.components.SurahDetailHeader
import com.quranwords.ui.surah.detail.components.currentPageFor
import com.quranwords.ui.surah.detail.components.readingProgressOf
import com.quranwords.ui.surah.detail.components.rememberSurahReadingScroll
import com.quranwords.ui.theme.AppMotion

/** Centred progress placeholder for first load and deep-link placement. */
@Composable
private fun SurahLoadingPlaceholder() {
    Box(
        modifier = Modifier.fillMaxSize(),
        contentAlignment = Alignment.Center
    ) {
        CircularProgressIndicator(color = MaterialTheme.colorScheme.primary)
    }
}

/**
 * Stateless surah detail screen. Collapse state is saveable; pagination,
 * last-read tracking and selection flow through [SurahDetailEvent].
 */
@OptIn(ExperimentalMaterial3Api::class, ExperimentalFoundationApi::class)
@Composable
fun SurahDetailScreen(
    uiState: SurahDetailUiState,
    surahId: Int,
    targetAyah: Int,
    enter: SurahEnter,
    collapseState: SurahCollapsingHeaderState,
    onEvent: (SurahDetailEvent) -> Unit,
    onNavigateBack: () -> Unit,
    onNavigateToAdjacentSurah: (surahId: Int, ayah: Int, fromNext: Boolean) -> Unit,
    onNavigateToRootDetail: (Int) -> Unit,
    snackbarHostState: SnackbarHostState
) {
    val listState = rememberSaveable(surahId, saver = androidx.compose.foundation.lazy.LazyListState.Saver) {
        androidx.compose.foundation.lazy.LazyListState()
    }
    val hasBasmalah = surahId != 9 && surahId != 1
    val listOffset = if (hasBasmalah) 1 else 0

    // Continuous-flow grouping — same pure function as SurahAyatList renders.
    // Lazy indices address blocks now, not ayat: an ayah can start mid-line,
    // so deep-link/last-read granularity is block-level by architecture.
    val flowGroups = remember(uiState.ayat) { groupAyatByPage(uiState.ayat) }
    // Ayah number -> lazy item index of its owning block.
    val blockIndexOfAyah = remember(flowGroups, listOffset) {
        buildBlockIndexOfAyah(flowGroups, listOffset)
    }
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
    var hasHandledInitialScroll by rememberSaveable(surahId, targetAyah) { mutableStateOf(false) }

    /**
     * False until the list has been placed at the deep-link target. While false
     * the ayat list is NOT composed, so index 0 (ayah 1) can never reach the
     * screen for even one frame. Starts settled for ayah-1 entries, which need
     * no placement.
     */
    var isInitialPositionSettled by rememberSaveable(surahId, targetAyah) {
        mutableStateOf(targetAyah <= 1)
    }

    // Deep-link pulse state: did the entry scroll actually move, and did the
    // pulse run already (survives rotation so it never re-fires)?
    var entryDidScroll by rememberSaveable(surahId, targetAyah) { mutableStateOf(false) }
    var pulseDone by rememberSaveable(surahId, targetAyah) { mutableStateOf(false) }
    var pulseAyah by remember { mutableStateOf<Int?>(null) }
    val context = LocalContext.current

    // Pending page-chip click: scroll once the requested page becomes loaded
    // (replaces the legacy delay(100) + VM-state peek hack).
    var pendingPage by remember { mutableStateOf<Int?>(null) }

    // Bookmarked ayah numbers for this surah: keys are "surahId:ayah".
    // Parsed here so the flow block stays a pure renderer of Set<Int>.
    val bookmarkedAyahNums = remember(uiState.bookmarkedAyat, surahId) {
        uiState.bookmarkedAyat.mapNotNull { key ->
            val parts = key.split(":")
            if (parts.size == 2 && parts[0].toIntOrNull() == surahId) {
                parts[1].toIntOrNull()
            } else {
                null
            }
        }.toSet()
    }

    // The screen's single nested-scroll connection: cancelling a live switch
    // pull unwinds before the header or the list may move (see
    // SurahReadingScrollConnection). The whole surah is always loaded, so the
    // only gates left are selection, sheet, list state and Quran boundaries.
    val nestedScrollConnection = rememberSurahReadingScroll(
        listState = listState,
        collapseState = collapseState,
        isSelectionActive = uiState.isSelectionMode,
        canSwitchDown = !uiState.isSelectionMode &&
            uiState.selectedWord == null &&
            uiState.ayat.isNotEmpty() &&
            surahId < SurahMetadata.SURAHS.size,
        canSwitchUp = !uiState.isSelectionMode &&
            uiState.selectedWord == null &&
            uiState.ayat.isNotEmpty() &&
            surahId > 1,
        onSwitch = { forward ->
            adjacentSurahTarget(surahId, forward)?.let { target ->
                onNavigateToAdjacentSurah(target.surahId, target.ayah, target.fromNext)
            }
        }
    )

    // Top-bar rings: derived once, read only inside ReadingProgressRings'
    // composition — per-frame scroll updates never recompose this screen.
    val surah = uiState.surah
    val surahPages = uiState.surahPages
    val readingProgressState = remember(listState, flowGroups, listOffset, surah, surahPages) {
        derivedStateOf { readingProgressOf(listState, flowGroups, listOffset, surah, surahPages) }
    }
    // Page-chip highlight: derived so the screen never subscribes to scroll
    // position — only the header recomposes, and only on page boundaries.
    val currentPageState = remember(listState, flowGroups, hasBasmalah) {
        derivedStateOf { currentPageFor(flowGroups, listState, hasBasmalah) }
    }

    // System back dismisses an active ayah selection first instead of leaving
    // the surah. Disabled while the morphology sheet is open so the sheet
    // consumes the back press (it registers its handler later in this scope).
    BackHandler(enabled = uiState.isSelectionMode && uiState.selectedWord == null) {
        onEvent(SurahDetailEvent.ClearSelection)
    }

    // Load surah only if not already loaded for this surahId. Adjacent switches
    // enter immediately: the vertical slide is already playing, so first-page
    // content must land inside it instead of after the warm-up delay. Deep links
    // also skip it — their content must not wait on the enter transition.
    LaunchedEffect(surahId) {
        if (uiState.surah?.id != surahId || uiState.ayat.isEmpty()) {
            hasHandledInitialScroll = false
            // Both flags re-armed together: a stale `true` here would make the
            // placement effect a no-op for the new surah. Inside this guard, so
            // a rotation with an already-loaded surah never re-hides the list.
            isInitialPositionSettled = targetAyah <= 1
            collapseState.reset()
            onEvent(
                SurahDetailEvent.Load(
                    surahId = surahId,
                    targetAyah = targetAyah,
                    immediate = enter != SurahEnter.Normal || targetAyah > 1
                )
            )
        }
    }

    // Initial placement at the target block. Deep links (targetAyah > 1) must
    // be INSTANT and silent: the whole surah is loaded in one shot, so the
    // target block is always present. animateScrollToItem stays for in-session
    // page-chip jumps only.
    LaunchedEffect(uiState.ayat, uiState.surah, hasHandledInitialScroll) {
        if (hasHandledInitialScroll) return@LaunchedEffect
        val currentSurah = uiState.surah
        if (uiState.ayat.isEmpty() || currentSurah == null) return@LaunchedEffect
        // A null index here means the DB numbering disagrees with the route;
        // settle on the last block instead of leaving the spinner up forever.
        val resolvedIndex = blockIndexOfAyah[targetAyah]
        val scrollIndex = resolvedIndex
            ?: (flowGroups.size - 1 + listOffset).coerceAtLeast(0)
        if (resolvedIndex != null) {
            entryDidScroll = scrollIndex != listState.firstVisibleItemIndex ||
                listState.firstVisibleItemScrollOffset != 0
        }
        // requestScrollToItem (not scrollToItem): the list is still gated
        // behind awaitingPlacement, so it has never laid out — the
        // suspending scrollToItem would wait for first layout forever.
        // The request applies on the next (i.e. first) remeasure instead.
        listState.requestScrollToItem(scrollIndex)
        isInitialPositionSettled = true
        hasHandledInitialScroll = true
        if (targetAyah in 1..currentSurah.ayahCount) {
            onEvent(SurahDetailEvent.AyahVisible(targetAyah))
        }
    }

    // Target-ayah pulse trigger: fires once, only if the entry scroll actually
    // moved the list. The owning MushafFlowBlock animates a GPU overlay itself
    // (no text relayout) and reports back via onPulseDone. Skipped when the
    // target is not loaded yet, and when the user disabled system animations
    // (reduced motion).
    val animatorScale = remember {
        Settings.Global.getFloat(
            context.contentResolver,
            Settings.Global.ANIMATOR_DURATION_SCALE,
            1f
        )
    }
    LaunchedEffect(hasHandledInitialScroll, uiState.ayat) {
        if (!hasHandledInitialScroll || pulseDone) return@LaunchedEffect
        // No deep-link pulse on adjacent-surah entries: the vertical slide
        // itself already marks the arrival.
        if (enter != SurahEnter.Normal) {
            pulseDone = true
            return@LaunchedEffect
        }
        if (uiState.ayat.none { it.ayah == targetAyah }) return@LaunchedEffect
        pulseDone = true
        if (!entryDidScroll || animatorScale == 0f) return@LaunchedEffect
        // The block animates the overlay itself and reports back via onPulseDone.
        pulseAyah = targetAyah
    }

    // Track the first visible block's first ayah for last-read updates.
    // Gated until the initial deep-link scroll completes so opening at
    // targetAyah never emits a spurious AyahVisible(1) first.
    LaunchedEffect(listState, uiState.ayat, hasHandledInitialScroll) {
        snapshotFlow { listState.firstVisibleItemIndex }
            .distinctUntilChanged()
            .collect { firstIdx ->
                if (!hasHandledInitialScroll) return@collect
                val group = flowGroups.getOrNull(firstIdx - listOffset)
                if (group != null) {
                    onEvent(SurahDetailEvent.AyahVisible(group.ayat.first().ayah))
                }
            }
    }

    // Scroll to a pending page chip. The whole surah is always loaded, so
    // the block resolves on the first pass — no fetch, no waiting.
    LaunchedEffect(uiState.ayat, pendingPage) {
        val page = pendingPage ?: return@LaunchedEffect
        val groupIndex = flowGroups.indexOfFirst { it.pageNumber == page }
        if (groupIndex != -1) {
            listState.animateScrollToItem(groupIndex + listOffset)
            pendingPage = null
        }
    }

    // True only while a deep-link placement is genuinely pending: data is in
    // hand AND the target's block can be resolved. Anything else — error, empty
    // result, or a target the DB numbering doesn't contain — falls through to
    // the normal branches, so no failure mode can brick the screen on a spinner.
    val awaitingPlacement = targetAyah > 1 &&
        !isInitialPositionSettled &&
        uiState.ayat.isNotEmpty() &&
        blockIndexOfAyah[targetAyah] != null

    Scaffold(
        contentWindowInsets = androidx.compose.foundation.layout.WindowInsets(0, 0, 0, 0),
        containerColor = MaterialTheme.colorScheme.background,
        snackbarHost = { SnackbarHost(snackbarHostState) }
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .nestedScroll(nestedScrollConnection)
                // Draw-phase pull translation: follows the finger while
                // dragging at an edge without recomposing the screen.
                .graphicsLayer { translationY = nestedScrollConnection.pullPx }
        ) {
            // Header ↔ selection bar swap: fade + subtle ±8dp slide on one small
            // node only — no list/text relayout, safe for low-end devices.
            val density = LocalDensity.current
            val barSlidePx = with(density) { 8.dp.roundToPx() }
            AnimatedContent(
                targetState = uiState.isSelectionMode,
                transitionSpec = {
                    (fadeIn(tween(AppMotion.DurationShort, easing = AppMotion.EasingStandard)) +
                        slideInVertically(
                            tween(AppMotion.DurationShort, easing = AppMotion.EasingStandard)
                        ) { -barSlidePx }) togetherWith
                        (fadeOut(tween(AppMotion.DurationShort, easing = AppMotion.EasingExit)) +
                            slideOutVertically(
                                tween(AppMotion.DurationShort, easing = AppMotion.EasingExit)
                            ) { -barSlidePx })
                },
                label = "headerSelectionSwap"
            ) { inSelection ->
                if (inSelection) {
                    SelectionTopBar(
                        selectedCount = uiState.selectedAyahs.size,
                        onDismiss = { onEvent(SurahDetailEvent.ClearSelection) },
                        onBookmark = { onEvent(SurahDetailEvent.BookmarkSelection) },
                        onCopy = { onEvent(SurahDetailEvent.CopySelection) },
                        onShare = { onEvent(SurahDetailEvent.ShareSelection) }
                    )
                } else {
                    SurahDetailHeader(
                        surah = uiState.surah,
                        fontSize = uiState.fontSize,
                        surahPages = uiState.surahPages,
                        currentPage = { currentPageState.value },
                        collapseState = collapseState,
                        readingProgress = { readingProgressState.value },
                        onNavigateBack = onNavigateBack,
                        onFontSizeChange = { onEvent(SurahDetailEvent.SetFontSize(it)) },
                        // Every page is already loaded: the pendingPage effect
                        // below scrolls to it with no fetch.
                        onPageClick = { page ->
                            pendingPage = page
                        }
                    )
                }
            }

            Box(modifier = Modifier.fillMaxSize().weight(1f)) {
                when {
                    (uiState.isLoading && uiState.ayat.isEmpty()) || awaitingPlacement -> {
                        // Deep links hold here until the list is placed at the
                        // target: the ayat list is not composed at all, so
                        // ayah 1 can never reach the screen for one frame.
                        SurahLoadingPlaceholder()
                    }
                    uiState.ayat.isEmpty() -> {
                        SurahDbMissingState()
                    }
                    else -> {
                        // The Android 12+ stretch effect consumes unconsumed edge
                        // deltas BEFORE parent nested-scroll connections see
                        // them, which would starve the adjacent-surah gesture.
                        // Disabled locally so the pull is the only edge feedback.
                        CompositionLocalProvider(LocalOverscrollConfiguration provides null) {
                            SurahAyatList(
                                ayat = uiState.ayat,
                                fontSize = uiState.fontSize,
                                quranFont = uiState.quranFont,
                                hasBasmalah = hasBasmalah,
                                isSelectionMode = uiState.isSelectionMode,
                                selectedAyahs = uiState.selectedAyahs,
                                bookmarkedAyahs = bookmarkedAyahNums,
                                pulseAyah = pulseAyah,
                                onPulseDone = { pulseAyah = null },
                                listState = listState,
                                onWordClick = { word, ayah ->
                                    if (!uiState.isSelectionMode) {
                                        onEvent(SurahDetailEvent.WordSelected(word, ayah))
                                    }
                                },
                                onToggleSelection = { onEvent(SurahDetailEvent.ToggleAyahSelection(it)) },
                                // Long-press: fresh anchor outside selection mode,
                                // range extension from the anchor inside it.
                                onEnterSelection = {
                                    if (uiState.isSelectionMode) {
                                        onEvent(SurahDetailEvent.RangeSelect(it))
                                    } else {
                                        onEvent(SurahDetailEvent.EnterSelection(it))
                                    }
                                }
                            )
                        }
                    }
                }

                // Edge badges while pulling toward an adjacent surah; the
                // gesture only arms these past its minimum pull, and only at
                // surahs that actually have a neighbor.
                SurahSwitchHints(direction = nestedScrollConnection.hintDirection, surahId = surahId)
            }
        }

        // Word Morphology Bottom Sheet
        uiState.selectedWord?.let { word ->
            MorphologyBottomSheet(
                word = word,
                ayah = uiState.selectedWordAyah,
                sheetState = sheetState,
                onDismiss = { onEvent(SurahDetailEvent.DismissWord) },
                onNavigateToRoot = { rootId, _ ->
                    if (rootId > 0) onNavigateToRootDetail(rootId)
                },
                aiSummary = uiState.aiSummary,
                aiExtended = uiState.aiExtended,
                aiModel = uiState.aiModel,
                aiGeneratedAt = uiState.aiGeneratedAt,
                isAiLoading = uiState.isAiLoading,
                fontSize = uiState.fontSize
            )
        }
    }
}
