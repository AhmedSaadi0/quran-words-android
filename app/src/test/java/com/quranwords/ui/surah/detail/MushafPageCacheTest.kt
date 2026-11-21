package com.quranwords.ui.surah.detail

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class MushafPageCacheTest {

    @Test
    fun lru_eviction_keeps_pinned_entry() {
        val cache = MushafPageCache()
        cache.pinnedPage = 1
        repeat(25) { page -> cache.putPage(page, emptyList()) }

        assertTrue(cache.containsKey(1))
        assertEquals(18, cache.size)
    }
}
