package io.github.ahmedsaadi0.quranwords.core

import io.github.ahmedsaadi0.quranwords.core.util.ArabicNormalizer
import org.junit.Assert.assertEquals
import org.junit.Test

class ArabicNormalizerTest {

  @Test
  fun `stripDiacritics removes harakat and sukun`() {
    assertEquals("محمد", ArabicNormalizer.stripDiacritics("مُحَمَّد"))
    assertEquals("الرحمن", ArabicNormalizer.stripDiacritics("الرَّحْمَـٰنِ"))
  }

  @Test
  fun `stripDiacritics removes tatweel`() {
    assertEquals("محمد", ArabicNormalizer.stripDiacritics("مـحـمـد"))
  }

  @Test
  fun `stripDiacritics keeps plain text untouched`() {
    assertEquals("ابقعو", ArabicNormalizer.stripDiacritics("ابقعو"))
  }

  @Test
  fun `normalizeAr unifies alef variants`() {
    assertEquals("احمد", ArabicNormalizer.normalizeAr("أحمد"))
    assertEquals("احمد", ArabicNormalizer.normalizeAr("إحمد"))
    assertEquals("احمد", ArabicNormalizer.normalizeAr("آحمد"))
    assertEquals("اسلام", ArabicNormalizer.normalizeAr("ٱسلام"))
  }

  @Test
  fun `normalizeAr unifies alef maqsura to ya`() {
    assertEquals("علي", ArabicNormalizer.normalizeAr("على"))
  }

  @Test
  fun `normalizeAr unifies taa marbuta to haa`() {
    assertEquals("فاطمه", ArabicNormalizer.normalizeAr("فاطمة"))
  }

  @Test
  fun `normalizeAr unifies waw and ya with hamza`() {
    assertEquals("مومن", ArabicNormalizer.normalizeAr("مؤمن"))
    assertEquals("شيي", ArabicNormalizer.normalizeAr("شيء"))
  }

  @Test
  fun `normalizeAr combines diacritics and letter unification`() {
    // "ٱلرَّحْمَٰنِ" -> strip diacritics (incl. U+0670 and tatweel) -> "ٱلرحمٰن" -> "الرحمن"
    assertEquals("الرحمن", ArabicNormalizer.normalizeAr("ٱلرَّحْمَٰنِ"))
    assertEquals("انعام", ArabicNormalizer.normalizeAr("الأَنعام"))
  }

  @Test
  fun `normalizeAr blank and empty inputs return empty string`() {
    assertEquals("", ArabicNormalizer.normalizeAr(""))
    assertEquals("", ArabicNormalizer.normalizeAr("   "))
    assertEquals("", ArabicNormalizer.normalizeAr("َ ِ ُ"))
  }

  @Test
  fun `normalizeAr trims surrounding whitespace`() {
    assertEquals("محمد", ArabicNormalizer.normalizeAr("  محمد  "))
  }
}
