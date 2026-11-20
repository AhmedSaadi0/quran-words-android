package com.quranwords.core

import com.quranwords.core.util.PagedListState
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class PagedListStateTest {

    @Test
    fun loaded_advances_offset_and_detects_last_page() {
        val full = PagedListState().begin().loaded(30, 30)
        assertEquals(30, full.offset)
        assertTrue(full.hasMore)
        assertFalse(full.isLoading)

        val last = full.begin().loaded(12, 30)
        assertEquals(42, last.offset)
        assertFalse(last.hasMore)
    }

    @Test
    fun failed_page_keeps_paging_armed() {
        val state = PagedListState().begin().failed()
        assertFalse(state.isLoading)
        assertTrue(state.hasMore)
        assertEquals(0, state.offset)
    }

    @Test
    fun has_more_after_empty_page_is_false() {
        assertFalse(PagedListState.hasMoreAfter(0, 30))
        assertTrue(PagedListState.hasMoreAfter(30, 30))
    }
}
