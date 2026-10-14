package com.quranwords.ui.surah.detail

import org.junit.Assert.assertEquals
import org.junit.Test

/**
 * Guards the deep-link window rule. A target inside the first page keeps plain
 * paging; a deeper target must be fetched through in ONE query (plus runway),
 * never reached by a chain of 20-ayah pages — that chain is what caused the
 * ayah-1 flash and the delayed scroll jump.
 */
class SurahDeepLinkWindowTest {

    @Test
    fun `target inside the first page keeps a single page`() {
        assertEquals(20, initialAyatLimit(targetAyah = 1, pageSize = 20, totalAyah = 286))
        assertEquals(20, initialAyatLimit(targetAyah = 20, pageSize = 20, totalAyah = 286))
    }

    @Test
    fun `deep target fetches through it with forward runway`() {
        // Juz 2 opens Al-Baqarah at 142.
        assertEquals(152, initialAyatLimit(targetAyah = 142, pageSize = 20, totalAyah = 286))
    }

    @Test
    fun `window never exceeds the surah`() {
        // Backward switch into Al-Baqarah's last ayah loads the surah once.
        assertEquals(286, initialAyatLimit(targetAyah = 286, pageSize = 20, totalAyah = 286))
        assertEquals(6, initialAyatLimit(targetAyah = 6, pageSize = 20, totalAyah = 6))
    }

    @Test
    fun `small surahs stay at one page`() {
        // An-Nas: 6 ayat, target 4 — the surah is already smaller than a page.
        assertEquals(20, initialAyatLimit(targetAyah = 4, pageSize = 20, totalAyah = 6))
    }

    @Test
    fun `window always covers the target and stays positive`() {
        for (total in listOf(7, 29, 110, 286)) {
            for (target in 1..total) {
                val limit = initialAyatLimit(targetAyah = target, pageSize = 20, totalAyah = total)
                assertEquals("$total/$target", true, limit >= 1)
                assertEquals("$total/$target", true, limit >= target)
                assertEquals("$total/$target", true, limit <= total.coerceAtLeast(20))
            }
        }
    }
}