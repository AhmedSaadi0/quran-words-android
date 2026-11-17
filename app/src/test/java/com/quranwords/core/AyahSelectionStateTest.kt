package com.quranwords.core

import com.quranwords.core.util.AyahRef
import com.quranwords.core.util.AyahSelectionState
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class AyahSelectionStateTest {

    @Test
    fun `identical ayah numbers in different surahs are distinct`() {
        val state = AyahSelectionState().enter(AyahRef(113, 1)).toggle(AyahRef(114, 1))
        assertEquals(setOf(AyahRef(113, 1), AyahRef(114, 1)), state.selected)
        assertTrue(state.isSelectionMode)
    }

    @Test
    fun `range stays inside the anchor surah`() {
        val state = AyahSelectionState().enter(AyahRef(4, 3)).rangeTo(AyahRef(4, 7))
        assertEquals((3..7).mapTo(mutableSetOf()) { AyahRef(4, it) }, state.selected)
    }

    @Test
    fun `cross surah tap adds and rebases the anchor`() {
        val state = AyahSelectionState().enter(AyahRef(4, 3)).rangeTo(AyahRef(5, 2))
        assertEquals(setOf(AyahRef(4, 3), AyahRef(5, 2)), state.selected)
        assertEquals(AyahRef(5, 2), state.anchor)
    }

    @Test
    fun `toggling the last ayah exits selection mode`() {
        val state = AyahSelectionState().enter(AyahRef(4, 3)).toggle(AyahRef(4, 3))
        assertTrue(state.selected.isEmpty())
        assertFalse(state.isSelectionMode)
    }

    @Test
    fun `bookmark key round trips`() {
        assertEquals("4:282", AyahRef(4, 282).key)
        assertEquals(AyahRef(4, 282), AyahRef.parse("4:282"))
        assertEquals(null, AyahRef.parse("bad"))
    }
}
