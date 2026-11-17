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
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.statusBars
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
import androidx.compose.ui.graphics.Brush
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
import com.quranwords.core.util.AyahRef
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
import com.quranwords.ui.surah.detail.components.WordActionTarget
import com.quranwords.ui.surah.detail.components.WordInteractionCallbacks
import com.quranwords.ui.surah.detail.components.buildBlockIndexOfAyah
import com.quranwords.ui.surah.detail.components.currentPageFor
import com.quranwords.ui.surah.detail.components.groupAyatByPage
import com.quranwords.ui.surah.detail.components.readingProgressOf
import com.quranwords.ui.surah.detail.components.readingProgressOfMushafPage
import com.quranwords.ui.surah.detail.components.rememberSurahReadingScroll
import com.quranwords.ui.theme.AppMotion

@Composable
private fun SurahLoadingPlaceholder() {
    Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
        CircularProgressIndicator(color = MaterialTheme.colorScheme.primary)
    }
}

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
    onEvent: (SurahDetailEvent) -> Unit,
    onNavigateBack: () -> Unit,
    onNavigateToAdjacentSurah: (surahId: Int, ayah: Int, fromNext: Boolean, initialImmersive: Boolean) -> Unit,
    onNavigateToRootDetail: (Int) -> Unit,
    snackbarHostState: SnackbarHostState
) {
    val listState = rememberSaveable(surahId, saver = androidx.compose.foundation.lazy.LazyListState.Saver) {
        androidx.compose.foundation.lazy.LazyListState()
    }
    val hasBasmalah = surahId != 9 && surahId != 1
    val listOffset = 1

    val flowGroups = remember(uiState.ayat) { groupAyatByPage(uiState.ayat) }
    val blockIndexOfAyah = remember(flowGroups, listOffset) { buildBlockIndexOfAyah(flowGroups, listOffset) }

    val isPagesMode = uiState.readingMode == ReadingModePreference.PAGES
    // Guardrail 1: never mount the pager until the page count, entry page and
    // its preloaded window are all available.
    val pagesReady = isPagesMode && uiState.isPagesReady &&
        uiState.totalPages > 0 && uiState.entryPage in 1..uiState.totalPages &&
        !uiState.pagesByNumber[uiState.entryPage].isNullOrEmpty()

    var hasHandledInitialScroll by rememberSaveable(surahId, targetAyah) { mutableStateOf(false) }
    var isInitialPositionSettled by rememberSaveable(surahId, targetAyah) { mutableStateOf(targetAyah <= 1) }
    var entryDidScroll by rememberSaveable(surahId, targetAyah) { mutableStateOf(false) }
    var pulseDone by rememberSaveable(surahId, targetAyah) { mutableStateOf(false) }
    var pendingPage by remember { mutableStateOf<Int?>(null) }
    var showFontSheet by rememberSaveable { mutableStateOf(false) }
    var wordAction by remember { mutableStateOf<WordActionTarget?>(null) }
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)

    val bookmarkedRefs = remember(uiState.bookmarkedAyat) {
        uiState.bookmarkedAyat.mapNotNull { AyahRef.parse(it) }.toSet()
    }

    val nestedScrollConnection = rememberSurahReadingScroll(
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
    val isRtlTurn = LocalLayoutDirection.current == LayoutDirection.Rtl

    val readingProgressState = remember(listState, flowGroups, listOffset, isPagesMode, uiState) {
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
    val currentPageState = remember(listState, flowGroups, listOffset, isPagesMode, uiState) {
        derivedStateOf {
            if (isPagesMode) uiState.currentPage
            else currentPageFor(flowGroups, listState, hasBasmalah = listOffset > 0)
        }
    }

    val density = LocalDensity.current
    val initialStatusTopPx = WindowInsets.statusBars.getTop(density)
    val statusBarTopPx = remember { initialStatusTopPx }
    val chromeTopPadding = with(density) {
        (statusBarTopPx + ReadingChromePillHeight.roundToPx() + 8.dp.roundToPx()).toDp()
    }

    val view = LocalView.current
    val context = LocalContext.current
    val activity = remember(context) {
        var ctx = context
        while (ctx is android.content.ContextWrapper && ctx !is Activity) ctx = ctx.baseContext
        ctx as? Activity
    }
    val immersiveState = rememberUpdatedState(uiState.isImmersive)
    val immersiveInteractionLocked = uiState.isSelectionMode || uiState.selectedWord != null

    val wordInteraction = WordInteractionCallbacks(
        onOpenWord = { word, ayah ->
            wordAction = null
            onEvent(SurahDetailEvent.WordSelected(word, ayah))
        },
        onWordLongPressed = { target -> wordAction = target },
        onToggleChrome = {
            if (wordAction != null) {
                wordAction = null
            } else {
                onEvent(SurahDetailEvent.SetImmersive(!immersiveState.value))
            }
        },
        onDismissWordAction = { wordAction = null },
        onCopyWord = { text ->
            wordAction = null
            onEvent(SurahDetailEvent.CopyWord(text))
        }
    )

    // A page change / scroll retires the toolbar.
    LaunchedEffect(if (isPagesMode) uiState.currentPage else listState.firstVisibleItemIndex, isPagesMode) {
        wordAction = null
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

    LaunchedEffect(uiState.isImmersive) {
        val window = activity?.window ?: return@LaunchedEffect
        val controller = WindowInsetsControllerCompat(window, view)
        controller.systemBarsBehavior = WindowInsetsControllerCompat.BEHAVIOR_SHOW_TRANSIENT_BARS_BY_SWIPE
        if (uiState.isImmersive) {
            controller.hide(WindowInsetsCompat.Type.statusBars() or WindowInsetsCompat.Type.navigationBars())
        } else {
            controller.show(WindowInsetsCompat.Type.statusBars() or WindowInsetsCompat.Type.navigationBars())
        }
    }
    DisposableEffect(activity, view) {
        onDispose {
            val window = activity?.window ?: return@onDispose
            WindowInsetsControllerCompat(window, view).show(
                WindowInsetsCompat.Type.statusBars() or WindowInsetsCompat.Type.navigationBars()
            )
        }
    }

    // Back priority: sheet → popup → selection → exit.
    BackHandler(enabled = uiState.isSelectionMode && uiState.selectedWord == null) {
        onEvent(SurahDetailEvent.ClearSelection)
    }
    BackHandler(enabled = wordAction != null && uiState.selectedWord == null) { wordAction = null }
    BackHandler(enabled = uiState.selectedWord != null) { onEvent(SurahDetailEvent.DismissWord) }

    // Load once per route. The VM preloads the page window before emitting.
    LaunchedEffect(surahId) {
        if (uiState.surah?.id != surahId || uiState.ayat.isEmpty()) {
            wordAction = null
            hasHandledInitialScroll = false
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

    // Pages mode placement is handled by the pager's initialPage; mark settled.
    LaunchedEffect(pagesReady) {
        if (pagesReady) {
            hasHandledInitialScroll = true
            isInitialPositionSettled = true
        }
    }

    val animatorScale = remember {
        Settings.Global.getFloat(context.contentResolver, Settings.Global.ANIMATOR_DURATION_SCALE, 1f)
    }

    // Vertical deep-link placement (instant, silent).
    LaunchedEffect(uiState.ayat, uiState.surah, hasHandledInitialScroll) {
        if (isPagesMode || hasHandledInitialScroll) return@LaunchedEffect
        val currentSurah = uiState.surah
        if (uiState.ayat.isEmpty() || currentSurah == null) return@LaunchedEffect
        val resolvedIndex = blockIndexOfAyah[AyahRef(surahId, targetAyah)]
        val scrollIndex = resolvedIndex ?: (flowGroups.size - 1 + listOffset).coerceAtLeast(0)
        if (resolvedIndex != null) {
            entryDidScroll = scrollIndex != listState.firstVisibleItemIndex ||
                listState.firstVisibleItemScrollOffset != 0
        }
        listState.requestScrollToItem(scrollIndex)
        isInitialPositionSettled = true
        hasHandledInitialScroll = true
        if (targetAyah in 1..currentSurah.ayahCount) {
            onEvent(SurahDetailEvent.AyahVisible(AyahRef(surahId, targetAyah)))
        }
    }

    // One-shot pulse: vertical deep link or a Pages-mode jump beyond the surah's first page.
    LaunchedEffect(hasHandledInitialScroll, uiState.ayat, pagesReady, uiState.entryPage) {
        if (!hasHandledInitialScroll || pulseDone || enter != SurahEnter.Normal || animatorScale == 0f) return@LaunchedEffect
        val ref = AyahRef(surahId, targetAyah)
        val shouldPulse = if (isPagesMode) {
            pagesReady && uiState.pageAyahs.any { it.surah == ref.surahId && it.ayah == ref.ayah } &&
                uiState.entryPage > (uiState.surahPages.firstOrNull() ?: 1)
        } else {
            uiState.ayat.any { it.ayah == targetAyah } && entryDidScroll
        }
        pulseDone = true
        if (shouldPulse) onEvent(SurahDetailEvent.SetPulseAyah(ref))
    }

    // Last-read tracking, keyed by surah + ayah on both modes.
    LaunchedEffect(listState, isPagesMode, uiState.ayat, uiState.currentPage, hasHandledInitialScroll) {
        if (isPagesMode) {
            snapshotFlow { uiState.currentPage }.distinctUntilChanged().collect { page ->
                if (!hasHandledInitialScroll || page < 1) return@collect
                val ayat = uiState.pageAyahs
                val active = ayat.firstOrNull { it.surah == uiState.activeSurahId }
                    ?: ayat.firstOrNull() ?: return@collect
                onEvent(SurahDetailEvent.AyahVisible(AyahRef(active.surah, active.ayah)))
            }
        } else {
            snapshotFlow { listState.firstVisibleItemIndex }.distinctUntilChanged().collect { firstIdx ->
                if (!hasHandledInitialScroll) return@collect
                val group = flowGroups.getOrNull(firstIdx - listOffset) ?: return@collect
                val ayah = group.ayat.firstOrNull() ?: return@collect
                onEvent(SurahDetailEvent.AyahVisible(AyahRef(ayah.surah, ayah.ayah)))
            }
        }
    }

    // Page chip jump in scroll mode; the pager handles its own in Pages mode.
    LaunchedEffect(uiState.ayat, pendingPage, isPagesMode, flowGroups) {
        if (isPagesMode) return@LaunchedEffect
        val page = pendingPage ?: return@LaunchedEffect
        val index = flowGroups.indexOfFirst { it.pageNumber == page }
        if (index >= 0) {
            listState.animateScrollToItem(index + listOffset)
            pendingPage = null
        }
    }

    // Mode flip: pages → vertical may need the surah currently on screen;
    // vertical → pages re-anchors on the visible mushaf page before mounting.
    var previousReadingMode by rememberSaveable { mutableStateOf<Int?>(null) }
    LaunchedEffect(uiState.readingMode) {
        wordAction = null
        val previous = previousReadingMode
        previousReadingMode = uiState.readingMode
        if (previous == null || previous == uiState.readingMode || !hasHandledInitialScroll) return@LaunchedEffect
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

    val awaitingPlacement = !isPagesMode && targetAyah > 1 && !isInitialPositionSettled &&
        uiState.ayat.isNotEmpty() && blockIndexOfAyah[AyahRef(surahId, targetAyah)] != null

    Scaffold(
        contentWindowInsets = WindowInsets(0, 0, 0, 0),
        containerColor = MaterialTheme.colorScheme.background,
        snackbarHost = { SnackbarHost(snackbarHostState) }
    ) { innerPadding ->
        Box(
            modifier = Modifier.fillMaxSize().padding(innerPadding).nestedScroll(nestedScrollConnection)
        ) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .graphicsLayer {
                        if (isPagesMode) {
                            translationX = if (isRtlTurn) -nestedScrollConnection.pullPx else nestedScrollConnection.pullPx
                        } else {
                            translationY = nestedScrollConnection.pullPx
                        }
                    }
            ) {
                if (isPagesMode) {
                    when {
                        !pagesReady || uiState.isLoading -> SurahLoadingPlaceholder()
                        uiState.pageAyahs.isEmpty() -> SurahDbMissingState()
                        else -> CompositionLocalProvider(LocalOverscrollConfiguration provides null) {
                            SurahPagesPager(
                                totalPages = uiState.totalPages,
                                entryPage = uiState.entryPage,
                                pagesByNumber = uiState.pagesByNumber,
                                quranFont = uiState.quranFont,
                                activeSurah = uiState.activeSurah,
                                isSelectionMode = uiState.isSelectionMode,
                                selectedAyahs = uiState.selectedAyahs,
                                bookmarkedAyahs = bookmarkedRefs,
                                pulseAyah = uiState.pulseAyah,
                                onPulseDone = { onEvent(SurahDetailEvent.SetPulseAyah(null)) },
                                wordInteraction = wordInteraction,
                                activeWordAction = wordAction,
                                pendingPage = pendingPage,
                                onPageChanged = { onEvent(SurahDetailEvent.PageChanged(it)) },
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
                        (uiState.isLoading && uiState.ayat.isEmpty()) || awaitingPlacement -> SurahLoadingPlaceholder()
                        uiState.ayat.isEmpty() -> SurahDbMissingState()
                        else -> CompositionLocalProvider(LocalOverscrollConfiguration provides null) {
                            SurahAyatList(
                                ayat = uiState.ayat,
                                contentPadding = PaddingValues(
                                    start = 16.dp, end = 16.dp, top = chromeTopPadding, bottom = 24.dp
                                ),
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
                                activeWordAction = wordAction,
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
                    direction = nestedScrollConnection.hintDirection,
                    surahId = if (isPagesMode) uiState.activeSurahId else surahId,
                    horizontal = isPagesMode
                )
            }

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
                    Box(Modifier.fillMaxSize()) {
                        if (isPagesMode && !uiState.isImmersive) {
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
                            showBar = !uiState.isImmersive,
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
        }

        uiState.selectedWord?.let { word ->
            MorphologyBottomSheet(
                word = word,
                ayah = uiState.selectedWordAyah,
                sheetState = sheetState,
                onDismiss = { onEvent(SurahDetailEvent.DismissWord) },
                onNavigateToRoot = { rootId, _ -> if (rootId > 0) onNavigateToRootDetail(rootId) },
                aiExtended = uiState.aiExtended,
                aiModel = uiState.aiModel,
                aiGeneratedAt = uiState.aiGeneratedAt,
                fontSize = uiState.fontSize
            )
        }

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
