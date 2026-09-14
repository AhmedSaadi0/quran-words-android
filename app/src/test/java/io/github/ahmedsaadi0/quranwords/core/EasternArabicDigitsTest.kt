package io.github.ahmedsaadi0.quranwords.core

import io.github.ahmedsaadi0.quranwords.core.util.toEasternArabicDigits
import org.junit.Assert.assertEquals
import org.junit.Test

class EasternArabicDigitsTest {

    @Test
    fun `zero maps to eastern zero`() {
        assertEquals("٠", 0.toEasternArabicDigits())
    }

    @Test
    fun `single digit maps correctly`() {
        assertEquals("٥", 5.toEasternArabicDigits())
    }

    @Test
    fun `multi digit number maps per digit`() {
        assertEquals("١٢", 12.toEasternArabicDigits())
        assertEquals("١١٤", 114.toEasternArabicDigits())
        assertEquals("٦٠٤", 604.toEasternArabicDigits())
        assertEquals("٢٨٢", 282.toEasternArabicDigits())
    }
}
