package io.github.ahmedsaadi0.quranwords.ui.roots.word

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dagger.hilt.android.lifecycle.HiltViewModel
import io.github.ahmedsaadi0.quranwords.core.util.QuranCopyFormatter
import io.github.ahmedsaadi0.quranwords.core.util.Result
import io.github.ahmedsaadi0.quranwords.core.util.runCatchingResult
import io.github.ahmedsaadi0.quranwords.domain.model.AyahOccurrenceModel
import io.github.ahmedsaadi0.quranwords.domain.repository.QuranRepository
import javax.inject.Inject
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

@HiltViewModel
class WordAyatViewModel @Inject constructor(
    private val repository: QuranRepository
) : ViewModel() {

    private val _wordText = MutableStateFlow("")
    val wordText: StateFlow<String> = _wordText.asStateFlow()

    private val _occurrences = MutableStateFlow<List<AyahOccurrenceModel>>(emptyList())
    val occurrences: StateFlow<List<AyahOccurrenceModel>> = _occurrences.asStateFlow()

    private val _hasMore = MutableStateFlow(true)
    val hasMore: StateFlow<Boolean> = _hasMore.asStateFlow()

    private val _isLoading = MutableStateFlow(true)
    val isLoading: StateFlow<Boolean> = _isLoading.asStateFlow()

    private val _isLoadingMore = MutableStateFlow(false)
    val isLoadingMore: StateFlow<Boolean> = _isLoadingMore.asStateFlow()

    private val _isCopyingAll = MutableStateFlow(false)
    val isCopyingAll: StateFlow<Boolean> = _isCopyingAll.asStateFlow()

    private val _totalCount = MutableStateFlow(0)
    val totalCount: StateFlow<Int> = _totalCount.asStateFlow()

    private val _error = MutableStateFlow<String?>(null)

    private var currentRootId: Int? = null
    private var currentWordId: Int? = null
    private var currentWordIds: List<Int> = emptyList()
    private var offset: Int = 0
    private val pageSize: Int = 30

    /**
     * Single-collection UiState; granular flows above stay as source of truth
     * until Phase 11 closure (RootDetail adapter precedent, Decision 14).
     */
    val uiState: StateFlow<WordAyatUiState> = combine(
        _wordText,
        _occurrences,
        _totalCount,
        _hasMore,
        _isLoading,
        _isLoadingMore,
        _isCopyingAll,
        _error
    ) { values ->
        @Suppress("UNCHECKED_CAST")
        WordAyatUiState(
            wordText = values[0] as String,
            occurrences = values[1] as List<AyahOccurrenceModel>,
            totalCount = values[2] as Int,
            hasMore = values[3] as Boolean,
            isLoading = values[4] as Boolean,
            isLoadingMore = values[5] as Boolean,
            isCopyingAll = values[6] as Boolean,
            error = values[7] as String?
        )
    }.stateIn(viewModelScope, SharingStarted.Eagerly, WordAyatUiState())

    fun onEvent(event: WordAyatEvent) {
        when (event) {
            is WordAyatEvent.Load -> loadWord(event.rootId, event.wordId)
            is WordAyatEvent.AyatNearingEnd -> loadMoreIfNeeded(event.lastVisibleIndex)
            WordAyatEvent.Retry -> retry()
            // Platform events (copy/share) are handled by the Route.
            WordAyatEvent.CopyAll, WordAyatEvent.ShareAll -> Unit
        }
    }

    fun loadWord(rootId: Int, wordId: Int) {
        if (rootId == currentRootId && wordId == currentWordId && _occurrences.value.isNotEmpty()) return
        currentRootId = rootId
        currentWordId = wordId
        offset = 0
        _occurrences.value = emptyList()
        _hasMore.value = true
        _isLoading.value = true
        _wordText.value = ""
        _totalCount.value = 0
        _error.value = null
        viewModelScope.launch {
            when (val result = runCatchingResult { loadFirstPage(rootId, wordId) }) {
                is Result.Success -> Unit // state mutated inside loadFirstPage
                is Result.Error -> {
                    _occurrences.value = emptyList()
                    _hasMore.value = false
                    _error.value = result.message
                }
            }
            _isLoading.value = false
        }
    }

    private suspend fun loadFirstPage(rootId: Int, wordId: Int) {
        val words = repository.getRootWords(rootId)
        // Expand the representative id to its whole diacritized group.
        val matched = words.firstOrNull { it.wordId == wordId }
            ?: words.firstOrNull { wordId in it.allIds }
        currentWordIds = matched?.allIds ?: listOf(wordId)
        _wordText.value = matched?.text ?: ""
        _totalCount.value = matched?.occurrencesCount ?: 0
        val first = repository.getWordOccurrencesPaged(rootId, currentWordIds, pageSize, 0)
        _occurrences.value = first
        offset = first.size
        if (_totalCount.value < first.size) {
            _totalCount.value = first.size
        }
        _hasMore.value = if (_totalCount.value > 0) {
            offset < _totalCount.value
        } else {
            first.size == pageSize
        }
    }

    fun loadMoreIfNeeded(lastVisibleIndex: Int) {
        if (_isLoadingMore.value || !_hasMore.value) return
        if (lastVisibleIndex >= _occurrences.value.size - 4) {
            loadMore()
        }
    }

    private fun retry() {
        val rootId = currentRootId
        val wordId = currentWordId
        if (rootId == null || wordId == null) return
        if (_occurrences.value.isEmpty()) {
            loadWord(rootId, wordId)
        } else {
            loadMore()
        }
    }

    private fun loadMore() {
        val rootId = currentRootId ?: return
        val wordIds = currentWordIds.ifEmpty { return }
        if (_isLoadingMore.value || !_hasMore.value) return
        viewModelScope.launch {
            _isLoadingMore.value = true
            delay(80)
            when (val result = runCatchingResult {
                repository.getWordOccurrencesPaged(rootId, wordIds, pageSize, offset)
            }) {
                is Result.Success -> {
                    _error.value = null
                    val next = result.data
                    if (next.isNotEmpty()) {
                        _occurrences.value = _occurrences.value + next
                        offset += next.size
                        if (_totalCount.value < _occurrences.value.size) {
                            _totalCount.value = _occurrences.value.size
                        }
                        _hasMore.value = if (_totalCount.value > 0) {
                            offset < _totalCount.value
                        } else {
                            next.size == pageSize
                        }
                    } else {
                        _hasMore.value = false
                    }
                }
                is Result.Error -> {
                    _hasMore.value = false
                    _error.value = result.message
                }
            }
            _isLoadingMore.value = false
        }
    }

    /** Suspend formatter consumed by the Route's ShareHandler (copy/share all). */
    suspend fun getAllFormatted(): String {
        val rootId = currentRootId ?: return ""
        val wordIds = currentWordIds.ifEmpty { return "" }
        if (_isCopyingAll.value) return ""
        _isCopyingAll.value = true
        return try {
            val all = repository.getAllWordOccurrences(rootId, wordIds)
            QuranCopyFormatter.formatOccurrences(all)
        } catch (_: Exception) {
            "" // ShareHandler surfaces the empty-message fallback.
        } finally {
            _isCopyingAll.value = false
        }
    }
}