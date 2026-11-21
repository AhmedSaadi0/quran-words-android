package com.quranwords.ui.roots.detail

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.quranwords.core.util.FontSizePreference
import com.quranwords.core.util.Result
import com.quranwords.core.util.SelectionState
import com.quranwords.core.util.formatAiMetaLine
import com.quranwords.core.util.runCatchingResult
import com.quranwords.domain.repository.QuranRepository
import com.quranwords.domain.repository.UserPreferencesRepository
import com.quranwords.ui.roots.detail.util.RootDetailTab
import com.quranwords.ui.theme.QuranFont
import dagger.hilt.android.lifecycle.HiltViewModel
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
    private val savedStateHandle: SavedStateHandle,
    private val copyFormatter: RootCopyFormatter = RootCopyFormatter(repository)
) : ViewModel() {

    private val paginator = RootOccurrencePaginator(repository, savedStateHandle) { _error.value = it }

    private val _rootDetail = MutableStateFlow<com.quranwords.domain.model.RootDetail?>(null)
    private val _isLoading = MutableStateFlow(false)
    private val _error = MutableStateFlow<String?>(null)
    private val _rootWords = MutableStateFlow<List<com.quranwords.domain.model.RootWordModel>>(emptyList())
    private val _isWordsLoading = MutableStateFlow(false)
    private val _wordSelection = MutableStateFlow(SelectionState())
    private val _meaningSelection = MutableStateFlow(SelectionState())
    private val _fontSize = MutableStateFlow(FontSizePreference.DEFAULT)
    private val _quranFontState = MutableStateFlow(QuranFont.KFGQPC_HAFS_1441)
    private val _reportVisible = MutableStateFlow(false)

    init {
        viewModelScope.launch { preferences.fontSize.collectLatest { _fontSize.value = it } }
        viewModelScope.launch { preferences.quranFontKey.collectLatest { _quranFontState.value = QuranFont.fromKey(it) } }
    }

    fun loadRootDetail(rootId: Int) {
        if (paginator.isLoaded(rootId) && _rootDetail.value?.item?.id == rootId && !_isLoading.value) return
        val restoreTarget = paginator.resetFor(rootId)
        clearWordSelection()
        clearMeaningSelection()
        viewModelScope.launch {
            _isLoading.value = true
            _error.value = null
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
            paginator.seed(detail, restoreTarget)
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

    fun loadMoreOccurrencesIfNeeded(lastVisibleIndex: Int) =
        paginator.loadMoreIfNeeded(lastVisibleIndex, viewModelScope)

    fun loadMoreOccurrences() = paginator.loadMore(viewModelScope)

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

    suspend fun getAllOccurrencesFormatted(action: CopyAction = CopyAction.COPY_OCCURRENCES_ALL): Result<String> =
        copyFormatter.allOccurrences(paginator.rootId, action)

    suspend fun getSelectedWordsOccurrencesFormatted(
        action: CopyAction = CopyAction.COPY_WORDS_SELECTED
    ): Result<String> = copyFormatter.selectedWordOccurrences(paginator.rootId, expandSelectedWordIds(), action)

    suspend fun getSelectedWordsOccurrencesCount(): Int =
        copyFormatter.selectedWordsCount(paginator.rootId, expandSelectedWordIds())

    suspend fun getAllMeaningsFormatted(action: CopyAction = CopyAction.COPY_MEANINGS_ALL): Result<String> =
        copyFormatter.meanings(_rootDetail.value, null, action)

    suspend fun getSelectedMeaningsFormatted(
        action: CopyAction = CopyAction.COPY_MEANINGS_SELECTED
    ): Result<String> = copyFormatter.meanings(_rootDetail.value, _meaningSelection.value.selectedIds, action)

    fun setReportVisible(visible: Boolean) {
        _reportVisible.value = visible
    }

    /** Single authoritative state for the Route/Screen split (P2-1/P2-2). */
    val uiState: StateFlow<RootDetailUiState> = run {
        val coreFlow = combine(
            _isLoading, _rootDetail, paginator.occurrences, paginator.hasMore, paginator.loadingMore
        ) { isLoading, detail, occurrences, hasMore, loadingMore ->
            CoreFlows(isLoading, detail, occurrences, hasMore, loadingMore)
        }
        val tabsFlow = combine(
            coreFlow, _rootWords, _isWordsLoading, _wordSelection, _meaningSelection
        ) { core, words, wordsLoading, wordSelection, meaningSelection ->
            core to TabFlows(words, wordsLoading, wordSelection, meaningSelection, null)
        }
        val tailFlow = combine(
            tabsFlow, copyFormatter.copyingAction, _reportVisible, _error, _fontSize
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
            RootDetailEvent.Retry -> paginator.rootId?.let(::loadRootDetail)
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
