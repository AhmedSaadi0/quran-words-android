package io.github.ahmedsaadi0.quranwords.ui.roots.word

import io.github.ahmedsaadi0.quranwords.domain.model.AyahOccurrenceModel

data class WordAyatUiState(
    val wordText: String = "",
    val occurrences: List<AyahOccurrenceModel> = emptyList(),
    val totalCount: Int = 0,
    val hasMore: Boolean = true,
    val isLoading: Boolean = true,
    val isLoadingMore: Boolean = false,
    val isCopyingAll: Boolean = false,
    val error: String? = null
)

sealed interface WordAyatEvent {
    data class Load(val rootId: Int, val wordId: Int) : WordAyatEvent
    data class AyatNearingEnd(val lastVisibleIndex: Int) : WordAyatEvent
    data object Retry : WordAyatEvent

    // Platform events intercepted by the Route (owns ShareHandler).
    data object CopyAll : WordAyatEvent
    data object ShareAll : WordAyatEvent
}