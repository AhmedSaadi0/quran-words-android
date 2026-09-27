package com.quranwords.domain.model

data class DbReleaseInfo(
    val versionCode: Int,
    val versionName: String,
    val downloadUrl: String,
    val compressedSize: Long = 0L,
    val uncompressedSize: Long = 0L,
    val sha256: String? = null,
    val publishedAt: String? = null,
    val releaseNotesAr: String = "",
    val releasePageUrl: String? = null,
    val minAppVersionCode: Int = 0
)

data class DbInstalledVersion(
    val versionCode: Int = 0,
    val versionName: String = "",
    val installedAt: Long = 0L
)

sealed interface DbUpdateState {
    data object UpToDate : DbUpdateState
    data class UpdateAvailable(val info: DbReleaseInfo) : DbUpdateState
    data object NoLocalDb : DbUpdateState
    data object Unknown : DbUpdateState
}
