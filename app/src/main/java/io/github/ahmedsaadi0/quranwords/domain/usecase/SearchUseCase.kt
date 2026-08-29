package io.github.ahmedsaadi0.quranwords.domain.usecase

import io.github.ahmedsaadi0.quranwords.core.util.ArabicNormalizer
import io.github.ahmedsaadi0.quranwords.core.util.Result
import io.github.ahmedsaadi0.quranwords.domain.model.SearchResult
import io.github.ahmedsaadi0.quranwords.domain.repository.QuranRepository
import javax.inject.Inject

class SearchUseCase @Inject constructor(
    private val repository: QuranRepository
) {
    suspend operator fun invoke(query: String): Result<SearchResult> {
        val trimmed = query.trim()
        if (trimmed.isBlank()) return Result.Success(SearchResult())
        val normalized = ArabicNormalizer.normalizeAr(trimmed)
        if (normalized.isBlank()) return Result.Success(SearchResult())
        return try {
            val result = repository.searchAll(normalized)
            // repository currently returns SearchResult directly; if it returns normalized vs original, we already handled
            // If repository had returned Result, we would just pass through
            Result.Success(result)
        } catch (e: Exception) {
            Result.Error(e.message ?: "Search failed", e)
        }
    }
}
