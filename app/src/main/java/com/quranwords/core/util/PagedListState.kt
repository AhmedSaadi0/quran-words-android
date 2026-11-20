package com.quranwords.core.util

/**
 * Shared paging rules (P2-7). Kept pure so the three manual-paging ViewModels
 * cannot drift on offset/hasMore decisions. Paging3 remains a separate ADR.
 */
data class PagedListState(
    val offset: Int = 0,
    val hasMore: Boolean = true,
    val isLoading: Boolean = false
) {
    fun begin(): PagedListState = copy(isLoading = true)

    fun loaded(count: Int, pageSize: Int): PagedListState = copy(
        offset = offset + count,
        hasMore = hasMoreAfter(count, pageSize),
        isLoading = false
    )

    /** A failed page is transient: keep hasMore armed so Retry can work. */
    fun failed(): PagedListState = copy(isLoading = false)

    fun reset(): PagedListState = PagedListState()

    companion object {
        fun hasMoreAfter(fetchedCount: Int, pageSize: Int): Boolean = fetchedCount == pageSize
    }
}
