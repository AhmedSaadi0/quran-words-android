package io.github.ahmedsaadi0.quranwords

import io.github.ahmedsaadi0.quranwords.ui.roots.detail.util.RootDetailTab
import io.github.ahmedsaadi0.quranwords.ui.viewmodel.cleanAiDate
import io.github.ahmedsaadi0.quranwords.ui.viewmodel.formatAiMetaLine
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class RootDetailRefactorTest {

    @Test
    fun `tab order preserves visible pager sequence`() {
        val tabs = RootDetailTab.entries.toList()
        assertEquals(
            listOf(
                RootDetailTab.MEANINGS,
                RootDetailTab.AYAT,
                RootDetailTab.WORDS,
                RootDetailTab.MASADIR,
                RootDetailTab.DERIVATIVES
            ),
            tabs
        )
    }

    @Test
    fun `tab contentIds preserve legacy testTags`() {
        assertEquals(0, RootDetailTab.MEANINGS.contentId)
        assertEquals(4, RootDetailTab.AYAT.contentId)
        assertEquals(3, RootDetailTab.WORDS.contentId)
        assertEquals(1, RootDetailTab.MASADIR.contentId)
        assertEquals(2, RootDetailTab.DERIVATIVES.contentId)
    }

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
