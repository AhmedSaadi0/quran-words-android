package io.github.ahmedsaadi0.quranwords.ui.setup

import android.net.Uri
import io.github.ahmedsaadi0.quranwords.domain.model.DbInstalledVersion
import io.github.ahmedsaadi0.quranwords.domain.model.DbReleaseInfo

/**
 * UI mirror of the download machine (Decision: data.remote stops leaking into
 * UI). The ViewModel maps [io.github.ahmedsaadi0.quranwords.data.remote.DownloadState]
 * 1:1 onto [SetupDownload].
 */
sealed interface SetupDownload {
    data object Idle : SetupDownload
    data class Progress(
        val percentage: Int,
        val downloadedBytes: Long,
        val totalBytes: Long,
        val speedKbps: Long,
        val isExtractPhase: Boolean
    ) : SetupDownload
    data class Extracting(val percentage: Int) : SetupDownload
    data class Completed(val versionCode: Int = 0, val versionName: String = "") : SetupDownload
    data class Error(val error: SetupError, val detail: String?) : SetupDownload
}

enum class SetupError {
    NETWORK,
    NOT_ZIP,
    CHECKSUM_MISMATCH,
    EXTRACT_FAILED,
    INVALID_DB,
    INCOMPLETE_FILE,
    INSTALL_FAILED,
    BAD_PICK,
    IMPORT_FAILED,
    MANIFEST_FAILED,
    UNKNOWN
}

data class SetupUiState(
    val download: SetupDownload = SetupDownload.Idle,
    val latestRelease: DbReleaseInfo? = null,
    val installedVersion: DbInstalledVersion = DbInstalledVersion(),
    val isCheckingUpdate: Boolean = false
) {
    val isBusy: Boolean
        get() = download is SetupDownload.Progress || download is SetupDownload.Extracting
}

sealed interface SetupEvent {
    data object StartDownload : SetupEvent
    data object CheckForUpdate : SetupEvent
    data class ImportDatabase(val uri: Uri) : SetupEvent
}