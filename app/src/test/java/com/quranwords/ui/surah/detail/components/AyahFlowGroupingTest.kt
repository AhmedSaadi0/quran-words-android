package com.quranwords.ui.surah.detail.components

import com.quranwords.core.util.AyahRef
import com.quranwords.domain.model.Ayah
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class AyahFlowGroupingTest {

    private fun ayah(num: Int, page: Int? = 604) = Ayah(
        id = num,
        surah = 1,
        ayah = num,
        textUthmani = "نص $num",
        textUthmaniPlain = "نص $num",
        textImlaei = "نص $num",
        wordCount = 3,
        pageNumber = page
    )

    @Test
    fun `empty input returns no groups`() {
        assertEquals(emptyList<AyahFlowGroup>(), groupAyatByPage(emptyList()))
    }

    @Test
    fun `single page run groups together`() {
        val ayat = (1..5).map { ayah(it, page = 604) }
        val groups = groupAyatByPage(ayat)
        assertEquals(1, groups.size)
        assertEquals("page_604", groups[0].key)
        assertEquals(604, groups[0].pageNumber)
        assertEquals(5, groups[0].ayat.size)
        assertEquals(0, groups[0].firstAyahIndex)
    }

    @Test
    fun `splits on page change with correct offsets`() {
        val ayat = listOf(ayah(1, 1), ayah(2, 1), ayah(3, 2), ayah(4, 2), ayah(5, 2))
        val groups = groupAyatByPage(ayat)
        assertEquals(2, groups.size)
        assertEquals(listOf(1, 2), groups[0].ayat.map { it.ayah })
        assertEquals(listOf(3, 4, 5), groups[1].ayat.map { it.ayah })
        assertEquals(0, groups[0].firstAyahIndex)
        assertEquals(2, groups[1].firstAyahIndex)
    }

    @Test
    fun `null pages chunk by cap with stable keys`() {
        val ayat = (1..5).map { ayah(it, page = null) }
        val groups = groupAyatByPage(ayat, unpagedCap = 2)
        assertEquals(3, groups.size)
        assertEquals(listOf(2, 2, 1), groups.map { it.ayat.size })
        assertEquals(listOf("unpaged_1", "unpaged_3", "unpaged_5"), groups.map { it.key })
        assertEquals(listOf(0, 2, 4), groups.map { it.firstAyahIndex })
        groups.forEach { assertNull(it.pageNumber) }
    }

    @Test
    fun `exact cap stays in one chunk`() {
        val ayat = (1..4).map { ayah(it, page = null) }
        val groups = groupAyatByPage(ayat, unpagedCap = 4)
        assertEquals(1, groups.size)
        assertEquals("unpaged_1", groups[0].key)
    }

    @Test
    fun `mixed paged and unpaged ayat`() {
        val ayat = listOf(
            ayah(1, 1), ayah(2, 1),
            ayah(3, null), ayah(4, null),
            ayah(5, 2)
        )
        val groups = groupAyatByPage(ayat)
        assertEquals(3, groups.size)
        assertEquals("page_1", groups[0].key)
        assertEquals("unpaged_3", groups[1].key)
        assertEquals("page_2", groups[2].key)
        assertEquals(4, groups[2].firstAyahIndex)
    }

    @Test
    fun `non-contiguous same page keeps LazyColumn keys unique`() {
        // Regression for "mushaf_page_132 already used": a null-page gap splits
        // the same mushaf page into two groups with identical group.key.
        val ayat = listOf(
            ayah(1, 132), ayah(2, 132),
            ayah(3, null),
            ayah(4, 132)
        )
        val groups = groupAyatByPage(ayat)
        assertEquals(3, groups.size)
        assertEquals(listOf("page_132", "unpaged_3", "page_132"), groups.map { it.key })
        assertEquals(groups.size, groups.map { uiKey(it) }.toSet().size)
    }

    @Test
    fun `duplicate groups keep LazyColumn keys unique and dedupe cleanly`() {
        // Defensive case: the same page appearing in two non-contiguous groups
        // with identical group.key. Grouping alone must still produce unique UI
        // keys, and a distinctBy on ayah number must collapse it back to two
        // groups.
        val firstFetch = listOf(ayah(1, 131), ayah(2, 131), ayah(3, 132), ayah(4, 132))
        val duplicated = firstFetch + firstFetch.take(2)
        val groups = groupAyatByPage(duplicated)
        assertEquals(3, groups.size)
        assertEquals(listOf("page_131", "page_132", "page_131"), groups.map { it.key })
        assertEquals(groups.size, groups.map { uiKey(it) }.toSet().size)

        val deduped = duplicated.distinctBy { it.ayah }
        assertEquals(listOf(1, 2, 3, 4), deduped.map { it.ayah })
        val dedupedGroups = groupAyatByPage(deduped)
        assertEquals(2, dedupedGroups.size)
        assertEquals(listOf("page_131", "page_132"), dedupedGroups.map { it.key })
    }

    private fun pageAyah(surah: Int, num: Int, page: Int? = 604) = ayah(num, page).copy(surah = surah)

    @Test
    fun `three-surah page splits into ordered segments with per-surah banners`() {
        // Juz Amma page: end of Al-Ikhlas, all of Al-Falaq, start of An-Nas.
        val page = listOf(
            pageAyah(112, 3), pageAyah(112, 4),
            pageAyah(113, 1), pageAyah(113, 2),
            pageAyah(114, 1), pageAyah(114, 2)
        )
        val segments = splitPageIntoSegments(page)
        assertEquals(listOf(112, 113, 114), segments.map { it.surahId })
        assertEquals(listOf(true, true, true), segments.map { it.startsOnPage })
        assertEquals(
            listOf(3, 4, 1, 2, 1, 2),
            segments.map { it.ayat.map { a -> a.ayah } }.flatten()
        )
    }

    @Test
    fun `boundary page marks continuation segment without a banner`() {
        val page = listOf(pageAyah(3, 199, 50), pageAyah(3, 200, 50), pageAyah(4, 1, 50), pageAyah(4, 2, 50))
        val segments = splitPageIntoSegments(page)
        assertEquals(listOf(3, 4), segments.map { it.surahId })
        assertEquals(listOf(false, true), segments.map { it.startsOnPage })
    }

    @Test
    fun `full page range yields one complete group for a shared Juz Amma page`() {
        // Repository's page-range query returns all surahs ordered by surah, ayah.
        val pageRange = listOf(
            pageAyah(112, 4, 604),
            pageAyah(113, 1, 604), pageAyah(113, 5, 604),
            pageAyah(114, 1, 604), pageAyah(114, 6, 604)
        )
        val groups = groupAyatByPage(pageRange)
        assertEquals(1, groups.size)
        assertEquals(604, groups[0].pageNumber)
        assertEquals(listOf(112, 113, 113, 114, 114), groups[0].ayat.map { it.surah })
        assertEquals(2, activeAyahCount(groups[0], 113))
        assertEquals(1, firstActiveAyah(groups[0], 113)?.ayah)
    }

    @Test
    fun `deep-link index ignores neighbour ayah numbers`() {
        val groups = listOf(
            AyahFlowGroup("page_50", 50, listOf(pageAyah(3, 200, 50)), 0),
            AyahFlowGroup("page_50", 50, listOf(pageAyah(4, 1, 50), pageAyah(4, 200, 50)), 1)
        )
        val index = buildBlockIndexOfAyah(groups, listOffset = 1)
        // Same ayah number, different surah — distinct keys, no collision.
        assertEquals(1, index[AyahRef(3, 200)])
        assertEquals(2, index[AyahRef(4, 200)])
        assertEquals(2, index[AyahRef(4, 1)])
    }

    @Test
    fun `active surah helpers pick the reader's ayat on a shared page`() {
        val group = AyahFlowGroup(
            "page_50", 50,
            listOf(pageAyah(3, 199, 50), pageAyah(3, 200, 50), pageAyah(4, 1, 50), pageAyah(4, 2, 50)),
            0
        )
        assertEquals(1, firstActiveAyah(group, 4)?.ayah)
        assertEquals(2, activeAyahCount(group, 4))
        assertEquals(199, firstActiveAyah(group, 3)?.ayah)
        assertEquals(2, activeAyahCount(group, 3))
    }

    @Test
    fun unpaged_group_renders_without_page_number() {
        val group = groupAyatByPage(listOf(ayah(1, page = null), ayah(2, page = null))).single()
        val chrome = mushafPageChrome(group, group.ayat)
        assertEquals(false, chrome.showHeader)
        assertEquals(false, chrome.showPageNumber)
        assertNull(group.pageNumber)
    }

    /**
     * Mirrors the LazyColumn key formula in SurahAyatList
     * (`mushaf_${group.key}_${group.firstAyahIndex}`) — keep in sync.
     */
    private fun uiKey(group: AyahFlowGroup) = "mushaf_${group.key}_${group.firstAyahIndex}"
}
