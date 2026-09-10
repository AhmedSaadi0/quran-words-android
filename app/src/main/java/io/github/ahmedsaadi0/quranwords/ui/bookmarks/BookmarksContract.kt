package io.github.ahmedsaadi0.quranwords.ui.bookmarks

import io.github.ahmedsaadi0.quranwords.core.util.BookmarkRef

data class BookmarksUiState(
    val isLoading: Boolean = true,
    val surahBookmarks: List<BookmarkRef> = emptyList(),
    val ayahBookmarks: List<BookmarkRef> = emptyList()
) {
    val isEmpty: Boolean get() = surahBookmarks.isEmpty() && ayahBookmarks.isEmpty()
}

sealed interface BookmarksEvent {
    data class ToggleSurahBookmark(val surahId: Int) : BookmarksEvent
    data class ToggleAyahBookmark(val surahId: Int, val ayah: Int) : BookmarksEvent
}