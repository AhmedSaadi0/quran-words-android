package io.github.ahmedsaadi0.quranwords.ui.surah.detail

import io.github.ahmedsaadi0.quranwords.domain.model.Ayah
import io.github.ahmedsaadi0.quranwords.domain.model.Surah
import io.github.ahmedsaadi0.quranwords.domain.model.WordToken
import io.github.ahmedsaadi0.quranwords.ui.theme.QuranFont

data class SurahDetailUiState(
    val surah: Surah? = null,
    val ayat: List<Ayah> = emptyList(),
    val isLoading: Boolean = true,
    val isLoadingMore: Boolean = false,
    val selectedWord: WordToken? = null,
    val selectedWordAyah: Ayah? = null,
    val aiSummary: String? = null,
    val aiModel: String? = null,
    val aiGeneratedAt: String? = null,
    val isAiLoading: Boolean = false,
    val fontSize: Float = 24f,
    val quranFont: QuranFont = QuranFont.KFGQPC_HAFS_1441,
    val bookmarkedSurahs: Set<String> = emptySet(),
    val bookmarkedAyat: Set<String> = emptySet(),
    val isDbReady: Boolean = false,
    val isSelectionMode: Boolean = false,
    val selectedAyahs: Set<Int> = emptySet(),
    val surahPages: List<Int> = emptyList(),
    val error: String? = null
) {
    val isSurahBookmarked: Boolean get() = bookmarkedSurahs.contains(surah?.id?.toString() ?: "")
}

sealed interface SurahDetailEvent {
    data class Load(val surahId: Int) : SurahDetailEvent
    data class WordSelected(val word: WordToken, val ayah: Ayah) : SurahDetailEvent
    data object DismissWord : SurahDetailEvent
    data class AyahVisible(val ayahNum: Int) : SurahDetailEvent
    data class NearingEnd(val lastVisibleIndex: Int) : SurahDetailEvent
    data class EnsureAyahLoaded(val ayah: Int) : SurahDetailEvent
    data class EnsurePageLoaded(val page: Int) : SurahDetailEvent
    data class ToggleAyahBookmark(val surahId: Int, val ayah: Int) : SurahDetailEvent
    data class ToggleSurahBookmark(val surahId: Int) : SurahDetailEvent
    data class SetFontSize(val size: Float) : SurahDetailEvent

    // Selection mode (long-press in selection mode extends a range via RangeSelect)
    data class EnterSelection(val ayah: Int) : SurahDetailEvent
    data class ToggleAyahSelection(val ayah: Int) : SurahDetailEvent
    data class RangeSelect(val ayah: Int) : SurahDetailEvent
    data object ClearSelection : SurahDetailEvent
    data object BookmarkSelection : SurahDetailEvent

    // Platform events intercepted by the Route (owns ShareHandler).
    data object CopySelection : SurahDetailEvent
    data object ShareSelection : SurahDetailEvent
}