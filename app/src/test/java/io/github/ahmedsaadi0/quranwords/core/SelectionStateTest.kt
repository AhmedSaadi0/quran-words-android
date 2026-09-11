package io.github.ahmedsaadi0.quranwords.core

import io.github.ahmedsaadi0.quranwords.core.util.SelectionState
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class SelectionStateTest {

  @Test
  fun `enter starts selection mode with the initial id`() {
    val state = SelectionState().enter(7)
    assertEquals(setOf(7), state.selectedIds)
    assertTrue(state.isSelectionMode)
  }

  @Test
  fun `toggle adds an id and enables selection mode`() {
    val state = SelectionState().enter(1).toggle(2)
    assertEquals(setOf(1, 2), state.selectedIds)
    assertTrue(state.isSelectionMode)
  }

  @Test
  fun `toggle removes an existing id`() {
    val state = SelectionState().enter(1).toggle(1)
    assertEquals(emptySet<Int>(), state.selectedIds)
    assertFalse(state.isSelectionMode)
  }

  @Test
  fun `toggle last remaining id exits selection mode`() {
    val state = SelectionState().enter(5).toggle(5)
    assertTrue(state.selectedIds.isEmpty())
    assertFalse(state.isSelectionMode)
  }

  @Test
  fun `selectAll replaces ids and enables selection mode`() {
    val state = SelectionState().enter(1).selectAll(listOf(3, 4, 5))
    assertEquals(setOf(3, 4, 5), state.selectedIds)
    assertTrue(state.isSelectionMode)
  }

  @Test
  fun `selectAll with empty list disables selection mode`() {
    val state = SelectionState().enter(1).selectAll(emptyList())
    assertTrue(state.selectedIds.isEmpty())
    assertFalse(state.isSelectionMode)
  }

  @Test
  fun `clear resets everything`() {
    val state = SelectionState().enter(1).toggle(2).clear()
    assertTrue(state.selectedIds.isEmpty())
    assertFalse(state.isSelectionMode)
  }

  @Test
  fun `default state is empty and inactive`() {
    val state = SelectionState()
    assertEquals(emptySet<Int>(), state.selectedIds)
    assertFalse(state.isSelectionMode)
  }
}