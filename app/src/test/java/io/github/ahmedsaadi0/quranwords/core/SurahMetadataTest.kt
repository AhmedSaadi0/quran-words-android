package io.github.ahmedsaadi0.quranwords.core

import io.github.ahmedsaadi0.quranwords.core.util.SurahMetadata
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class SurahMetadataTest {

    @Test
    fun `juz list has 30 verified entries`() {
        assertEquals(30, SurahMetadata.JUZ_LIST.size)
    }

    @Test
    fun `juz exact coordinates match DB ground truth`() {
        val byId = SurahMetadata.JUZ_LIST.associateBy { it.id }
        assertEquals(1 to 1, byId.getValue(1).startSurahId to byId.getValue(1).startAyah)
        assertEquals(2 to 142, byId.getValue(2).startSurahId to byId.getValue(2).startAyah)
        assertEquals(2 to 253, byId.getValue(3).startSurahId to byId.getValue(3).startAyah)
        assertEquals(3 to 93, byId.getValue(4).startSurahId to byId.getValue(4).startAyah)
        assertEquals(78 to 1, byId.getValue(30).startSurahId to byId.getValue(30).startAyah)
    }

    @Test
    fun `juz boundaries are strictly monotonic`() {
        val keys = SurahMetadata.JUZ_LIST.sortedBy { it.id }
            .map { it.startSurahId * 10_000 + it.startAyah }
        for (i in 1 until keys.size) {
            assertTrue("Juz ${i + 1} regresses: ${keys[i]} <= ${keys[i - 1]}", keys[i] > keys[i - 1])
        }
    }

    @Test
    fun `hizb list has 60 entries and is monotonic`() {
        assertEquals(60, SurahMetadata.HIZB_LIST.size)
        val keys = SurahMetadata.HIZB_LIST.sortedBy { it.id }
            .map { it.startSurahId * 10_000 + it.startAyah }
        assertEquals(1 * 10_000 + 1, keys.first())
        assertEquals(87 * 10_000 + 1, keys.last())
        for (i in 1 until keys.size) {
            assertTrue("Hizb ${i + 1} regresses", keys[i] > keys[i - 1])
        }
    }

    @Test
    fun `rub list has 240 entries and is monotonic`() {
        assertEquals(240, SurahMetadata.RUB_LIST.size)
        val keys = SurahMetadata.RUB_LIST.sortedBy { it.id }
            .map { it.startSurahId * 10_000 + it.startAyah }
        assertEquals(1 * 10_000 + 1, keys.first())
        assertEquals(100 * 10_000 + 9, keys.last())
        for (i in 1 until keys.size) {
            assertTrue("Rub ${i + 1} regresses", keys[i] > keys[i - 1])
        }
    }
}
