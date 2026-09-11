package io.github.ahmedsaadi0.quranwords.ui

import io.github.ahmedsaadi0.quranwords.core.util.SurahMeta
import io.github.ahmedsaadi0.quranwords.domain.model.Ayah
import io.github.ahmedsaadi0.quranwords.ui.mushaf.model.MushafSegment
import io.github.ahmedsaadi0.quranwords.ui.mushaf.model.buildMushafPage
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class BuildMushafPageTest {

  private val meta: (Int) -> SurahMeta? = { id ->
    when (id) {
      1 -> SurahMeta(1, "الفاتحة", "Al-Fatihah", 7, "مكية", 1)
      2 -> SurahMeta(2, "البقرة", "Al-Baqarah", 286, "مدنية", 1)
      9 -> SurahMeta(9, "التوبة", "At-Tawbah", 129, "مدنية", 10)
      else -> null
    }
  }

  private fun ayah(surah: Int, num: Int, page: Int = 2) = Ayah(
    id = surah * 1000 + num,
    surah = surah,
    ayah = num,
    textUthmani = "t",
    textUthmaniPlain = "p",
    textImlaei = "i",
    wordCount = 1,
    pageNumber = page
  )

  @Test
  fun `mid-surah page yields single text run without banner`() {
    val page = buildMushafPage(2, listOf(ayah(2, 5), ayah(2, 6)), meta)

    assertEquals(1, page.segments.size)
    val run = page.segments[0] as MushafSegment.TextRun
    assertEquals(listOf(5, 6), run.ayat.map { it.ayah })
  }

  @Test
  fun `surah start at page top yields banner with basmalah then text`() {
    val page = buildMushafPage(2, listOf(ayah(2, 1), ayah(2, 2)), meta)

    assertEquals(2, page.segments.size)
    val banner = page.segments[0] as MushafSegment.SurahStart
    assertEquals(2, banner.surahId)
    assertEquals("البقرة", banner.nameAr)
    assertEquals("مدنية", banner.revelationType)
    assertEquals(286, banner.ayahCount)
    assertTrue(banner.showBasmalah)
    assertEquals(listOf(1, 2), (page.segments[1] as MushafSegment.TextRun).ayat.map { it.ayah })
  }

  @Test
  fun `tawbah start hides basmalah`() {
    val page = buildMushafPage(3, listOf(ayah(9, 1)), meta)

    val banner = page.segments[0] as MushafSegment.SurahStart
    assertEquals(9, banner.surahId)
    assertFalse(banner.showBasmalah)
  }

  @Test
  fun `fatihah start hides separate basmalah line`() {
    val page = buildMushafPage(1, listOf(ayah(1, 1)), meta)

    val banner = page.segments[0] as MushafSegment.SurahStart
    assertFalse(banner.showBasmalah)
  }

  @Test
  fun `mid-page surah transition yields inline banner between runs`() {
    val ayat = listOf(ayah(1, 6), ayah(1, 7), ayah(2, 1), ayah(2, 2))
    val page = buildMushafPage(2, ayat, meta)

    assertEquals(3, page.segments.size)
    assertEquals(listOf(6, 7), (page.segments[0] as MushafSegment.TextRun).ayat.map { it.ayah })
    assertEquals(2, (page.segments[1] as MushafSegment.SurahStart).surahId)
    assertEquals(listOf(1, 2), (page.segments[2] as MushafSegment.TextRun).ayat.map { it.ayah })
  }

  @Test
  fun `empty ayat yields empty segments`() {
    val page = buildMushafPage(5, emptyList(), meta)

    assertTrue(page.segments.isEmpty())
    assertTrue(page.ayat.isEmpty())
  }

  @Test
  fun `missing metadata still emits banner with blank labels`() {
    val page = buildMushafPage(10, listOf(ayah(114, 1, page = 10)), meta)

    val banner = page.segments[0] as MushafSegment.SurahStart
    assertEquals(114, banner.surahId)
    assertEquals("", banner.nameAr)
    assertTrue(banner.showBasmalah)
  }
}
