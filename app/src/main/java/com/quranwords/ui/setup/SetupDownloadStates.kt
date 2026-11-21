package com.quranwords.ui.setup

import androidx.compose.animation.Crossfade
import androidx.compose.animation.animateContentSize
import androidx.compose.animation.core.tween
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.quranwords.R
import com.quranwords.core.util.SizeUnit
import com.quranwords.core.util.formatFileSize
import com.quranwords.domain.model.DbInstalledVersion
import com.quranwords.domain.model.DbReleaseInfo
import com.quranwords.ui.components.formattedFileSize
import com.quranwords.ui.theme.AppMotion
import com.quranwords.ui.theme.ShapeMedium
import com.quranwords.ui.theme.ShapeSmall
/** The five download states, isolated from the setup shell (P3-6). */
@Composable
internal fun SetupDownloadStates(
    state: SetupDownload,
    latestRelease: DbReleaseInfo?,
    installedVersion: DbInstalledVersion,
    onEvent: (SetupEvent) -> Unit,
    onNavigateBack: () -> Unit
) {
    Card(
        modifier = Modifier.fillMaxWidth()
            .animateContentSize(animationSpec = tween(AppMotion.DurationMedium)),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        shape = ShapeMedium,
        border = CardDefaults.outlinedCardBorder()
    ) {
        Column(
            modifier = Modifier.fillMaxWidth().padding(20.dp)
                .animateContentSize(animationSpec = tween(AppMotion.DurationMedium)),
            verticalArrangement = Arrangement.spacedBy(16.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            val stateKey = when (state) {
                is SetupDownload.Idle -> 0
                is SetupDownload.Progress -> 1
                is SetupDownload.Extracting -> 2
                is SetupDownload.Completed -> 3
                is SetupDownload.Error -> 4
            }
            Crossfade(
                targetState = stateKey,
                animationSpec = tween(AppMotion.DurationMedium),
                label = "downloadStateCrossfade"
            ) {
                when (state) {
                    is SetupDownload.Idle -> IdleState(onEvent)
                    is SetupDownload.Progress -> ProgressState(state)
                    is SetupDownload.Extracting -> ExtractingState(state)
                    is SetupDownload.Completed -> CompletedState(latestRelease, installedVersion, onEvent, onNavigateBack)
                    is SetupDownload.Error -> ErrorState(state, onEvent)
                }
            }
        }
    }
}

@Composable
private fun IdleState(onEvent: (SetupEvent) -> Unit) {
    Column(verticalArrangement = Arrangement.spacedBy(16.dp), horizontalAlignment = Alignment.CenterHorizontally, modifier = Modifier.fillMaxWidth()) {
        Text(stringResource(R.string.db_status_ready), style = MaterialTheme.typography.titleSmall,
            fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.onSurface)
        Button(onClick = { onEvent(SetupEvent.StartDownload) },
            colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.primary),
            shape = ShapeSmall,
            modifier = Modifier.fillMaxWidth().testTag("start_download_button")) {
            Text(stringResource(R.string.db_start_download))
        }
    }
}

@Composable
private fun ProgressState(state: SetupDownload.Progress) {
    val extract = state.isExtractPhase
    Column(modifier = Modifier.fillMaxWidth(), verticalArrangement = Arrangement.spacedBy(10.dp)) {
        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
            Text(
                text = if (extract) stringResource(R.string.db_extracting, state.percentage)
                else stringResource(R.string.db_downloading, state.percentage),
                style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.primary
            )
            if (state.speedKbps > 0 && !extract) {
                Text(stringResource(R.string.db_speed_kbps, state.speedKbps),
                    style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
        }
        LinearProgressIndicator(
            progress = { state.percentage / 100f },
            modifier = Modifier.fillMaxWidth().height(10.dp).clip(ShapeSmall),
            color = MaterialTheme.colorScheme.primary,
            trackColor = MaterialTheme.colorScheme.surfaceVariant
        )
        val doneSize = formatFileSize(state.downloadedBytes)
        val totalSize = formatFileSize(state.totalBytes)
        val done = if (doneSize?.unit == SizeUnit.MB) doneSize.value else 0
        val total = if (totalSize?.unit == SizeUnit.MB) totalSize.value else 0
        Text(
            text = if (extract) stringResource(R.string.db_extracting_device)
            else if (total > 0) stringResource(R.string.db_progress_ratio, done, total)
            else stringResource(R.string.db_progress_single, done),
            style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant
        )
    }
}

@Composable
private fun ExtractingState(state: SetupDownload.Extracting) {
    Column(modifier = Modifier.fillMaxWidth(), verticalArrangement = Arrangement.spacedBy(10.dp),
        horizontalAlignment = Alignment.CenterHorizontally) {
        Text(stringResource(R.string.db_extracting, state.percentage), style = MaterialTheme.typography.titleSmall,
            fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.primary)
        LinearProgressIndicator(
            progress = { state.percentage / 100f },
            modifier = Modifier.fillMaxWidth().height(10.dp).clip(ShapeSmall),
            color = MaterialTheme.colorScheme.primary,
            trackColor = MaterialTheme.colorScheme.surfaceVariant
        )
    }
}

@Composable
private fun CompletedState(
    latestRelease: DbReleaseInfo?,
    installedVersion: DbInstalledVersion,
    onEvent: (SetupEvent) -> Unit,
    onNavigateBack: () -> Unit
) {
    val pending = latestRelease?.takeIf {
        it.versionCode > installedVersion.versionCode && installedVersion.versionCode > 0
    }
    Column(verticalArrangement = Arrangement.spacedBy(12.dp), horizontalAlignment = Alignment.CenterHorizontally,
        modifier = Modifier.fillMaxWidth()) {
        if (pending != null) {
            Text(stringResource(R.string.db_update_available, pending.versionName), style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.primary, textAlign = TextAlign.Center)
            Text(stringResource(R.string.db_update_size, formattedFileSize(pending.compressedSize)) +
                stringResource(R.string.db_update_auto), style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant, textAlign = TextAlign.Center)
            Button(onClick = { onEvent(SetupEvent.StartDownload) },
                colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.primary),
                shape = ShapeSmall,
                modifier = Modifier.fillMaxWidth().testTag("start_update_download_button")) {
                Text(stringResource(R.string.db_download_update))
            }
        } else {
            Text(stringResource(R.string.db_install_success), style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.primary)
            Text(stringResource(R.string.db_install_success_body), style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant, textAlign = TextAlign.Center)
        }
        Button(onClick = onNavigateBack,
            colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.primary),
            shape = ShapeSmall,
            modifier = Modifier.fillMaxWidth().testTag("finish_setup_button")) {
            Text(stringResource(R.string.db_back_to_app))
        }
    }
}

@Composable
private fun ErrorState(state: SetupDownload.Error, onEvent: (SetupEvent) -> Unit) {
    Column(verticalArrangement = Arrangement.spacedBy(12.dp), horizontalAlignment = Alignment.CenterHorizontally,
        modifier = Modifier.fillMaxWidth()) {
        Text(stringResource(R.string.db_download_failed), style = MaterialTheme.typography.titleSmall,
            fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.error)
        Text(downloadErrorMessage(state.error), style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.error, textAlign = TextAlign.Center)
        Button(onClick = { onEvent(SetupEvent.StartDownload) },
            colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.primary),
            shape = ShapeSmall,
            modifier = Modifier.fillMaxWidth().testTag("retry_download_button")) {
            Text(stringResource(R.string.common_retry))
        }
    }
}
