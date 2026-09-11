package io.github.ahmedsaadi0.quranwords.ui.mushaf

import io.github.ahmedsaadi0.quranwords.domain.model.Ayah
import io.github.ahmedsaadi0.quranwords.domain.model.WordToken
import io.github.ahmedsaadi0.quranwords.ui.mushaf.model.MushafPageUi

data class MushafReaderUiState(
    val totalPages: Int = 604,
    /** 1-based page the pager must open once loading finishes; null until resolved. */
    val initialPage: Int? = null,
    val isInitialLoad: Boolean = true,
    /** 1-based current page, updated on [MushafReaderEvent.PageSettled]. */
    val currentPage: Int = 1,
    val pages: Map<Int, MushafPageUi> = emptyMap(),
    val loadingPages: Set<Int> = emptySet(),
    /** True = top bar + bottom slider hidden (tap toggles). Phase 1: in-app bars only. */
    val isImmersive: Boolean = false,
    val bookmarkedAyat: Set<String> = emptySet(),
    val selectedWord: WordToken? = null,
    val selectedWordAyah: Ayah? = null,
    val aiSummary: String? = null,
    val aiModel: String? = null,
    val aiGeneratedAt: String? = null,
    val isAiLoading: Boolean = false,
    val isDbReady: Boolean = false,
    val error: String? = null
)

sealed interface MushafReaderEvent {
    /** Cold open: resolve (surah, ayah) to its Mushaf page via getAyahWithWords. */
    data class OpenAtAyah(val surahId: Int, val ayah: Int) : MushafReaderEvent
    /** Cold open: jump straight to a known 1-based page. */
    data class OpenAtPage(val page: Int) : MushafReaderEvent
    /** Pager reports its settled position (single source of position truth). */
    data class PageSettled(val page: Int) : MushafReaderEvent
    /** Tap on empty page area toggles immersive bars. */
    data object PageTapped : MushafReaderEvent
    /** Tap on a word inside the justified body. */
    data class WordTapped(val wordAyahId: Int, val position: Int) : MushafReaderEvent
    /** Tap on an ayah-end marker toggles its bookmark. */
    data class MarkerTapped(val surahId: Int, val ayah: Int) : MushafReaderEvent
    data object DismissWord : MushafReaderEvent
}

sealed interface MushafReaderEffect {
    data object NavigateBack : MushafReaderEffect
}
