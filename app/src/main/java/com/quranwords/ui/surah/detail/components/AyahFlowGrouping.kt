package com.quranwords.ui.surah.detail.components

import com.quranwords.core.util.AyahRef
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

/** Ayah number → lazy item index of its owning block (deep-link target lookup).
 * With a straddled page [activeSurahId] is passed so a neighbour's ayah number
 * (every surah restarts at 1) can never resolve to the active surah's page. */
fun buildBlockIndexOfAyah(
    groups: List<AyahFlowGroup>,
    listOffset: Int
): Map<AyahRef, Int> =
    buildMap {
        groups.forEachIndexed { groupIndex, group ->
            val lazyIndex = groupIndex + listOffset
            group.ayat.forEach { ayah -> put(AyahRef(ayah.surah, ayah.ayah), lazyIndex) }
        }
    }

/**
 * One surah's ayat within one mushaf page. A shared page is NOT one continuous
 * text flow: every surah that starts on the page gets its own banner + Basmalah
 * and its own justified block (Juz Amma pages can hold three surahs; a short
 * surah like Al-Falaq has firstPage == lastPage with neighbours on both sides).
 * Pure Kotlin so the banner/start rules can be unit-tested.
 */
data class MushafPageSegment(
    val surahId: Int,
    val ayat: List<Ayah>,
    /** True when this surah's ayah 1 is part of this page (show banner + Basmalah). */
    val startsOnPage: Boolean,
    /** Only the active surah owns selection/bookmarks/last-read/TalkBack actions. */
    val isActiveSurah: Boolean
)

/** Splits one page's ayat into ordered surah segments, preserving page order. */
fun splitPageIntoSegments(pageAyat: List<Ayah>, activeSurahId: Int?): List<MushafPageSegment> {
    if (pageAyat.isEmpty()) return emptyList()
    val segments = mutableListOf<MushafPageSegment>()
    var i = 0
    while (i < pageAyat.size) {
        val surahId = pageAyat[i].surah
        var j = i + 1
        while (j < pageAyat.size && pageAyat[j].surah == surahId) j++
        val part = pageAyat.subList(i, j).toList()
        segments += MushafPageSegment(
            surahId = surahId,
            ayat = part,
            startsOnPage = part.any { it.ayah == 1 },
            // Legacy unpaged data has no surah context — keep everything active.
            isActiveSurah = activeSurahId == null || surahId == activeSurahId
        )
        i = j
    }
    return segments
}

/** First ayah of the active surah on a (possibly shared) page, for rings/last-read. */
fun firstActiveAyah(group: AyahFlowGroup, activeSurahId: Int?): Ayah? =
    group.ayat.firstOrNull { activeSurahId == null || it.surah == activeSurahId }

/** Count of active-surah ayat on a shared page (rings' in-page progress). */
fun activeAyahCount(group: AyahFlowGroup, activeSurahId: Int?): Int =
    if (activeSurahId == null) group.ayat.size
    else group.ayat.count { it.surah == activeSurahId }

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
