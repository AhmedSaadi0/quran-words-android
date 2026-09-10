package io.github.ahmedsaadi0.quranwords.ui.search

import io.github.ahmedsaadi0.quranwords.domain.model.SearchResult

/** Result tabs in pager order (index == ordinal) — replaces magic ints 0..3. */
enum class SearchTab {
    ROOTS,
    MASADIR,
    DERIVATIVES,
    AYAT
}

data class SearchUiState(
    val query: String = "",
    val results: SearchResult = SearchResult(),
    val isSearching: Boolean = false,
    val isLoadingMore: Boolean = false,
    val error: String? = null
)

sealed interface SearchEvent {
    data class QueryChanged(val query: String) : SearchEvent
    data class NearingEnd(val tab: SearchTab, val lastVisibleIndex: Int) : SearchEvent
    data object Retry : SearchEvent
}