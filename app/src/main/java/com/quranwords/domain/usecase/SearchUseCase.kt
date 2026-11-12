package com.quranwords.domain.usecase

import com.quranwords.core.util.ArabicNormalizer
import com.quranwords.core.util.Result
import com.quranwords.core.util.VerseReferenceParser
import com.quranwords.domain.model.SearchResult
import com.quranwords.domain.repository.QuranRepository
import javax.inject.Inject

/**
 * Business-value use case (AGENTS §9): Arabic normalization + blank guard +
 * reference resolution live here, not in the repository. The repository stays
 * a pure SQL passthrough.
 *
 * Reference queries ("2:255", "البقرة 255") resolve to an exact ayah that
 * the UI surfaces as a gold banner; the text query still runs so the
 * ALL view shows the exact hit first.
 */
class SearchUseCase @Inject constructor(
    private val repository: QuranRepository
) {
    suspend operator fun invoke(query: String, surahFilter: Int? = null): Result<SearchResult> {
        val trimmed = query.trim()
        if (trimmed.isBlank()) return Result.Success(SearchResult())
        if (ArabicNormalizer.normalizeAr(trimmed).isBlank()) return Result.Success(SearchResult())
        val ref = VerseReferenceParser.parse(trimmed)
        // Instant surah matches are in-memory (no DB round trip) — done here
        // so SearchScreen never queries the DB for surahs.
        val normQ = ArabicNormalizer.normalizeAr(trimmed)
        val surahs = com.quranwords.core.util.SurahMetadata.SURAHS.filter { meta ->
            ArabicNormalizer.normalizeAr(meta.nameAr).contains(normQ) ||
                meta.nameEn.contains(trimmed, ignoreCase = true) ||
                meta.id.toString() == trimmed
        }.map { meta ->
            com.quranwords.domain.model.Surah(meta.id, meta.nameAr, meta.nameEn, meta.ayahCount, meta.revelationType, meta.juzStart)
        }
        return try {
            val base = repository.searchAll(trimmed)
            val exact = ref?.let { (s, a) -> repository.getAyahDirect(s, a) }
            val ayat = if (surahFilter != null) {
                repository.searchAyatPaged(trimmed, com.quranwords.core.util.DatabaseConstants.SEARCH_PAGE_SIZE, 0, surahFilter)
            } else base.ayat
            Result.Success(base.copy(exactReference = exact, surahs = surahs, ayat = ayat))
        } catch (e: Exception) {
            Result.Error(e.message ?: "Search failed", e)
        }
    }
}
