package com.quranwords.core.util

/**
 * Multi-ayah selection for the reader, keyed by [AyahRef] so surahs sharing a
 * mushaf page never collide. Mirrors the pure semantics of [SelectionState]
 * but keeps range extension inside a single surah.
 */
data class AyahSelectionState(
    val selected: Set<AyahRef> = emptySet(),
    val isSelectionMode: Boolean = false,
    val anchor: AyahRef? = null
) {
    fun enter(ref: AyahRef): AyahSelectionState =
        AyahSelectionState(setOf(ref), true, ref)

    fun toggle(ref: AyahRef): AyahSelectionState {
        val next = selected.toMutableSet()
        if (!next.add(ref)) next.remove(ref)
        if (next.isEmpty()) return AyahSelectionState()
        return copy(selected = next, isSelectionMode = true)
    }

    /**
     * Extends the range from [anchor] to [ref] when both are in the same
     * surah (union, like [SelectionState.rangeTo]); a cross-surah tap simply
     * adds that ayah and rebases the anchor there.
     */
    fun rangeTo(ref: AyahRef): AyahSelectionState {
        val from = anchor ?: return enter(ref)
        if (from.surahId != ref.surahId) {
            return copy(selected = selected + ref, isSelectionMode = true, anchor = ref)
        }
        val range = (minOf(from.ayah, ref.ayah)..maxOf(from.ayah, ref.ayah))
            .mapTo(mutableSetOf()) { AyahRef(from.surahId, it) }
        return copy(selected = selected + range, isSelectionMode = true)
    }

    fun clear(): AyahSelectionState = AyahSelectionState()
}
