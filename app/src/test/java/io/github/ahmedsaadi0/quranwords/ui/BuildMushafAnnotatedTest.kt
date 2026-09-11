package io.github.ahmedsaadi0.quranwords.ui

import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.font.FontWeight
import io.github.ahmedsaadi0.quranwords.domain.model.Ayah
import io.github.ahmedsaadi0.quranwords.domain.model.WordToken
import io.github.ahmedsaadi0.quranwords.ui.mushaf.components.buildMushafAnnotated
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class BuildMushafAnnotatedTest {

  private val markerStyle = SpanStyle(color = Color.Black, fontWeight = FontWeight.Bold)

  private fun word(wordAyahId: Int, position: Int, text: String) = WordToken(
    wordId = wordAyahId,
    wordAyahId = wordAyahId,
    position = position,
    text = text,
    textClean = text,
    translation = "",
    rootId = null,
    rootText = null,
    pos = null,
    posNameAr = null,
    form = null,
    formNameAr = null,
    aspect = null,
    mood = null,
    voice = null,
    person = null,
    gender = null,
    number = null,
    grammaticalCase = null,
    state = null,
    derivation = null,
    special = null,
    segments = null
  )

  private fun ayah(surah: Int, num: Int, words: List<WordToken> = emptyList(), text: String = "plain") = Ayah(
    id = surah * 1000 + num,
    surah = surah,
    ayah = num,
    textUthmani = text,
    textUthmaniPlain = text,
    textImlaei = text,
    wordCount = words.size,
    words = words,
    pageNumber = 1
  )

  @Test
  fun `words and markers concatenate in order with annotations`() {
    val ayat = listOf(
      ayah(2, 5, listOf(word(101, 1, "a"), word(102, 2, "b"))),
      ayah(2, 6, listOf(word(103, 1, "c")))
    )

    val annotated = buildMushafAnnotated(ayat, markerStyle)

    assertEquals("a b ﴿٥﴾ c ﴿٦﴾ ", annotated.text)
    assertEquals("101:1", annotated.getStringAnnotations(0, 1).single().item)
    assertEquals("word", annotated.getStringAnnotations(0, 1).single().tag)
    assertEquals("102:2", annotated.getStringAnnotations(2, 3).single().item)
    assertEquals("2:5", annotated.getStringAnnotations(4, 7).single().item)
    assertEquals("marker", annotated.getStringAnnotations(4, 7).single().tag)
    assertEquals("103:1", annotated.getStringAnnotations(8, 9).single().item)
    assertEquals("2:6", annotated.getStringAnnotations(10, 13).single().item)
  }

  @Test
  fun `wordless ayah falls back to plain text with marker only`() {
    val annotated = buildMushafAnnotated(listOf(ayah(3, 1)), markerStyle)

    assertEquals("plain ﴿١﴾ ", annotated.text)
    assertTrue(annotated.getStringAnnotations(0, 5).none { it.tag == "word" })
    assertEquals("3:1", annotated.getStringAnnotations(6, 9).single().item)
  }

  @Test
  fun `empty ayat list builds empty string`() {
    val annotated = buildMushafAnnotated(emptyList(), markerStyle)

    assertEquals("", annotated.text)
  }
}
