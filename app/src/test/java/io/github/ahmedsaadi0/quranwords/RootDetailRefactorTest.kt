package io.github.ahmedsaadi0.quranwords

import io.github.ahmedsaadi0.quranwords.ui.roots.detail.util.RootDetailTab
import org.junit.Assert.assertEquals
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
}
