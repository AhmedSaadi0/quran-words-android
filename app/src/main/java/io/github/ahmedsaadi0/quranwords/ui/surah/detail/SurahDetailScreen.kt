package io.github.ahmedsaadi0.quranwords.ui.surah.detail

import androidx.activity.compose.BackHandler
import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.compose.animation.togetherWith
import androidx.compose.animation.core.tween
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Download
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.runtime.snapshotFlow
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.input.nestedscroll.nestedScroll
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import kotlinx.coroutines.flow.distinctUntilChanged
import io.github.ahmedsaadi0.quranwords.R
import io.github.ahmedsaadi0.quranwords.ui.components.MorphologyBottomSheet
import io.github.ahmedsaadi0.quranwords.ui.surah.detail.components.AyahFlowGroup
import io.github.ahmedsaadi0.quranwords.ui.surah.detail.components.SelectionTopBar
import io.github.ahmedsaadi0.quranwords.ui.surah.detail.components.SurahAyatList
import io.github.ahmedsaadi0.quranwords.ui.surah.detail.components.groupAyatByPage
import io.github.ahmedsaadi0.quranwords.ui.surah.detail.components.SurahCollapsingHeaderState
import io.github.ahmedsaadi0.quranwords.ui.surah.detail.components.SurahDetailHeader
import io.github.ahmedsaadi0.quranwords.ui.surah.detail.components.rememberNestedScrollCollapse
import io.github.ahmedsaadi0.quranwords.ui.theme.AppMotion

/**
 * Stateless surah detail screen. Collapse state is saveable; pagination,
 * last-read tracking and selection flow through [SurahDetailEvent].
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SurahDetailScreen(
    uiState: SurahDetailUiState,
    surahId: Int,
    targetAyah: Int,
    collapseState: SurahCollapsingHeaderState,
    onEvent: (SurahDetailEvent) -> Unit,
    onNavigateBack: () -> Unit,
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
        buildMap {
            flowGroups.forEachIndexed { groupIndex, group ->
                val lazyIndex = groupIndex + listOffset
                group.ayat.forEach { ayah -> put(ayah.ayah, lazyIndex) }
            }
        }
    }
    // Lazy item index -> source-list index of the block's last ayah (pagination).
    val blockLastAyahListIndex = remember(flowGroups, listOffset) {
        buildMap {
            flowGroups.forEachIndexed { groupIndex, group ->
                put(groupIndex + listOffset, group.firstAyahIndex + group.ayat.size - 1)
            }
        }
    }
    val sheetState = rememberModalBottomSheetState (skipPartiallyExpanded = true)
    var hasHandledInitialScroll by rememberSaveable(surahId, targetAyah) { mutableStateOf(false) }

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

    // Quick-return connection on the common ancestor of header + list so list
    // scroll deltas reach onPreScroll. Disabled while selection is active.
    val nestedScrollConnection = rememberNestedScrollCollapse(collapseState, uiState.isSelectionMode)

    // System back dismisses an active ayah selection first instead of leaving
    // the surah. Disabled while the morphology sheet is open so the sheet
    // consumes the back press (it registers its handler later in this scope).
    BackHandler(enabled = uiState.isSelectionMode && uiState.selectedWord == null) {
        onEvent(SurahDetailEvent.ClearSelection)
    }

    // Load surah only if not already loaded for this surahId
    LaunchedEffect(surahId) {
        if (uiState.surah?.id != surahId || uiState.ayat.isEmpty()) {
            hasHandledInitialScroll = false
            collapseState.reset()
            onEvent(SurahDetailEvent.Load(surahId))
        }
    }

    // Initial scroll to the block owning the target ayah, then record last-read
    LaunchedEffect(uiState.ayat, uiState.surah, hasHandledInitialScroll) {
        if (hasHandledInitialScroll) return@LaunchedEffect
        val currentSurah = uiState.surah
        if (uiState.ayat.isEmpty() || currentSurah == null) return@LaunchedEffect
        val scrollIndex = blockIndexOfAyah[targetAyah]
        if (scrollIndex == null && uiState.ayat.size < currentSurah.ayahCount) {
            onEvent(SurahDetailEvent.EnsureAyahLoaded(targetAyah))
            return@LaunchedEffect
        }
        if (scrollIndex != null) {
            if (kotlin.math.abs(listState.firstVisibleItemIndex - scrollIndex) > 20) {
                listState.scrollToItem(scrollIndex)
            } else {
                listState.animateScrollToItem(scrollIndex)
            }
        }
        hasHandledInitialScroll = true
        if (targetAyah in 1..currentSurah.ayahCount) {
            onEvent(SurahDetailEvent.AyahVisible(targetAyah))
        }
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

    // Pagination trigger (keyed on loaded size: trailing spacer indices resolve
    // to size - 1, so the effect must see fresh sizes)
    LaunchedEffect(listState, uiState.ayat.size) {
        snapshotFlow { listState.layoutInfo.visibleItemsInfo.lastOrNull()?.index ?: 0 }
            .collect { lastIdx ->
                val ayatLastIdx = blockLastAyahListIndex[lastIdx]
                    ?: if (lastIdx < listOffset) -1 else uiState.ayat.size - 1
                onEvent(SurahDetailEvent.NearingEnd(ayatLastIdx))
            }
    }

    // Scroll to a pending page chip once its page block is loaded
    // (page chips map 1:1 to page groups).
    LaunchedEffect(uiState.ayat, pendingPage) {
        val page = pendingPage ?: return@LaunchedEffect
        val groupIndex = flowGroups.indexOfFirst { it.pageNumber == page }
        if (groupIndex != -1) {
            listState.animateScrollToItem(groupIndex + listOffset)
            pendingPage = null
        }
    }

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
                        isBookmarked = uiState.isSurahBookmarked,
                        fontSize = uiState.fontSize,
                        surahPages = uiState.surahPages,
                        currentPage = currentPageFor(flowGroups, listState, hasBasmalah),
                        collapseState = collapseState,
                        onNavigateBack = onNavigateBack,
                        onToggleBookmark = { onEvent(SurahDetailEvent.ToggleSurahBookmark(surahId)) },
                        onFontSizeChange = { onEvent(SurahDetailEvent.SetFontSize(it)) },
                        onPageClick = { page ->
                            pendingPage = page
                            onEvent(SurahDetailEvent.EnsurePageLoaded(page))
                        }
                    )
                }
            }

            Box(modifier = Modifier.fillMaxSize().weight(1f)) {
                when {
                    uiState.isLoading && uiState.ayat.isEmpty() -> {
                        Box(
                            modifier = Modifier.fillMaxSize(),
                            contentAlignment = Alignment.Center
                        ) {
                            CircularProgressIndicator(color = MaterialTheme.colorScheme.primary)
                        }
                    }
                    uiState.ayat.isEmpty() -> {
                        Column(
                            modifier = Modifier
                                .fillMaxSize()
                                .padding(24.dp),
                            verticalArrangement = androidx.compose.foundation.layout.Arrangement.Center,
                            horizontalAlignment = Alignment.CenterHorizontally
                        ) {
                            Icon(
                                imageVector = Icons.Outlined.Download,
                                contentDescription = null,
                                tint = MaterialTheme.colorScheme.onSurfaceVariant,
                                modifier = Modifier.padding(bottom = 16.dp)
                            )
                            Text(
                                text = stringResource(R.string.db_missing_title),
                                style = MaterialTheme.typography.titleLarge,
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.onBackground,
                                textAlign = TextAlign.Center
                            )
                            Spacer(modifier = Modifier.height(8.dp))
                            Text(
                                text = stringResource(R.string.db_missing_body),
                                style = MaterialTheme.typography.bodyMedium,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                                textAlign = TextAlign.Center
                            )
                        }
                    }
                    else -> {
                        SurahAyatList(
                            ayat = uiState.ayat,
                            fontSize = uiState.fontSize,
                            quranFont = uiState.quranFont,
                            hasBasmalah = hasBasmalah,
                            isSelectionMode = uiState.isSelectionMode,
                            selectedAyahs = uiState.selectedAyahs,
                            bookmarkedAyahs = bookmarkedAyahNums,
                            isLoadingMore = uiState.isLoadingMore,
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
                aiModel = uiState.aiModel,
                aiGeneratedAt = uiState.aiGeneratedAt,
                isAiLoading = uiState.isAiLoading
            )
        }
    }
}

private fun currentPageFor(
    groups: List<AyahFlowGroup>,
    listState: androidx.compose.foundation.lazy.LazyListState,
    hasBasmalah: Boolean
): Int? {
    if (groups.isEmpty()) return null
    val groupIndex = (listState.firstVisibleItemIndex - if (hasBasmalah) 1 else 0)
        .coerceIn(0, groups.size - 1)
    return groups[groupIndex].pageNumber
        // Unpaged fallback chunk: report the nearest preceding page.
        ?: groups.take(groupIndex + 1).lastOrNull { it.pageNumber != null }?.pageNumber
}