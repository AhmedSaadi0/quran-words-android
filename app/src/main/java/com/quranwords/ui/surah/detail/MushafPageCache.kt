package com.quranwords.ui.surah.detail

import com.quranwords.domain.model.Ayah

private const val PAGE_CACHE_CAPACITY = 18

/**
 * Bounded access-order cache of resolved mushaf pages (P3-4). Main-thread
 * confined: the owning ViewModel publishes detached `toMap()` snapshots.
 */
internal class MushafPageCache :
    LinkedHashMap<Int, List<Ayah>>(PAGE_CACHE_CAPACITY + 2, 0.75f, true) {

    /** Entry page is pinned so it never disappears from published snapshots. */
    var pinnedPage: Int? = null

    fun putPage(page: Int, ayat: List<Ayah>) {
        put(page, ayat)
        trim()
    }

    fun putPages(pages: Map<Int, List<Ayah>>) {
        putAll(pages)
        trim()
    }

    /** Evicts the least-recently-used non-pinned page until within capacity. */
    private fun trim() {
        while (size > PAGE_CACHE_CAPACITY) {
            val victim = keys.firstOrNull { it != pinnedPage } ?: return
            remove(victim)
        }
    }
}
