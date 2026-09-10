package io.github.ahmedsaadi0.quranwords.core

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class BookmarkRefTest {

  @Test
  fun `surah key round trip`() {
    val ref = BookmarkRef.parseSurah("5")
    assertEquals(BookmarkRef(surahId = 5), ref)
    assertEquals("5", ref?.key)
  }

  @Test
  fun `ayah key round trip`() {
    val ref = BookmarkRef.parseAyah("2:282")
    assertEquals(BookmarkRef(surahId = 2, ayah = 282), ref)
    assertEquals("2:282", ref?.key)
  }

  @Test
  fun `malformed keys return null instead of silent fallbacks`() {
    assertNull(BookmarkRef.parseSurah("abc"))
    assertNull(BookmarkRef.parseAyah("abc"))
    assertNull(BookmarkRef.parseAyah("2"))
    assertNull(BookmarkRef.parseAyah("a:b"))
    assertNull(BookmarkRef.parseAyah("2:"))
  }

  @Test
  fun `ayah order sorts by surah then ayah`() {
    val sorted = listOf(
      BookmarkRef(2, 282),
      BookmarkRef(1, 7),
      BookmarkRef(2, 183),
      BookmarkRef(10)
    ).sortedWith(BookmarkRef.AYAH_ORDER)
    assertEquals(
      listOf(
        BookmarkRef(1, 7),
        BookmarkRef(2, 183),
        BookmarkRef(2, 282),
        BookmarkRef(10)
      ),
      sorted
    )
  }
}