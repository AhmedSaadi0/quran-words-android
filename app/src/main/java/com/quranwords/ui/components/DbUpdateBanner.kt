package com.quranwords.ui.components

import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowUpward
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.quranwords.R
import com.quranwords.domain.model.DbReleaseInfo
import com.quranwords.ui.theme.ShapeMedium
import com.quranwords.ui.theme.ShapeSmall
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
                Icon(
                    imageVector = Icons.Filled.ArrowUpward,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.primary,
                    modifier = Modifier.testTag("db_update_icon")
                )
                Text(
                    text = stringResource(R.string.db_update_title, info.versionName),
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onSurface,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                    modifier = Modifier.weight(1f)
                )
            }
            val installedLine = if (installedName.isNotBlank()) {
                stringResource(R.string.db_update_installed, installedName)
            } else null
            val downloadLine = stringResource(R.string.db_update_download, formattedFileSize(info.compressedSize))
            val uncompressedLine = if (info.uncompressedSize > 0) {
                stringResource(R.string.db_update_uncompressed, formattedFileSize(info.uncompressedSize))
            } else null
            Text(
                text = buildString {
                    installedLine?.let(::append)
                    append(downloadLine)
                    uncompressedLine?.let { append(" ").append(it) }
                },
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            // Release notes are Arabic reference data from the manifest — never translated.
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
                    Text(stringResource(R.string.db_update_now))
                }
                TextButton(
                    onClick = onDismissClick,
                    modifier = Modifier.testTag("db_update_later_btn")
                ) {
                    Text(stringResource(R.string.db_update_later))
                }
            }
        }
    }
}
