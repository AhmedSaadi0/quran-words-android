package io.github.ahmedsaadi0.quranwords.core.util

import org.junit.Assert.assertEquals
import org.junit.Test

class ArabicNormalizerTest {
    @Test
    fun `stripDiacritics removes tashkeel`() {
        assertEquals("الرحمن", ArabicNormalizer.stripDiacritics("الرَّحْمَٰنِ"))
        assertEquals("بسم", ArabicNormalizer.stripDiacritics("بِسْمِ"))
    }

    @Test
    fun `normalizeAr handles hamza and ta marbuta`() {
        assertEquals("ا", ArabicNormalizer.normalizeAr("أ"))
        assertEquals("ا", ArabicNormalizer.normalizeAr("إ"))
        assertEquals("ا", ArabicNormalizer.normalizeAr("آ"))
        assertEquals("ه", ArabicNormalizer.normalizeAr("ة"))
        assertEquals("ي", ArabicNormalizer.normalizeAr("ى"))
        assertEquals("ا", ArabicNormalizer.normalizeAr("ٱ"))
    }

    @Test
    fun `normalizeAr trims and handles blank`() {
        assertEquals("", ArabicNormalizer.normalizeAr(""))
        assertEquals("", ArabicNormalizer.normalizeAr("   "))
        assertEquals("كتب", ArabicNormalizer.normalizeAr(" كتب "))
    }

    @Test
    fun `normalizeAr strips diacritics and normalizes`() {
        assertEquals("الرحمن", ArabicNormalizer.normalizeAr("الرَّحْمَٰنِ"))
        assertEquals("كتاب", ArabicNormalizer.normalizeAr("كِتَابٌ"))
    }
}
