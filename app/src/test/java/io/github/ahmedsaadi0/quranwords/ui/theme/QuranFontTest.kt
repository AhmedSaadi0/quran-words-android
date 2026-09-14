package io.github.ahmedsaadi0.quranwords.ui.theme

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class QuranFontTest {

    @Test
    fun `fromKey resolves all known keys`() {
        assertEquals(QuranFont.KFGQPC_HAFS_1441, QuranFont.fromKey("kfgqpc_hafs_1441"))
        assertEquals(QuranFont.AMIRI_QURAN, QuranFont.fromKey("amiri_quran"))
        assertEquals(QuranFont.SCHEHERAZADE_NEW, QuranFont.fromKey("scheherazade_new"))
        assertEquals(QuranFont.NOTO_NASKH_ARABIC, QuranFont.fromKey("noto_naskh_arabic"))
    }

    @Test
    fun `fromKey falls back to default on unknown null and empty keys`() {
        assertEquals(QuranFont.KFGQPC_HAFS_1441, QuranFont.fromKey("unknown_font"))
        assertEquals(QuranFont.KFGQPC_HAFS_1441, QuranFont.fromKey(null))
        assertEquals(QuranFont.KFGQPC_HAFS_1441, QuranFont.fromKey(""))
    }

    @Test
    fun `lineHeightMultipliers are positive and match spec`() {
        assertEquals(2.2f, QuranFont.KFGQPC_HAFS_1441.lineHeightMultiplier)
        assertEquals(2.6f, QuranFont.AMIRI_QURAN.lineHeightMultiplier)
        assertEquals(2.3f, QuranFont.SCHEHERAZADE_NEW.lineHeightMultiplier)
        assertEquals(2.4f, QuranFont.NOTO_NASKH_ARABIC.lineHeightMultiplier)
        QuranFont.entries.forEach { font ->
            assertTrue(font.lineHeightMultiplier > 0f)
        }
    }
}
