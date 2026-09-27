package com.quranwords.domain.usecase

import com.quranwords.domain.model.DbUpdateState
import com.quranwords.domain.repository.DbCheckResult
import com.quranwords.domain.repository.DbUpdateRepository
import javax.inject.Inject

class CheckDbUpdateUseCase @Inject constructor(
    private val repository: DbUpdateRepository
) {
    suspend operator fun invoke(): DbCheckResult<DbUpdateState> {
        return try {
            repository.checkForUpdate()
        } catch (e: Exception) {
            DbCheckResult.Success(DbUpdateState.Unknown)
        }
    }
}
