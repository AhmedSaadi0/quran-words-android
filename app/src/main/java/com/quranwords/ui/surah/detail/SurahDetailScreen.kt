package com.quranwords.ui.surah.detail

import android.app.Activity
import android.provider.Settings
import androidx.activity.compose.BackHandler
import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.LocalOverscrollConfiguration
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.statusBars
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.runtime.snapshotFlow
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.input.nestedscroll.nestedScroll
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.LocalLayoutDirection
import androidx.compose.ui.platform.LocalView
import androidx.compose.ui.unit.LayoutDirection
import androidx.compose.ui.unit.dp
import androidx.core.view.WindowInsetsCompat
import androidx.core.view.WindowInsetsControllerCompat
import kotlinx.coroutines.flow.distinctUntilChanged
import com.quranwords.core.util.ReadingModePreference
import com.quranwords.core.util.SurahMetadata
import com.quranwords.core.util.adjacentSurahTarget
import com.quranwords.ui.components.MorphologyBottomSheet
import com.quranwords.ui.navigation.SurahEnter
import com.quranwords.ui.surah.detail.components.FontSizeSheet
import com.quranwords.ui.surah.detail.components.ReadingChromePillHeight
import com.quranwords.ui.surah.detail.components.ReadingTopBarOverlay
import com.quranwords.ui.surah.detail.components.SelectionTopBar
import com.quranwords.ui.surah.detail.components.SurahAyatList
import com.quranwords.ui.surah.detail.components.SurahDbMissingState
import com.quranwords.ui.surah.detail.components.SurahPagesPager
import com.quranwords.ui.surah.detail.components.SurahSwitchHints
import com.quranwords.ui.surah.detail.components.buildBlockIndexOfAyah
import com.quranwords.ui.surah.detail.components.groupAyatByPage
import com.quranwords.ui.surah.detail.components.currentPageFor
import com.quranwords.ui.surah.detail.components.currentPageForPager
import com.quranwords.ui.surah.detail.components.readingProgressOf
import com.quranwords.ui.surah.detail.components.readingProgressOfPager
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
 * Stateless surah reading screen. The chrome is a persistent overlay: the
 * back button fades while immersed, and the rings/options pill never leaves.
 * Last-read tracking and selection flow through [SurahDetailEvent].
 */
@OptIn(ExperimentalMaterial3Api::class, ExperimentalFoundationApi::class)
@Composable
fun SurahDetailScreen(
    uiState: SurahDetailUiState,
    surahId: Int,
    targetAyah: Int,
    enter: SurahEnter,
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
    // Banner row is always index 0 (Full Plan v2, D2) — it replaces the old
    // conditional Basmalah row, so the offset is unconditionally 1.
    val listOffset = 1

    // Continuous-flow grouping — same pure function as SurahAyatList renders.
    // Lazy indices address blocks now, not ayat: an ayah can start mid-line,
    // so deep-link/last-read granularity is block-level by architecture.
    val flowGroups = remember(uiState.ayat) { groupAyatByPage(uiState.ayat) }
    // Ayah number -> lazy item index of its owning block.
    val blockIndexOfAyah = remember(flowGroups, listOffset) {
        buildBlockIndexOfAyah(flowGroups, listOffset)
    }
    // Book-pages mode state, declared unconditionally so flipping the mode
    // never recreates the pager (position continuity across modes). The
    // pageCount lambda must read the LATEST groups — rememberPagerState
    // captures its lambda once — hence the state holder.
    val flowGroupsState = rememberUpdatedState(flowGroups)
    val pagerState = rememberPagerState(pageCount = { flowGroupsState.value.size })
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
    // Mushaf font-size sheet (reading options menu): composed only while open.
    var showFontSheet by rememberSaveable { mutableStateOf(false) }

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
    // pull unwinds before anything else may move (see
    // SurahReadingScrollConnection). The whole surah is always loaded, so the
    // only gates left are selection, sheet, list state and Quran boundaries.
    val isPagesMode = uiState.readingMode == ReadingModePreference.PAGES
    val nestedScrollConnection = rememberSurahReadingScroll(
        // Edge gates switch container with the mode: pull past the last page
        // flips into the next surah exactly like past the list's end.
        canScrollForward = {
            if (isPagesMode) pagerState.canScrollForward else listState.canScrollForward
        },
        canScrollBackward = {
            if (isPagesMode) pagerState.canScrollBackward else listState.canScrollBackward
        },
        readingMode = uiState.readingMode,
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
    // Layout direction for the horizontal rubber-band visual (PAGES mode):
    // a plain boolean — it changes only when the app locale changes.
    val isRtlTurn = LocalLayoutDirection.current == LayoutDirection.Rtl

    // Top-bar rings: derived once, read only inside ReadingProgressRings'
    // composition — per-frame scroll updates never recompose this screen.
    val surah = uiState.surah
    val surahPages = uiState.surahPages
    val readingProgressState = remember(
        listState, pagerState, flowGroups, listOffset, surah, surahPages, isPagesMode
    ) {
        derivedStateOf {
            if (isPagesMode) {
                readingProgressOfPager(pagerState.currentPage, flowGroups, surah, surahPages)
            } else {
                readingProgressOf(listState, flowGroups, listOffset, surah, surahPages)
            }
        }
    }
    // Page-chip highlight: derived so the screen never subscribes to scroll
    // position — only the pill recomposes, and only on page boundaries.
    val currentPageState = remember(listState, pagerState, flowGroups, listOffset, isPagesMode) {
        derivedStateOf {
            if (isPagesMode) {
                currentPageForPager(flowGroups, pagerState.currentPage)
            } else {
                currentPageFor(flowGroups, listState, hasBasmalah = listOffset > 0)
            }
        }
    }

    // Status-bar top captured ONCE while the bars are certainly visible.
    // Immersive hide/show must never re-measure the LazyColumn: the list's
    // top padding stays byte-identical, so reading never jumps when the
    // chrome hides or returns (the pill floats over the reserved space).
    val density = LocalDensity.current
    val initialStatusTopPx = WindowInsets.statusBars.getTop(density)
    val statusBarTopPx = remember { initialStatusTopPx }
    // Reserved top space = status bar + pill + breathing room below it.
    val chromeTopPadding = with(density) {
        (statusBarTopPx + ReadingChromePillHeight.roundToPx() + 8.dp.roundToPx()).toDp()
    }

    // Immersive reading: hide system chrome (status + navigation) while
    // scrolling down so the whole viewport is the surah; return on scroll up.
    // Bar visibility never recomposes the reading list — it is a window effect.
    val view = LocalView.current
    val activity = remember(context) {
        var ctx = context
        while (ctx is android.content.ContextWrapper && ctx !is Activity) {
            ctx = ctx.baseContext
        }
        ctx as? Activity
    }
    var isImmersive by rememberSaveable(surahId) { mutableStateOf(false) }
    val immersiveInteractionLocked = uiState.isSelectionMode || uiState.selectedWord != null

    LaunchedEffect(listState, pagerState, isPagesMode, immersiveInteractionLocked) {
        if (isPagesMode) {
            // Book pages: chrome follows the FLIP — any forward page hides it,
            // any backward page (or resting on page 0) brings it back.
            var lastPage = pagerState.currentPage
            snapshotFlow { pagerState.currentPage }
                .distinctUntilChanged()
                .collect { page ->
                    if (immersiveInteractionLocked) {
                        if (isImmersive) isImmersive = false
                        lastPage = page
                        return@collect
                    }
                    when {
                        page <= 0 -> if (isImmersive) isImmersive = false
                        page > lastPage -> if (!isImmersive) isImmersive = true
                        page < lastPage -> if (isImmersive) isImmersive = false
                    }
                    lastPage = page
                }
        } else {
            var lastScrollPx = 0
            snapshotFlow {
                listState.firstVisibleItemIndex * 10000 + listState.firstVisibleItemScrollOffset
            }.distinctUntilChanged().collect { posPx ->
                if (immersiveInteractionLocked) {
                    if (isImmersive) isImmersive = false
                    lastScrollPx = posPx
                    return@collect
                }
                val delta = posPx - lastScrollPx
                lastScrollPx = posPx
                // Obsidian-like, instant: any downward drag maximizes, any
                // upward drag restores — no threshold delay, no dwell.
                // At the very top we always keep chrome visible.
                if (posPx <= 5) {
                    if (isImmersive) isImmersive = false
                } else if (delta > 2) {
                    if (!isImmersive) isImmersive = true
                } else if (delta < -2) {
                    if (isImmersive) isImmersive = false
                }
            }
        }
    }

    LaunchedEffect(isImmersive) {
        val window = activity?.window ?: return@LaunchedEffect
        val controller = WindowInsetsControllerCompat(window, view)
        controller.systemBarsBehavior = WindowInsetsControllerCompat.BEHAVIOR_SHOW_TRANSIENT_BARS_BY_SWIPE
        if (isImmersive) {
            controller.hide(
                WindowInsetsCompat.Type.statusBars() or
                    WindowInsetsCompat.Type.navigationBars()
            )
        } else {
            controller.show(
                WindowInsetsCompat.Type.statusBars() or
                    WindowInsetsCompat.Type.navigationBars()
            )
        }
    }

    DisposableEffect(activity, view) {
        onDispose {
            val window = activity?.window ?: return@onDispose
            WindowInsetsControllerCompat(window, view).show(
                WindowInsetsCompat.Type.statusBars() or
                    WindowInsetsCompat.Type.navigationBars()
            )
        }
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
        if (isPagesMode) {
            // Pager pages map 1:1 onto flow groups — no banner offset.
            val scrollGroup = resolvedIndex
                ?.let { it - listOffset }
                ?.coerceIn(0, flowGroups.size - 1)
                ?: (flowGroups.size - 1).coerceAtLeast(0)
            if (resolvedIndex != null) {
                entryDidScroll = scrollGroup != 0
            }
            // requestScrollToPage (not animate): the pager is still gated
            // behind awaitingPlacement, so it has never laid out — the
            // suspending call would wait for first layout forever.
            // The request applies on the next (i.e. first) remeasure instead.
            pagerState.requestScrollToPage(scrollGroup)
        } else {
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
        }
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
    LaunchedEffect(listState, pagerState, isPagesMode, uiState.ayat, hasHandledInitialScroll) {
        if (isPagesMode) {
            // Book pages: a settled page IS a flow group (1:1 mapping).
            snapshotFlow { pagerState.settledPage }
                .distinctUntilChanged()
                .collect { page ->
                    if (!hasHandledInitialScroll) return@collect
                    flowGroups.getOrNull(page)?.let { group ->
                        onEvent(SurahDetailEvent.AyahVisible(group.ayat.first().ayah))
                    }
                }
        } else {
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
    }

    // Scroll to a pending page chip. The whole surah is always loaded, so
    // the block resolves on the first pass — no fetch, no waiting.
    LaunchedEffect(uiState.ayat, pendingPage, isPagesMode) {
        val page = pendingPage ?: return@LaunchedEffect
        val groupIndex = flowGroups.indexOfFirst { it.pageNumber == page }
        if (groupIndex != -1) {
            if (isPagesMode) {
                pagerState.animateScrollToPage(groupIndex)
            } else {
                listState.animateScrollToItem(groupIndex + listOffset)
            }
            pendingPage = null
        }
    }

    // Mode flips mid-session hand the reading position to the new container:
    // the visible flow group seeds it (list item = group + banner offset,
    // pager page = group) so the reader never restarts at the top. Skipped
    // before the initial placement so deep links own the very first position.
    LaunchedEffect(uiState.readingMode) {
        if (!hasHandledInitialScroll) return@LaunchedEffect
        if (isPagesMode) {
            val groupIndex = (listState.firstVisibleItemIndex - listOffset)
                .coerceIn(0, flowGroups.size - 1)
            if (pagerState.currentPage != groupIndex) {
                pagerState.requestScrollToPage(groupIndex)
            }
        } else {
            val itemIndex = (pagerState.currentPage + listOffset)
                .coerceIn(0, flowGroups.size - 1 + listOffset)
            if (listState.firstVisibleItemIndex != itemIndex) {
                listState.requestScrollToItem(itemIndex)
            }
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
        contentWindowInsets = WindowInsets(0, 0, 0, 0),
        containerColor = MaterialTheme.colorScheme.background,
        snackbarHost = { SnackbarHost(snackbarHostState) }
    ) { innerPadding ->
        // Overlay chrome over full-size reading content (Obsidian-like):
        // the reading list is always full-screen; the header floats above
        // it and slides out/in with the scroll gesture — no layout jump.
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .nestedScroll(nestedScrollConnection)
        ) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    // The rubber-band pull rides the ACTIVE axis only
                    // (draw-phase read of the connection's pull — it never
                    // recomposes per frame).
                    .graphicsLayer {
                        if (isPagesMode) {
                            translationX = if (isRtlTurn) {
                                -nestedScrollConnection.pullPx
                            } else {
                                nestedScrollConnection.pullPx
                            }
                        } else {
                            translationY = nestedScrollConnection.pullPx
                        }
                    }
            ) {
                when {
                    (uiState.isLoading && uiState.ayat.isEmpty()) || awaitingPlacement -> {
                        SurahLoadingPlaceholder()
                    }
                    uiState.ayat.isEmpty() -> {
                        SurahDbMissingState()
                    }
                    else -> {
                        CompositionLocalProvider(LocalOverscrollConfiguration provides null) {
                            if (isPagesMode) {
                                SurahPagesPager(
                                    flowGroups = flowGroups,
                                    ayat = uiState.ayat,
                                    quranFont = uiState.quranFont,
                                    pagerState = pagerState,
                                    hasBasmalah = hasBasmalah,
                                    surah = uiState.surah,
                                    isSelectionMode = uiState.isSelectionMode,
                                    selectedAyahs = uiState.selectedAyahs,
                                    bookmarkedAyahs = bookmarkedAyahNums,
                                    pulseAyah = pulseAyah,
                                    onPulseDone = { pulseAyah = null },
                                    onWordClick = { word, ayah ->
                                        if (!uiState.isSelectionMode) {
                                            onEvent(SurahDetailEvent.WordSelected(word, ayah))
                                        }
                                    },
                                    onToggleSelection = { onEvent(SurahDetailEvent.ToggleAyahSelection(it)) },
                                    onEnterSelection = {
                                        if (uiState.isSelectionMode) {
                                            onEvent(SurahDetailEvent.RangeSelect(it))
                                        } else {
                                            onEvent(SurahDetailEvent.EnterSelection(it))
                                        }
                                    },
                                    chromeTopPadding = chromeTopPadding
                                )
                            } else {
                                SurahAyatList(
                                    ayat = uiState.ayat,
                                    contentPadding = PaddingValues(
                                        start = 16.dp,
                                        end = 16.dp,
                                        top = chromeTopPadding,
                                        bottom = 24.dp
                                    ),
                                    fontSize = uiState.fontSize,
                                    quranFont = uiState.quranFont,
                                    hasBasmalah = hasBasmalah,
                                    surah = uiState.surah,
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
                }
                SurahSwitchHints(
                    direction = nestedScrollConnection.hintDirection,
                    surahId = surahId,
                    horizontal = isPagesMode
                )
            }

            // Reading chrome: the back button fades with immersion, the rings
            // + options pill never leaves (it only dims while scrolling), and
            // selection replaces the whole overlay with SelectionTopBar.
            val barSlidePx = with(density) { 8.dp.roundToPx() }
            AnimatedContent(
                targetState = uiState.isSelectionMode,
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
                        selectedCount = uiState.selectedAyahs.size,
                        onDismiss = { onEvent(SurahDetailEvent.ClearSelection) },
                        onBookmark = { onEvent(SurahDetailEvent.BookmarkSelection) },
                        onCopy = { onEvent(SurahDetailEvent.CopySelection) },
                        onShare = { onEvent(SurahDetailEvent.ShareSelection) }
                    )
                } else {
                    ReadingTopBarOverlay(
                        showBar = !isImmersive,
                        readingProgress = { readingProgressState.value },
                        pages = uiState.surahPages,
                        currentPage = { currentPageState.value },
                        pillTransparency = uiState.pillTransparency,
                        onBack = onNavigateBack,
                        onOpenFontSize = { showFontSheet = true },
                        onPageSelected = { page -> pendingPage = page }
                    )
                }
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

        // Mushaf font-size + pill transparency sheet (reading options):
        // composed only while open, so it leaves the composition tree
        // entirely once dismissed.
        if (showFontSheet) {
            FontSizeSheet(
                fontSize = uiState.fontSize,
                quranFont = uiState.quranFont,
                pillTransparency = uiState.pillTransparency,
                readingMode = uiState.readingMode,
                onFontSizeChange = { onEvent(SurahDetailEvent.SetFontSize(it)) },
                onPillTransparencyChange = { onEvent(SurahDetailEvent.SetPillTransparency(it)) },
                onReadingModeChange = { onEvent(SurahDetailEvent.SetReadingMode(it)) },
                onDismiss = { showFontSheet = false }
            )
        }
    }
}
