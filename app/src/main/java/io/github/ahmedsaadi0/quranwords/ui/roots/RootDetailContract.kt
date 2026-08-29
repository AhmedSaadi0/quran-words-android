package io.github.ahmedsaadi0.quranwords.ui.roots

import io.github.ahmedsaadi0.quranwords.domain.model.DerivativeModel
import io.github.ahmedsaadi0.quranwords.domain.model.MasdarModel
import io.github.ahmedsaadi0.quranwords.domain.model.RootDetail
import io.github.ahmedsaadi0.quranwords.domain.model.RootMeaningModel
import io.github.ahmedsaadi0.quranwords.domain.model.AyahOccurrenceModel

data class RootDetailUiState(
    val isLoading: Boolean = true,
    val rootDetail: RootDetail? = null,
    val occurrences: List<AyahOccurrenceModel> = emptyList(),
    val isOccurrencesLoadingMore: Boolean = false,
    val hasMoreOccurrences: Boolean = true,
    val selectedTab: Int = 0,
    val error: String? = null
)

sealed interface RootDetailEvent {
    data class TabSelected(val index: Int) : RootDetailEvent
    data class AyahClicked(val surahId: Int, val ayahNum: Int) : RootDetailEvent
    data object LoadMoreOccurrences : RootDetailEvent
    data object Retry : RootDetailEvent
}

sealed interface RootDetailEffect {
    data class NavigateToSurah(val surahId: Int, val ayahNum: Int) : RootDetailEffect
    data class ShowSnackbar(val message: String) : RootDetailEffect
}
