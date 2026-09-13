package io.github.ahmedsaadi0.quranwords.mushaf

import io.github.ahmedsaadi0.quranwords.core.font.MushafFontResolver
import io.github.ahmedsaadi0.quranwords.core.util.MushafConstants
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test
import java.io.File

class MushafConstantsTest {

  @Test
  fun `page bounds accept 1 and 604 only`() {
    assertTrue(MushafConstants.isValidPage(1))
    assertTrue(MushafConstants.isValidPage(604))
    assertFalse(MushafConstants.isValidPage(0))
    assertFalse(MushafConstants.isValidPage(605))
  }

  @Test
  fun `font file and url follow per-page template`() {
    assertEquals("p1.ttf", MushafConstants.fontFileName(1))
    assertEquals("p604.ttf", MushafConstants.fontFileName(604))
    assertEquals(
      "https://verses.quran.foundation/fonts/quran/hafs/v2/ttf/p531.ttf",
      MushafConstants.fontUrl(531)
    )
  }

  @Test(expected = IllegalArgumentException::class)
  fun `font file rejects out-of-range page`() {
    MushafConstants.fontFileName(605)
  }

  @Test
  fun `eastern arabic numerals render footer page numbers`() {
    assertEquals("١", MushafConstants.toEasternArabic(1))
    assertEquals("٦٠٤", MushafConstants.toEasternArabic(604))
    assertEquals("١٥", MushafConstants.toEasternArabic(15))
  }
}

class MushafFontResolverTest {

  private val filesDir = File("/tmp/mushaf-test-files")

  @Test
  fun `cached file lives under fonts qpc dir`() {
    val f = MushafFontResolver.cachedFile(filesDir, 531)
    assertEquals(File(File(filesDir, "fonts/qpc"), "p531.ttf"), f)
  }

  @Test
  fun `prefetch window centers page plus-minus 3 clamped`() {
    assertEquals((1..4).toList(), MushafFontResolver.prefetchWindow(1))
    assertEquals((601..604).toList(), MushafFontResolver.prefetchWindow(604))
    assertEquals((528..534).toList(), MushafFontResolver.prefetchWindow(531))
    assertEquals(MushafConstants.FONT_CACHE_SIZE, MushafFontResolver.prefetchWindow(300).size)
  }

  @Test
  fun `missing cache file reports false`() {
    assertFalse(MushafFontResolver.isCached(File("/tmp/mushaf-no-such-dir-xyz"), 1))
    assertFalse(MushafFontResolver.isCached(filesDir, 0))
  }
}
