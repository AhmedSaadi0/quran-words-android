package io.github.ahmedsaadi0.quranwords.domain.usecase

import io.github.ahmedsaadi0.quranwords.domain.model.DbUpdateState
import io.github.ahmedsaadi0.quranwords.domain.repository.DbCheckResult
import io.github.ahmedsaadi0.quranwords.domain.repository.DbUpdateRepository
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
