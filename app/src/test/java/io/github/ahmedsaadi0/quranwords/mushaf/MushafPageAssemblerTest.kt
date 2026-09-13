package io.github.ahmedsaadi0.quranwords.mushaf

import io.github.ahmedsaadi0.quranwords.domain.model.MushafLineType
import io.github.ahmedsaadi0.quranwords.domain.model.MushafPageMetaRow
import io.github.ahmedsaadi0.quranwords.domain.model.MushafSpecialRow
import io.github.ahmedsaadi0.quranwords.domain.model.MushafWordRow
import io.github.ahmedsaadi0.quranwords.domain.mushaf.MushafPageAssembler
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class MushafPageAssemblerTest {

  private fun meta(page: Int = 2, minLine: Int = 1, maxLine: Int = 8) =
    MushafPageMetaRow(page, 2, 1, 2, 5, 1, minLine, maxLine)

  private fun word(line: Int, pos: Int, id: Int? = 100 + line * 10 + pos, type: String = "word") =
    MushafWordRow(id, 2, line, pos, "\uFC41", type, 2, 1, pos)

  @Test
  fun `merges header basmalah and text lines in printed order`() {
    val page = MushafPageAssembler.assemble(
      meta(),
      listOf(word(3, 1), word(3, 2, type = "end"), word(4, 1)),
      listOf(
        MushafSpecialRow(2, 1, MushafLineType.SURAH_HEADER, 2),
        MushafSpecialRow(2, 2, MushafLineType.BASMALAH, 2)
      )
    )
    assertEquals(2, page.pageNumber)
    assertEquals(listOf(1, 2, 3, 4), page.lines.map { it.lineNumber })
    assertEquals(MushafLineType.SURAH_HEADER, page.lines[0].lineType)
    assertEquals(2, page.lines[0].surahId)
    assertTrue(page.lines[0].words.isEmpty())
    assertEquals(MushafLineType.BASMALAH, page.lines[1].lineType)
    assertEquals(MushafLineType.TEXT, page.lines[2].lineType)
    assertNull(page.lines[2].surahId)
    assertEquals(2, page.lines[2].words.size)
  }

  @Test
  fun `orders glyphs by posInLine regardless of input order`() {
    val page = MushafPageAssembler.assemble(
      meta(minLine = 3, maxLine = 3),
      listOf(word(3, 3, id = 3), word(3, 1, id = 1), word(3, 2, id = 2)),
      emptyList()
    )
    assertEquals(listOf(3), page.lines.map { it.lineNumber })
    assertEquals(listOf(1, 2, 3), page.lines[0].words.map { it.wordAyahId })
  }

  @Test
  fun `short pages keep only content lines`() {
    val page = MushafPageAssembler.assemble(
      meta(page = 1, minLine = 1, maxLine = 8),
      listOf(word(2, 1)),
      listOf(MushafSpecialRow(1, 1, MushafLineType.SURAH_HEADER, 1))
    )
    assertEquals(listOf(1, 2), page.lines.map { it.lineNumber })
  }

  @Test
  fun `end markers carry null wordAyahId and marker flag`() {
    val page = MushafPageAssembler.assemble(
      meta(minLine = 3, maxLine = 3),
      listOf(word(3, 1, id = 31), word(3, 2, id = null, type = "end")),
      emptyList()
    )
    val marker = page.lines[0].words[1]
    assertNull(marker.wordAyahId)
    assertTrue(marker.isAyahMarker)
  }

  @Test(expected = IllegalStateException::class)
  fun `gap line with no content fails fast`() {
    MushafPageAssembler.assemble(
      meta(minLine = 1, maxLine = 3),
      listOf(word(3, 1)),
      emptyList()
    )
  }

  @Test(expected = IllegalArgumentException::class)
  fun `line range beyond 15 is rejected`() {
    MushafPageAssembler.assemble(meta(minLine = 1, maxLine = 16), emptyList(), emptyList())
  }
}
