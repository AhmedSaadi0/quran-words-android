package io.github.ahmedsaadi0.quranwords.core

import io.github.ahmedsaadi0.quranwords.core.util.cleanAiDate
import io.github.ahmedsaadi0.quranwords.core.util.formatAiMetaLine
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class AiMetaFormatterTest {

    @Test
    fun `cleanAiDate blank returns empty`() {
        assertEquals("", cleanAiDate(null))
        assertEquals("", cleanAiDate("  "))
    }

    @Test
    fun `cleanAiDate replaces T and truncates to 16`() {
        assertEquals("2024-01-02 15:04", cleanAiDate("2024-01-02T15:04:05"))
    }

    @Test
    fun `cleanAiDate short stays intact`() {
        assertEquals("2024-01-02", cleanAiDate("2024-01-02"))
    }

    @Test
    fun `formatAiMetaLine both blank returns null`() {
        assertNull(formatAiMetaLine(null, null))
        assertNull(formatAiMetaLine("  ", "  "))
    }

    @Test
    fun `formatAiMetaLine model only`() {
        assertEquals("gpt-4", formatAiMetaLine("gpt-4", null))
    }

    @Test
    fun `formatAiMetaLine date only`() {
        assertEquals("2024-01-02 15:04", formatAiMetaLine(null, "2024-01-02T15:04:05"))
    }

    @Test
    fun `formatAiMetaLine joins with separator without emoji`() {
        val line = formatAiMetaLine("gpt-4", "2024-01-02T15:04:05")
        assertEquals("gpt-4  •  2024-01-02 15:04", line)
    }
}
