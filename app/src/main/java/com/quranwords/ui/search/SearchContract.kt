package com.quranwords.ui.search

import com.quranwords.domain.model.SearchResult

/**
 * Pagination keys in pager order (index == ordinal). WORDS appended last —
 * keys for the four legacy tabs stay stable; not a UI tab list.
 */
enum class SearchTab {
    ROOTS,
    MASADIR,
    DERIVATIVES,
    AYAT,
    WORDS
}

/**
 * Unified search scopes (Full Plan v2, D4). Five top-level scopes; masadir +
 * derivatives remain as sub-scopes under LEXICON so the morphological corpus
 * (5,273 / 16,245 rows) is never lost.
 */
enum class SearchScope { ALL, AYAHS, WORDS, SURAHS, LEXICON }

enum class LexiconSubScope { ROOTS, MASADIR, DERIVATIVES }

data class SearchUiState(
    val query: String = "",
    val results: SearchResult = SearchResult(),
    val scope: SearchScope = SearchScope.ALL,
    val lexiconSubScope: LexiconSubScope = LexiconSubScope.ROOTS,
    val surahFilter: Int? = null,
    val recentSearches: List<String> = emptyList(),
    val topRoots: List<com.quranwords.domain.model.RootItem> = emptyList(),
    val bookmarkedAyat: Set<String> = emptySet(),
    val isSearching: Boolean = false,
    val isLoadingMore: Boolean = false,
    val error: String? = null
)

sealed interface SearchEvent {
    data class QueryChanged(val query: String) : SearchEvent
    data class NearingEnd(val tab: SearchTab, val lastVisibleIndex: Int) : SearchEvent
    data class ScopeChanged(val scope: SearchScope) : SearchEvent
    data class LexiconSubScopeChanged(val subScope: LexiconSubScope) : SearchEvent
    data class SurahFilterChanged(val surahId: Int?) : SearchEvent
    data class RecentSearchSelected(val query: String) : SearchEvent
    data class RecentSearchRemoved(val query: String) : SearchEvent
    data object ClearRecents : SearchEvent
    data object Retry : SearchEvent
}