package com.quranwords.ui.surah.detail.components

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
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

    @Test
    fun `already rectangular at max costs one check`() {
        var checks = 0
        val result = refineFontSizeForRectangle(
            maxFitSp = 32f,
            isRectangle = { checks++; true },
            fitsVertically = { true },
            slackWithinCap = { true }
        )
        assertEquals(32f, result, 0f)
        assertEquals(1, checks)
    }

    @Test
    fun `picks the largest qualifying size in 0.25sp steps`() {
        val result = refineFontSizeForRectangle(
            maxFitSp = 32f,
            isRectangle = { it <= 31.5f },
            fitsVertically = { true },
            slackWithinCap = { true }
        )
        assertEquals(31.5f, result, 0.01f)
    }

    @Test
    fun `rejects candidates that break the vertical height budget`() {
        val result = refineFontSizeForRectangle(
            maxFitSp = 32f,
            isRectangle = { it <= 31.5f },
            fitsVertically = { it >= 31.75f },
            slackWithinCap = { true }
        )
        assertEquals(32f, result, 0f)
    }

    @Test
    fun `rejects candidates whose slack exceeds the stretch cap`() {
        val result = refineFontSizeForRectangle(
            maxFitSp = 32f,
            isRectangle = { it <= 31.5f },
            fitsVertically = { true },
            slackWithinCap = { it >= 31.75f }
        )
        assertEquals(32f, result, 0f)
    }

    @Test
    fun `graceful fallback when nothing inside the shrink bound qualifies`() {
        val result = refineFontSizeForRectangle(
            maxFitSp = 32f,
            isRectangle = { it <= 28f }, // needs more than the 2.5sp cap
            fitsVertically = { true },
            slackWithinCap = { true }
        )
        assertEquals(32f, result, 0f)
    }

    @Test
    fun `shrink cap is twelve percent for smaller fonts`() {
        // 20sp * 0.12 = 2.4sp -> lowest candidate 17.6sp.
        val inside = refineFontSizeForRectangle(
            maxFitSp = 20f,
            isRectangle = { it <= 17.75f },
            fitsVertically = { true },
            slackWithinCap = { true }
        )
        assertEquals(17.75f, inside, 0.01f)
        val outside = refineFontSizeForRectangle(
            maxFitSp = 20f,
            isRectangle = { it <= 17.5f },
            fitsVertically = { true },
            slackWithinCap = { true }
        )
        assertEquals(20f, outside, 0f)
    }

    @Test
    fun `never drops below the fit floor and tolerates zero step`() {
        val floored = refineFontSizeForRectangle(
            maxFitSp = MUSHAF_FIT_MIN_SP + 0.1f,
            isRectangle = { false },
            fitsVertically = { true },
            slackWithinCap = { true }
        )
        assertEquals(MUSHAF_FIT_MIN_SP + 0.1f, floored, 0f)
        val zeroStep = refineFontSizeForRectangle(
            maxFitSp = 32f, stepSp = 0f,
            isRectangle = { true }, fitsVertically = { true }, slackWithinCap = { true }
        )
        assertEquals(32f, zeroStep, 0f)
    }

    @Test
    fun `fit cache key is geometry-only`() {
        val key = MushafFitKey(listOf(1, 2), 1000, 700, "kfgqpc", 1f)
        assertEquals(key, MushafFitKey(listOf(1, 2), 1000, 700, "kfgqpc", 1f))
        assertNotEquals(key, key.copy(ayahIds = listOf(1, 3)))
        assertNotEquals(key, key.copy(innerWidthPx = 999))
        assertNotEquals(key, key.copy(innerHeightPx = 699))
        assertNotEquals(key, key.copy(quranFontKey = "other"))
        assertNotEquals(key, key.copy(fontScale = 1.2f))
        // Colours must never enter the key — they don't affect text bounds.
        assertTrue(
            MushafFitKey::class.java.declaredFields.none {
                it.name.contains("color", ignoreCase = true)
            }
        )
    }

    @Test
    fun `prefilter accepts a within-threshold gap`() {
        val fill = resolveLastLineFill(listOf(100f, 200f), lineLeft = 90f)
        assertEquals(10f, fill?.gapWidthPx ?: 0f, 0f)
        assertTrue(lastLineCanBeJustified(fill, spaceWidthPx = 6f)) // 10 <= 2*6
    }

    @Test
    fun `prefilter rejects a gap beyond the renderer multiplier`() {
        val fill = resolveLastLineFill(listOf(100f, 200f), lineLeft = 90f)
        assertFalse(lastLineCanBeJustified(fill, spaceWidthPx = 4f)) // 10 > 2*4
    }

    @Test
    fun `prefilter treats an already full line as complete`() {
        val fill = resolveLastLineFill(listOf(90f, 200f), lineLeft = 90f)
        assertTrue(fill?.isAlreadyFull == true)
        assertTrue(lastLineCanBeJustified(fill, spaceWidthPx = 1f))
    }

    @Test
    fun `prefilter rejects single token and missing fill`() {
        assertNull(resolveLastLineFill(listOf(100f), lineLeft = 90f))
        assertFalse(lastLineCanBeJustified(null, spaceWidthPx = 100f))
    }
}
