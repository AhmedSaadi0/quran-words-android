package io.github.ahmedsaadi0.quranwords

import io.github.ahmedsaadi0.quranwords.core.util.QuranCopyFormatter
import io.github.ahmedsaadi0.quranwords.domain.model.AyahOccurrenceModel
import io.github.ahmedsaadi0.quranwords.domain.model.RootMeaningModel
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class QuranCopyFormatterTest {

    @Test
    fun `formatOccurrences empty returns empty`() {
        assertEquals("", QuranCopyFormatter.formatOccurrences(emptyList()))
    }

    @Test
    fun `formatOccurrences single uses B1 format`() {
        val occ = AyahOccurrenceModel(2, "البقرة", 183, "آية النص", "كُتِبَ")
        val result = QuranCopyFormatter.formatOccurrences(listOf(occ))
        assertEquals("آية النص ﴿183﴾\n[سورة البقرة: 183]", result)
    }

    @Test
    fun `formatOccurrences multiple separates blocks with double newline`() {
        val list = listOf(
            AyahOccurrenceModel(1, "الفاتحة", 2, "نص أول", "الحمد"),
            AyahOccurrenceModel(2, "البقرة", 183, "نص ثان", "كتب")
        )
        val result = QuranCopyFormatter.formatOccurrences(list)
        assertTrue(result.contains("\n\n"))
        assertTrue(result.contains("[سورة الفاتحة: 2]"))
        assertTrue(result.contains("[سورة البقرة: 183]"))
    }

    @Test
    fun `formatOccurrences preserves surah-ayah order for multi-word selection`() {
        val reversed = listOf(
            AyahOccurrenceModel(2, "البقرة", 282, "نص ب", "كتاب"),
            AyahOccurrenceModel(1, "الفاتحة", 2, "نص أ", "الحمد")
        )
        val sorted = reversed.sortedWith(compareBy({ it.surahId }, { it.ayahNum }))
        val result = QuranCopyFormatter.formatOccurrences(sorted)
        assertTrue(result.indexOf("الفاتحة") < result.indexOf("البقرة"))
    }

    @Test
    fun `formatAiSummary blank returns empty`() {
        assertEquals("", QuranCopyFormatter.formatAiSummary("كتب", "  "))
    }

    @Test
    fun `formatAiSummary prefixes root header`() {
        val result = QuranCopyFormatter.formatAiSummary("كتب", "ملخص تجريبي")
        assertEquals("[الملخص الذكي للجذر: كتب]\n\nملخص تجريبي", result)
    }

    @Test
    fun `formatMeanings empty returns empty`() {
        assertEquals("", QuranCopyFormatter.formatMeanings("كتب", emptyList()))
    }

    @Test
    fun `formatMeanings separates entries with double newline`() {
        val list = listOf(
            RootMeaningModel(1, "تعريف أول", "لسان العرب"),
            RootMeaningModel(2, "تعريف ثان", "الصحاح")
        )
        val result = QuranCopyFormatter.formatMeanings("كتب", list)
        assertEquals(
            "[معاني الجذر: كتب]\n\n▪ لسان العرب\nتعريف أول\n\n▪ الصحاح\nتعريف ثان",
            result
        )
    }
}
