package io.github.ahmedsaadi0.quranwords.ui.setup

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dagger.hilt.android.lifecycle.HiltViewModel
import io.github.ahmedsaadi0.quranwords.data.remote.DatabaseDownloadManager
import io.github.ahmedsaadi0.quranwords.data.remote.DownloadError
import io.github.ahmedsaadi0.quranwords.data.remote.DownloadPhase
import io.github.ahmedsaadi0.quranwords.data.remote.DownloadState
import io.github.ahmedsaadi0.quranwords.domain.model.DbInstalledVersion
import io.github.ahmedsaadi0.quranwords.domain.model.DbReleaseInfo
import io.github.ahmedsaadi0.quranwords.domain.repository.DbCheckResult
import io.github.ahmedsaadi0.quranwords.domain.repository.DbUpdateRepository
import io.github.ahmedsaadi0.quranwords.domain.repository.QuranRepository
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
    private val downloadManager: DatabaseDownloadManager,
    private val dbUpdateRepository: DbUpdateRepository,
    private val quranRepository: QuranRepository
) : ViewModel() {

    private val _download = MutableStateFlow<SetupDownload>(
        if (downloadManager.isDatabaseReady()) SetupDownload.Completed() else SetupDownload.Idle
    )
    private val _latestRelease = MutableStateFlow<DbReleaseInfo?>(null)
    private val _installedVersion = MutableStateFlow(DbInstalledVersion())
    private val _isCheckingUpdate = MutableStateFlow(false)

    init {
        viewModelScope.launch {
            _installedVersion.value = dbUpdateRepository.getInstalledVersion()
        }
    }

    /**
     * Single-collection UiState; the raw download flow is mapped to the UI
     * mirror so data.remote types never reach the screen.
     */
    val uiState: StateFlow<SetupUiState> = combine(
        _download,
        _latestRelease,
        _installedVersion,
        _isCheckingUpdate
    ) { download, latestRelease, installedVersion, isCheckingUpdate ->
        SetupUiState(
            download = download,
            latestRelease = latestRelease,
            installedVersion = installedVersion,
            isCheckingUpdate = isCheckingUpdate
        )
    }.stateIn(viewModelScope, SharingStarted.Eagerly, SetupUiState())

    fun onEvent(event: SetupEvent) {
        when (event) {
            SetupEvent.StartDownload -> startDownload()
            SetupEvent.CheckForUpdate -> checkForUpdate()
            is SetupEvent.ImportDatabase -> importDatabase(event.uri)
        }
    }

    fun checkForUpdate() {
        if (_isCheckingUpdate.value) return
        viewModelScope.launch {
            _isCheckingUpdate.value = true
            try {
                _installedVersion.value = dbUpdateRepository.getInstalledVersion()
                when (val r = dbUpdateRepository.getLatestRelease()) {
                    is DbCheckResult.Success -> _latestRelease.value = r.data
                    is DbCheckResult.Error -> Unit
                }
            } finally {
                _isCheckingUpdate.value = false
            }
        }
    }

    fun startDownload() {
        viewModelScope.launch {
            val info = resolveReleaseForDownload() ?: return@launch
            downloadManager.downloadRelease(info).collectLatest { state ->
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
            downloadManager.importDatabase(uri).collectLatest { state ->
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