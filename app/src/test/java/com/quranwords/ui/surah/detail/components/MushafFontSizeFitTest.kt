package com.quranwords.ui.surah.detail.components

import org.junit.Assert.assertEquals
import org.junit.Test

/** Pure line-spacing math behind the pages-mode fit (no Compose fixtures). */
class MushafFontSizeFitTest {

    @Test
    fun `no leftover means no stretch`() {
        assertEquals(0f, resolveExtraLineSpacingPx(0f, 10, 20f) { true }, 0f)
        assertEquals(0f, resolveExtraLineSpacingPx(-2f, 10, 20f) { true }, 0f)
    }

    @Test
    fun `distributed extra is floored to whole pixels`() {
        // 4.9px / 4 lines = 1.225 -> 1px so 4 lines stay within the leftover.
        assertEquals(1f, resolveExtraLineSpacingPx(4.9f, 4, 20f) { true }, 0f)
        // Below one px per line: no stretch at all.
        assertEquals(0f, resolveExtraLineSpacingPx(3.9f, 4, 20f) { true }, 0f)
    }

    @Test
    fun `stretch is capped to a quarter of natural leading`() {
        // 100px leftover over 4 lines would be 25px; 20px natural x 0.25 = 5px.
        assertEquals(5f, resolveExtraLineSpacingPx(100f, 4, 20f) { true }, 0f)
    }

    @Test
    fun `measurement verification steps down on rounding overflow`() {
        // Raw extra would be 3px, but the real measured layout only fits 2px.
        val result = resolveExtraLineSpacingPx(12f, 4, 40f) { extra -> extra <= 2f }
        assertEquals(2f, result, 0f)
    }

    @Test
    fun `zero lines never stretches`() {
        assertEquals(0f, resolveExtraLineSpacingPx(50f, 0, 20f) { true }, 0f)
    }
}
