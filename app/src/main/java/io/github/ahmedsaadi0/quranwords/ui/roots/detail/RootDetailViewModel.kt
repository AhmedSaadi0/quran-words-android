package io.github.ahmedsaadi0.quranwords.ui.roots.detail

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dagger.hilt.android.lifecycle.HiltViewModel
import io.github.ahmedsaadi0.quranwords.core.util.QuranCopyFormatter
import io.github.ahmedsaadi0.quranwords.core.util.Result
import io.github.ahmedsaadi0.quranwords.core.util.SelectionState
import io.github.ahmedsaadi0.quranwords.core.util.formatAiMetaLine
import io.github.ahmedsaadi0.quranwords.core.util.runCatchingResult
import io.github.ahmedsaadi0.quranwords.domain.model.AyahOccurrenceModel
import io.github.ahmedsaadi0.quranwords.domain.model.RootDetail
import io.github.ahmedsaadi0.quranwords.domain.model.RootWordModel
import io.github.ahmedsaadi0.quranwords.domain.repository.QuranRepository
import io.github.ahmedsaadi0.quranwords.ui.roots.detail.util.RootDetailTab
import javax.inject.Inject
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

@HiltViewModel
class RootDetailViewModel @Inject constructor(
    private val repository: QuranRepository,
    private val savedStateHandle: SavedStateHandle
) : ViewModel() {

    private val _rootDetail = MutableStateFlow<RootDetail?>(null)
    private val _isLoading = MutableStateFlow(false)
    private val _error = MutableStateFlow<String?>(null)

    // Paginated ayat occurrences for the current root
    private val _occurrences = MutableStateFlow<List<AyahOccurrenceModel>>(emptyList())
    val occurrences: StateFlow<List<AyahOccurrenceModel>> = _occurrences.asStateFlow()

    private val _occurrencesHasMore = MutableStateFlow(true)
    private val _isOccurrencesLoadingMore = MutableStateFlow(false)

    private var currentRootIdForOcc: Int? = null
    private var occOffset: Int = 0
    private val occPageSize: Int = 30
    private var occTotalCount: Int = 0

    private val _isCopyingAll = MutableStateFlow(false)
    val isCopyingAll: StateFlow<Boolean> = _isCopyingAll.asStateFlow()

    // Per-action copying state (Phase 3): replaces the global boolean for UI.
    // [_isCopyingAll] is kept in sync for backward compat until all call-sites migrate.
    private val _copyingAction = MutableStateFlow<CopyAction?>(null)
    val copyingAction: StateFlow<CopyAction?> = _copyingAction.asStateFlow()

    private fun beginCopying(action: CopyAction): Boolean {
        if (_copyingAction.value != null) return false
        _copyingAction.value = action
        _isCopyingAll.value = true
        return true
    }

    private fun endCopying() {
        _copyingAction.value = null
        _isCopyingAll.value = false
    }

    // Distinct Quran words for the current root (grouped by words.id, most frequent first)
    private val _rootWords = MutableStateFlow<List<RootWordModel>>(emptyList())
    val rootWords: StateFlow<List<RootWordModel>> = _rootWords.asStateFlow()
    private val _isWordsLoading = MutableStateFlow(false)

    // Multi-word selection (long-press in Words tab)
    private val _wordSelection = MutableStateFlow(SelectionState())
    val wordSelection: StateFlow<SelectionState> = _wordSelection.asStateFlow()

    // Multi-meaning selection (long-press in Meanings tab)
    private val _meaningSelection = MutableStateFlow(SelectionState())
    val meaningSelection: StateFlow<SelectionState> = _meaningSelection.asStateFlow()

    init {
        // Restore pagination across process death if available
        savedStateHandle.get<Int>("currentRootIdForOcc")?.let { currentRootIdForOcc = it }
        savedStateHandle.get<Int>("occOffset")?.let { occOffset = it }
        savedStateHandle.get<Int>("occTotalCount")?.let { occTotalCount = it }
    }

    fun loadRootDetail(rootId: Int) {
        // Guard: if same root already loaded and data exists, keep current pagination/scroll state
        if (rootId == currentRootIdForOcc
            && _rootDetail.value?.item?.id == rootId
            && !_isLoading.value
            && _occurrences.value.isNotEmpty()
        ) return
        // Capture saved pagination for process-death restoration before overwriting
        val previousSavedRoot = savedStateHandle.get<Int>("currentRootIdForOcc")
        val savedOffset = savedStateHandle.get<Int>("occOffset") ?: 0
        val restoreTarget = if (previousSavedRoot == rootId && savedOffset > 0) savedOffset else 0
        // Persist current root for process death
        savedStateHandle["currentRootIdForOcc"] = rootId
        clearWordSelection()
        clearMeaningSelection()
        viewModelScope.launch {
            _isLoading.value = true
            _error.value = null
            currentRootIdForOcc = rootId
            occOffset = 0
            _occurrences.value = emptyList()
            _occurrencesHasMore.value = true
            occTotalCount = 0
            _rootWords.value = emptyList()
            val detail = repository.getRootDetail(rootId)
            _rootDetail.value = detail
            if (detail != null) {
                _occurrences.value = detail.ayatOccurrences
                occOffset = detail.ayatOccurrences.size
                occTotalCount = detail.item.occurrencesCount
                _occurrencesHasMore.value = occOffset < occTotalCount
                savedStateHandle["occOffset"] = occOffset
                savedStateHandle["occTotalCount"] = occTotalCount
                // Process-death restoration: fetch additional pages up to previously saved offset
                if (restoreTarget > occOffset && restoreTarget <= occTotalCount) {
                    while (occOffset < restoreTarget && _occurrencesHasMore.value) {
                        val next = repository.getRootOccurrencesPaged(rootId, occPageSize, occOffset)
                        if (next.isEmpty()) {
                            _occurrencesHasMore.value = false
                            break
                        }
                        _occurrences.value = _occurrences.value + next
                        occOffset += next.size
                        savedStateHandle["occOffset"] = occOffset
                        _occurrencesHasMore.value = occOffset < occTotalCount
                    }
                }
            }
            _isLoading.value = false
            loadRootWords(rootId)
        }
    }

    fun loadRootWords(rootId: Int) {
        viewModelScope.launch {
            _isWordsLoading.value = true
            when (val result = runCatchingResult { repository.getRootWords(rootId) }) {
                is Result.Success -> _rootWords.value = result.data
                is Result.Error -> _error.value = result.message
            }
            _isWordsLoading.value = false
        }
    }

    fun loadMoreOccurrencesIfNeeded(lastVisibleIndex: Int) {
        if (_isOccurrencesLoadingMore.value || !_occurrencesHasMore.value) return
        // lastVisibleIndex is index inside occurrences list (0-based)
        if (lastVisibleIndex >= _occurrences.value.size - 4) {
            loadMoreOccurrences()
        }
    }

    fun loadMoreOccurrences() {
        val rootId = currentRootIdForOcc ?: return
        if (_isOccurrencesLoadingMore.value || !_occurrencesHasMore.value) return
        viewModelScope.launch {
            _isOccurrencesLoadingMore.value = true
            delay(80)
            val next = repository.getRootOccurrencesPaged(rootId, occPageSize, occOffset)
            if (next.isNotEmpty()) {
                _occurrences.value = _occurrences.value + next
                occOffset += next.size
                savedStateHandle["occOffset"] = occOffset
                _occurrencesHasMore.value = occOffset < occTotalCount
            } else {
                _occurrencesHasMore.value = false
            }
            _isOccurrencesLoadingMore.value = false
        }
    }

    // Called from UI when ayat tab's LazyColumn nears bottom (with header offset already subtracted)
    fun ensureOccurrencesLoadedForCount() {
        // No-op if already has data; used if total count was unknown
        if (_occurrences.value.isEmpty() && occTotalCount > 0) {
            loadMoreOccurrences()
        }
    }

    /** Expands selected group keys to every diacritized variant id. */
    private fun expandSelectedWordIds(): List<Int> {
        val groups = _rootWords.value.associateBy { it.wordId }
        return _wordSelection.value.selectedIds.flatMap { repId ->
            groups[repId]?.allIds ?: listOf(repId)
        }.distinct()
    }

    fun clearWordSelection() {
        _wordSelection.value = SelectionState()
    }

    fun clearMeaningSelection() {
        _meaningSelection.value = SelectionState()
    }

    /**
     * Fetches ALL occurrences for current root (single query, bypasses pagination)
     * and returns formatted text for copy/share (Format B1).
     * Exposes loading via [isCopyingAll] (legacy) and [copyingAction] (per-action).
     */
    suspend fun getAllOccurrencesFormatted(
        action: CopyAction = CopyAction.COPY_OCCURRENCES_ALL,
    ): String {
        val rootId = currentRootIdForOcc ?: return ""
        if (!beginCopying(action)) return ""
        return try {
            val all = repository.getAllRootOccurrences(rootId)
            QuranCopyFormatter.formatOccurrences(all)
        } catch (_: Exception) {
            ""
        } finally {
            endCopying()
        }
    }

    /**
     * Fetches ALL ayat for the currently selected words (deduplicated, sorted)
     * and returns formatted text for copy/share. Exposes loading via [isCopyingAll].
     */
    suspend fun getSelectedWordsOccurrencesFormatted(
        action: CopyAction = CopyAction.COPY_WORDS_SELECTED,
    ): String {
        val rootId = currentRootIdForOcc ?: return ""
        val wordIds = expandSelectedWordIds()
        if (wordIds.isEmpty() || !beginCopying(action)) return ""
        return try {
            val all = repository.getAllOccurrencesForWords(rootId, wordIds)
            QuranCopyFormatter.formatOccurrences(all)
        } catch (_: Exception) {
            ""
        } finally {
            endCopying()
        }
    }

    suspend fun getSelectedWordsOccurrencesCount(): Int {
        val rootId = currentRootIdForOcc ?: return 0
        val wordIds = expandSelectedWordIds()
        if (wordIds.isEmpty()) return 0
        return try {
            repository.getAllOccurrencesForWords(rootId, wordIds).size
        } catch (_: Exception) {
            0
        }
    }

    /**
     * Formats ALL lexicon meanings for the current root for copy/share.
     * Meanings are fully loaded with the detail (no pagination).
     * Exposes loading via [isCopyingAll].
     */
    suspend fun getAllMeaningsFormatted(
        action: CopyAction = CopyAction.COPY_MEANINGS_ALL,
    ): String {
        val detail = _rootDetail.value ?: return ""
        if (!beginCopying(action)) return ""
        return try {
            QuranCopyFormatter.formatMeanings(
                detail.item.root,
                detail.meanings
            )
        } catch (_: Exception) {
            ""
        } finally {
            endCopying()
        }
    }

    /**
     * Formats only the selected lexicon meanings for copy/share.
     * Exposes loading via [isCopyingAll].
     */
    suspend fun getSelectedMeaningsFormatted(
        action: CopyAction = CopyAction.COPY_MEANINGS_SELECTED,
    ): String {
        val detail = _rootDetail.value ?: return ""
        val ids = _meaningSelection.value.selectedIds
        if (ids.isEmpty() || !beginCopying(action)) return ""
        return try {
            QuranCopyFormatter.formatMeanings(
                detail.item.root,
                detail.meanings.filter { it.id in ids }
            )
        } catch (_: Exception) {
            ""
        } finally {
            endCopying()
        }
    }

    // --- unified UiState (derived, no repo change) ---

    private val _reportVisible = MutableStateFlow(false)
    val reportVisible: StateFlow<Boolean> = _reportVisible.asStateFlow()

    fun setReportVisible(visible: Boolean) {
        _reportVisible.value = visible
    }

    /**
     * Single-collection UiState for the Route/Screen split.
     * Combines the granular flows; old flows stay as source of truth.
     */
    val uiState: StateFlow<RootDetailUiState> = combine(
        _isLoading,
        _rootDetail,
        _occurrences,
        _occurrencesHasMore,
        _isOccurrencesLoadingMore,
        _rootWords,
        _isWordsLoading,
        _wordSelection,
        _meaningSelection,
        _copyingAction,
        _reportVisible,
        _error
    ) { args ->
        @Suppress("UNCHECKED_CAST")
        val isLoading = args[0] as Boolean
        val detail = args[1] as RootDetail?
        val occ = args[2] as List<AyahOccurrenceModel>
        val hasMore = args[3] as Boolean
        val loadingMore = args[4] as Boolean
        val words = args[5] as List<RootWordModel>
        val wordsLoading = args[6] as Boolean
        val wordSelection = args[7] as SelectionState
        val meaningSelection = args[8] as SelectionState
        val copying = args[9] as CopyAction?
        val report = args[10] as Boolean
        val error = args[11] as String?

        val subtitle = detail?.aiSummary?.takeIf { it.isNotBlank() }
            ?: detail?.item?.glossAr?.takeIf { it.isNotBlank() }
        val metaLine = formatAiMetaLine(detail?.aiModel, detail?.aiGeneratedAt)
        RootDetailUiState(
            isLoading = isLoading,
            detail = detail,
            rootText = detail?.item?.root.orEmpty(),
            subtitleText = subtitle,
            hasSubtitle = !subtitle.isNullOrBlank(),
            aiMetaLine = metaLine,
            hasAiMeta = !metaLine.isNullOrBlank() ||
                !detail?.aiModel.isNullOrBlank() ||
                !detail?.aiGeneratedAt.isNullOrBlank(),
            tabs = listOf(
                RootDetailTabUi(RootDetailTab.MEANINGS, detail?.meanings?.size ?: 0),
                RootDetailTabUi(RootDetailTab.AYAT, detail?.item?.occurrencesCount ?: 0),
                RootDetailTabUi(RootDetailTab.WORDS, words.size),
                RootDetailTabUi(RootDetailTab.MASADIR, detail?.masadir?.size ?: 0),
                RootDetailTabUi(RootDetailTab.DERIVATIVES, detail?.derivatives?.size ?: 0)
            ),
            meanings = MeaningsTabState(
                meanings = detail?.meanings.orEmpty(),
                selectedIds = meaningSelection.selectedIds,
                isSelectionMode = meaningSelection.isSelectionMode
            ),
            words = WordsTabState(
                words = words,
                isLoading = wordsLoading,
                selectedIds = wordSelection.selectedIds,
                isSelectionMode = wordSelection.isSelectionMode
            ),
            ayat = AyatTabState(
                occurrences = occ,
                totalCount = detail?.item?.occurrencesCount ?: 0,
                hasMore = hasMore,
                isLoadingMore = loadingMore
            ),
            copyingAction = copying,
            isCopying = copying != null,
            reportDialogVisible = report,
            error = error
        )
    }.stateIn(viewModelScope, SharingStarted.Eagerly, RootDetailUiState())

    private val _effect = MutableSharedFlow<RootDetailEffect>(extraBufferCapacity = 8)
    val effect: SharedFlow<RootDetailEffect> = _effect

    /**
     * Single entry for UI events (UDF). State-only events are handled here;
     * copy/share/report-platform events are intercepted by the Route (which owns
     * ShareHandler + Context) and never reach this function. Navigation that
     * depends on VM state (word click vs selection) emits one-shot [effect].
     */
    fun onEvent(event: RootDetailEvent) {
        when (event) {
            is RootDetailEvent.Load -> loadRootDetail(event.rootId)
            is RootDetailEvent.ToggleMeaning -> _meaningSelection.update { it.toggle(event.id) }
            is RootDetailEvent.EnterMeaningSelection -> _meaningSelection.update { it.enter(event.id) }
            RootDetailEvent.SelectAllMeanings -> _meaningSelection.update {
                it.selectAll(_rootDetail.value?.meanings?.map { meaning -> meaning.id } ?: emptyList())
            }
            RootDetailEvent.ClearMeaningSelection -> clearMeaningSelection()
            is RootDetailEvent.WordClicked -> {
                if (_wordSelection.value.isSelectionMode) {
                    _wordSelection.update { sel -> sel.toggle(event.wordId) }
                } else {
                    _effect.tryEmit(RootDetailEffect.NavigateToWordAyat(event.rootId, event.wordId))
                }
            }
            is RootDetailEvent.WordLongPressed -> {
                if (_wordSelection.value.isSelectionMode) {
                    _wordSelection.update { sel -> sel.toggle(event.wordId) }
                } else {
                    _wordSelection.update { sel -> sel.enter(event.wordId) }
                }
            }
            RootDetailEvent.SelectAllWords -> _wordSelection.update {
                it.selectAll(_rootWords.value.map { word -> word.wordId })
            }
            RootDetailEvent.ClearWordSelection -> clearWordSelection()
            is RootDetailEvent.OccurrenceClicked ->
                _effect.tryEmit(RootDetailEffect.NavigateToSurah(event.surahId, event.ayahNum))
            is RootDetailEvent.AyatNearingEnd -> loadMoreOccurrencesIfNeeded(event.lastVisibleIndex)
            RootDetailEvent.ShowReport -> setReportVisible(true)
            RootDetailEvent.DismissReport -> setReportVisible(false)
            RootDetailEvent.Retry -> currentRootIdForOcc?.let { loadRootDetail(it) }
            // Platform events (copy/share/report markdown) are handled by the Route.
            RootDetailEvent.CopyAiSummary, RootDetailEvent.ShareAiSummary,
            RootDetailEvent.CopyAllMeanings, RootDetailEvent.ShareAllMeanings,
            RootDetailEvent.CopySelectedMeanings, RootDetailEvent.ShareSelectedMeanings,
            RootDetailEvent.CopySelectedWords, RootDetailEvent.ShareSelectedWords,
            RootDetailEvent.CopyAllOccurrences, RootDetailEvent.ShareAllOccurrences,
            is RootDetailEvent.CopyReport, is RootDetailEvent.ShareReport,
            is RootDetailEvent.OpenReportUrl -> Unit
        }
    }
}