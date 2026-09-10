package io.github.ahmedsaadi0.quranwords.ui.surah.detail

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
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import io.github.ahmedsaadi0.quranwords.R
import io.github.ahmedsaadi0.quranwords.ui.components.MorphologyBottomSheet
import io.github.ahmedsaadi0.quranwords.ui.surah.detail.components.SelectionTopBar
import io.github.ahmedsaadi0.quranwords.ui.surah.detail.components.SurahAyatList
import io.github.ahmedsaadi0.quranwords.ui.surah.detail.components.SurahCollapsingHeaderState
import io.github.ahmedsaadi0.quranwords.ui.surah.detail.components.SurahDetailHeader
import io.github.ahmedsaadi0.quranwords.ui.surah.detail.components.rememberSurahCollapsingHeaderState

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
    val sheetState = rememberModalBottomSheetState (skipPartiallyExpanded = true)
    var hasHandledInitialScroll by rememberSaveable(surahId, targetAyah) { mutableStateOf(false) }

    // Pending page-chip click: scroll once the requested page becomes loaded
    // (replaces the legacy delay(100) + VM-state peek hack).
    var pendingPage by remember { mutableStateOf<Int?>(null) }

    // Load surah only if not already loaded for this surahId
    LaunchedEffect(surahId) {
        if (uiState.surah?.id != surahId || uiState.ayat.isEmpty()) {
            hasHandledInitialScroll = false
            collapseState.reset()
            onEvent(SurahDetailEvent.Load(surahId))
        }
    }

    // Initial scroll to the target ayah, then record last-read
    LaunchedEffect(uiState.ayat, uiState.surah, hasHandledInitialScroll) {
        if (hasHandledInitialScroll) return@LaunchedEffect
        val currentSurah = uiState.surah
        if (uiState.ayat.isEmpty() || currentSurah == null) return@LaunchedEffect
        val idx = uiState.ayat.indexOfFirst { it.ayah == targetAyah }
        if (idx == -1 && uiState.ayat.size < currentSurah.ayahCount) {
            onEvent(SurahDetailEvent.EnsureAyahLoaded(targetAyah))
            return@LaunchedEffect
        }
        if (idx != -1) {
            val scrollIndex = idx + if (hasBasmalah) 1 else 0
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

    // Track the first visible ayah for last-read updates
    LaunchedEffect(listState, uiState.ayat) {
        snapshotFlow { listState.firstVisibleItemIndex }
            .collect { firstIdx ->
                val ayat = uiState.ayat
                if (ayat.isNotEmpty()) {
                    val ayatIdx = firstIdx - if (hasBasmalah) 1 else 0
                    if (ayatIdx in ayat.indices) {
                        onEvent(SurahDetailEvent.AyahVisible(ayat[ayatIdx].ayah))
                    }
                }
            }
    }

    // Pagination trigger
    LaunchedEffect(listState) {
        snapshotFlow { listState.layoutInfo.visibleItemsInfo.lastOrNull()?.index ?: 0 }
            .collect { lastIdx ->
                val ayatLastIdx = lastIdx - if (hasBasmalah) 1 else 0
                onEvent(SurahDetailEvent.NearingEnd(ayatLastIdx))
            }
    }

    // Scroll to a pending page chip once its page is loaded
    LaunchedEffect(uiState.ayat, pendingPage) {
        val page = pendingPage ?: return@LaunchedEffect
        val idx = uiState.ayat.indexOfFirst { it.pageNumber == page }
        if (idx != -1) {
            listState.animateScrollToItem(idx + if (hasBasmalah) 1 else 0)
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
        ) {
            if (uiState.isSelectionMode) {
                SelectionTopBar(
                    selectedCount = uiState.selectedAyahs.size,
                    onDismiss = { onEvent(SurahDetailEvent.ClearSelection) },
                    onSelectAll = { onEvent(SurahDetailEvent.SelectAllAyahs) },
                    onCopy = { onEvent(SurahDetailEvent.CopySelection) },
                    onShare = { onEvent(SurahDetailEvent.ShareSelection) }
                )
            } else {
                SurahDetailHeader(
                    surah = uiState.surah,
                    isBookmarked = uiState.isSurahBookmarked,
                    fontSize = uiState.fontSize,
                    surahPages = uiState.surahPages,
                    currentPage = currentPageFor(uiState, listState, hasBasmalah),
                    collapseState = collapseState,
                    isSelectionActive = uiState.isSelectionMode,
                    onNavigateBack = onNavigateBack,
                    onToggleBookmark = { onEvent(SurahDetailEvent.ToggleSurahBookmark(surahId)) },
                    onFontSizeChange = { onEvent(SurahDetailEvent.SetFontSize(it)) },
                    onPageClick = { page ->
                        pendingPage = page
                        onEvent(SurahDetailEvent.EnsurePageLoaded(page))
                    }
                )
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
                            surah = uiState.surah,
                            fontSize = uiState.fontSize,
                            bookmarkedAyat = uiState.bookmarkedAyat,
                            surahId = surahId,
                            hasBasmalah = hasBasmalah,
                            isSelectionMode = uiState.isSelectionMode,
                            selectedAyahs = uiState.selectedAyahs,
                            isLoadingMore = uiState.isLoadingMore,
                            listState = listState,
                            onWordClick = { word, ayah ->
                                if (!uiState.isSelectionMode) {
                                    onEvent(SurahDetailEvent.WordSelected(word, ayah))
                                }
                            },
                            onToggleSelection = { onEvent(SurahDetailEvent.ToggleAyahSelection(it)) },
                            onEnterSelection = { onEvent(SurahDetailEvent.EnterSelection(it)) },
                            onBookmarkClick = { onEvent(SurahDetailEvent.ToggleAyahBookmark(surahId, it)) }
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
    uiState: SurahDetailUiState,
    listState: androidx.compose.foundation.lazy.LazyListState,
    hasBasmalah: Boolean
): Int? {
    if (uiState.ayat.isEmpty()) return null
    val visibleIdx = (listState.firstVisibleItemIndex - if (hasBasmalah) 1 else 0)
        .coerceIn(0, uiState.ayat.size - 1)
    return uiState.ayat.getOrNull(visibleIdx)?.pageNumber
}