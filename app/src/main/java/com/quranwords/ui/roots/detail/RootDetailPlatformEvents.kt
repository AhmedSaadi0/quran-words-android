package com.quranwords.ui.roots.detail

import android.content.Context
import com.quranwords.R
import com.quranwords.core.util.QuranCopyFormatter
import com.quranwords.domain.model.AiExtendedSection
import com.quranwords.domain.model.AiExtendedSummary
import com.quranwords.ui.roots.detail.util.ShareHandler
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.launch

/** Platform event routing for RootDetail, split from the Route shell (P3-7). */
internal fun handleRootDetailEvent(
    event: RootDetailEvent,
    uiState: RootDetailUiState,
    viewModel: RootDetailViewModel,
    shareHandler: ShareHandler,
    context: Context,
    scope: CoroutineScope
) {
    when (event) {
        is RootDetailEvent.CopyAiSection -> {
            val (title, section) = aiSectionFor(event.tag, uiState.detail?.aiExtended, context)
            shareHandler.copyImmediate(
                QuranCopyFormatter.formatAiSection(title, section),
                context.getString(R.string.copied_text)
            )
        }
        is RootDetailEvent.CopyReport,
        is RootDetailEvent.ShareReport,
        is RootDetailEvent.OpenReportUrl ->
            handleReportActions(event, shareHandler, context)
        is RootDetailEvent.CopyAllMeanings,
        RootDetailEvent.ShareAllMeanings,
        RootDetailEvent.CopySelectedMeanings,
        RootDetailEvent.ShareSelectedMeanings ->
            handleMeaningsCopyShare(event, uiState, viewModel, shareHandler, context, scope)
        RootDetailEvent.CopySelectedWords,
        RootDetailEvent.ShareSelectedWords ->
            handleWordsCopyShare(event, viewModel, shareHandler, context, scope)
        RootDetailEvent.CopyAllOccurrences,
        RootDetailEvent.ShareAllOccurrences ->
            handleOccurrencesCopyShare(event, uiState, viewModel, shareHandler, context, scope)
        else -> viewModel.onEvent(event)
    }
}

private fun handleReportActions(
    event: RootDetailEvent,
    shareHandler: ShareHandler,
    context: Context
) = when (event) {
    is RootDetailEvent.CopyReport -> shareHandler.copyImmediate(
        event.markdown,
        context.getString(R.string.report_copied)
    )
    is RootDetailEvent.ShareReport -> shareHandler.shareImmediate(
        event.markdown,
        context.getString(R.string.report_share_title),
        context.getString(R.string.no_share_app)
    )
    is RootDetailEvent.OpenReportUrl -> shareHandler.openUrl(
        event.url,
        context.getString(R.string.no_browser)
    )
    else -> Unit
}

private fun handleMeaningsCopyShare(
    event: RootDetailEvent,
    uiState: RootDetailUiState,
    viewModel: RootDetailViewModel,
    shareHandler: ShareHandler,
    context: Context,
    scope: CoroutineScope
) {
    when (event) {
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
            val count = uiState.meanings.selectedIds.size
            shareHandler.copyFormatted(
                provider = { viewModel.getSelectedMeaningsFormatted(CopyAction.COPY_MEANINGS_SELECTED) },
                successMessage = context.resources.getQuantityString(
                    R.plurals.copied_meanings_count, count, count
                ),
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
        else -> Unit
    }
}

private fun handleWordsCopyShare(
    event: RootDetailEvent,
    viewModel: RootDetailViewModel,
    shareHandler: ShareHandler,
    context: Context,
    scope: CoroutineScope
) {
    when (event) {
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
        else -> Unit
    }
}

private fun handleOccurrencesCopyShare(
    event: RootDetailEvent,
    uiState: RootDetailUiState,
    viewModel: RootDetailViewModel,
    shareHandler: ShareHandler,
    context: Context,
    scope: CoroutineScope
) {
    when (event) {
        RootDetailEvent.CopyAllOccurrences -> scope.launch {
            val total = uiState.ayat.totalCount
            shareHandler.copyFormatted(
                provider = { viewModel.getAllOccurrencesFormatted(CopyAction.COPY_OCCURRENCES_ALL) },
                successMessage = context.resources.getQuantityString(
                    R.plurals.copied_ayat_count, total, total
                ),
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
        else -> Unit
    }
}

/** Tag → (display title, section) for per-card copy payloads. */
private fun aiSectionFor(
    tag: String,
    extended: AiExtendedSummary?,
    context: Context
): Pair<String, AiExtendedSection?> = when (tag) {
    "hissi" -> context.getString(R.string.ai_section_hissi) to extended?.hissi
    "tatawwur" -> context.getString(R.string.ai_section_tatawwur) to extended?.tatawwur
    "furuq" -> context.getString(R.string.ai_section_furuq) to extended?.furuq
    else -> "" to null
}
