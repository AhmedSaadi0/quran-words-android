package io.github.ahmedsaadi0.quranwords.domain.usecase

import io.github.ahmedsaadi0.quranwords.core.util.Result
import io.github.ahmedsaadi0.quranwords.domain.model.RootDetail
import io.github.ahmedsaadi0.quranwords.domain.repository.QuranRepository
import javax.inject.Inject

class GetRootDetailUseCase @Inject constructor(
    private val repository: QuranRepository
) {
    suspend operator fun invoke(rootId: Int): Result<RootDetail?> {
        if (rootId <= 0) return Result.Error("Invalid rootId: $rootId")
        return try {
            val detail = repository.getRootDetail(rootId)
            if (detail == null) Result.Error("Root not found: $rootId")
            else Result.Success(detail)
        } catch (e: Exception) {
            Result.Error(e.message ?: "Failed to load root", e)
        }
    }
}

class GetRootByTextUseCase @Inject constructor(
    private val repository: QuranRepository
) {
    suspend operator fun invoke(rootText: String): Result<io.github.ahmedsaadi0.quranwords.domain.model.RootDetail?> {
        val trimmed = rootText.trim()
        if (trimmed.isBlank()) return Result.Error("Root text is blank")
        return try {
            Result.Success(repository.getRootByText(trimmed))
        } catch (e: Exception) {
            Result.Error(e.message ?: "Failed to find root", e)
        }
    }
}
