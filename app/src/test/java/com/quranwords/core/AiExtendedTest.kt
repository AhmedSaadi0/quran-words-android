package com.quranwords.core

import com.quranwords.core.util.AiRefsParser
import com.quranwords.core.util.QuranCopyFormatter
import com.quranwords.domain.model.AiExtendedSection
import com.quranwords.domain.model.AiExtendedSummary
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
    fun `extended nonEmptyCount counts summary plus sections`() {
        val extended = AiExtendedSummary(
            hissi = AiExtendedSection("نص", listOf("مرجع"))
        )
        assertEquals(2, extended.nonEmptyCount("ملخص"))
        assertEquals(1, extended.nonEmptyCount(null))
    }

    @Test
    fun `formatAiSummary extended includes sections and refs`() {
        val extended = AiExtendedSummary(
            summaryRefs = listOf("جامع الأقسام"),
            hissi = AiExtendedSection("نص حسي", listOf("مقاييس اللغة"))
        )
        val result = QuranCopyFormatter.formatAiSummary(
            "اله", "ملخص ذكي",
            extended, listOf("الأصل الحسي", "التطور", "التشريح", "الشبكة", "الفروق")
        )
        assertTrue(result.contains("ملخص ذكي"))
        assertTrue(result.contains("جامع الأقسام"))
        assertTrue(result.contains("الأصل الحسي"))
        assertTrue(result.contains("نص حسي"))
        assertTrue(result.contains("مقاييس اللغة"))
    }

    @Test
    fun `formatAiSummary extended blank returns empty`() {
        val result = QuranCopyFormatter.formatAiSummary("كتب", "  ", AiExtendedSummary())
        assertEquals("", result)
    }

    @Test
    fun `formatAiSummary extended numbers citations by appearance`() {
        val extended = AiExtendedSummary(
            furuq = AiExtendedSection(
                "فرق [الصحاح] و[مقاييس اللغة]",
                listOf("مقاييس اللغة", "الصحاح")
            )
        )
        val result = QuranCopyFormatter.formatAiSummary(
            "شعر", "", extended, listOf("الفروق")
        )
        assertTrue(result.contains("فرق [١] و[٢]"))
        assertTrue(result.contains("١. الصحاح"))
        assertTrue(result.contains("٢. مقاييس اللغة"))
    }

    @Test
    fun `formatAiSummary main summary cites markers and numbers summaryRefs`() {
        val extended = AiExtendedSummary(
            summaryRefs = listOf("مقاييس اللغة", "الصحاح")
        )
        val result = QuranCopyFormatter.formatAiSummary(
            "شعر", "أصله عند [مَقَايِيسِ اللُّغَةِ] باب", extended, emptyList()
        )
        assertTrue(result.contains("أصله عند [١] باب"))
        assertTrue(result.contains("١. مقاييس اللغة"))
        assertTrue(result.contains("٢. الصحاح"))
    }
}
