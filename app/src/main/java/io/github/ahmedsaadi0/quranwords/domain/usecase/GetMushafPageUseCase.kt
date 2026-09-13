package io.github.ahmedsaadi0.quranwords.domain.usecase

import io.github.ahmedsaadi0.quranwords.core.util.MushafConstants
import io.github.ahmedsaadi0.quranwords.core.util.Result
import io.github.ahmedsaadi0.quranwords.domain.model.MushafPage
import io.github.ahmedsaadi0.quranwords.domain.repository.QuranRepository
import javax.inject.Inject

/**
 * Loads one QPC Mushaf page. Business value over a raw repository call:
 * page-bounds validation, DB-readiness guard, and missing-layout mapping
 * to an actionable error (user needs a DB update, not an empty screen).
 */
class GetMushafPageUseCase @Inject constructor(
    private val repository: QuranRepository
) {
    suspend operator fun invoke(page: Int): Result<MushafPage> {
        if (!MushafConstants.isValidPage(page)) {
            return Result.Error("Invalid Mushaf page: $page (expected 1..604)")
        }
        // Data first: the bundled preview sidecar serves demo pages even
        // when the main database was never downloaded.
        val data = try {
            repository.getMushafPage(page)
        } catch (e: Exception) {
            return Result.Error(e.message ?: "Failed to load Mushaf page $page", e)
        }
        if (data != null) return Result.Success(data)
        if (!repository.isDatabaseReady()) {
            return Result.Error("Database not ready")
        }
        return Result.Error(
            "Mushaf layout missing for page $page — preview covers pages 1, 2, 3, 531, 602"
        )
    }
}
