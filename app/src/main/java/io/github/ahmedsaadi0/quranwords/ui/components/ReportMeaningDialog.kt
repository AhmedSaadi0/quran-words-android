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
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
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
                text = "الإبلاغ عن الملخص الذكي",
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
                    text = "الجذر: [ $rootText ]",
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
                            text = "المعنى المُبلغ عنه",
                            style = MaterialTheme.typography.labelMedium,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.primary
                        )
                        Text(
                            text = aiSummary.ifBlank { "لا يوجد ملخص ذكي معروض لهذا الجذر — يمكنك الإبلاغ عن غيابه." },
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurface
                        )
                    }
                }

                Text(
                    text = "نوع البلاغ",
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
                            label = { Text(type.ar) },
                            modifier = Modifier.testTag("report_type_${type.name}")
                        )
                    }
                }

                OutlinedTextField(
                    value = description,
                    onValueChange = { viewModel.setDescription(it.take(2000)) },
                    label = { Text("اشرح المشكلة *") },
                    placeholder = { Text("مثال: الملخص يذكر أن الجذر يدل على الكتابة بينما السياق القرآني يدل على…") },
                    minLines = 3,
                    maxLines = 8,
                    isError = description.isNotBlank() && !canSubmit,
                    supportingText = {
                        Text(
                            text = if (description.isNotBlank() && !canSubmit) {
                                "اشرح المشكلة بعشر أحرف على الأقل (${description.trim().length}/${MeaningReportLimits.MIN_DESCRIPTION})"
                            } else {
                                "${description.length}/${MeaningReportLimits.MAX_DESCRIPTION}"
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
                    label = { Text("التصحيح المقترح (اختياري)") },
                    placeholder = { Text("مثال: الصواب أن الملخص يذكر…") },
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
                            Toast.makeText(context, "تم نسخ نص البلاغ", Toast.LENGTH_SHORT).show()
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
                        Text("نسخ البلاغ")
                    }
                    OutlinedButton(
                        onClick = {
                            try {
                                val sendIntent = Intent(Intent.ACTION_SEND).apply {
                                    type = "text/plain"
                                    putExtra(Intent.EXTRA_TEXT, currentMarkdown())
                                }
                                context.startActivity(
                                    Intent.createChooser(sendIntent, "مشاركة البلاغ")
                                )
                            } catch (_: ActivityNotFoundException) {
                                Toast.makeText(context, "لا يوجد تطبيق للمشاركة", Toast.LENGTH_SHORT).show()
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
                        Text("مشاركة")
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
                Text("إرسال عبر GitHub")
            }
        },
        dismissButton = {
            TextButton(onClick = ::dismissAndReset) {
                Text("إلغاء")
            }
        }
    )
}
