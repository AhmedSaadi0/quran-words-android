package com.quranwords.ui.surah.detail.components

import androidx.compose.ui.geometry.Rect
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class MushafPulseGeometryTest {

    @Test
    fun no_overlap_returns_null() {
        assertNull(ayahLineBand(10, 20, 0, 8, Rect(0f, 0f, 8f, 10f), Rect(0f, 0f, 8f, 10f), 0f, 100f))
    }

    @Test
    fun band_is_clipped_to_intersection_and_line_bounds() {
        val band = ayahLineBand(
            segStart = 6, segEnd = 12, lineStart = 0, lineEnd = 20,
            firstBox = Rect(-5f, 0f, 4f, 12f),
            lastBox = Rect(8f, 0f, 140f, 12f),
            lineLeft = 0f, lineRight = 100f
        )
        assertEquals(Rect(0f, 0f, 100f, 12f), band)
    }

    @Test
    fun collapsed_glyph_box_widens_to_the_whole_line() {
        val band = ayahLineBand(
            segStart = 0, segEnd = 3, lineStart = 0, lineEnd = 3,
            firstBox = Rect(42f, 0f, 42f, 12f),
            lastBox = Rect(42f, 0f, 42f, 12f),
            lineLeft = 10f, lineRight = 90f
        )
        assertEquals(Rect(10f, 0f, 90f, 12f), band)
    }
}
