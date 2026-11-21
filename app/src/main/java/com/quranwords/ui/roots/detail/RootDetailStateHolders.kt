package com.quranwords.ui.roots.detail

import com.quranwords.core.util.SelectionState
import com.quranwords.domain.model.AyahOccurrenceModel
import com.quranwords.domain.model.RootDetail
import com.quranwords.domain.model.RootWordModel

internal data class CoreFlows(
    val isLoading: Boolean,
    val detail: RootDetail?,
    val occurrences: List<AyahOccurrenceModel>,
    val hasMore: Boolean,
    val loadingMore: Boolean
)

internal data class TabFlows(
    val words: List<RootWordModel>,
    val wordsLoading: Boolean,
    val wordSelection: SelectionState,
    val meaningSelection: SelectionState,
    val copying: CopyAction?
)

internal data class TailFlows(
    val core: CoreFlows,
    val tabs: TabFlows,
    val copying: CopyAction?,
    val report: Boolean,
    val error: String?,
    val fontSize: Float
)
