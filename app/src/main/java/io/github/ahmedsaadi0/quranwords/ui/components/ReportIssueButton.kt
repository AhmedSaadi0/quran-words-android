package io.github.ahmedsaadi0.quranwords.ui.components

import android.content.Intent
import android.net.Uri
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.OpenInNew
import androidx.compose.material.icons.filled.Flag
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import io.github.ahmedsaadi0.quranwords.util.BuildIssueUrlOptions
import io.github.ahmedsaadi0.quranwords.util.buildGithubIssueUrl

@Composable
fun ReportIssueButton(
    rootText: String,
    rootId: Int? = null,
    pageUrl: String? = null,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val href = remember(rootText, rootId, pageUrl) {
        buildGithubIssueUrl(BuildIssueUrlOptions(rootText = rootText, rootId = rootId, pageUrl = pageUrl))
    }

    OutlinedButton(
        onClick = {
            val intent = Intent(Intent.ACTION_VIEW, Uri.parse(href))
            context.startActivity(intent)
        },
        modifier = modifier.testTag("report_issue_button_${rootText}"),
        shape = RoundedCornerShape(8.dp)
    ) {
        androidx.compose.material3.Icon(
            imageVector = Icons.Default.Flag,
            contentDescription = null,
            modifier = Modifier.padding(end = 6.dp),
            tint = MaterialTheme.colorScheme.primary
        )
        Text(
            text = "الإبلاغ عن معنى",
            style = MaterialTheme.typography.labelMedium,
            fontWeight = FontWeight.Medium
        )
        androidx.compose.material3.Icon(
            imageVector = Icons.AutoMirrored.Filled.OpenInNew,
            contentDescription = null,
            modifier = Modifier.padding(start = 6.dp),
            tint = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.6f)
        )
    }
}

@Composable
fun ReportIssueCard(
    rootText: String,
    rootId: Int? = null,
    modifier: Modifier = Modifier
) {
    Card(
        modifier = modifier
            .fillMaxWidth()
            .testTag("report_issue_card_${rootText}"),
        shape = RoundedCornerShape(12.dp),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.tertiaryContainer.copy(alpha = 0.18f)
        ),
        border = androidx.compose.foundation.BorderStroke(
            width = 1.dp,
            color = MaterialTheme.colorScheme.tertiary.copy(alpha = 0.45f)
        )
    ) {
        // Mimics web: rounded-xl border border-dashed bg-amber-50/60 px-4 py-4 flex-col gap-3 sm:flex-row
        // Compose doesn't have native dashed, using solid with alpha to approximate; layout is Row on large screens equivalent
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column(
                    modifier = Modifier.weight(1f),
                    verticalArrangement = Arrangement.spacedBy(4.dp)
                ) {
                    Text(
                        text = "هل وجدت معنى غير صحيح أو ناقص؟",
                        style = MaterialTheme.typography.titleSmall,
                        fontWeight = FontWeight.Medium,
                        color = MaterialTheme.colorScheme.onSurface,
                        lineHeight = 18.sp
                    )
                    Text(
                        text = "ساهم في تحسين البيانات — سيُفتح نموذج بلاغ جاهز على GitHub.",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        lineHeight = 16.sp
                    )
                    Text(
                        text = "/ Found an issue? Report on GitHub.",
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.7f)
                    )
                }
                Box(modifier = Modifier.padding(start = 12.dp)) {
                    ReportIssueButton(
                        rootText = rootText,
                        rootId = rootId,
                        modifier = Modifier
                    )
                }
            }
        }
    }
}
