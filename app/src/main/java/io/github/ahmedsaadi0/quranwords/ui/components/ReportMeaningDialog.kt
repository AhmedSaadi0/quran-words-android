package io.github.ahmedsaadi0.quranwords.ui.components

import android.content.ActivityNotFoundException
import android.content.Intent
import android.widget.Toast
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
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalClipboardManager
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import io.github.ahmedsaadi0.quranwords.R
import io.github.ahmedsaadi0.quranwords.ui.viewmodel.ReportMeaningViewModel
import io.github.ahmedsaadi0.quranwords.util.MeaningReportLimits
import io.github.ahmedsaadi0.quranwords.util.MeaningReportType
import io.github.ahmedsaadi0.quranwords.util.ReportAyahSample
import io.github.ahmedsaadi0.quranwords.util.buildMeaningReportIssueUrl
import io.github.ahmedsaadi0.quranwords.util.buildMeaningReportMarkdown

/**
 * حوار البلاغ عن الملخص الذكي: نوع البلاغ + وصف + تصحيح مقترح.
 * البلاغ مرتبط دائمًا بالمعنى المكتوب بالذكاء الاصطناعي (يُعرض للمراجعة أعلى النموذج)،
 * ويُنتج نصًا غنيًا يُرسل عبر GitHub أو النسخ/المشاركة.
 */
@OptIn(ExperimentalLayoutApi::class)
@Composable
fun ReportMeaningDialog(
    rootText: String,
    rootId: Int?,
    aiSummary: String,
    samples: List<ReportAyahSample>,
    onDismissRequest: () -> Unit,
    viewModel: ReportMeaningViewModel = hiltViewModel(),
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val clipboardManager = LocalClipboardManager.current

    val reportType by viewModel.reportType.collectAsState()
    val description by viewModel.description.collectAsState()
    val suggestion by viewModel.suggestion.collectAsState()
    val canSubmit by viewModel.canSubmit.collectAsState()

    fun currentMarkdown(): String {
        val content = viewModel.buildContent(
            rootText = rootText,
            rootId = rootId,
            aiSummary = aiSummary,
            samples = samples
        )
        return buildMeaningReportMarkdown(content)
    }

    fun dismissAndReset() {
        viewModel.reset()
        onDismissRequest()
    }

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
                            text = aiSummary.ifBlank { context.getString(R.string.report_no_summary) },
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
                            onClick = { viewModel.setReportType(type) },
                            label = { Text(stringResource(type.labelRes)) },
                            modifier = Modifier.testTag("report_type_${type.name}")
                        )
                    }
                }

                OutlinedTextField(
                    value = description,
                    onValueChange = { viewModel.setDescription(it.take(2000)) },
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
                    onValueChange = { viewModel.setSuggestion(it.take(2000)) },
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
                        onClick = {
                            clipboardManager.setText(AnnotatedString(currentMarkdown()))
                            Toast.makeText(context, context.getString(R.string.report_copied), Toast.LENGTH_SHORT).show()
                        },
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
                        onClick = {
                            try {
                                val sendIntent = Intent(Intent.ACTION_SEND).apply {
                                    type = "text/plain"
                                    putExtra(Intent.EXTRA_TEXT, currentMarkdown())
                                }
                                context.startActivity(
                                    Intent.createChooser(sendIntent, context.getString(R.string.report_share_title))
                                )
                            } catch (_: ActivityNotFoundException) {
                                Toast.makeText(context, context.getString(R.string.no_share_app), Toast.LENGTH_SHORT).show()
                            }
                        },
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
                    try {
                        val content = viewModel.buildContent(
                            rootText = rootText,
                            rootId = rootId,
                            aiSummary = aiSummary,
                            samples = samples
                        )
                        val url = buildMeaningReportIssueUrl(content)
                        context.startActivity(
                            Intent(
                                Intent.ACTION_VIEW,
                                android.net.Uri.parse(url)
                            )
                        )
                    } catch (_: ActivityNotFoundException) {
                    }
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
