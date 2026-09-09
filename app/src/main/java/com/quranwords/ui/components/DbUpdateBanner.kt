package com.quranwords.ui.components

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.quranwords.domain.model.DbReleaseInfo
import com.quranwords.ui.theme.ShapeMedium
import com.quranwords.ui.theme.ShapeSmall

fun formatDbSize(bytes: Long): String {
    if (bytes <= 0) return "—"
    val mb = bytes / (1024 * 1024)
    if (mb >= 1) return "$mb ميجابايت"
    val kb = bytes / 1024
    return "$kb ك.ب"
}

@Composable
fun DbUpdateBanner(
    info: DbReleaseInfo,
    installedName: String,
    onUpdateClick: () -> Unit,
    onDismissClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    Card(
        modifier = modifier
            .fillMaxWidth()
            .testTag("db_update_banner"),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.tertiaryContainer.copy(alpha = 0.45f)
        ),
        shape = ShapeMedium,
        border = CardDefaults.outlinedCardBorder()
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Text("⬆️", style = MaterialTheme.typography.titleMedium)
                Text(
                    text = "تحديث جديد لقاعدة البيانات ${info.versionName}",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onSurface,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                    modifier = Modifier.weight(1f)
                )
            }
            Text(
                text = buildString {
                    if (installedName.isNotBlank()) append("المثبتة: $installedName • ")
                    append("التنزيل: ${formatDbSize(info.compressedSize)}")
                    if (info.uncompressedSize > 0) append(" (بعد الفك ${formatDbSize(info.uncompressedSize)})")
                },
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            if (info.releaseNotesAr.isNotBlank()) {
                Text(
                    text = info.releaseNotesAr.take(220),
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    maxLines = 3,
                    overflow = TextOverflow.Ellipsis
                )
            }
            Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                Button(
                    onClick = onUpdateClick,
                    shape = ShapeSmall,
                    colors = ButtonDefaults.buttonColors(
                        containerColor = MaterialTheme.colorScheme.tertiary
                    ),
                    modifier = Modifier.testTag("db_update_now_btn")
                ) {
                    Text("التحديث الآن")
                }
                TextButton(
                    onClick = onDismissClick,
                    modifier = Modifier.testTag("db_update_later_btn")
                ) {
                    Text("لاحقًا")
                }
            }
        }
    }
}
