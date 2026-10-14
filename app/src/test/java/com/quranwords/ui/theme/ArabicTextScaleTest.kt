package com.quranwords.ui.theme

import com.quranwords.core.util.FontSizePreference
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * Locks the Arabic ergonomic scale. The critical property is that a verse is
 * always 1:1 with the mushaf at the same preference, and that body copy never
 * collapses back to Material's Latin 14sp default at any slider position.
 */
class ArabicTextScaleTest {

    @Test
    fun `default preference gives an accessible baseline`() {
        val scale = ArabicTextScale.calculate(userFontSize = 24f)

        // Verse is 1:1 with the mushaf reading view.
        assertEquals(24f, scale.verseTextSize.value, 0.01f)
        // Body starts at 17sp, never Material's Latin 14sp default.
        assertEquals(17f, scale.explanationBodySize.value, 0.01f)
        assertEquals(20f, scale.wordTitleSize.value, 0.01f)
        assertEquals(14f, scale.metadataLabelSize.value, 0.01f)
    }

    @Test
    fun `maximum preference scales bodies for low vision and caps headlines`() {
        val scale = ArabicTextScale.calculate(userFontSize = 48f)

        assertEquals(48f, scale.verseTextSize.value, 0.01f)
        // Headline capped so badges stay on screen at peak zoom.
        assertTrue(scale.wordTitleSize.value <= 36f)
        assertEquals(36f, scale.wordTitleSize.value, 0.01f)
        // 17 + 24 * 0.75 = 35sp — readable without eye strain.
        assertEquals(35f, scale.explanationBodySize.value, 0.01f)
        assertEquals(26f, scale.metadataLabelSize.value, 0.01f)
    }

    @Test
    fun `minimum preference clamps instead of shrinking below the floor`() {
        val scale = ArabicTextScale.calculate(userFontSize = 10f)

        assertEquals(FontSizePreference.MIN, scale.verseTextSize.value, 0.01f)
        assertEquals(13f, scale.wordTitleSize.value, 0.01f)
        assertEquals(17f, scale.explanationBodySize.value, 0.01f)
        assertEquals(13f, scale.metadataLabelSize.value, 0.01f)
    }

    @Test
    fun `out of range values clamp to the shared preference bounds`() {
        assertEquals(FontSizePreference.MIN, ArabicTextScale.calculate(0f).verseTextSize.value, 0.01f)
        assertEquals(FontSizePreference.MAX, ArabicTextScale.calculate(120f).verseTextSize.value, 0.01f)
        assertEquals(24f, ArabicTextScale.clamp(24f), 0.01f)
        assertEquals(18f, ArabicTextScale.clamp(4f), 0.01f)
        assertEquals(48f, ArabicTextScale.clamp(99f), 0.01f)
    }

    @Test
    fun `verse line height follows the active quran cut multiplier`() {
        val standard = ArabicTextScale.calculate(24f, quranLineHeightMultiplier = 2.2f)
        val tall = ArabicTextScale.calculate(24f, quranLineHeightMultiplier = 2.6f)

        assertEquals(52.8f, standard.verseLineHeight.value, 0.01f)
        assertEquals(62.4f, tall.verseLineHeight.value, 0.01f)
    }

    @Test
    fun `hierarchy never inverts across the whole slider range`() {
        var size = FontSizePreference.MIN
        while (size <= FontSizePreference.MAX) {
            val scale = ArabicTextScale.calculate(size)
            assertTrue(
                "verse >= word title at $size",
                scale.verseTextSize.value >= scale.wordTitleSize.value
            )
            assertTrue(
                "word title >= body at $size",
                scale.wordTitleSize.value >= scale.explanationBodySize.value
            )
            assertTrue(
                "body >= metadata at $size",
                scale.explanationBodySize.value >= scale.metadataLabelSize.value
            )
            size += 1f
        }
    }

    @Test
    fun `body copy never falls below the legibility floor`() {
        for (raw in listOf(1f, 5f, 12f, 18f, 24f, 36f, 48f, 100f)) {
            val scale = ArabicTextScale.calculate(raw)
            assertTrue("body >= 16sp at $raw", scale.explanationBodySize.value >= 16f)
            assertTrue("metadata >= 13sp at $raw", scale.metadataLabelSize.value >= 13f)
            assertTrue("verse >= 18sp at $raw", scale.verseTextSize.value >= 18f)
        }
    }

    @Test
    fun `arabic leading keeps tashkeel clear at every size`() {
        for (raw in listOf(18f, 24f, 36f, 48f)) {
            val scale = ArabicTextScale.calculate(raw)
            assertTrue(
                "body leading >= 1.7x at $raw",
                scale.explanationLineHeight.value >= scale.explanationBodySize.value * 1.7f - 0.01f
            )
            assertTrue(
                "headline leading >= 1.7x at $raw",
                scale.wordTitleLineHeight.value >= scale.wordTitleSize.value * 1.7f - 0.01f
            )
        }
    }
}