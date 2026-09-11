package io.github.ahmedsaadi0.quranwords.core

import io.github.ahmedsaadi0.quranwords.core.util.AyahHighlighter
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class AyahHighlighterTest {

  @Test
  fun `blank matched word yields no ranges`() {
    assertTrue(AyahHighlighter.matchRanges("some ayah", "").isEmpty())
    assertTrue(AyahHighlighter.matchRanges("some ayah", "   ").isEmpty())
  }

  @Test
  fun `direct substring highlights every occurrence`() {
    val text = "abc def abc"
    val ranges = AyahHighlighter.matchRanges(text, "abc")
    assertEquals(listOf(0..2, 8..10), ranges)
  }

  @Test
  fun `no match yields no ranges`() {
    assertTrue(AyahHighlighter.matchRanges("abc def", "xyz").isEmpty())
  }

  @Test
  fun `token fallback matches across diacritics`() {
    val text = "مُحَمَّد قال الحق"
    val ranges = AyahHighlighter.matchRanges(text, "محمد")
    assertEquals(1, ranges.size)
    assertEquals("مُحَمَّد", text.substring(ranges.first().first, ranges.first().last + 1))
  }

  @Test
  fun `token fallback matches normalized alef variants`() {
    val text = "أحمد واقف"
    val ranges = AyahHighlighter.matchRanges(text, "احمد")
    assertEquals(1, ranges.size)
    assertEquals("أحمد", text.substring(ranges.first().first, ranges.first().last + 1))
  }

  @Test
  fun `token fallback highlights whole tokens only`() {
    val text = "قال قالوا قال"
    val ranges = AyahHighlighter.matchRanges(text, "قالوا")
    assertEquals(1, ranges.size)
    assertEquals("قالوا", text.substring(ranges.first().first, ranges.first().last + 1))
  }

  @Test
  fun `ranges index into ayah text not including suffix`() {
    val text = "كتب كتب كتب"
    val ranges = AyahHighlighter.matchRanges(text, "كتب")
    assertEquals(3, ranges.size)
    ranges.forEach { assertTrue(it.last < text.length) }
  }
}