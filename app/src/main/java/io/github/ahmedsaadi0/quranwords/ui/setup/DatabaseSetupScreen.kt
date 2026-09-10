package io.github.ahmedsaadi0.quranwords.ui.setup

import androidx.compose.animation.Crossfade
import androidx.compose.animation.animateContentSize
import androidx.compose.animation.core.tween
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.outlined.CheckCircle
import androidx.compose.material.icons.outlined.Download
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import io.github.ahmedsaadi0.quranwords.R
import io.github.ahmedsaadi0.quranwords.ui.theme.AppMotion

@OptIn(ExperimentalMaterial3Api::class, ExperimentalFoundationApi::class)
@Composable
fun DatabaseSetupScreen(
    uiState: SetupUiState,
    onEvent: (SetupEvent) -> Unit,
    onPickImportFile: () -> Unit,
    onNavigateBack: () -> Unit
) {
    val downloadState = uiState.download
    val latestRelease = uiState.latestRelease
    val installedVersion = uiState.installedVersion
    val isCheckingUpdate = uiState.isCheckingUpdate
    var showImportDialog by remember { mutableStateOf(false) }
    var showNotes by remember { mutableStateOf(false) }

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Text(
                        text = stringResource(R.string.db_setup_title),
                        fontWeight = FontWeight.Bold
                    )
                },
                navigationIcon = {
                    IconButton(
                        onClick = onNavigateBack,
                        modifier = Modifier.testTag("back_button")
                    ) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                            contentDescription = stringResource(R.string.cd_back)
                        )
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.surface
                )
            )
        },
        containerColor = MaterialTheme.colorScheme.background,
        contentWindowInsets = WindowInsets(0, 0, 0, 0)
    ) { innerPadding ->
        val isBusy = downloadState is SetupDownload.Progress ||
            downloadState is SetupDownload.Extracting
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .testTag("db_setup_screen"),
            contentPadding = PaddingValues(20.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(20.dp)
        ) {
            // Icon
            item {
                Box(
                    modifier = Modifier
                        .size(80.dp)
                        .clip(CircleShape)
                        .background(MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.6f)),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = if (downloadState is SetupDownload.Completed) Icons.Outlined.CheckCircle
                        else Icons.Outlined.Download,
                        contentDescription = null,
                        tint = if (downloadState is SetupDownload.Completed) MaterialTheme.colorScheme.primary
                        else MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.size(38.dp)
                    )
                }
            }

            item {
                Text(
                    text = stringResource(R.string.db_setup_heading),
                    style = MaterialTheme.typography.titleLarge,
                    fontWeight = FontWeight.Bold,
                    textAlign = TextAlign.Center,
                    color = MaterialTheme.colorScheme.onBackground
                )
            }

            item {
                Text(
                    text = stringResource(R.string.db_setup_desc),
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    textAlign = TextAlign.Center,
                    lineHeight = 24.sp
                )
            }

            item {
                VersionStatusCard(
                    installedCode = installedVersion.versionCode,
                    installedName = installedVersion.versionName,
                    latestRelease = latestRelease,
                    isChecking = isCheckingUpdate,
                    showNotes = showNotes,
                    onToggleNotes = { showNotes = !showNotes },
                    onCheckClick = { onEvent(SetupEvent.CheckForUpdate) }
                )
            }

            item {
                Card(
                    modifier = Modifier
                        .fillMaxWidth()
                        .animateContentSize(animationSpec = tween(durationMillis = AppMotion.DurationMedium)),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                    shape = RoundedCornerShape(16.dp),
                    border = CardDefaults.outlinedCardBorder()
                ) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(20.dp)
                        .animateContentSize(animationSpec = tween(durationMillis = AppMotion.DurationMedium)),
                    verticalArrangement = Arrangement.spacedBy(16.dp),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    // مفتاح ثابت لكل نوع حالة: يمنع إعادة حركة التلاشي مع كل نبضة
                    // تقدم (Progress جديد كل نسبة)، ويبقيها للانتقالات الحقيقية فقط.
                    val stateKey = when (downloadState) {
                        is SetupDownload.Idle -> 0
                        is SetupDownload.Progress -> 1
                        is SetupDownload.Extracting -> 2
                        is SetupDownload.Completed -> 3
                        is SetupDownload.Error -> 4
                    }
                    Crossfade(
                        targetState = stateKey,
                        animationSpec = tween(durationMillis = AppMotion.DurationMedium),
                        label = "downloadStateCrossfade"
                    ) {
                        when (val state = downloadState) {
                            is SetupDownload.Idle -> {
                                Column(
                                    verticalArrangement = Arrangement.spacedBy(16.dp),
                                    horizontalAlignment = Alignment.CenterHorizontally,
                                    modifier = Modifier.fillMaxWidth()
                                ) {
                                    Text(
                                        text = stringResource(R.string.db_status_ready),
                                        style = MaterialTheme.typography.titleSmall,
                                        fontWeight = FontWeight.Bold,
                                        color = MaterialTheme.colorScheme.onSurface
                                    )
                                    Button(
                                        onClick = { onEvent(SetupEvent.StartDownload) },
                                        colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.primary),
                                        shape = RoundedCornerShape(12.dp),
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .testTag("start_download_button")
                                    ) {
                                        Text(stringResource(R.string.db_start_download))
                                    }
                                }
                            }
                            is SetupDownload.Progress -> {
                                val isExtractPhase = state.isExtractPhase
                                Column(
                                    modifier = Modifier.fillMaxWidth(),
                                    verticalArrangement = Arrangement.spacedBy(10.dp)
                                ) {
                                    Row(
                                        modifier = Modifier.fillMaxWidth(),
                                        horizontalArrangement = Arrangement.SpaceBetween
                                    ) {
                                        Text(
                                            text = if (isExtractPhase) stringResource(R.string.db_extracting, state.percentage)
                                            else stringResource(R.string.db_downloading, state.percentage),
                                            style = MaterialTheme.typography.titleSmall,
                                            fontWeight = FontWeight.Bold,
                                            color = MaterialTheme.colorScheme.primary
                                        )
                                        if (state.speedKbps > 0 && !isExtractPhase) {
                                            Text(
                                                text = stringResource(R.string.db_speed_kbps, state.speedKbps),
                                                style = MaterialTheme.typography.labelSmall,
                                                color = MaterialTheme.colorScheme.onSurfaceVariant
                                            )
                                        }
                                    }

                                    LinearProgressIndicator(
                                        progress = { state.percentage / 100f },
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .height(10.dp)
                                            .clip(RoundedCornerShape(12.dp)),
                                        color = MaterialTheme.colorScheme.primary,
                                        trackColor = MaterialTheme.colorScheme.surfaceVariant
                                    )

                                    val mbDownloaded = state.downloadedBytes / (1024 * 1024)
                                    val mbTotal = state.totalBytes / (1024 * 1024)
                                    Text(
                                        text = if (isExtractPhase) stringResource(R.string.db_extracting_device)
                                        else if (mbTotal > 0) stringResource(R.string.db_progress_ratio, mbDownloaded, mbTotal)
                                        else stringResource(R.string.db_progress_single, mbDownloaded),
                                        style = MaterialTheme.typography.bodySmall,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant
                                    )
                                }
                            }
                            is SetupDownload.Extracting -> {
                                Column(
                                    modifier = Modifier.fillMaxWidth(),
                                    verticalArrangement = Arrangement.spacedBy(10.dp),
                                    horizontalAlignment = Alignment.CenterHorizontally
                                ) {
                                    Text(
                                        text = stringResource(R.string.db_extracting, state.percentage),
                                        style = MaterialTheme.typography.titleSmall,
                                        fontWeight = FontWeight.Bold,
                                        color = MaterialTheme.colorScheme.primary
                                    )
                                    LinearProgressIndicator(
                                        progress = { state.percentage / 100f },
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .height(10.dp)
                                            .clip(RoundedCornerShape(12.dp)),
                                        color = MaterialTheme.colorScheme.primary,
                                        trackColor = MaterialTheme.colorScheme.surfaceVariant
                                    )
                                }
                            }
                            is SetupDownload.Completed -> {
                                val pendingUpdate = latestRelease?.takeIf {
                                    it.versionCode > installedVersion.versionCode &&
                                        installedVersion.versionCode > 0
                                }
                                Column(
                                    verticalArrangement = Arrangement.spacedBy(12.dp),
                                    horizontalAlignment = Alignment.CenterHorizontally,
                                    modifier = Modifier.fillMaxWidth()
                                ) {
                                    if (pendingUpdate != null) {
                                        Text(
                                            text = stringResource(R.string.db_update_available, pendingUpdate.versionName),
                                            style = MaterialTheme.typography.titleMedium,
                                            fontWeight = FontWeight.Bold,
                                            color = MaterialTheme.colorScheme.primary,
                                            textAlign = TextAlign.Center
                                        )
                                        Text(
                                            text = stringResource(R.string.db_update_size, formatVersionSize(pendingUpdate.compressedSize)) +
                                                stringResource(R.string.db_update_auto),
                                            style = MaterialTheme.typography.bodySmall,
                                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                                            textAlign = TextAlign.Center
                                        )
                                        Button(
                                            onClick = { onEvent(SetupEvent.StartDownload) },
                                            colors = ButtonDefaults.buttonColors(
                                                containerColor = MaterialTheme.colorScheme.primary
                                            ),
                                            shape = RoundedCornerShape(12.dp),
                                            modifier = Modifier
                                                .fillMaxWidth()
                                                .testTag("start_update_download_button")
                                        ) {
                                            Text(stringResource(R.string.db_download_update))
                                        }
                                    } else {
                                        Text(
                                            text = stringResource(R.string.db_install_success),
                                            style = MaterialTheme.typography.titleMedium,
                                            fontWeight = FontWeight.Bold,
                                            color = MaterialTheme.colorScheme.primary
                                        )
                                        Text(
                                            text = stringResource(R.string.db_install_success_body),
                                            style = MaterialTheme.typography.bodySmall,
                                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                                            textAlign = TextAlign.Center
                                        )
                                    }
                                    Button(
                                        onClick = onNavigateBack,
                                        colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.primary),
                                        shape = RoundedCornerShape(12.dp),
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .testTag("finish_setup_button")
                                    ) {
                                        Text(stringResource(R.string.db_back_to_app))
                                    }
                                }
                            }
                            is SetupDownload.Error -> {
                                Column(
                                    verticalArrangement = Arrangement.spacedBy(12.dp),
                                    horizontalAlignment = Alignment.CenterHorizontally,
                                    modifier = Modifier.fillMaxWidth()
                                ) {
                                    Text(
                                        text = stringResource(R.string.db_download_failed),
                                        style = MaterialTheme.typography.titleSmall,
                                        fontWeight = FontWeight.Bold,
                                        color = MaterialTheme.colorScheme.error
                                    )
                                    Text(
                                        // Localized by error code; raw detail stays in logs only.
                                        text = downloadErrorMessage(state.error),
                                        style = MaterialTheme.typography.bodySmall,
                                        color = MaterialTheme.colorScheme.error,
                                        textAlign = TextAlign.Center
                                    )
                                    Button(
                                        onClick = { onEvent(SetupEvent.StartDownload) },
                                        colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.primary),
                                        shape = RoundedCornerShape(12.dp),
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .testTag("retry_download_button")
                                    ) {
                                        Text(stringResource(R.string.common_retry))
                                    }
                                }
                            }
                        }
                    }
                }
                }
            }

            if (!isBusy) {
                item {
                    OutlinedButton(
                        onClick = { showImportDialog = true },
                        shape = RoundedCornerShape(12.dp),
                        modifier = Modifier
                            .fillMaxWidth()
                            .testTag("import_db_button")
                    ) {
                        Text(stringResource(R.string.db_import_title))
                    }
                }
            }

            item {
                OutlinedButton(
                    onClick = onNavigateBack,
                    shape = RoundedCornerShape(12.dp),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Text(stringResource(R.string.db_preview_action))
                }
            }

            item {
                Spacer(modifier = Modifier.height(4.dp))
            }
        }
    }

    if (showImportDialog) {
        AlertDialog(
            onDismissRequest = { showImportDialog = false },
            title = { Text(text = stringResource(R.string.db_import_dialog_title), fontWeight = FontWeight.Bold) },
            text = {
                Text(
                    text = stringResource(R.string.db_import_dialog_body),
                    style = MaterialTheme.typography.bodyMedium
                )
            },
            confirmButton = {
                TextButton(
                    onClick = {
                        showImportDialog = false
                        onPickImportFile()
                    },
                    modifier = Modifier.testTag("confirm_import_button")
                ) {
                    Text(stringResource(R.string.db_pick_file))
                }
            },
            dismissButton = {
                TextButton(onClick = { showImportDialog = false }) {
                    Text(stringResource(R.string.cd_cancel))
                }
            }
        )
    }
}

@Composable
private fun VersionStatusCard(
    installedCode: Int,
    installedName: String,
    latestRelease: io.github.ahmedsaadi0.quranwords.domain.model.DbReleaseInfo?,
    isChecking: Boolean,
    showNotes: Boolean,
    onToggleNotes: () -> Unit,
    onCheckClick: () -> Unit
) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .testTag("db_version_card"),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        shape = RoundedCornerShape(16.dp),
        border = CardDefaults.outlinedCardBorder()
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
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
                // Resolve in @Composable context: stringResource cannot be called
                // from inside the buildString lambda.
                val latestLine = stringResource(
                    R.string.db_latest,
                    latestRelease.versionName,
                    latestRelease.versionCode
                ) + if (isNew) stringResource(R.string.db_update_avail_tag) else ""
                Text(
                    text = latestLine,
                    style = MaterialTheme.typography.bodySmall,
                    fontWeight = if (isNew) FontWeight.Bold else FontWeight.Normal,
                    color = if (isNew) MaterialTheme.colorScheme.primary
                    else MaterialTheme.colorScheme.onSurfaceVariant
                )
                Text(
                    // releaseNotesAr is Arabic reference data from the manifest — never translated.
                    text = stringResource(R.string.db_update_size, formatVersionSize(latestRelease.compressedSize)) +
                        (if (latestRelease.uncompressedSize > 0)
                            stringResource(R.string.db_size_after, formatVersionSize(latestRelease.uncompressedSize)) else ""),
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                if (latestRelease.releaseNotesAr.isNotBlank()) {
                    TextButton(
                        onClick = onToggleNotes,
                        modifier = Modifier.testTag("toggle_release_notes_btn")
                    ) {
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
                    text = stringResource(R.string.db_manifest_error),
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }
    }
}

@Composable
private fun downloadErrorMessage(error: SetupError): String = stringResource(
    when (error) {
        SetupError.NETWORK -> R.string.dl_err_download
        SetupError.NOT_ZIP -> R.string.dl_err_not_zip
        SetupError.CHECKSUM_MISMATCH -> R.string.dl_err_checksum
        SetupError.EXTRACT_FAILED -> R.string.dl_err_extract
        SetupError.INVALID_DB -> R.string.dl_err_invalid_db
        SetupError.INCOMPLETE_FILE -> R.string.dl_err_incomplete
        SetupError.INSTALL_FAILED -> R.string.dl_err_install
        SetupError.BAD_PICK -> R.string.dl_err_bad_pick
        SetupError.IMPORT_FAILED -> R.string.dl_err_import
        SetupError.MANIFEST_FAILED -> R.string.dl_err_manifest
        SetupError.UNKNOWN -> R.string.dl_err_unknown
    }
)

@Composable
private fun formatVersionSize(bytes: Long): String {
    if (bytes <= 0) return "—"
    val mb = bytes / (1024 * 1024)
    if (mb >= 1) return stringResource(R.string.db_size_mb, mb)
    return stringResource(R.string.db_size_kb, bytes / 1024)
}
