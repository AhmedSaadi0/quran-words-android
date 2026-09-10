package io.github.ahmedsaadi0.quranwords.ui.roots

import io.github.ahmedsaadi0.quranwords.domain.model.RootItem

data class RootsListUiState(
    val isLoading: Boolean = true,
    val query: String = "",
    val filteredRoots: List<RootItem> = emptyList(),
    val totalCount: Int = 0,
    val error: String? = null
)

sealed interface RootsListEvent {
    data class QueryChanged(val query: String) : RootsListEvent
    data object Retry : RootsListEvent
}

/**
 * Stateless screen contract. Root-item navigation stays a screen callback
 * (AGENTS §11) — only state events flow through [RootsListEvent].
 */