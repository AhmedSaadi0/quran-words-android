package com.quranwords.core

import com.quranwords.core.util.QuranIndex
import com.quranwords.core.util.ReadingProgress
import com.quranwords.core.util.SurahMetadata
import com.quranwords.core.util.adjacentSurahTarget
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class ReadingProgressTest {

    @Test
    fun `global index anchors at the Quran boundaries`() {
        assertEquals(6236, QuranIndex.TOTAL_AYAT)
        assertEquals(1, QuranIndex.globalAyahIndex(1, 1))
        assertEquals(6236, QuranIndex.globalAyahIndex(114, 6))
        assertEquals(7 + 142, QuranIndex.globalAyahIndex(2, 142))
        assertEquals(0, QuranIndex.globalAyahIndex(0, 1))
        assertEquals(0, QuranIndex.globalAyahIndex(1, 999))
    }

    @Test
    fun `juz starts match verified metadata coordinates`() {
        assertEquals(1, QuranIndex.juzStartIndex(1))
        assertEquals(7 + 142, QuranIndex.juzStartIndex(2))
        assertEquals(7 + 253, QuranIndex.juzStartIndex(3))
        assertEquals(QuranIndex.globalAyahIndex(78, 1), QuranIndex.juzStartIndex(30))
        assertEquals(0, QuranIndex.juzStartIndex(31))
    }

    @Test
    fun `juz lengths sum to the full mushaf`() {
        assertEquals(6236, (1..30).sumOf { QuranIndex.juzLength(it) })
        // Juz 1 = 1:1 .. 2:141 → 148 ayat
        assertEquals(148, QuranIndex.juzLength(1))
        // Juz 30 = 78:1 .. 114:6
        assertEquals(QuranIndex.TOTAL_AYAT + 1 - QuranIndex.juzStartIndex(30), QuranIndex.juzLength(30))
    }

    @Test
    fun `juz lookup flips exactly at boundary indices`() {
        assertEquals(1, QuranIndex.juzAt(1))
        assertEquals(1, QuranIndex.juzAt(148)) // 2:141, last ayah of juz 1
        assertEquals(2, QuranIndex.juzAt(149)) // 2:142, first ayah of juz 2
        assertEquals(3, QuranIndex.juzAt(7 + 253))
        assertEquals(30, QuranIndex.juzAt(QuranIndex.TOTAL_AYAT))
        // clamped input
        assertEquals(1, QuranIndex.juzAt(0))
        assertEquals(30, QuranIndex.juzAt(999_999))
    }

    @Test
    fun `juz start of a block reports zero juz progress`() {
        // 2:142 is the first ayah of juz 2 → the ring must start empty.
        val progress = ReadingProgress.of(
            surahId = 2,
            blockFirstAyah = 142,
            blockAyahCount = 1,
            scrolledFraction = 0f,
            currentPage = 2,
            surahPages = (2..49).toList()
        )
        assertNotNull(progress)
        assertEquals(2, progress!!.juz)
        assertEquals(0f, progress.juzFraction, 0.0001f)
        assertEquals(2, progress.currentPage)
        assertEquals(48, progress.totalPages)
        assertEquals(1f / 48, progress.pageFraction, 0.0001f)
    }

    @Test
    fun `last ayah of a juz reports full juz progress`() {
        // 2:141 is the last ayah of juz 1 → the ring must be complete.
        val progress = ReadingProgress.of(
            surahId = 2,
            blockFirstAyah = 141,
            blockAyahCount = 1,
            scrolledFraction = 0f,
            currentPage = 2,
            surahPages = (2..49).toList()
        )
        assertNotNull(progress)
        assertEquals(1, progress!!.juz)
        assertEquals(1f, progress.juzFraction, 0.0001f)
    }

    @Test
    fun `juz progress grows between the first and last ayah of a juz`() {
        val pages = (2..49).toList()
        val first = ReadingProgress.of(2, 142, 1, 0f, 2, pages)!!
        val middle = ReadingProgress.of(2, 197, 1, 0f, 3, pages)!!
        val last = ReadingProgress.of(2, 252, 1, 0f, 3, pages)!!
        assertTrue(first.juzFraction < middle.juzFraction)
        assertTrue(middle.juzFraction < last.juzFraction)
        assertEquals(2, middle.juz)
    }

    @Test
    fun `al-baqarah spans juz 1 to 3`() {
        val juzSet = (1..286).map { ayah ->
            QuranIndex.juzAt(QuranIndex.globalAyahIndex(2, ayah))
        }.toSet()
        assertEquals(setOf(1, 2, 3), juzSet)
    }

    @Test
    fun `final ayah of the Quran is the end of juz 30`() {
        val progress = ReadingProgress.of(
            surahId = 114,
            blockFirstAyah = 1,
            blockAyahCount = 6,
            scrolledFraction = 1f,
            currentPage = 604,
            surahPages = listOf(604)
        )
        assertNotNull(progress)
        assertEquals(30, progress!!.juz)
        assertEquals(1f, progress.juzFraction, 0.0001f)
        assertEquals(1f, progress.pageFraction, 0.0001f)
    }

    @Test
    fun `page ring advances with the surah page list`() {
        val pages = listOf(2, 3, 4)
        assertEquals(1f / 3, ReadingProgress.of(2, 1, 1, 0f, 2, pages)!!.pageFraction, 0.0001f)
        assertEquals(2f / 3, ReadingProgress.of(2, 1, 1, 0f, 3, pages)!!.pageFraction, 0.0001f)
        assertEquals(1f, ReadingProgress.of(2, 1, 1, 0f, 4, pages)!!.pageFraction, 0.0001f)
        assertEquals(3, ReadingProgress.of(2, 1, 1, 0f, 4, pages)!!.totalPages)
    }

    @Test
    fun `page outside the surah list keeps its number but shows no arc`() {
        val progress = ReadingProgress.of(
            surahId = 2,
            blockFirstAyah = 1,
            blockAyahCount = 1,
            scrolledFraction = 0f,
            currentPage = 99,
            surahPages = listOf(2, 3, 4)
        )
        assertNotNull(progress)
        assertEquals(99, progress!!.currentPage)
        assertEquals(0f, progress.pageFraction, 0.0001f)
        // Empty page list (pages failed to load) degrades gracefully too.
        val noPages = ReadingProgress.of(2, 1, 1, 0f, 5, emptyList())!!
        assertEquals(0, noPages.totalPages)
        assertEquals(0f, noPages.pageFraction, 0.0001f)
        assertEquals(5, noPages.currentPage)
    }

    @Test
    fun `fractions stay within 0..1 across a sweep`() {
        for (surahId in listOf(1, 2, 9, 36, 67, 114)) {
            for (fraction in listOf(0f, 0.25f, 0.5f, 0.75f, 1f)) {
                val p = ReadingProgress.of(
                    surahId = surahId,
                    blockFirstAyah = 1,
                    blockAyahCount = SurahMetadata.SURAHS[surahId - 1].ayahCount,
                    scrolledFraction = fraction,
                    currentPage = 1,
                    surahPages = listOf(1)
                )
                assertNotNull(p)
                assertTrue(p!!.pageFraction in 0f..1f)
                assertTrue(p.juzFraction in 0f..1f)
                assertTrue(p.juz in 1..30)
            }
        }
    }

    @Test
    fun `invalid input returns null`() {
        val pages = listOf(1)
        assertNull(ReadingProgress.of(0, 1, 1, 0f, 1, pages))
        assertNull(ReadingProgress.of(1, 0, 1, 0f, 1, pages)) // blockFirstAyah < 1
        assertNull(ReadingProgress.of(1, 1, 0, 0f, 1, pages)) // blockAyahCount < 1
        assertNull(ReadingProgress.of(1, 999, 1, 0f, 1, pages)) // ayah beyond the surah
        assertNull(ReadingProgress.of(1, 1, 1, 0f, null, pages)) // page unknown → rings hidden
    }

    @Test
    fun `adjacent targets respect Quran boundaries`() {
        assertNull(adjacentSurahTarget(1, forward = false))
        assertNull(adjacentSurahTarget(114, forward = true))

        val next = adjacentSurahTarget(5, forward = true)
        assertEquals(6, next!!.surahId)
        assertEquals(1, next.ayah)
        assertTrue(next.fromNext)

        val prev = adjacentSurahTarget(5, forward = false)
        assertEquals(4, prev!!.surahId)
        assertEquals(176, prev.ayah) // an-Nisa' ayah count
        assertEquals(false, prev.fromNext)

        val boundary = adjacentSurahTarget(1, forward = true)
        assertEquals(2, boundary!!.surahId)
        assertEquals(1, boundary.ayah)
    }
}
