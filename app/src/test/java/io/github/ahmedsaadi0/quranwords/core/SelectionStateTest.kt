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

  @Test
  fun `enter sets the anchor to the initial id`() {
    val state = SelectionState().enter(7)
    assertEquals(7, state.anchorId)
  }

  @Test
  fun `toggle keeps the anchor while selection is non-empty`() {
    val state = SelectionState().enter(5).toggle(8).toggle(5)
    assertEquals(setOf(8), state.selectedIds)
    assertEquals(5, state.anchorId)
    assertTrue(state.isSelectionMode)
  }

  @Test
  fun `toggle to empty resets the anchor`() {
    val state = SelectionState().enter(5).toggle(5)
    assertEquals(null, state.anchorId)
    assertFalse(state.isSelectionMode)
  }

  @Test
  fun `rangeTo extends forward from the anchor with union`() {
    val state = SelectionState().enter(5).toggle(8).rangeTo(10)
    assertEquals(setOf(5, 8, 6, 7, 9, 10), state.selectedIds)
    assertEquals(5, state.anchorId)
    assertTrue(state.isSelectionMode)
  }

  @Test
  fun `rangeTo extends backward from the anchor`() {
    val state = SelectionState().enter(10).rangeTo(7)
    assertEquals(setOf(7, 8, 9, 10), state.selectedIds)
  }

  @Test
  fun `rangeTo without anchor behaves like enter`() {
    val state = SelectionState().rangeTo(4)
    assertEquals(setOf(4), state.selectedIds)
    assertEquals(4, state.anchorId)
    assertTrue(state.isSelectionMode)
  }

  @Test
  fun `clear resets the anchor`() {
    val state = SelectionState().enter(1).rangeTo(3).clear()
    assertEquals(null, state.anchorId)
    assertFalse(state.isSelectionMode)
  }
}