package io.github.ahmedsaadi0.quranwords.core

import io.github.ahmedsaadi0.quranwords.core.util.sanitizeUthmanicText
import org.junit.Assert.assertEquals
import org.junit.Assert.assertSame
import org.junit.Test

/**
 * Vectors mirror the audited rows of ayah 2:5 in `quran_words.db`, built from
 * explicit escapes (never pasted glyphs) so mark order is unambiguous.
 */
class UthmanicSanitizerTest {

    // هُدًۭى (words.id 33)
    private val hudanRaw = "ه" + "ُ" + "د" + "ً" + "ۭ" + "ى"
    private val hudanClean = "ه" + "ُ" + "د" + "ً" + "ى"

    // أُو۟لَـٰٓئِكَ (words.id 52): tatweel + dagger + maddah in DB order
    private val ulaikaRaw = "أ" + "ُ" + "و" + "۟" + "ل" + "َ" + "ـ" + "ٰ" + "ٓ" +
        "ئ" + "ِ" + "ك" + "َ"
    private val ulaikaClean = "أ" + "ُ" + "و" + "ْ" + "ل" + "َ" + "ٰ" + "ٓ" +
        "ئ" + "ِ" + "ك" + "َ"

    // رَّبِّهِمْ ۖ (words.id 55)
    private val rabbihimRaw = "ر" + "ّ" + "َ" + "ب" + "ّ" + "ِ" + "ه" + "ِ" +
        "م" + "ْ" + " " + "ۖ"
    private val rabbihimClean = "ر" + "ّ" + "َ" + "ب" + "ّ" + "ِ" + "ه" + "ِ" +
        "م" + "ْ" + "ۖ"

    @Test
    fun `strips small low meem after tanween`() {
        assertEquals(hudanClean, sanitizeUthmanicText(hudanRaw))
    }

    @Test
    fun `drops tatweel before dagger and normalizes rounded zero to sukun`() {
        assertEquals(ulaikaClean, sanitizeUthmanicText(ulaikaRaw))
    }

    @Test
    fun `reorders maddah plus dagger to canonical order`() {
        assertEquals("ٰ" + "ٓ", sanitizeUthmanicText("ٓ" + "ٰ"))
    }

    @Test
    fun `trims inner space before pause mark but keeps the mark`() {
        assertEquals(rabbihimClean, sanitizeUthmanicText(rabbihimRaw))
    }

    @Test
    fun `keeps leading space before pause mark at string start`() {
        assertEquals(" " + "ۖ", sanitizeUthmanicText(" " + "ۖ"))
    }

    @Test
    fun `strips zero-width artifacts`() {
        assertEquals("هدى", sanitizeUthmanicText("ه" + "\u200C" + "دى"))
        assertEquals("هدى", sanitizeUthmanicText("ه" + "\u200B" + "دى"))
    }

    @Test
    fun `preserves already sanitized text including sukun`() {
        val clean = "أ" + "ُ" + "و" + "ْ" + "ل" + "َ" + "ٰ" + "ٓ" + "ئ" + "ِ" + "ك" + "َ"
        assertEquals(clean, sanitizeUthmanicText(clean))
    }

    @Test
    fun `returns same instance when already clean`() {
        val clean = "بسم الله الرحمن الرحيم"
        assertSame(clean, sanitizeUthmanicText(clean))
        assertEquals("", sanitizeUthmanicText(""))
    }

    @Test
    fun `normalizes rounded zero on waw to sukun`() {
        // أُو۟ -> أُوْ : KFGQPC has no U+06DF anchor on و, sukun renders clean
        val input = "أ" + "ُ" + "و" + "۟"
        val expected = "أ" + "ُ" + "و" + "ْ"
        assertEquals(expected, sanitizeUthmanicText(input))
    }

    @Test
    fun `normalizes rounded zero on silent alef to sukun`() {
        // كَفَرُوا۟ -> كَفَرُواْ
        val input = "ك" + "َ" + "ف" + "َ" + "ر" + "ُ" + "و" + "ا" + "۟"
        val expected = "ك" + "َ" + "ف" + "َ" + "ر" + "ُ" + "و" + "ا" + "ْ"
        assertEquals(expected, sanitizeUthmanicText(input))
    }

    @Test
    fun `leaves true quranic sukun U06E1 untouched`() {
        val input = "م" + "ۡ" + "ن"
        assertEquals(input, sanitizeUthmanicText(input))
    }

    @Test
    fun `is idempotent on all audited vectors`() {
        val vectors = listOf(
            hudanRaw,
            ulaikaRaw,
            rabbihimRaw,
            "ٓ" + "ٰ",
            "سَ" + "وَ" + "آ" + "ءٌ"
        )
        vectors.forEach { raw ->
            val once = sanitizeUthmanicText(raw)
            assertEquals(once, sanitizeUthmanicText(once))
        }
    }
}
