package com.quranwords.ui.surah.detail.components

import com.quranwords.domain.model.Ayah

/**
 * One continuous-flow text unit: a run of ayat rendered together inside a
 * single [MushafFlowBlock] Text, wrapping naturally across the line width
 * like the printed Mushaf. Pure Kotlin, no Compose dependencies — the single
 * source of truth for grouping, shared by [SurahAyatList] (rendering) and
 * the screen (scroll-index mapping).
 */
data class AyahFlowGroup(
    val key: String,
    val pageNumber: Int?,
    val ayat: List<Ayah>,
    /** Index of the group's first ayah in the source list (separators + scroll mapping). */
    val firstAyahIndex: Int
)

/** Max ayat per fallback chunk when [Ayah.pageNumber] is missing. */
const val UNPAGED_GROUP_CAP = 10

/**
 * Groups [ayat] (surah order) into flow blocks: maximal runs of equal
 * non-null [Ayah.pageNumber]; consecutive null-page ayat accumulate into
 * fallback chunks capped at [unpagedCap] so blocks stay bounded.
 */
fun groupAyatByPage(ayat: List<Ayah>, unpagedCap: Int = UNPAGED_GROUP_CAP): List<AyahFlowGroup> {
    val groups = mutableListOf<AyahFlowGroup>()
    var i = 0
    while (i < ayat.size) {
        val page = ayat[i].pageNumber
        if (page != null) {
            var j = i
            while (j < ayat.size && ayat[j].pageNumber == page) j++
            groups.add(
                AyahFlowGroup(
                    key = "page_$page",
                    pageNumber = page,
                    ayat = ayat.subList(i, j).toList(),
                    firstAyahIndex = i
                )
            )
            i = j
        } else {
            var j = i
            while (j < ayat.size && ayat[j].pageNumber == null && j - i < unpagedCap) j++
            val chunk = ayat.subList(i, j).toList()
            groups.add(
                AyahFlowGroup(
                    key = "unpaged_${chunk.first().ayah}",
                    pageNumber = null,
                    ayat = chunk,
                    firstAyahIndex = i
                )
            )
            i = j
        }
    }
    return groups
}

/** Ayah number → lazy item index of its owning block (deep-link target lookup). */
fun buildBlockIndexOfAyah(groups: List<AyahFlowGroup>, listOffset: Int): Map<Int, Int> =
    buildMap {
        groups.forEachIndexed { groupIndex, group ->
            val lazyIndex = groupIndex + listOffset
            group.ayat.forEach { ayah -> put(ayah.ayah, lazyIndex) }
        }
    }

/**
 * Chrome visibility flags for one flow group inside its surah — precomputed
 * by [mushafPageChrome] and shared by the vertical list renderer
 * ([MushafPageContent]) and the book-pages pager, so both render
 * byte-identical chrome.
 */
data class MushafPageChromeFlags(
    /** Surah | juz | page header row above the block. */
    val showHeader: Boolean,
    /** Whether this group opens a new hizb (surfaces in the page header). */
    val isHizbStart: Boolean,
    /** Page-number separator below the block. */
    val showPageNumber: Boolean
)

/**
 * Pure chrome-flag resolution for [group] within the full [ayat] list:
 * the page-number boundary is detected from the ayah preceding the group's
 * first ayah, and a hizb start surfaces in the page header (the standalone
 * juz/hizb separator was removed — juz/hizb live in the header line).
 */
fun mushafPageChrome(group: AyahFlowGroup, ayat: List<Ayah>): MushafPageChromeFlags {
    val first = group.ayat.first()
    val prevAyah = ayat.getOrNull(group.firstAyahIndex - 1)
    val isPageStart = prevAyah?.pageNumber != first.pageNumber
    return MushafPageChromeFlags(
        showHeader = first.pageNumber != null,
        isHizbStart = prevAyah?.hizb != first.hizb,
        showPageNumber = isPageStart && first.pageNumber != null
    )
}
