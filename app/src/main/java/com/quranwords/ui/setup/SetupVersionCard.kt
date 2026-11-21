package com.quranwords.ui.setup

import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.quranwords.R
import com.quranwords.domain.model.DbReleaseInfo
import com.quranwords.ui.components.formattedFileSize
import com.quranwords.ui.theme.ShapeMedium
@Composable
internal fun VersionStatusCard(
    installedCode: Int,
    installedName: String,
    latestRelease: DbReleaseInfo?,
    isChecking: Boolean,
    manifestError: String?,
    showNotes: Boolean,
    onToggleNotes: () -> Unit,
    onCheckClick: () -> Unit
) {
    Card(
        modifier = Modifier.fillMaxWidth().testTag("db_version_card"),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        shape = ShapeMedium,
        border = CardDefaults.outlinedCardBorder()
    ) {
        Column(
            modifier = Modifier.fillMaxWidth().padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = stringResource(R.string.db_version_status),
                    style = MaterialTheme.typography.titleSmall,
                    fontWeight = FontWeight.Bold
                )
                OutlinedButton(
                    onClick = onCheckClick,
                    enabled = !isChecking,
                    modifier = Modifier.testTag("check_update_button")
                ) {
                    Text(if (isChecking) stringResource(R.string.db_checking) else stringResource(R.string.db_check_update))
                }
            }
            Text(
                text = if (installedCode > 0) stringResource(R.string.db_installed_known, installedName, installedCode)
                else stringResource(R.string.db_installed_unknown),
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            if (latestRelease != null) {
                val isNew = latestRelease.versionCode > installedCode && installedCode > 0
                val latestLine = stringResource(
                    R.string.db_latest, latestRelease.versionName, latestRelease.versionCode
                ) + if (isNew) stringResource(R.string.db_update_avail_tag) else ""
                Text(
                    text = latestLine,
                    style = MaterialTheme.typography.bodySmall,
                    fontWeight = if (isNew) FontWeight.Bold else FontWeight.Normal,
                    color = if (isNew) MaterialTheme.colorScheme.primary
                    else MaterialTheme.colorScheme.onSurfaceVariant
                )
                Text(
                    text = stringResource(R.string.db_update_size, formattedFileSize(latestRelease.compressedSize)) +
                        (if (latestRelease.uncompressedSize > 0)
                            stringResource(R.string.db_size_after, formattedFileSize(latestRelease.uncompressedSize)) else ""),
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                if (latestRelease.releaseNotesAr.isNotBlank()) {
                    TextButton(onClick = onToggleNotes, modifier = Modifier.testTag("toggle_release_notes_btn")) {
                        Text(if (showNotes) stringResource(R.string.db_hide_notes) else stringResource(R.string.db_show_notes))
                    }
                    if (showNotes) {
                        Text(
                            text = latestRelease.releaseNotesAr,
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurface,
                            modifier = Modifier.testTag("release_notes_text")
                        )
                    }
                }
            } else if (!isChecking) {
                Text(
                    text = manifestError ?: stringResource(R.string.db_manifest_error),
                    style = MaterialTheme.typography.bodySmall,
                    color = if (manifestError != null) MaterialTheme.colorScheme.error
                    else MaterialTheme.colorScheme.onSurfaceVariant
                )
                if (manifestError != null) {
                    TextButton(onClick = onCheckClick, modifier = Modifier.testTag("manifest_retry_btn")) {
                        Text(stringResource(R.string.common_retry))
                    }
                }
            }
        }
    }
}
