package com.quranwords.ui.surah.detail

import com.quranwords.core.util.AyahRef
import com.quranwords.core.util.AyahSelectionState
import com.quranwords.domain.model.Ayah
import com.quranwords.domain.model.AiExtendedSummary
import com.quranwords.domain.model.Surah
import com.quranwords.domain.model.WordToken
import com.quranwords.ui.surah.detail.components.WordActionTarget
import com.quranwords.ui.theme.QuranFont

internal const val KEY_INITIAL_IMMERSIVE = "initialImmersive"
internal const val KEY_IMMERSIVE_STATE = "immersiveState"
internal const val KEY_FONT_SHEET_VISIBLE = "fontSheetVisible"
internal const val KEY_PENDING_PAGE = "pendingPage"
internal const val PREFETCH_RADIUS = 2

internal data class ReaderPrefsA(
    val fontSize: Float,
    val quranFont: QuranFont,
    val bookmarks: Set<String>
)

/** Everything resolved during initial Load, emitted in one atomic update. */
internal data class LoadedReader(
    val surah: Surah?,
    val ayat: List<Ayah>,
    val pageCount: Int,
    val entry: Int,
    val activeId: Int,
    val entryAyat: List<Ayah>
)

/** One atomic state object keeps the many reader slices in sync. */
internal data class ReaderCoreState(
    val surah: Surah? = null,
    val ayat: List<Ayah> = emptyList(),
    val totalPages: Int = 0,
    val entryPage: Int = 0,
    val currentPage: Int = 0,
    val pagesByNumber: Map<Int, List<Ayah>> = emptyMap(),
    val failedPages: Set<Int> = emptySet(),
    val isPagesReady: Boolean = false,
    val activeSurah: Surah? = null,
    val activeSurahId: Int = 0,
    val pageAyahs: List<Ayah> = emptyList(),
    val isImmersive: Boolean = false,
    val isLoading: Boolean = true,
    val selectedWord: WordToken? = null,
    val selectedWordAyah: Ayah? = null,
    val wordAction: WordActionTarget? = null,
    val pendingPage: Int? = null,
    val fontSheetVisible: Boolean = false,
    val aiExtended: AiExtendedSummary? = null,
    val aiModel: String? = null,
    val aiGeneratedAt: String? = null,
    val pulseAyah: AyahRef? = null,
    val selection: AyahSelectionState = AyahSelectionState(),
    val surahPages: List<Int> = emptyList(),
    val error: String? = null
)
