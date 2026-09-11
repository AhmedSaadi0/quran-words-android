package io.github.ahmedsaadi0.quranwords.ui.surah

import io.github.ahmedsaadi0.quranwords.core.util.RevelationFilter
import io.github.ahmedsaadi0.quranwords.domain.model.Surah

data class SurahIndexUiState(
    val isLoading: Boolean = true,
    val query: String = "",
    val filter: RevelationFilter = RevelationFilter.ALL,
    val filteredSurahs: List<Surah> = emptyList(),
    val bookmarkedSurahIds: Set<Int> = emptySet(),
    val error: String? = null
)

sealed interface SurahIndexEvent {
    data class QueryChanged(val query: String) : SurahIndexEvent
    data class FilterChanged(val filter: RevelationFilter) : SurahIndexEvent
    data class ToggleBookmark(val surahId: Int) : SurahIndexEvent
}