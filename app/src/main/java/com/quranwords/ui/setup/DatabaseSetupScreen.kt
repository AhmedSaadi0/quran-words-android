package com.quranwords.ui.setup

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.outlined.CheckCircle
import androidx.compose.material.icons.outlined.Download
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.quranwords.R

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun DatabaseSetupScreen(
    uiState: SetupUiState,
    onEvent: (SetupEvent) -> Unit,
    onPickImportFile: () -> Unit,
    onNavigateBack: () -> Unit
) {
    val isBusy = uiState.download is SetupDownload.Progress ||
        uiState.download is SetupDownload.Extracting

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(stringResource(R.string.db_setup_title), fontWeight = FontWeight.Bold) },
                navigationIcon = {
                    IconButton(onClick = onNavigateBack, modifier = Modifier.testTag("back_button")) {
                        Icon(
                            Icons.AutoMirrored.Filled.ArrowBack,
                            contentDescription = stringResource(R.string.cd_back)
                        )
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = MaterialTheme.colorScheme.surface)
            )
        },
        containerColor = MaterialTheme.colorScheme.background,
        contentWindowInsets = WindowInsets(0, 0, 0, 0)
    ) { innerPadding ->
        LazyColumn(
            modifier = Modifier.fillMaxSize().padding(innerPadding).testTag("db_setup_screen"),
            contentPadding = PaddingValues(20.dp),
            horizontalAlignment = androidx.compose.ui.Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(20.dp)
        ) {
            item {
                SetupHeader(isCompleted = uiState.download is SetupDownload.Completed)
            }
            item {
                VersionStatusCard(
                    installedCode = uiState.installedVersion.versionCode,
                    installedName = uiState.installedVersion.versionName,
                    latestRelease = uiState.latestRelease,
                    isChecking = uiState.isCheckingUpdate,
                    manifestError = uiState.manifestError,
                    showNotes = uiState.releaseNotesExpanded,
                    onToggleNotes = { onEvent(SetupEvent.SetReleaseNotesExpanded(!uiState.releaseNotesExpanded)) },
                    onCheckClick = { onEvent(SetupEvent.CheckForUpdate) }
                )
            }
            item {
                SetupDownloadStates(
                    state = uiState.download,
                    latestRelease = uiState.latestRelease,
                    installedVersion = uiState.installedVersion,
                    onEvent = onEvent,
                    onNavigateBack = onNavigateBack
                )
            }
            if (!isBusy) {
                item {
                    OutlinedButton(
                        onClick = { onEvent(SetupEvent.SetImportDialogVisible(true)) },
                        shape = RoundedCornerShape(12.dp),
                        modifier = Modifier.fillMaxWidth().testTag("import_db_button")
                    ) { Text(stringResource(R.string.db_import_title)) }
                }
            }
            item {
                OutlinedButton(
                    onClick = onNavigateBack,
                    shape = RoundedCornerShape(12.dp),
                    modifier = Modifier.fillMaxWidth()
                ) { Text(stringResource(R.string.db_preview_action)) }
            }
            item { Spacer(Modifier.height(4.dp)) }
        }
    }

    if (uiState.importDialogVisible) {
        SetupImportDialog(
            onDismiss = { onEvent(SetupEvent.SetImportDialogVisible(false)) },
            onPickFile = onPickImportFile
        )
    }
}

@Composable
private fun SetupHeader(isCompleted: Boolean) {
    Column(
        modifier = Modifier.fillMaxWidth(),
        verticalArrangement = Arrangement.spacedBy(20.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Box(
            modifier = Modifier.size(80.dp).clip(CircleShape)
                .background(MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.6f)),
            contentAlignment = Alignment.Center
        ) {
            Icon(
                imageVector = if (isCompleted) Icons.Outlined.CheckCircle else Icons.Outlined.Download,
                contentDescription = null,
                tint = if (isCompleted) MaterialTheme.colorScheme.primary
                else MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.size(38.dp)
            )
        }
        Text(stringResource(R.string.db_setup_heading), style = MaterialTheme.typography.titleLarge,
            fontWeight = FontWeight.Bold, textAlign = TextAlign.Center, color = MaterialTheme.colorScheme.onBackground)
        Text(stringResource(R.string.db_setup_desc), style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant, textAlign = TextAlign.Center, lineHeight = 24.sp)
    }
}
