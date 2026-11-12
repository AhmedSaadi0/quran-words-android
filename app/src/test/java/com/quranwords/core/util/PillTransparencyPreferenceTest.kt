package com.quranwords.core.util

import org.junit.Assert.assertEquals
import org.junit.Test

class PillTransparencyPreferenceTest {

    @Test
    fun `defaults expose the documented contract`() {
        assertEquals(0.4f, PillTransparencyPreference.DEFAULT, 0.0001f)
        assertEquals(0f, PillTransparencyPreference.MIN, 0.0001f)
        assertEquals(0.8f, PillTransparencyPreference.MAX, 0.0001f)
    }

    @Test
    fun `clamp keeps in-range values untouched`() {
        assertEquals(0f, PillTransparencyPreference.clamp(0f), 0.0001f)
        assertEquals(0.4f, PillTransparencyPreference.clamp(0.4f), 0.0001f)
        assertEquals(0.8f, PillTransparencyPreference.clamp(0.8f), 0.0001f)
    }

    @Test
    fun `clamp pulls out-of-range values to the bounds`() {
        assertEquals(0f, PillTransparencyPreference.clamp(-0.5f), 0.0001f)
        assertEquals(0.8f, PillTransparencyPreference.clamp(1.5f), 0.0001f)
    }

    @Test
    fun `alphaFor is the mirror of transparency`() {
        assertEquals(1f, PillTransparencyPreference.alphaFor(0f), 0.0001f)
        assertEquals(0.6f, PillTransparencyPreference.alphaFor(0.4f), 0.0001f)
        assertEquals(0.2f, PillTransparencyPreference.alphaFor(0.8f), 0.0001f)
    }
}
