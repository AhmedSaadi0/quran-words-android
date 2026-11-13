package com.quranwords.ui.roots

import com.quranwords.domain.model.RootItem

/**
 * Sort orders offered by the roots lexicon screen. Every order is stable:
 * ties fall back to the root id so the list never shuffles between
 * emissions (AGENTS §10).
 */
enum class RootsSort {
    /** Corpus order (`roots.id` ascending) — the default lexicon order. */
    Default,

    /** Grouped by the AI model that produced the summary. */
    ByModel,

    /** Most occurrences in the Quran first. */
    ByOccurrences,

    /** Most masadirs first. */
    ByMasadir,

    /** Most derivatives first. */
    ByDerivatives,

    /** Alphabetical by the root letters. */
    Alphabetical
}

/**
 * A distinct AI model found in the corpus plus how many roots use it.
 * The model string is the raw database value — never mapped to a friendly
 * name, because the model list changes with every corpus release.
 */
data class RootModelOption(
    val model: String,
    val count: Int
)

data class RootsListUiState(
    val isLoading: Boolean = true,
    val query: String = "",
    val filteredRoots: List<RootItem> = emptyList(),
    val totalCount: Int = 0,
    val error: String? = null,
    /** Distinct models present in the loaded corpus (blank ones excluded). */
    val availableModels: List<RootModelOption> = emptyList(),
    /** Models the user picked — an empty set means "no model filter". */
    val selectedModels: Set<String> = emptySet(),
    val sort: RootsSort = RootsSort.Default
)

sealed interface RootsListEvent {
    data class QueryChanged(val query: String) : RootsListEvent

    /** Adds/removes a model from the active filter (multi-select). */
    data class ModelFilterToggled(val model: String) : RootsListEvent

    data object ModelFiltersCleared : RootsListEvent

    data class SortChanged(val sort: RootsSort) : RootsListEvent

    data object Retry : RootsListEvent
}

/**
 * Stateless screen contract. Root-item navigation stays a screen callback
 * (AGENTS §11) — only state events flow through [RootsListEvent].
 */
