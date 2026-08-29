package io.github.ahmedsaadi0.quranwords.ui.surah

import io.github.ahmedsaadi0.quranwords.domain.model.Ayah
import io.github.ahmedsaadi0.quranwords.domain.model.Surah
import io.github.ahmedsaadi0.quranwords.domain.model.WordToken

data class SurahDetailUiState(
    val isLoading: Boolean = true,
    val surah: Surah? = null,
    val ayat: List<Ayah> = emptyList(),
    val isLoadingMore: Boolean = false,
    val selectedWord: WordToken? = null,
    val selectedAyah: Ayah? = null,
    val targetAyah: Int = 1,
    val error: String? = null
)

sealed interface SurahDetailEvent {
    data class WordClicked(val word: WordToken, val ayah: Ayah) : SurahDetailEvent
    data object DismissWordSheet : SurahDetailEvent
    data class LoadMore(val lastVisibleIndex: Int) : SurahDetailEvent
    data class BookmarkAyah(val ayahNum: Int) : SurahDetailEvent
    data object ToggleSurahBookmark : SurahDetailEvent
    data class ChangeFontSize(val delta: Float) : SurahDetailEvent
}

sealed interface SurahDetailEffect {
    data class NavigateToRoot(val rootId: Int) : SurahDetailEffect
    data class ScrollToAyah(val ayahNum: Int) : SurahDetailEffect
}
