package com.quranwords.ui.setup

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dagger.hilt.android.lifecycle.HiltViewModel
import com.quranwords.domain.model.DbInstalledVersion
import com.quranwords.domain.model.DbReleaseInfo
import com.quranwords.domain.model.DownloadError
import com.quranwords.domain.model.DownloadPhase
import com.quranwords.domain.model.DownloadState
import com.quranwords.domain.repository.DatabaseDownloadRepository
import com.quranwords.domain.repository.DbCheckResult
import com.quranwords.domain.repository.DbUpdateRepository
import com.quranwords.domain.repository.QuranRepository
import javax.inject.Inject
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

@HiltViewModel
class DatabaseSetupViewModel @Inject constructor(
    private val downloadRepository: DatabaseDownloadRepository,
    private val dbUpdateRepository: DbUpdateRepository,
    private val quranRepository: QuranRepository
) : ViewModel() {

    private val _download = MutableStateFlow<SetupDownload>(
        if (downloadRepository.isDatabaseReady()) SetupDownload.Completed() else SetupDownload.Idle
    )
    private val _latestRelease = MutableStateFlow<DbReleaseInfo?>(null)
    private val _installedVersion = MutableStateFlow(DbInstalledVersion())
    private val _isCheckingUpdate = MutableStateFlow(false)
    private val _manifestError = MutableStateFlow<String?>(null)
    private val _importDialogVisible = MutableStateFlow(false)
    private val _releaseNotesExpanded = MutableStateFlow(false)

    init {
        viewModelScope.launch {
            _installedVersion.value = dbUpdateRepository.getInstalledVersion()
        }
    }

    /**
     * Single-collection UiState; the raw download flow is mapped to the UI
     * mirror so data.remote types never reach the screen.
     */
    private data class SetupCoreFlows(
        val download: SetupDownload,
        val latestRelease: DbReleaseInfo?,
        val installedVersion: DbInstalledVersion,
        val isCheckingUpdate: Boolean,
        val manifestError: String?
    )

    val uiState: StateFlow<SetupUiState> = run {
        val coreFlow = combine(
            _download, _latestRelease, _installedVersion, _isCheckingUpdate, _manifestError
        ) { download, latest, installed, checking, error ->
            SetupCoreFlows(download, latest, installed, checking, error)
        }
        combine(coreFlow, _importDialogVisible, _releaseNotesExpanded) { core, importDialog, notes ->
            SetupUiState(
                download = core.download,
                latestRelease = core.latestRelease,
                installedVersion = core.installedVersion,
                isCheckingUpdate = core.isCheckingUpdate,
                manifestError = core.manifestError,
                importDialogVisible = importDialog,
                releaseNotesExpanded = notes
            )
        }
    }.stateIn(viewModelScope, SharingStarted.Eagerly, SetupUiState())

    fun onEvent(event: SetupEvent) {
        when (event) {
            SetupEvent.StartDownload -> startDownload()
            SetupEvent.CheckForUpdate -> checkForUpdate()
            is SetupEvent.ImportDatabase -> importDatabase(event.uri)
            is SetupEvent.SetImportDialogVisible -> _importDialogVisible.value = event.visible
            is SetupEvent.SetReleaseNotesExpanded -> _releaseNotesExpanded.value = event.expanded
        }
    }

    fun checkForUpdate() {
        if (_isCheckingUpdate.value) return
        viewModelScope.launch {
            _isCheckingUpdate.value = true
            _manifestError.value = null
            try {
                _installedVersion.value = dbUpdateRepository.getInstalledVersion()
                when (val r = dbUpdateRepository.getLatestRelease()) {
                    is DbCheckResult.Success -> _latestRelease.value = r.data
                    is DbCheckResult.Error -> _manifestError.value = r.message
                }
            } finally {
                _isCheckingUpdate.value = false
            }
        }
    }

    fun startDownload() {
        viewModelScope.launch {
            val info = resolveReleaseForDownload() ?: return@launch
            downloadRepository.downloadRelease(info).collectLatest { state ->
                _download.value = state.toUi()
                if (state is DownloadState.Completed && state.versionCode > 0) {
                    quranRepository.closeDb()
                    dbUpdateRepository.setInstalledVersion(state.versionCode, state.versionName)
                    _installedVersion.value = dbUpdateRepository.getInstalledVersion()
                }
            }
        }
    }

    private suspend fun resolveReleaseForDownload(): DbReleaseInfo? {
        _latestRelease.value?.let { return it }
        return when (val r = dbUpdateRepository.getLatestRelease()) {
            is DbCheckResult.Success -> {
                _latestRelease.value = r.data
                r.data
            }
            is DbCheckResult.Error -> {
                _download.value = SetupDownload.Error(SetupError.MANIFEST_FAILED, r.message)
                null
            }
        }
    }

    fun importDatabase(uri: android.net.Uri) {
        viewModelScope.launch {
            downloadRepository.importDatabase(uri.toString()).collectLatest { state ->
                _download.value = state.toUi()
                if (state is DownloadState.Completed) {
                    quranRepository.closeDb()
                    _installedVersion.value = dbUpdateRepository.getInstalledVersion()
                }
            }
        }
    }

    private fun DownloadState.toUi(): SetupDownload = when (this) {
        DownloadState.Idle -> SetupDownload.Idle
        is DownloadState.Progress -> SetupDownload.Progress(
            percentage = percentage,
            downloadedBytes = downloadedBytes,
            totalBytes = totalBytes,
            speedKbps = speedKbps,
            isExtractPhase = phase == DownloadPhase.EXTRACT
        )
        is DownloadState.Extracting -> SetupDownload.Extracting(percentage)
        is DownloadState.Completed -> SetupDownload.Completed(versionCode, versionName)
        is DownloadState.Error -> SetupDownload.Error(error.toUi(), detail)
    }

    private fun DownloadError.toUi(): SetupError = when (this) {
        DownloadError.NETWORK -> SetupError.NETWORK
        DownloadError.NOT_ZIP -> SetupError.NOT_ZIP
        DownloadError.CHECKSUM_MISMATCH -> SetupError.CHECKSUM_MISMATCH
        DownloadError.EXTRACT_FAILED -> SetupError.EXTRACT_FAILED
        DownloadError.INVALID_DB -> SetupError.INVALID_DB
        DownloadError.INCOMPLETE_FILE -> SetupError.INCOMPLETE_FILE
        DownloadError.INSTALL_FAILED -> SetupError.INSTALL_FAILED
        DownloadError.BAD_PICK -> SetupError.BAD_PICK
        DownloadError.IMPORT_FAILED -> SetupError.IMPORT_FAILED
        DownloadError.MANIFEST_FAILED -> SetupError.MANIFEST_FAILED
        DownloadError.UNKNOWN -> SetupError.UNKNOWN
    }
}