package com.quranwords.ui.surah

import com.quranwords.core.util.RevelationFilter
import com.quranwords.domain.model.Surah

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