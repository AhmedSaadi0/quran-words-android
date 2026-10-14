package com.quranwords.ui.surah.detail.components

import androidx.compose.foundation.lazy.LazyListState
import com.quranwords.core.util.ReadingProgress
import com.quranwords.domain.model.Surah

/**
 * Screen-level scroll derivations for the surah header — kept out of
 * `SurahDetailScreen` so the screen stays focused on composition/effects.
 *
 * Both are pure reads of [LazyListState] + the flow-block groups; callers wrap
 * them in `derivedStateOf` so only the consuming node recomposes.
 */

/**
 * Position for the top-bar rings: finds the flow block owning the viewport top
 * (basmalah index is skipped via [listOffset]), the mushaf page it sits on and
 * the smooth juz progress inside it. Returns null while nothing is loaded or
 * the page number is unavailable.
 */
fun readingProgressOf(
    listState: LazyListState,
    groups: List<AyahFlowGroup>,
    listOffset: Int,
    surah: Surah?,
    surahPages: List<Int>
): ReadingProgress? {
    if (surah == null || groups.isEmpty()) return null
    val visible = listState.layoutInfo.visibleItemsInfo
        .firstOrNull { it.index >= listOffset }
    val group = if (visible != null) {
        groups.getOrNull(visible.index - listOffset) ?: return null
    } else {
        groups.first()
    }
    val fraction = if (visible != null && visible.size > 0) {
        -visible.offset.toFloat() / visible.size
    } else {
        0f
    }
    return ReadingProgress.of(
        surahId = surah.id,
        blockFirstAyah = group.ayat.first().ayah,
        blockAyahCount = group.ayat.size,
        scrolledFraction = fraction,
        currentPage = currentPageFor(groups, listState, hasBasmalah = listOffset > 0),
        surahPages = surahPages
    )
}

/** Mushaf page chip for the viewport's first block (unpaged chunks report the nearest preceding page). */
fun currentPageFor(
    groups: List<AyahFlowGroup>,
    listState: LazyListState,
    hasBasmalah: Boolean
): Int? {
    if (groups.isEmpty()) return null
    val groupIndex = (listState.firstVisibleItemIndex - if (hasBasmalah) 1 else 0)
        .coerceIn(0, groups.size - 1)
    return groups[groupIndex].pageNumber
        ?: groups.take(groupIndex + 1).lastOrNull { it.pageNumber != null }?.pageNumber
}
