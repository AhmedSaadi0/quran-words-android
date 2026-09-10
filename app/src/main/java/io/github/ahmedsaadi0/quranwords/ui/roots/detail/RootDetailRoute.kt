package io.github.ahmedsaadi0.quranwords.ui.roots.detail

import androidx.compose.material3.SnackbarHostState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.platform.LocalContext
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import io.github.ahmedsaadi0.quranwords.R
import io.github.ahmedsaadi0.quranwords.core.util.QuranCopyFormatter
import io.github.ahmedsaadi0.quranwords.ui.roots.detail.util.rememberShareHandler
import io.github.ahmedsaadi0.quranwords.ui.viewmodel.RootViewModel
import kotlinx.coroutines.launch

/**
 * Stateful entry point: the ONLY layer aware of the ViewModel, coroutine
 * scope and platform context. Collects [RootViewModel.uiState] once,
 * routes [RootDetailEvent] (state → VM, platform → ShareHandler) and
 * delegates one-shot [RootDetailEffect] navigation.
 */
@Composable
fun RootDetailRoute(
    rootId: Int,
    onNavigateBack: () -> Unit,
    onNavigateToSurahDetail: (Int, Int) -> Unit,
    onNavigateToWordAyat: (Int, Int) -> Unit = { _, _ -> },
    rootViewModel: RootViewModel = hiltViewModel(),
) {
    val uiState by rootViewModel.uiState.collectAsStateWithLifecycle()
    val scope = rememberCoroutineScope()
    val snackbarHostState = remember { SnackbarHostState() }
    val shareHandler = rememberShareHandler(snackbarHostState, scope)
    val context = LocalContext.current

    LaunchedEffect(rootId) {
        rootViewModel.onEvent(RootDetailEvent.Load(rootId))
    }

    LaunchedEffect(rootViewModel) {
        rootViewModel.effect.collect { effect ->
            when (effect) {
                is RootDetailEffect.NavigateToSurah ->
                    onNavigateToSurahDetail(effect.surahId, effect.ayahNum)
                is RootDetailEffect.NavigateToWordAyat ->
                    onNavigateToWordAyat(effect.rootId, effect.wordId)
            }
        }
    }

    RootDetailScreen(
        uiState = uiState,
        rootId = rootId,
        onEvent = { event -> handleRootDetailEvent(event, uiState, rootViewModel, shareHandler, context, scope) },
        onNavigateBack = onNavigateBack,
        snackbarHostState = snackbarHostState
    )
}

private fun handleRootDetailEvent(
    event: RootDetailEvent,
    uiState: RootDetailUiState,
    viewModel: RootViewModel,
    shareHandler: io.github.ahmedsaadi0.quranwords.ui.roots.detail.util.ShareHandler,
    context: android.content.Context,
    scope: kotlinx.coroutines.CoroutineScope,
) {
    when (event) {
        RootDetailEvent.CopyAiSummary -> {
            val formatted = QuranCopyFormatter.formatAiSummary(
                uiState.rootText, uiState.detail?.aiSummary ?: ""
            )
            shareHandler.copyImmediate(formatted, context.getString(R.string.ai_summary_copied))
        }
        RootDetailEvent.ShareAiSummary -> {
            val formatted = QuranCopyFormatter.formatAiSummary(
                uiState.rootText, uiState.detail?.aiSummary ?: ""
            )
            shareHandler.shareImmediate(
                formatted,
                context.getString(R.string.common_share),
                context.getString(R.string.no_share_app)
            )
        }
        RootDetailEvent.CopyAllMeanings -> scope.launch {
            shareHandler.copyFormatted(
                provider = { viewModel.getAllMeaningsFormatted(CopyAction.COPY_MEANINGS_ALL) },
                successMessage = context.resources.getQuantityString(
                    R.plurals.copied_meanings_count,
                    uiState.meanings.meanings.size, uiState.meanings.meanings.size
                ),
                emptyMessage = context.getString(R.string.no_meanings_copy)
            )
        }
        RootDetailEvent.ShareAllMeanings -> scope.launch {
            shareHandler.shareFormatted(
                provider = { viewModel.getAllMeaningsFormatted(CopyAction.SHARE_MEANINGS_ALL) },
                chooserTitle = context.getString(R.string.share_meanings),
                emptyMessage = context.getString(R.string.no_meanings_share),
                noAppMessage = context.getString(R.string.no_share_app)
            )
        }
        RootDetailEvent.CopySelectedMeanings -> scope.launch {
            val n = uiState.meanings.selectedIds.size
            shareHandler.copyFormatted(
                provider = { viewModel.getSelectedMeaningsFormatted(CopyAction.COPY_MEANINGS_SELECTED) },
                successMessage = context.resources.getQuantityString(R.plurals.copied_meanings_count, n, n),
                emptyMessage = context.getString(R.string.no_meanings_copy)
            )
        }
        RootDetailEvent.ShareSelectedMeanings -> scope.launch {
            shareHandler.shareFormatted(
                provider = { viewModel.getSelectedMeaningsFormatted(CopyAction.SHARE_MEANINGS_SELECTED) },
                chooserTitle = context.getString(R.string.share_meanings),
                emptyMessage = context.getString(R.string.no_meanings_share),
                noAppMessage = context.getString(R.string.no_share_app)
            )
        }
        RootDetailEvent.CopySelectedWords -> scope.launch {
            shareHandler.copyFormatted(
                provider = { viewModel.getSelectedWordsOccurrencesFormatted(CopyAction.COPY_WORDS_SELECTED) },
                successMessage = context.getString(R.string.root_copied_selected),
                emptyMessage = context.getString(R.string.no_ayat_copy)
            )
        }
        RootDetailEvent.ShareSelectedWords -> scope.launch {
            shareHandler.shareFormatted(
                provider = { viewModel.getSelectedWordsOccurrencesFormatted(CopyAction.SHARE_WORDS_SELECTED) },
                chooserTitle = context.getString(R.string.share_ayat),
                emptyMessage = context.getString(R.string.no_ayat_share),
                noAppMessage = context.getString(R.string.no_share_app)
            )
        }
        RootDetailEvent.CopyAllOccurrences -> scope.launch {
            val total = uiState.ayat.totalCount
            shareHandler.copyFormatted(
                provider = { viewModel.getAllOccurrencesFormatted(CopyAction.COPY_OCCURRENCES_ALL) },
                successMessage = context.resources.getQuantityString(R.plurals.copied_ayat_count, total, total),
                emptyMessage = context.getString(R.string.no_ayat_copy)
            )
        }
        RootDetailEvent.ShareAllOccurrences -> scope.launch {
            shareHandler.shareFormatted(
                provider = { viewModel.getAllOccurrencesFormatted(CopyAction.SHARE_OCCURRENCES_ALL) },
                chooserTitle = context.getString(R.string.share_ayat),
                emptyMessage = context.getString(R.string.no_ayat_share),
                noAppMessage = context.getString(R.string.no_share_app)
            )
        }
        is RootDetailEvent.CopyReport ->
            shareHandler.copyImmediate(event.markdown, context.getString(R.string.report_copied))
        is RootDetailEvent.ShareReport -> shareHandler.shareImmediate(
            event.markdown,
            context.getString(R.string.report_share_title),
            context.getString(R.string.no_share_app)
        )
        is RootDetailEvent.OpenReportUrl -> shareHandler.openUrl(event.url)
        // State + navigation-decision events go to the ViewModel (nav via Effect).
        else -> viewModel.onEvent(event)
    }
}
