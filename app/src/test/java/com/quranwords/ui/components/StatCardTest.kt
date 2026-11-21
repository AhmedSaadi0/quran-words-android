package com.quranwords.ui.components

import org.junit.Assert.assertEquals
import org.junit.Test
import java.util.Locale

class StatCardTest {
    @Test
    fun arabic_locale_uses_eastern_digits() {
        assertEquals("٧٧٬٤٢٩", formatStatValue(77429, Locale("ar")))
    }

    @Test
    fun english_locale_keeps_grouped_latin_digits() {
        assertEquals("77,429", formatStatValue(77429, Locale.ENGLISH))
    }
}
