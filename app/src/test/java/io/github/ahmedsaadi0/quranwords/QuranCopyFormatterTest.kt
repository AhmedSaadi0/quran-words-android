package io.github.ahmedsaadi0.quranwords

import io.github.ahmedsaadi0.quranwords.core.util.QuranCopyFormatter
import io.github.ahmedsaadi0.quranwords.domain.model.AyahOccurrenceModel
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
}
