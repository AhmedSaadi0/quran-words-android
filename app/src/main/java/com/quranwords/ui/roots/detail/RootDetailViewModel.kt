package com.quranwords.ui.roots.detail

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dagger.hilt.android.lifecycle.HiltViewModel
import com.quranwords.core.util.QuranCopyFormatter
import com.quranwords.core.util.Result
import com.quranwords.core.util.SelectionState
import com.quranwords.core.util.formatAiMetaLine
import com.quranwords.core.util.runCatchingResult
import com.quranwords.domain.model.AyahOccurrenceModel
import com.quranwords.domain.model.RootDetail
import com.quranwords.domain.model.RootWordModel
import com.quranwords.domain.repository.QuranRepository
import com.quranwords.domain.repository.UserPreferencesRepository
import com.quranwords.ui.roots.detail.util.RootDetailTab
import com.quranwords.ui.theme.QuranFont
import javax.inject.Inject
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

@HiltViewModel
class RootDetailViewModel @Inject constructor(
    private val repository: QuranRepository,
    private val preferences: UserPreferencesRepository,
    private val savedStateHandle: SavedStateHandle
) : ViewModel() {

    private val _rootDetail = MutableStateFlow<RootDetail?>(null)
    private val _isLoading = MutableStateFlow(false)
    private val _error = MutableStateFlow<String?>(null)

    // Paginated ayat occurrences for the current root
    private val _occurrences = MutableStateFlow<List<AyahOccurrenceModel>>(emptyList())

    private val _occurrencesHasMore = MutableStateFlow(true)
    private val _isOccurrencesLoadingMore = MutableStateFlow(false)

    private var currentRootIdForOcc: Int? = null
    private var occOffset: Int = 0
    private val occPageSize: Int = 30
    private var occTotalCount: Int = 0

    // Per-action copying state; exposed only through uiState.
    private val _copyingAction = MutableStateFlow<CopyAction?>(null)

    private fun beginCopying(action: CopyAction): Boolean {
        if (_copyingAction.value != null) return false
        _copyingAction.value = action
        return true
    }

    private fun endCopying() {
        _copyingAction.value = null
    }

    // Distinct Quran words for the current root (grouped by words.id, most frequent first)
    private val _rootWords = MutableStateFlow<List<RootWordModel>>(emptyList())
    private val _isWordsLoading = MutableStateFlow(false)

    // Multi-word selection (long-press in Words tab)
    private val _wordSelection = MutableStateFlow(SelectionState())

    // Multi-meaning selection (long-press in Meanings tab)
    private val _meaningSelection = MutableStateFlow(SelectionState())

    // Shared reading font size (same DataStore key as SurahDetail).
    private val _fontSize = MutableStateFlow(24f)

    // Selected mushaf typeface (same DataStore key as SurahDetail).
    private val _quranFontState = MutableStateFlow(QuranFont.KFGQPC_HAFS_1441)

    init {
        // Restore pagination across process death if available
        savedStateHandle.get<Int>("currentRootIdForOcc")?.let { currentRootIdForOcc = it }
        savedStateHandle.get<Int>("occOffset")?.let { occOffset = it }
        savedStateHandle.get<Int>("occTotalCount")?.let { occTotalCount = it }
        viewModelScope.launch {
            preferences.fontSize.collectLatest { _fontSize.value = it }
        }
        viewModelScope.launch {
            preferences.quranFontKey.collectLatest { _quranFontState.value = QuranFont.fromKey(it) }
        }
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
            val detail = when (val result = runCatchingResult { repository.getRootDetail(rootId) }) {
                is Result.Success -> result.data
                is Result.Error -> {
                    _error.value = result.message
                    _isLoading.value = false
                    return@launch
                }
            }
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
                        val next = when (val result = runCatchingResult {
                            repository.getRootOccurrencesPaged(rootId, occPageSize, occOffset)
                        }) {
                            is Result.Success -> result.data
                            is Result.Error -> {
                                _error.value = result.message
                                break
                            }
                        }
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
            val next = when (val result = runCatchingResult {
                repository.getRootOccurrencesPaged(rootId, occPageSize, occOffset)
            }) {
                is Result.Success -> result.data
                is Result.Error -> {
                    _error.value = result.message
                    _isOccurrencesLoadingMore.value = false
                    return@launch
                }
            }
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
     */
    suspend fun getAllOccurrencesFormatted(
        action: CopyAction = CopyAction.COPY_OCCURRENCES_ALL,
    ): Result<String> {
        val rootId = currentRootIdForOcc ?: return Result.Success("")
        if (!beginCopying(action)) return Result.Success("")
        return try {
            runCatchingResult { QuranCopyFormatter.formatOccurrences(repository.getAllRootOccurrences(rootId)) }
        } finally {
            endCopying()
        }
    }

    /**
     * Fetches ALL ayat for the currently selected words (deduplicated, sorted)
     * and returns formatted text for copy/share. Loading is exposed via [copyingAction].
     */
    suspend fun getSelectedWordsOccurrencesFormatted(
        action: CopyAction = CopyAction.COPY_WORDS_SELECTED,
    ): Result<String> {
        val rootId = currentRootIdForOcc ?: return Result.Success("")
        val wordIds = expandSelectedWordIds()
        if (wordIds.isEmpty() || !beginCopying(action)) return Result.Success("")
        return try {
            runCatchingResult { QuranCopyFormatter.formatOccurrences(repository.getAllOccurrencesForWords(rootId, wordIds)) }
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
     * Exposes loading via [copyingAction].
     */
    suspend fun getAllMeaningsFormatted(
        action: CopyAction = CopyAction.COPY_MEANINGS_ALL,
    ): Result<String> {
        val detail = _rootDetail.value ?: return Result.Success("")
        if (!beginCopying(action)) return Result.Success("")
        return try {
            runCatchingResult { QuranCopyFormatter.formatMeanings(detail.item.root, detail.meanings) }
        } finally {
            endCopying()
        }
    }

    /**
     * Formats only the selected lexicon meanings for copy/share.
     * Exposes loading via [copyingAction].
     */
    suspend fun getSelectedMeaningsFormatted(
        action: CopyAction = CopyAction.COPY_MEANINGS_SELECTED,
    ): Result<String> {
        val detail = _rootDetail.value ?: return Result.Success("")
        val ids = _meaningSelection.value.selectedIds
        if (ids.isEmpty() || !beginCopying(action)) return Result.Success("")
        return try {
            runCatchingResult {
                QuranCopyFormatter.formatMeanings(
                    detail.item.root,
                    detail.meanings.filter { it.id in ids }
                )
            }
        } finally {
            endCopying()
        }
    }

    // --- unified UiState (derived, no repo change) ---

    private val _reportVisible = MutableStateFlow(false)

    fun setReportVisible(visible: Boolean) {
        _reportVisible.value = visible
    }

    private data class CoreFlows(
        val isLoading: Boolean,
        val detail: RootDetail?,
        val occurrences: List<AyahOccurrenceModel>,
        val hasMore: Boolean,
        val loadingMore: Boolean
    )

    private data class TabFlows(
        val words: List<RootWordModel>,
        val wordsLoading: Boolean,
        val wordSelection: SelectionState,
        val meaningSelection: SelectionState,
        val copying: CopyAction?
    )

    private data class TailFlows(
        val core: CoreFlows,
        val tabs: TabFlows,
        val copying: CopyAction?,
        val report: Boolean,
        val error: String?,
        val fontSize: Float
    )

    /** Single authoritative state for the Route/Screen split (P2-1/P2-2). */
    val uiState: StateFlow<RootDetailUiState> = run {
        val coreFlow = combine(
            _isLoading, _rootDetail, _occurrences, _occurrencesHasMore, _isOccurrencesLoadingMore
        ) { isLoading, detail, occurrences, hasMore, loadingMore ->
            CoreFlows(isLoading, detail, occurrences, hasMore, loadingMore)
        }
        val tabsFlow = combine(
            coreFlow, _rootWords, _isWordsLoading, _wordSelection, _meaningSelection
        ) { core, words, wordsLoading, wordSelection, meaningSelection ->
            core to TabFlows(words, wordsLoading, wordSelection, meaningSelection, null)
        }
        val tailFlow = combine(
            tabsFlow, _copyingAction, _reportVisible, _error, _fontSize
        ) { pair, copying, report, error, fontSize ->
            TailFlows(pair.first, pair.second.copy(copying = copying), copying, report, error, fontSize)
        }
        combine(tailFlow, _quranFontState) { tail, quranFont ->
            val core = tail.core
            val tabs = tail.tabs
            val detail = core.detail
            val metaLine = formatAiMetaLine(detail?.aiModel, detail?.aiGeneratedAt)
            RootDetailUiState(
                isLoading = core.isLoading,
                detail = detail,
                rootText = detail?.item?.root.orEmpty(),
                aiMetaLine = metaLine,
                hasAiMeta = !metaLine.isNullOrBlank() ||
                    !detail?.aiModel.isNullOrBlank() ||
                    !detail?.aiGeneratedAt.isNullOrBlank(),
                tabs = listOf(
                    RootDetailTabUi(
                        RootDetailTab.AI_SUMMARY,
                        detail?.aiExtended?.sections()?.count { it.isNotBlank() } ?: 0
                    ),
                    RootDetailTabUi(RootDetailTab.MEANINGS, detail?.meanings?.size ?: 0),
                    RootDetailTabUi(RootDetailTab.AYAT, detail?.item?.occurrencesCount ?: 0),
                    RootDetailTabUi(RootDetailTab.WORDS, tabs.words.size),
                    RootDetailTabUi(RootDetailTab.MASADIR, detail?.masadir?.size ?: 0),
                    RootDetailTabUi(RootDetailTab.DERIVATIVES, detail?.derivatives?.size ?: 0)
                ),
                meanings = MeaningsTabState(
                    meanings = detail?.meanings.orEmpty(),
                    selectedIds = tabs.meaningSelection.selectedIds,
                    isSelectionMode = tabs.meaningSelection.isSelectionMode
                ),
                words = WordsTabState(
                    words = tabs.words,
                    isLoading = tabs.wordsLoading,
                    selectedIds = tabs.wordSelection.selectedIds,
                    isSelectionMode = tabs.wordSelection.isSelectionMode
                ),
                ayat = AyatTabState(
                    occurrences = core.occurrences,
                    totalCount = detail?.item?.occurrencesCount ?: 0,
                    hasMore = core.hasMore,
                    isLoadingMore = core.loadingMore
                ),
                copyingAction = tail.copying,
                reportDialogVisible = tail.report,
                fontSize = tail.fontSize,
                quranFont = quranFont,
                error = tail.error
            )
        }
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
            is RootDetailEvent.CopyAiSection,
            RootDetailEvent.CopyAllMeanings, RootDetailEvent.ShareAllMeanings,
            RootDetailEvent.CopySelectedMeanings, RootDetailEvent.ShareSelectedMeanings,
            RootDetailEvent.CopySelectedWords, RootDetailEvent.ShareSelectedWords,
            RootDetailEvent.CopyAllOccurrences, RootDetailEvent.ShareAllOccurrences,
            is RootDetailEvent.CopyReport, is RootDetailEvent.ShareReport,
            is RootDetailEvent.OpenReportUrl -> Unit
        }
    }
}