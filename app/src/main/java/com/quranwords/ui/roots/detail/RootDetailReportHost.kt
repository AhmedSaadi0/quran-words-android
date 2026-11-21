package com.quranwords.ui.roots.detail

import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.platform.LocalContext
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.quranwords.R
import com.quranwords.core.util.MarkdownBlocks
import com.quranwords.ui.components.ReportMeaningDialog
import com.quranwords.ui.components.ReportMeaningUiState
import com.quranwords.ui.roots.detail.report.ReportMeaningViewModel
import com.quranwords.ui.roots.detail.util.ShareHandler
import com.quranwords.util.ReportAyahSample
import com.quranwords.util.buildMeaningReportIssueUrl
import com.quranwords.util.buildMeaningReportMarkdown
import kotlinx.coroutines.CoroutineScope

/** Report dialog plus its platform handlers, isolated from the Route (P3-7). */
@Composable
internal fun RootDetailReportDialog(
    uiState: RootDetailUiState,
    viewModel: RootDetailViewModel,
    reportViewModel: ReportMeaningViewModel,
    shareHandler: ShareHandler,
    scope: CoroutineScope
) {
    val context = LocalContext.current
    val reportType by reportViewModel.reportType.collectAsStateWithLifecycle()
    val description by reportViewModel.description.collectAsStateWithLifecycle()
    val suggestion by reportViewModel.suggestion.collectAsStateWithLifecycle()
    val canSubmit by reportViewModel.canSubmit.collectAsStateWithLifecycle()
    val detail = uiState.detail
    val samples = uiState.ayat.occurrences.take(2).map {
        ReportAyahSample(it.surahNameAr, it.ayahNum, it.textUthmani)
    }

    fun markdown(): String = buildMeaningReportMarkdown(
        reportViewModel.buildContent(
            rootText = uiState.rootText,
            rootId = detail?.item?.id,
            aiSummary = detail?.aiSummary.orEmpty(),
            samples = samples
        )
    )

    fun url(): String = buildMeaningReportIssueUrl(
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
        state = ReportMeaningUiState(reportType, description, suggestion, canSubmit),
        onReportTypeChange = reportViewModel::setReportType,
        onDescriptionChange = reportViewModel::setDescription,
        onSuggestionChange = reportViewModel::setSuggestion,
        onCopyReport = { shareHandler.copyImmediate(markdown(), context.getString(R.string.report_copied)) },
        onShareReport = {
            shareHandler.shareImmediate(
                markdown(),
                context.getString(R.string.report_share_title),
                context.getString(R.string.no_share_app)
            )
        },
        onOpenUrl = { shareHandler.openUrl(url(), context.getString(R.string.no_browser)) },
        onDismiss = {
            reportViewModel.reset()
            viewModel.onEvent(RootDetailEvent.DismissReport)
        }
    )
}
