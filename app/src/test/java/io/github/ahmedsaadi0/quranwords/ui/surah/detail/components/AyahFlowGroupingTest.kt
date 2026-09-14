package io.github.ahmedsaadi0.quranwords.ui.surah.detail.components

import io.github.ahmedsaadi0.quranwords.domain.model.Ayah
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
}
