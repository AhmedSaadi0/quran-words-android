package com.quranwords.ui.components

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.outlined.OpenInNew
import androidx.compose.material.icons.outlined.ContentCopy
import androidx.compose.material.icons.outlined.Flag
import androidx.compose.material.icons.outlined.Share
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.quranwords.R
import com.quranwords.core.util.MarkdownBlocks
import com.quranwords.util.MeaningReportLimits
import com.quranwords.util.MeaningReportType

/**
 * حوار البلاغ عن الملخص الذكي: نوع البلاغ + وصف + تصحيح مقترح.
 * البلاغ مرتبط دائمًا بالمعنى المكتوب بالذكاء الاصطناعي (يُعرض للمراجعة أعلى النموذج)،
 * ويُنتج نصًا غنيًا يُرسل عبر GitHub أو النسخ/المشاركة.
 *
 * Platform side-effects (clipboard, share sheet, browser) are hoisted to the
 * caller via [onCopyReport]/[onShareReport]/[onOpenUrl] (Phase 2c) so this
 * dialog is fully stateless (P2-4): the Route owns the ViewModel and supplies
 * plain state + callbacks.
 */
data class ReportMeaningUiState(
    val reportType: MeaningReportType = MeaningReportType.INCORRECT,
    val description: String = "",
    val suggestion: String = "",
    val canSubmit: Boolean = false
)

@OptIn(ExperimentalLayoutApi::class)
@Composable
fun ReportMeaningDialog(
    rootText: String,
    aiSummary: String,
    state: ReportMeaningUiState,
    onReportTypeChange: (MeaningReportType) -> Unit,
    onDescriptionChange: (String) -> Unit,
    onSuggestionChange: (String) -> Unit,
    onCopyReport: () -> Unit,
    onShareReport: () -> Unit,
    onOpenUrl: () -> Unit,
    onDismiss: () -> Unit,
    modifier: Modifier = Modifier
) {
    val reportType = state.reportType
    val description = state.description
    val suggestion = state.suggestion
    val canSubmit = state.canSubmit

    fun dismissAndReset() = onDismiss()

    AlertDialog(
        onDismissRequest = ::dismissAndReset,
        modifier = modifier,
        icon = {
            Icon(
                imageVector = Icons.Outlined.Flag,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.primary
            )
        },
        title = {
            Text(
                text = stringResource(R.string.report_title),
                style = MaterialTheme.typography.headlineSmall
            )
        },
        text = {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .verticalScroll(rememberScrollState()),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                Text(
                    // rootText is Arabic reference data (never translated).
                    text = stringResource(R.string.report_root_template, rootText),
                    style = MaterialTheme.typography.titleSmall,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onSurface
                )

                Card(
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("report_target_card"),
                    colors = CardDefaults.cardColors(
                        containerColor = MaterialTheme.colorScheme.secondaryContainer.copy(alpha = 0.5f)
                    ),
                    shape = RoundedCornerShape(12.dp),
                    border = CardDefaults.outlinedCardBorder()
                ) {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(12.dp),
                        verticalArrangement = Arrangement.spacedBy(4.dp)
                    ) {
                        Text(
                            text = stringResource(R.string.report_target),
                            style = MaterialTheme.typography.labelMedium,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.primary
                        )
                        Text(
                            // aiSummary is Arabic reference data; only the empty fallback is chrome.
                            // Markdown markers are stripped so the reported text is clean.
                            text = remember(aiSummary) {
                                aiSummary.takeIf { MarkdownBlocks.hasMarkdown(it) }
                                    ?.let { MarkdownBlocks.renderPlain(it) }
                                    ?: aiSummary
                            }.ifBlank { stringResource(R.string.report_no_summary) },
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurface
                        )
                    }
                }

                Text(
                    text = stringResource(R.string.report_type_label),
                    style = MaterialTheme.typography.labelLarge,
                    fontWeight = FontWeight.Bold
                )
                FlowRow(
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    MeaningReportType.entries.forEach { type ->
                        FilterChip(
                            selected = reportType == type,
                            onClick = { onReportTypeChange(type) },
                            label = { Text(stringResource(type.labelRes)) },
                            modifier = Modifier.testTag("report_type_${type.name}")
                        )
                    }
                }

                OutlinedTextField(
                    value = description,
                    onValueChange = { onDescriptionChange(it.take(2000)) },
                    label = { Text(stringResource(R.string.report_desc_label)) },
                    placeholder = { Text(stringResource(R.string.report_desc_hint)) },
                    minLines = 3,
                    maxLines = 8,
                    isError = description.isNotBlank() && !canSubmit,
                    supportingText = {
                        Text(
                            text = if (description.isNotBlank() && !canSubmit) {
                                stringResource(
                                    R.string.report_desc_error,
                                    description.trim().length,
                                    MeaningReportLimits.MIN_DESCRIPTION
                                )
                            } else {
                                stringResource(
                                    R.string.report_desc_count,
                                    description.length,
                                    MeaningReportLimits.MAX_DESCRIPTION
                                )
                            }
                        )
                    },
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("report_description_field")
                )

                OutlinedTextField(
                    value = suggestion,
                    onValueChange = { onSuggestionChange(it.take(2000)) },
                    label = { Text(stringResource(R.string.report_suggest_label)) },
                    placeholder = { Text(stringResource(R.string.report_suggest_hint)) },
                    minLines = 2,
                    maxLines = 6,
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("report_suggestion_field")
                )

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    OutlinedButton(
                        onClick = onCopyReport,
                        enabled = canSubmit,
                        modifier = Modifier
                            .weight(1f)
                            .testTag("copy_report_btn")
                    ) {
                        Icon(
                            imageVector = Icons.Outlined.ContentCopy,
                            contentDescription = null
                        )
                        Text(stringResource(R.string.report_copy))
                    }
                    OutlinedButton(
                        onClick = onShareReport,
                        enabled = canSubmit,
                        modifier = Modifier
                            .weight(1f)
                            .testTag("share_report_btn")
                    ) {
                        Icon(
                            imageVector = Icons.Outlined.Share,
                            contentDescription = null
                        )
                        Text(stringResource(R.string.common_share))
                    }
                }
            }
        },
        confirmButton = {
            Button(
                onClick = {
                    onOpenUrl()
                    dismissAndReset()
                },
                enabled = canSubmit,
                modifier = Modifier.testTag("open_github_report_btn")
            ) {
                Icon(
                    imageVector = Icons.AutoMirrored.Outlined.OpenInNew,
                    contentDescription = null
                )
                Text(stringResource(R.string.report_github))
            }
        },
        dismissButton = {
            TextButton(onClick = ::dismissAndReset) {
                Text(stringResource(R.string.cd_cancel))
            }
        }
    )
}
