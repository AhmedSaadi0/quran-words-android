package io.github.ahmedsaadi0.quranwords.core.util

/**
 * Pure multi-selection state machine shared by the ayah, word, and meaning
 * selection UIs (AGENTS §4.3 DRY — replaces three duplicated toggle/enter/
 * selectAll/clear blocks). Exits selection mode automatically when the last
 * item is deselected or the selection is cleared.
 *
 * [anchorId] remembers the ayah where the selection started so a long-press
 * can extend a range ([rangeTo]) from the anchor to any other ayah. It is
 * kept on toggle (even if the anchor itself is deselected) and reset when
 * the selection empties or is cleared.
 */
data class SelectionState(
    val selectedIds: Set<Int> = emptySet(),
    val isSelectionMode: Boolean = false,
    val anchorId: Int? = null
) {
    fun enter(initialId: Int): SelectionState =
        SelectionState(setOf(initialId), true, initialId)

    fun toggle(id: Int): SelectionState {
        val next = selectedIds.toMutableSet()
        if (!next.add(id)) next.remove(id)
        if (next.isEmpty()) return SelectionState()
        return copy(selectedIds = next, isSelectionMode = true)
    }

    /**
     * Extends the selection with every id between the anchor and [id]
     * (inclusive, either direction). Union: previously selected ids outside
     * the range are kept. Falls back to [enter] when there is no anchor.
     */
    fun rangeTo(id: Int): SelectionState {
        val anchor = anchorId ?: return enter(id)
        val range = (minOf(anchor, id)..maxOf(anchor, id)).toSet()
        return copy(selectedIds = selectedIds + range, isSelectionMode = true)
    }

    fun selectAll(ids: List<Int>): SelectionState = SelectionState(ids.toSet(), ids.isNotEmpty())

    fun clear(): SelectionState = SelectionState(emptySet(), false)
}