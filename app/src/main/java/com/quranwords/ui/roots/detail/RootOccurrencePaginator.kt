package com.quranwords.ui.roots.detail

import androidx.lifecycle.SavedStateHandle
import com.quranwords.core.util.Result
import com.quranwords.core.util.runCatchingResult
import com.quranwords.domain.model.AyahOccurrenceModel
import com.quranwords.domain.model.RootDetail
import com.quranwords.domain.repository.QuranRepository
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.launch
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow

/** Occurrence loading and process-death restoration, isolated from the VM (P3-5). */
internal class RootOccurrencePaginator(
    private val repository: QuranRepository,
    private val savedStateHandle: SavedStateHandle,
    private val onError: (String) -> Unit
) {
    private val _occurrences = MutableStateFlow<List<AyahOccurrenceModel>>(emptyList())
    val occurrences: StateFlow<List<AyahOccurrenceModel>> = _occurrences

    private val _hasMore = MutableStateFlow(true)
    val hasMore: StateFlow<Boolean> = _hasMore

    private val _loadingMore = MutableStateFlow(false)
    val loadingMore: StateFlow<Boolean> = _loadingMore

    val rootId: Int?
        get() = currentRootId

    private var currentRootId: Int? = null
    private var offset = 0
    private val pageSize = 30
    private var totalCount = 0

    init {
        savedStateHandle.get<Int>("currentRootIdForOcc")?.let { currentRootId = it }
        savedStateHandle.get<Int>("occOffset")?.let { offset = it }
        savedStateHandle.get<Int>("occTotalCount")?.let { totalCount = it }
    }

    fun isLoaded(rootId: Int): Boolean =
        rootId == currentRootId && _occurrences.value.isNotEmpty()

    /** Resets state for a new root and returns the saved offset to restore. */
    fun resetFor(rootId: Int): Int {
        val savedRoot = savedStateHandle.get<Int>("currentRootIdForOcc")
        val savedOffset = savedStateHandle.get<Int>("occOffset") ?: 0
        val restoreTarget = if (savedRoot == rootId && savedOffset > 0) savedOffset else 0
        savedStateHandle["currentRootIdForOcc"] = rootId
        currentRootId = rootId
        offset = 0
        totalCount = 0
        _occurrences.value = emptyList()
        _hasMore.value = true
        return restoreTarget
    }

    suspend fun seed(detail: RootDetail?, restoreTarget: Int) {
        if (detail == null) return
        _occurrences.value = detail.ayatOccurrences
        offset = detail.ayatOccurrences.size
        totalCount = detail.item.occurrencesCount
        _hasMore.value = offset < totalCount
        saveOffset()
        if (restoreTarget <= offset || restoreTarget > totalCount) return
        while (offset < restoreTarget && _hasMore.value) {
            val next = when (val result = runCatchingResult {
                repository.getRootOccurrencesPaged(currentRootId!!, pageSize, offset)
            }) {
                is Result.Success -> result.data
                is Result.Error -> {
                    onError(result.message)
                    break
                }
            }
            if (next.isEmpty()) {
                _hasMore.value = false
                break
            }
            _occurrences.value = _occurrences.value + next
            offset += next.size
            saveOffset()
            _hasMore.value = offset < totalCount
        }
    }

    fun loadMoreIfNeeded(lastVisibleIndex: Int, scope: CoroutineScope) {
        if (_loadingMore.value || !_hasMore.value) return
        if (lastVisibleIndex >= _occurrences.value.size - 4) loadMore(scope)
    }

    fun loadMore(scope: CoroutineScope) {
        val rootId = currentRootId ?: return
        if (_loadingMore.value || !_hasMore.value) return
        _loadingMore.value = true
        scope.launch {
            val next = when (val result = runCatchingResult {
                repository.getRootOccurrencesPaged(rootId, pageSize, offset)
            }) {
                is Result.Success -> result.data
                is Result.Error -> {
                    onError(result.message)
                    _loadingMore.value = false
                    return@launch
                }
            }
            if (next.isNotEmpty()) {
                _occurrences.value = _occurrences.value + next
                offset += next.size
                saveOffset()
                _hasMore.value = offset < totalCount
            } else {
                _hasMore.value = false
            }
            _loadingMore.value = false
        }
    }

    private fun saveOffset() {
        savedStateHandle["occOffset"] = offset
        savedStateHandle["occTotalCount"] = totalCount
    }
}
