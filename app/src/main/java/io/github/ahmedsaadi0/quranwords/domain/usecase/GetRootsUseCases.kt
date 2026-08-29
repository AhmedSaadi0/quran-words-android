package io.github.ahmedsaadi0.quranwords.domain.usecase

import io.github.ahmedsaadi0.quranwords.core.util.Result
import io.github.ahmedsaadi0.quranwords.domain.model.AyahOccurrenceModel
import io.github.ahmedsaadi0.quranwords.domain.model.RootItem
import io.github.ahmedsaadi0.quranwords.domain.repository.QuranRepository
import javax.inject.Inject

class GetRootsPagedUseCase @Inject constructor(
    private val repository: QuranRepository
) {
    suspend operator fun invoke(limit: Int, offset: Int): Result<List<RootItem>> {
        if (limit <= 0 || offset < 0) return Result.Error("Invalid paging")
        return try {
            Result.Success(repository.getRootsPaged(limit, offset))
        } catch (e: Exception) {
            Result.Error(e.message ?: "Failed to load roots", e)
        }
    }
}

class GetRootOccurrencesPagedUseCase @Inject constructor(
    private val repository: QuranRepository
) {
    suspend operator fun invoke(rootId: Int, limit: Int, offset: Int): Result<List<AyahOccurrenceModel>> {
        if (rootId <= 0 || limit <= 0 || offset < 0) return Result.Error("Invalid params")
        return try {
            Result.Success(repository.getRootOccurrencesPaged(rootId, limit, offset))
        } catch (e: Exception) {
            Result.Error(e.message ?: "Failed to load occurrences", e)
        }
    }
}

class GetRootOccurrencesCountUseCase @Inject constructor(
    private val repository: QuranRepository
) {
    suspend operator fun invoke(rootId: Int): Result<Int> {
        if (rootId <= 0) return Result.Error("Invalid rootId")
        return try {
            Result.Success(repository.getRootOccurrencesCount(rootId))
        } catch (e: Exception) {
            Result.Error(e.message ?: "Failed to load count", e)
        }
    }
}

class IsDatabaseReadyUseCase @Inject constructor(
    private val repository: QuranRepository
) {
    operator fun invoke(): Boolean = repository.isDatabaseReady()
}
