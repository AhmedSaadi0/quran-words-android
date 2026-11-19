package com.quranwords.domain.model

/**
 * Domain mirror of the database download machine (P1-1). No Android types so
 * the UI can depend on a contract instead of the OkHttp implementation.
 */
enum class DownloadPhase { DOWNLOAD, EXTRACT }

/** Localizable failure reason; [DownloadState.Error.detail] is for logs only. */
enum class DownloadError {
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

sealed interface DownloadState {
    data object Idle : DownloadState
    data class Progress(
        val percentage: Int,
        val downloadedBytes: Long,
        val totalBytes: Long,
        val speedKbps: Long = 0,
        val phase: DownloadPhase = DownloadPhase.DOWNLOAD
    ) : DownloadState
    data class Extracting(val percentage: Int) : DownloadState
    data class Completed(val versionCode: Int = 0, val versionName: String = "") : DownloadState
    data class Error(val error: DownloadError, val detail: String? = null) : DownloadState
}
