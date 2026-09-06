package io.github.ahmedsaadi0.quranwords.domain.repository

import io.github.ahmedsaadi0.quranwords.domain.model.DbInstalledVersion
import io.github.ahmedsaadi0.quranwords.domain.model.DbReleaseInfo
import io.github.ahmedsaadi0.quranwords.domain.model.DbUpdateState

sealed interface DbCheckResult<out T> {
    data class Success<T>(val data: T) : DbCheckResult<T>
    data class Error(val message: String, val cause: Throwable? = null) : DbCheckResult<Nothing>
}

interface DbUpdateRepository {
    suspend fun getInstalledVersion(): DbInstalledVersion
    suspend fun setInstalledVersion(code: Int, name: String)
    suspend fun getDismissedVersionCode(): Int
    suspend fun setDismissedVersionCode(code: Int)
    suspend fun getLatestRelease(): DbCheckResult<DbReleaseInfo>
    suspend fun checkForUpdate(): DbCheckResult<DbUpdateState>
}
