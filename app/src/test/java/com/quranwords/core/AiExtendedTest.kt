package com.quranwords.core

import com.quranwords.core.util.AiRefsParser
import com.quranwords.core.util.QuranCopyFormatter
import com.quranwords.domain.model.AiExtendedSection
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class AiExtendedTest {

    @Test
    fun `parse null and blank return empty`() {
        assertEquals(emptyList<String>(), AiRefsParser.parse(null))
        assertEquals(emptyList<String>(), AiRefsParser.parse("  "))
    }

    @Test
    fun `parse json array returns items`() {
        val raw = "[\"مقاييس اللغة لابن فارس\", \"لسان العرب لابن منظور\"]"
        assertEquals(
            listOf("مقاييس اللغة لابن فارس", "لسان العرب لابن منظور"),
            AiRefsParser.parse(raw)
        )
    }

    @Test
    fun `parse empty array returns empty`() {
        assertEquals(emptyList<String>(), AiRefsParser.parse("[]"))
    }

    @Test
    fun `formatAiSection renders title body and refs`() {
        val section = AiExtendedSection("نص حسي", listOf("مقاييس اللغة"))
        val result = QuranCopyFormatter.formatAiSection("المعنى الأصلي", section)
        assertTrue(result.contains("المعنى الأصلي"))
        assertTrue(result.contains("نص حسي"))
        assertTrue(result.contains("مقاييس اللغة"))
    }

    @Test
    fun `formatAiSection blank returns empty`() {
        assertEquals("", QuranCopyFormatter.formatAiSection("الفروق", AiExtendedSection()))
    }

    @Test
    fun `formatAiSection numbers citations by appearance`() {
        val section = AiExtendedSection(
            "فرق [الصحاح] و[مقاييس اللغة]",
            listOf("مقاييس اللغة", "الصحاح")
        )
        val result = QuranCopyFormatter.formatAiSection("الفروق", section)
        assertTrue(result.contains("فرق [١] و[٢]"))
        assertTrue(result.contains("١. الصحاح"))
        assertTrue(result.contains("٢. مقاييس اللغة"))
    }
}
