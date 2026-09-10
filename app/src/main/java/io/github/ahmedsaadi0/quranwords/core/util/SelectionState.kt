package io.github.ahmedsaadi0.quranwords.core.util

/**
 * Pure multi-selection state machine shared by the ayah, word, and meaning
 * selection UIs (AGENTS §4.3 DRY — replaces three duplicated toggle/enter/
 * selectAll/clear blocks). Exits selection mode automatically when the last
 * item is deselected or the selection is cleared.
 */
data class SelectionState(
    val selectedIds: Set<Int> = emptySet(),
    val isSelectionMode: Boolean = false
) {
    fun enter(initialId: Int): SelectionState = SelectionState(setOf(initialId), true)

    fun toggle(id: Int): SelectionState {
        val next = selectedIds.toMutableSet()
        if (!next.add(id)) next.remove(id)
        return SelectionState(next, next.isNotEmpty())
    }

    fun selectAll(ids: List<Int>): SelectionState = SelectionState(ids.toSet(), ids.isNotEmpty())

    fun clear(): SelectionState = SelectionState(emptySet(), false)
}