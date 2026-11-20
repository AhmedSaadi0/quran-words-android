package com.quranwords.ui.surah.detail

import com.quranwords.core.util.AyahRef
import com.quranwords.core.util.ReadingModePreference
import com.quranwords.domain.model.Ayah
import com.quranwords.domain.model.AiExtendedSummary
import com.quranwords.domain.model.Surah
import com.quranwords.domain.model.WordToken
import com.quranwords.ui.surah.detail.components.WordActionTarget
import com.quranwords.ui.theme.QuranFont

data class SurahDetailUiState(
    val surah: Surah? = null,
    /** Surah-scoped ayat — vertical reading mode only. */
    val ayat: List<Ayah> = emptyList(),
    /** Lazy 1..N pager: only a small window of pages is cached at a time. */
    val totalPages: Int = 0,
    val entryPage: Int = 0,
    val currentPage: Int = 0,
    val pagesByNumber: Map<Int, List<Ayah>> = emptyMap(),
    /** Pages whose load failed — the pager offers Retry instead of a spinner. */
    val failedPages: Set<Int> = emptySet(),
    val isPagesReady: Boolean = false,
    /** Surah of the settled page in Pages mode (route surah in Scroll mode). */
    val activeSurah: Surah? = null,
    val activeSurahId: Int = 0,
    /** Ayat of the current mushaf page — all surahs on it, in order. */
    val pageAyahs: List<Ayah> = emptyList(),
    /** Carried from the route so surah-to-surah switches preserve immersion. */
    val isImmersive: Boolean = false,
    val isLoading: Boolean = true,
    val selectedWord: WordToken? = null,
    val selectedWordAyah: Ayah? = null,
    /** Floating word-action toolbar; owned by the VM so rotation keeps it. */
    val wordAction: WordActionTarget? = null,
    /** Page-chip jump target that has not been applied yet. */
    val pendingPage: Int? = null,
    val fontSheetVisible: Boolean = false,
    val aiExtended: AiExtendedSummary? = null,
    val aiModel: String? = null,
    val aiGeneratedAt: String? = null,
    val pulseAyah: AyahRef? = null,
    val fontSize: Float = 24f,
    val quranFont: QuranFont = QuranFont.KFGQPC_HAFS_1441,
    /** Pill transparency while the status bar is hidden (0 opaque .. 0.8). */
    val pillTransparency: Float = 0.4f,
    /** Reading layout mode (see ReadingModePreference). */
    val readingMode: Int = ReadingModePreference.DEFAULT,
    val bookmarkedAyat: Set<String> = emptySet(),
    val isDbReady: Boolean = false,
    val isSelectionMode: Boolean = false,
    val selectedAyahs: Set<AyahRef> = emptySet(),
    val surahPages: List<Int> = emptyList(),
    val error: String? = null
)

sealed interface SurahDetailEvent {
    /**
     * @param targetAyah ayah the reader is entering at; the whole surah is
     *   loaded in one shot for Scroll mode, and its mushaf page resolved for
     *   Pages mode (which preloads the page window before going ready).
     * @param immediate skips the enter-animation warm-up delay.
     */
    data class Load(
        val surahId: Int,
        val targetAyah: Int = 1,
        val immediate: Boolean = false
    ) : SurahDetailEvent
    data class WordSelected(val word: WordToken, val ayah: Ayah) : SurahDetailEvent
    data class CopyWord(val text: String) : SurahDetailEvent
    data class SetImmersive(val value: Boolean) : SurahDetailEvent
    data class SetPulseAyah(val ref: AyahRef?) : SurahDetailEvent
    data class SetWordAction(val target: WordActionTarget?) : SurahDetailEvent
    data class SetPendingPage(val page: Int?) : SurahDetailEvent
    data class SetFontSheetVisible(val visible: Boolean) : SurahDetailEvent
    data object DismissWord : SurahDetailEvent
    data class AyahVisible(val ref: AyahRef) : SurahDetailEvent
    data class PageChanged(val page: Int) : SurahDetailEvent
    /** Mode flip: re-anchor the unified pager on the page currently being read. */
    data class SetEntryPage(val page: Int) : SurahDetailEvent
    data class RetryPage(val page: Int) : SurahDetailEvent
    data class ToggleAyahBookmark(val ref: AyahRef) : SurahDetailEvent
    data class SetFontSize(val size: Float) : SurahDetailEvent
    data class SetPillTransparency(val transparency: Float) : SurahDetailEvent
    data class SetReadingMode(val mode: Int) : SurahDetailEvent

    data class EnterSelection(val ref: AyahRef) : SurahDetailEvent
    data class ToggleAyahSelection(val ref: AyahRef) : SurahDetailEvent
    data class RangeSelect(val ref: AyahRef) : SurahDetailEvent
    data object ClearSelection : SurahDetailEvent
    data object BookmarkSelection : SurahDetailEvent

    data object CopySelection : SurahDetailEvent
    data object ShareSelection : SurahDetailEvent
}

sealed interface SurahDetailEffect {
    data class BookmarkSelectionSucceeded(val count: Int) : SurahDetailEffect
    data class BookmarkSelectionFailed(val message: String) : SurahDetailEffect
}
