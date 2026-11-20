package com.quranwords.ui.roots.detail

import androidx.compose.material3.SnackbarHostState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.platform.LocalContext
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.quranwords.R
import com.quranwords.core.util.MarkdownBlocks
import com.quranwords.core.util.QuranCopyFormatter
import com.quranwords.ui.components.ReportMeaningDialog
import com.quranwords.ui.components.ReportMeaningUiState
import com.quranwords.ui.roots.detail.report.ReportMeaningViewModel
import com.quranwords.ui.roots.detail.util.rememberShareHandler
import com.quranwords.util.ReportAyahSample
import com.quranwords.util.buildMeaningReportIssueUrl
import com.quranwords.util.buildMeaningReportMarkdown
import kotlinx.coroutines.launch

/**
 * Stateful entry point: the ONLY layer aware of the ViewModel, coroutine
 * scope and platform context. Collects [RootDetailViewModel.uiState] once,
 * routes [RootDetailEvent] (state → VM, platform → ShareHandler) and
 * delegates one-shot [RootDetailEffect] navigation.
 */
@Composable
fun RootDetailRoute(
    rootId: Int,
    onNavigateBack: () -> Unit,
    onNavigateToSurahDetail: (Int, Int) -> Unit,
    onNavigateToWordAyat: (Int, Int) -> Unit = { _, _ -> },
    viewModel: RootDetailViewModel = hiltViewModel(),
    reportViewModel: ReportMeaningViewModel = hiltViewModel(),
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    val reportType by reportViewModel.reportType.collectAsStateWithLifecycle()
    val reportDescription by reportViewModel.description.collectAsStateWithLifecycle()
    val reportSuggestion by reportViewModel.suggestion.collectAsStateWithLifecycle()
    val reportCanSubmit by reportViewModel.canSubmit.collectAsStateWithLifecycle()
    val scope = rememberCoroutineScope()
    val snackbarHostState = remember { SnackbarHostState() }
    val shareHandler = rememberShareHandler(snackbarHostState, scope)
    val context = LocalContext.current

    LaunchedEffect(rootId) {
        viewModel.onEvent(RootDetailEvent.Load(rootId))
    }

    LaunchedEffect(viewModel) {
        viewModel.effect.collect { effect ->
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
        onEvent = { event -> handleRootDetailEvent(event, uiState, viewModel, shareHandler, context, scope) },
        onNavigateBack = onNavigateBack,
        snackbarHostState = snackbarHostState
    )

    if (uiState.reportDialogVisible) {
        val detail = uiState.detail
        val samples = uiState.ayat.occurrences.take(2).map {
            ReportAyahSample(it.surahNameAr, it.ayahNum, it.textUthmani)
        }
        fun reportMarkdown(): String = buildMeaningReportMarkdown(
            reportViewModel.buildContent(
                rootText = uiState.rootText,
                rootId = detail?.item?.id,
                aiSummary = detail?.aiSummary.orEmpty(),
                samples = samples
            )
        )
        fun reportUrl(): String = buildMeaningReportIssueUrl(
            reportViewModel.buildContent(
                rootText = uiState.rootText,
                rootId = detail?.item?.id,
                // Raw markdown from the corpus would corrupt the issue template.
                aiSummary = detail?.aiSummary.orEmpty().let { text ->
                    if (MarkdownBlocks.hasMarkdown(text)) MarkdownBlocks.renderPlain(text) else text
                },
                samples = samples
            )
        )
        ReportMeaningDialog(
            rootText = uiState.rootText,
            aiSummary = detail?.aiSummary.orEmpty(),
            state = ReportMeaningUiState(reportType, reportDescription, reportSuggestion, reportCanSubmit),
            onReportTypeChange = reportViewModel::setReportType,
            onDescriptionChange = reportViewModel::setDescription,
            onSuggestionChange = reportViewModel::setSuggestion,
            onCopyReport = { shareHandler.copyImmediate(reportMarkdown(), context.getString(R.string.report_copied)) },
            onShareReport = { shareHandler.shareImmediate(
                reportMarkdown(), context.getString(R.string.report_share_title), context.getString(R.string.no_share_app)
            ) },
            onOpenUrl = { shareHandler.openUrl(reportUrl(), context.getString(R.string.no_browser)) },
            onDismiss = {
                reportViewModel.reset()
                viewModel.onEvent(RootDetailEvent.DismissReport)
            }
        )
    }
}

private fun handleRootDetailEvent(
    event: RootDetailEvent,
    uiState: RootDetailUiState,
    viewModel: RootDetailViewModel,
    shareHandler: com.quranwords.ui.roots.detail.util.ShareHandler,
    context: android.content.Context,
    scope: kotlinx.coroutines.CoroutineScope,
) {
    when (event) {
        is RootDetailEvent.CopyAiSection -> {
            val (title, section) = aiSectionFor(event.tag, uiState.detail?.aiExtended, context)
            val formatted = QuranCopyFormatter.formatAiSection(title, section)
            shareHandler.copyImmediate(formatted, context.getString(R.string.copied_text))
        }
        RootDetailEvent.CopyAllMeanings -> scope.launch {
            shareHandler.copyFormatted(
                provider = { viewModel.getAllMeaningsFormatted(CopyAction.COPY_MEANINGS_ALL) },
                successMessage = context.resources.getQuantityString(
                    R.plurals.copied_meanings_count,
                    uiState.meanings.meanings.size, uiState.meanings.meanings.size
                ),
                emptyMessage = context.getString(R.string.no_meanings_copy),
                failureMessage = context.getString(R.string.copy_failed)
            )
        }
        RootDetailEvent.ShareAllMeanings -> scope.launch {
            shareHandler.shareFormatted(
                provider = { viewModel.getAllMeaningsFormatted(CopyAction.SHARE_MEANINGS_ALL) },
                chooserTitle = context.getString(R.string.share_meanings),
                emptyMessage = context.getString(R.string.no_meanings_share),
                noAppMessage = context.getString(R.string.no_share_app),
                failureMessage = context.getString(R.string.share_failed)
            )
        }
        RootDetailEvent.CopySelectedMeanings -> scope.launch {
            val n = uiState.meanings.selectedIds.size
            shareHandler.copyFormatted(
                provider = { viewModel.getSelectedMeaningsFormatted(CopyAction.COPY_MEANINGS_SELECTED) },
                successMessage = context.resources.getQuantityString(R.plurals.copied_meanings_count, n, n),
                emptyMessage = context.getString(R.string.no_meanings_copy),
                failureMessage = context.getString(R.string.copy_failed)
            )
        }
        RootDetailEvent.ShareSelectedMeanings -> scope.launch {
            shareHandler.shareFormatted(
                provider = { viewModel.getSelectedMeaningsFormatted(CopyAction.SHARE_MEANINGS_SELECTED) },
                chooserTitle = context.getString(R.string.share_meanings),
                emptyMessage = context.getString(R.string.no_meanings_share),
                noAppMessage = context.getString(R.string.no_share_app),
                failureMessage = context.getString(R.string.share_failed)
            )
        }
        RootDetailEvent.CopySelectedWords -> scope.launch {
            shareHandler.copyFormatted(
                provider = { viewModel.getSelectedWordsOccurrencesFormatted(CopyAction.COPY_WORDS_SELECTED) },
                successMessage = context.getString(R.string.root_copied_selected),
                emptyMessage = context.getString(R.string.no_ayat_copy),
                failureMessage = context.getString(R.string.copy_failed)
            )
        }
        RootDetailEvent.ShareSelectedWords -> scope.launch {
            shareHandler.shareFormatted(
                provider = { viewModel.getSelectedWordsOccurrencesFormatted(CopyAction.SHARE_WORDS_SELECTED) },
                chooserTitle = context.getString(R.string.share_ayat),
                emptyMessage = context.getString(R.string.no_ayat_share),
                noAppMessage = context.getString(R.string.no_share_app),
                failureMessage = context.getString(R.string.share_failed)
            )
        }
        RootDetailEvent.CopyAllOccurrences -> scope.launch {
            val total = uiState.ayat.totalCount
            shareHandler.copyFormatted(
                provider = { viewModel.getAllOccurrencesFormatted(CopyAction.COPY_OCCURRENCES_ALL) },
                successMessage = context.resources.getQuantityString(R.plurals.copied_ayat_count, total, total),
                emptyMessage = context.getString(R.string.no_ayat_copy),
                failureMessage = context.getString(R.string.copy_failed)
            )
        }
        RootDetailEvent.ShareAllOccurrences -> scope.launch {
            shareHandler.shareFormatted(
                provider = { viewModel.getAllOccurrencesFormatted(CopyAction.SHARE_OCCURRENCES_ALL) },
                chooserTitle = context.getString(R.string.share_ayat),
                emptyMessage = context.getString(R.string.no_ayat_share),
                noAppMessage = context.getString(R.string.no_share_app),
                failureMessage = context.getString(R.string.share_failed)
            )
        }
        is RootDetailEvent.CopyReport ->
            shareHandler.copyImmediate(event.markdown, context.getString(R.string.report_copied))
        is RootDetailEvent.ShareReport -> shareHandler.shareImmediate(
            event.markdown,
            context.getString(R.string.report_share_title),
            context.getString(R.string.no_share_app)
        )
        is RootDetailEvent.OpenReportUrl -> shareHandler.openUrl(
            event.url, context.getString(R.string.no_browser)
        )
        // State + navigation-decision events go to the ViewModel (nav via Effect).
        else -> viewModel.onEvent(event)
    }
}

/** Tag → (display title, section) for per-card copy payloads. */
private fun aiSectionFor(
    tag: String,
    extended: com.quranwords.domain.model.AiExtendedSummary?,
    context: android.content.Context,
): Pair<String, com.quranwords.domain.model.AiExtendedSection?> = when (tag) {
    "hissi" -> context.getString(R.string.ai_section_hissi) to extended?.hissi
    "tatawwur" -> context.getString(R.string.ai_section_tatawwur) to extended?.tatawwur
    "furuq" -> context.getString(R.string.ai_section_furuq) to extended?.furuq
    else -> "" to null
}
