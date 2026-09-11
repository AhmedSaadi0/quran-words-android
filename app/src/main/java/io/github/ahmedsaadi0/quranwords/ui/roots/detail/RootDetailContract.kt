package io.github.ahmedsaadi0.quranwords.ui.roots.detail

import io.github.ahmedsaadi0.quranwords.domain.model.AyahOccurrenceModel
import io.github.ahmedsaadi0.quranwords.domain.model.RootDetail
import io.github.ahmedsaadi0.quranwords.domain.model.RootMeaningModel
import io.github.ahmedsaadi0.quranwords.domain.model.RootWordModel
import io.github.ahmedsaadi0.quranwords.ui.roots.detail.util.RootDetailTab

/**
 * UDF contract for RootDetail (Phase 3).
 *
 * [RootDetailUiState] is derived in [io.github.ahmedsaadi0.quranwords.ui.roots.detail.RootDetailViewModel]
 * via `combine` of the existing granular flows — no repository change.
 * Old granular flows stay (deprecated) until the UI fully migrates.
 */
enum class CopyAction {
    COPY_AI_SUMMARY,
    SHARE_AI_SUMMARY,
    COPY_MEANINGS_ALL,
    SHARE_MEANINGS_ALL,
    COPY_MEANINGS_SELECTED,
    SHARE_MEANINGS_SELECTED,
    COPY_WORDS_SELECTED,
    SHARE_WORDS_SELECTED,
    COPY_OCCURRENCES_ALL,
    SHARE_OCCURRENCES_ALL,
}

data class MeaningsTabState(
    val meanings: List<RootMeaningModel> = emptyList(),
    val selectedIds: Set<Int> = emptySet(),
    val isSelectionMode: Boolean = false,
)

data class WordsTabState(
    val words: List<RootWordModel> = emptyList(),
    val isLoading: Boolean = false,
    val selectedIds: Set<Int> = emptySet(),
    val isSelectionMode: Boolean = false,
)

data class AyatTabState(
    val occurrences: List<AyahOccurrenceModel> = emptyList(),
    val totalCount: Int = 0,
    val hasMore: Boolean = true,
    val isLoadingMore: Boolean = false,
)

data class RootDetailTabUi(
    val tab: RootDetailTab,
    val count: Int,
)

data class RootDetailUiState(
    val isLoading: Boolean = true,
    val detail: RootDetail? = null,
    val rootText: String = "",
    val subtitleText: String? = null,
    val hasSubtitle: Boolean = false,
    val aiMetaLine: String? = null,
    val hasAiMeta: Boolean = false,
    val tabs: List<RootDetailTabUi> = emptyList(),
    val meanings: MeaningsTabState = MeaningsTabState(),
    val words: WordsTabState = WordsTabState(),
    val ayat: AyatTabState = AyatTabState(),
    val copyingAction: CopyAction? = null,
    /** Derived: true while any copy/share is in flight (backward compat for old `isCopyingAll`). */
    val isCopying: Boolean = false,
    val reportDialogVisible: Boolean = false,
    val error: String? = null,
)

sealed interface RootDetailEvent {
    data class Load(val rootId: Int) : RootDetailEvent
    data object CopyAiSummary : RootDetailEvent
    data object ShareAiSummary : RootDetailEvent
    data object ShowReport : RootDetailEvent
    data object DismissReport : RootDetailEvent
    data class CopyReport(val markdown: String) : RootDetailEvent
    data class ShareReport(val markdown: String) : RootDetailEvent
    data class OpenReportUrl(val url: String) : RootDetailEvent

    data class ToggleMeaning(val id: Int) : RootDetailEvent
    data class EnterMeaningSelection(val id: Int) : RootDetailEvent
    data object SelectAllMeanings : RootDetailEvent
    data object ClearMeaningSelection : RootDetailEvent
    data object CopyAllMeanings : RootDetailEvent
    data object ShareAllMeanings : RootDetailEvent
    data object CopySelectedMeanings : RootDetailEvent
    data object ShareSelectedMeanings : RootDetailEvent

    data class WordClicked(val rootId: Int, val wordId: Int) : RootDetailEvent
    data class WordLongPressed(val wordId: Int) : RootDetailEvent
    data object SelectAllWords : RootDetailEvent
    data object ClearWordSelection : RootDetailEvent
    data object CopySelectedWords : RootDetailEvent
    data object ShareSelectedWords : RootDetailEvent

    data class OccurrenceClicked(val surahId: Int, val ayahNum: Int) : RootDetailEvent
    data class AyatNearingEnd(val lastVisibleIndex: Int) : RootDetailEvent
    data object CopyAllOccurrences : RootDetailEvent
    data object ShareAllOccurrences : RootDetailEvent
    data object Retry : RootDetailEvent
}

sealed interface RootDetailEffect {
    data class NavigateToSurah(val surahId: Int, val ayahNum: Int) : RootDetailEffect
    data class NavigateToWordAyat(val rootId: Int, val wordId: Int) : RootDetailEffect
}
