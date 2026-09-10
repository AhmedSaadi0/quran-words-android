package io.github.ahmedsaadi0.quranwords.ui.roots.detail

import androidx.compose.animation.core.tween
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyListState
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.SideEffect
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.foundation.layout.Box
import androidx.compose.ui.input.nestedscroll.nestedScroll
import io.github.ahmedsaadi0.quranwords.ui.components.ReportMeaningDialog
import io.github.ahmedsaadi0.quranwords.ui.roots.detail.components.RootDetailHeader
import io.github.ahmedsaadi0.quranwords.ui.roots.detail.components.rememberCollapsingHeaderState
import io.github.ahmedsaadi0.quranwords.ui.roots.detail.tabs.AyatTab
import io.github.ahmedsaadi0.quranwords.ui.roots.detail.tabs.DerivativesTab
import io.github.ahmedsaadi0.quranwords.ui.roots.detail.tabs.MasadirTab
import io.github.ahmedsaadi0.quranwords.ui.roots.detail.tabs.MeaningsTab
import io.github.ahmedsaadi0.quranwords.ui.roots.detail.tabs.WordsTab
import io.github.ahmedsaadi0.quranwords.ui.roots.detail.util.RootDetailTab
import io.github.ahmedsaadi0.quranwords.ui.theme.AppMotion
import io.github.ahmedsaadi0.quranwords.util.ReportAyahSample
import kotlinx.coroutines.launch

/**
 * Pure stateless coordinator: Scaffold + [RootDetailHeader] + pager of tabs.
 * Zero references to ViewModel, Context, clipboard or intents — all platform
 * work flows through [onEvent] to the Route. Previewable with mock [uiState].
 */
@OptIn(ExperimentalFoundationApi::class)
@Composable
fun RootDetailScreen(
    uiState: RootDetailUiState,
    rootId: Int,
    onEvent: (RootDetailEvent) -> Unit,
    onNavigateBack: () -> Unit,
    snackbarHostState: SnackbarHostState,
    modifier: Modifier = Modifier,
) {
    val pagerTabs = remember { RootDetailTab.entries.toList() }
    val pagerState = rememberPagerState(initialPage = 0, pageCount = { pagerTabs.size })
    val scope = rememberCoroutineScope()
    val headerState = rememberCollapsingHeaderState()
    val nestedScroll = remember(headerState) { headerState.nestedScrollConnection }
    // Freeze the collapsing header while multi-selecting (words/meanings).
    SideEffect {
        headerState.isSelectionMode = uiState.words.isSelectionMode || uiState.meanings.isSelectionMode
    }
    val meaningsState = rememberSaveable(saver = LazyListState.Saver) { LazyListState() }
    val masadirState = rememberSaveable(saver = LazyListState.Saver) { LazyListState() }
    val derivativesState = rememberSaveable(saver = LazyListState.Saver) { LazyListState() }
    val ayatState = rememberSaveable(saver = LazyListState.Saver) { LazyListState() }
    val wordsState = rememberSaveable(saver = LazyListState.Saver) { LazyListState() }
    val copying = uiState.copyingAction
    val copyMeaningsAll = copying == CopyAction.COPY_MEANINGS_ALL || copying == CopyAction.SHARE_MEANINGS_ALL
    val copyMeaningsSel = copying == CopyAction.COPY_MEANINGS_SELECTED || copying == CopyAction.SHARE_MEANINGS_SELECTED
    val copyWordsSel = copying == CopyAction.COPY_WORDS_SELECTED || copying == CopyAction.SHARE_WORDS_SELECTED
    val copyOccAll = copying == CopyAction.COPY_OCCURRENCES_ALL || copying == CopyAction.SHARE_OCCURRENCES_ALL
    val tabCounts = remember(uiState.tabs) { uiState.tabs.associate { it.tab to it.count } }
    val detail = uiState.detail

    Scaffold(
        modifier = modifier,
        containerColor = MaterialTheme.colorScheme.background,
        contentWindowInsets = WindowInsets(0, 0, 0, 0),
        snackbarHost = { SnackbarHost(snackbarHostState) }
    ) { innerPadding ->
        if (uiState.isLoading || detail == null) {
            Box(Modifier.fillMaxSize().padding(innerPadding), contentAlignment = Alignment.Center) {
                CircularProgressIndicator(color = MaterialTheme.colorScheme.primary)
            }
        } else {
            Column(Modifier.fillMaxSize().padding(innerPadding).nestedScroll(nestedScroll)) {
                RootDetailHeader(
                    state = headerState,
                    rootText = uiState.rootText,
                    subtitleText = uiState.subtitleText,
                    hasSubtitle = uiState.hasSubtitle,
                    aiMetaLine = uiState.aiMetaLine,
                    hasAiMeta = uiState.hasAiMeta,
                    tabCounts = tabCounts,
                    pagerTabs = pagerTabs,
                    selectedTabIndex = pagerState.currentPage,
                    onNavigateBack = onNavigateBack,
                    onReportClick = { onEvent(RootDetailEvent.ShowReport) },
                    onCopyAiSummary = { onEvent(RootDetailEvent.CopyAiSummary) },
                    onShareAiSummary = { onEvent(RootDetailEvent.ShareAiSummary) },
                    onTabClick = { i -> scope.launch { pagerState.animateScrollToPage(i, animationSpec = tween(AppMotion.DurationMedium, easing = AppMotion.EasingStandard)) } }
                )
                HorizontalPager(state = pagerState, modifier = Modifier.weight(1f).fillMaxWidth(), beyondViewportPageCount = 1) { page ->
                    when (pagerTabs[page]) {
                        RootDetailTab.MEANINGS -> MeaningsTab(meanings = uiState.meanings.meanings, selectedIds = uiState.meanings.selectedIds, isSelectionMode = uiState.meanings.isSelectionMode, isCopyingAll = copyMeaningsAll, isCopyingSelected = copyMeaningsSel, onCopyAll = { onEvent(RootDetailEvent.CopyAllMeanings) }, onShareAll = { onEvent(RootDetailEvent.ShareAllMeanings) }, onCopySelected = { onEvent(RootDetailEvent.CopySelectedMeanings) }, onShareSelected = { onEvent(RootDetailEvent.ShareSelectedMeanings) }, onSelectAll = { onEvent(RootDetailEvent.SelectAllMeanings) }, onClearSelection = { onEvent(RootDetailEvent.ClearMeaningSelection) }, onToggleSelection = { onEvent(RootDetailEvent.ToggleMeaning(it)) }, onEnterSelection = { onEvent(RootDetailEvent.EnterMeaningSelection(it)) }, listState = meaningsState)
                        RootDetailTab.MASADIR -> MasadirTab(masadir = detail.masadir, listState = masadirState)
                        RootDetailTab.DERIVATIVES -> DerivativesTab(derivatives = detail.derivatives, listState = derivativesState)
                        RootDetailTab.WORDS -> WordsTab(words = uiState.words.words, isLoading = uiState.words.isLoading, selectedIds = uiState.words.selectedIds, isSelectionMode = uiState.words.isSelectionMode, isCopyingSelected = copyWordsSel, onCopySelected = { onEvent(RootDetailEvent.CopySelectedWords) }, onShareSelected = { onEvent(RootDetailEvent.ShareSelectedWords) }, onSelectAll = { onEvent(RootDetailEvent.SelectAllWords) }, onClearSelection = { onEvent(RootDetailEvent.ClearWordSelection) }, onWordClick = { onEvent(RootDetailEvent.WordClicked(rootId, it)) }, onToggleSelection = { onEvent(RootDetailEvent.WordLongPressed(it)) }, onEnterSelection = { onEvent(RootDetailEvent.WordLongPressed(it)) }, listState = wordsState)
                        RootDetailTab.AYAT -> AyatTab(occurrences = uiState.ayat.occurrences, totalCount = uiState.ayat.totalCount, hasMore = uiState.ayat.hasMore, isLoadingMore = uiState.ayat.isLoadingMore, isCopyingAll = copyOccAll, onCopyAll = { onEvent(RootDetailEvent.CopyAllOccurrences) }, onShareAll = { onEvent(RootDetailEvent.ShareAllOccurrences) }, onOccurrenceClick = { s, a -> onEvent(RootDetailEvent.OccurrenceClicked(s, a)) }, onNearEnd = { onEvent(RootDetailEvent.AyatNearingEnd(it)) }, listState = ayatState)
                    }
                }
            }
            if (uiState.reportDialogVisible) {
                val samples = remember(uiState.ayat.occurrences) {
                    uiState.ayat.occurrences.take(2).map { ReportAyahSample(it.surahNameAr, it.ayahNum, it.textUthmani) }
                }
                ReportMeaningDialog(rootText = uiState.rootText, rootId = detail.item.id, aiSummary = detail.aiSummary ?: "", samples = samples, onDismissRequest = { onEvent(RootDetailEvent.DismissReport) }, onCopyReport = { onEvent(RootDetailEvent.CopyReport(it)) }, onShareReport = { onEvent(RootDetailEvent.ShareReport(it)) }, onOpenUrl = { onEvent(RootDetailEvent.OpenReportUrl(it)) })
            }
        }
    }
}
