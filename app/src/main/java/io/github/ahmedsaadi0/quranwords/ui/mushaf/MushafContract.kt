package io.github.ahmedsaadi0.quranwords.ui.mushaf

import androidx.compose.ui.text.font.FontFamily
import io.github.ahmedsaadi0.quranwords.domain.model.Ayah
import io.github.ahmedsaadi0.quranwords.domain.model.MushafPage
import io.github.ahmedsaadi0.quranwords.domain.model.WordToken

/** Preview scope (spike): bundled fonts + sidecar DB cover these pages. */
val MUSHAF_DEMO_PAGES = listOf(1, 2, 3, 531, 602)

data class MushafPageData(
    val page: MushafPage,
    val fontFamily: FontFamily,
    /**
     * Shared `U+FDFD` font for basmalah lines; null when unavailable — the UI
     * then falls back to the Unicode basmalah string, never a blank line.
     */
    val basmalahFont: FontFamily? = null
)

data class MushafUiState(
    val currentPage: Int = MUSHAF_DEMO_PAGES.first(),
    val loaded: Map<Int, MushafPageData> = emptyMap(),
    val loadingPages: Set<Int> = emptySet(),
    val error: String? = null,
    val selectedWord: WordToken? = null,
    val selectedWordAyah: Ayah? = null,
    val aiSummary: String? = null,
    val aiModel: String? = null,
    val aiGeneratedAt: String? = null,
    val isAiLoading: Boolean = false,
    /** Set when a tapped word cannot be resolved (main DB missing). */
    val wordLookupFailed: Boolean = false
)

sealed interface MushafEvent {
    data class PageSettled(val page: Int) : MushafEvent
    data class WordTapped(val wordAyahId: Int) : MushafEvent
    data object DismissWord : MushafEvent
    data object Retry : MushafEvent
    data object WordLookupFailedShown : MushafEvent
}
