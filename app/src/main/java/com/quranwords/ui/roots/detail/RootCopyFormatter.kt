package com.quranwords.ui.roots.detail

import com.quranwords.core.util.QuranCopyFormatter
import com.quranwords.core.util.Result
import com.quranwords.core.util.runCatchingResult
import com.quranwords.domain.model.RootDetail
import com.quranwords.domain.repository.QuranRepository
import javax.inject.Inject
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow

/** Copy/share payloads and per-action gating, isolated from the VM (P3-5). */
class RootCopyFormatter @Inject constructor(private val repository: QuranRepository) {

    private val _copyingAction = MutableStateFlow<CopyAction?>(null)
    val copyingAction: StateFlow<CopyAction?> = _copyingAction

    private fun begin(action: CopyAction): Boolean {
        if (_copyingAction.value != null) return false
        _copyingAction.value = action
        return true
    }

    private fun end() {
        _copyingAction.value = null
    }

    suspend fun allOccurrences(rootId: Int?, action: CopyAction): Result<String> {
        if (rootId == null || !begin(action)) return Result.Success("")
        return try {
            runCatchingResult { QuranCopyFormatter.formatOccurrences(repository.getAllRootOccurrences(rootId)) }
        } finally {
            end()
        }
    }

    suspend fun selectedWordOccurrences(
        rootId: Int?,
        wordIds: List<Int>,
        action: CopyAction
    ): Result<String> {
        if (rootId == null || wordIds.isEmpty() || !begin(action)) return Result.Success("")
        return try {
            runCatchingResult {
                QuranCopyFormatter.formatOccurrences(repository.getAllOccurrencesForWords(rootId, wordIds))
            }
        } finally {
            end()
        }
    }

    fun meanings(detail: RootDetail?, ids: Set<Int>?, action: CopyAction): Result<String> {
        if (detail == null || (ids != null && ids.isEmpty()) || !begin(action)) return Result.Success("")
        return try {
            runCatchingResult {
                val selected = ids?.let { set -> detail.meanings.filter { it.id in set } }
                    ?: detail.meanings
                QuranCopyFormatter.formatMeanings(detail.item.root, selected)
            }
        } finally {
            end()
        }
    }

    suspend fun selectedWordsCount(rootId: Int?, wordIds: List<Int>): Int {
        if (rootId == null || wordIds.isEmpty()) return 0
        return when (val result = runCatchingResult {
            repository.getAllOccurrencesForWords(rootId, wordIds).size
        }) {
            is Result.Success -> result.data
            is Result.Error -> 0
        }
    }
}
