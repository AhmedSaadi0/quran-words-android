package com.quranwords.data.repository

import com.quranwords.core.util.DatabaseConstants
import com.quranwords.data.remote.DatabaseDownloadManager
import com.quranwords.data.remote.ManifestRemoteDataSource
import com.quranwords.domain.model.DbInstalledVersion
import com.quranwords.domain.model.DbReleaseInfo
import com.quranwords.domain.model.DbUpdateState
import com.quranwords.domain.repository.DbCheckResult
import com.quranwords.domain.repository.DbUpdateRepository
import kotlinx.coroutines.flow.first
import javax.inject.Inject

class DbUpdateRepositoryImpl @Inject constructor(
    private val manifestSource: ManifestRemoteDataSource,
    private val preferences: UserPreferencesRepository,
    private val downloadManager: DatabaseDownloadManager
) : DbUpdateRepository {

    override suspend fun getInstalledVersion(): DbInstalledVersion {
        val code = try {
            preferences.dbVersionCode.first()
        } catch (_: Exception) {
            0
        }
        val name = try {
            preferences.dbVersionName.first()
        } catch (_: Exception) {
            ""
        }
        if (code > 0) return DbInstalledVersion(code, name)
        // Migration: existing DB file without stored version is the v1 baseline.
        return if (downloadManager.isDatabaseReady()) {
            try {
                preferences.setInstalledDbVersion(
                    DatabaseConstants.DB_BASELINE_VERSION_CODE,
                    DatabaseConstants.DB_BASELINE_VERSION_NAME
                )
            } catch (_: Exception) {
            }
            DbInstalledVersion(
                DatabaseConstants.DB_BASELINE_VERSION_CODE,
                DatabaseConstants.DB_BASELINE_VERSION_NAME
            )
        } else {
            DbInstalledVersion(0, "")
        }
    }

    override suspend fun setInstalledVersion(code: Int, name: String) {
        preferences.setInstalledDbVersion(code, name)
    }

    override suspend fun getDismissedVersionCode(): Int {
        return try {
            preferences.dismissedDbVersionCode.first()
        } catch (_: Exception) {
            0
        }
    }

    override suspend fun setDismissedVersionCode(code: Int) {
        preferences.setDismissedDbVersion(code)
    }

    override suspend fun getLatestRelease(): DbCheckResult<DbReleaseInfo> {
        return try {
            DbCheckResult.Success(manifestSource.fetchLatest())
        } catch (e: Exception) {
            DbCheckResult.Error(e.message ?: "تعذر جلب معلومات الإصدار", e)
        }
    }

    override suspend fun checkForUpdate(): DbCheckResult<DbUpdateState> {
        val installed = getInstalledVersion()
        val latest = when (val r = getLatestRelease()) {
            is DbCheckResult.Success -> r.data
            is DbCheckResult.Error -> return DbCheckResult.Success(DbUpdateState.Unknown)
        }
        if (installed.versionCode <= 0) {
            return DbCheckResult.Success(DbUpdateState.NoLocalDb)
        }
        if (latest.versionCode <= installed.versionCode) {
            return DbCheckResult.Success(DbUpdateState.UpToDate)
        }
        if (getDismissedVersionCode() >= latest.versionCode) {
            return DbCheckResult.Success(DbUpdateState.UpToDate)
        }
        try {
            preferences.setLastDbCheckAt()
        } catch (_: Exception) {
        }
        return DbCheckResult.Success(DbUpdateState.UpdateAvailable(latest))
    }
}
