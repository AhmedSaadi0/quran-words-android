package com.quranwords.ui.surah.detail

import com.quranwords.domain.model.Ayah
import com.quranwords.domain.model.AiExtendedSummary
import com.quranwords.domain.model.Surah
import com.quranwords.domain.model.WordToken
import com.quranwords.ui.theme.QuranFont

data class SurahDetailUiState(
    val surah: Surah? = null,
    val ayat: List<Ayah> = emptyList(),
    val isLoading: Boolean = true,
    val selectedWord: WordToken? = null,
    val selectedWordAyah: Ayah? = null,
    val aiSummary: String? = null,
    val aiExtended: AiExtendedSummary? = null,
    val aiModel: String? = null,
    val aiGeneratedAt: String? = null,
    val isAiLoading: Boolean = false,
    val fontSize: Float = 24f,
    val quranFont: QuranFont = QuranFont.KFGQPC_HAFS_1441,
    val bookmarkedAyat: Set<String> = emptySet(),
    val isDbReady: Boolean = false,
    val isSelectionMode: Boolean = false,
    val selectedAyahs: Set<Int> = emptySet(),
    val surahPages: List<Int> = emptyList(),
    val error: String? = null
)

sealed interface SurahDetailEvent {
    /**
     * @param targetAyah ayah the reader is entering at; the whole surah is
     *   loaded in one shot, so the target is always present and the list can
     *   be placed at it with no render-at-ayah-1 frame and no scroll hunt.
     * @param immediate skips the enter-animation warm-up delay — used by
     * adjacent-surah switches whose vertical slide is already playing.
     */
    data class Load(
        val surahId: Int,
        val targetAyah: Int = 1,
        val immediate: Boolean = false
    ) : SurahDetailEvent
    data class WordSelected(val word: WordToken, val ayah: Ayah) : SurahDetailEvent
    data object DismissWord : SurahDetailEvent
    data class AyahVisible(val ayahNum: Int) : SurahDetailEvent
    data class ToggleAyahBookmark(val surahId: Int, val ayah: Int) : SurahDetailEvent
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