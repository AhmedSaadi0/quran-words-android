package io.github.ahmedsaadi0.quranwords.ui.bookmarks

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dagger.hilt.android.lifecycle.HiltViewModel
import io.github.ahmedsaadi0.quranwords.core.util.BookmarkRef
import io.github.ahmedsaadi0.quranwords.domain.repository.UserPreferencesRepository
import javax.inject.Inject
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

/**
 * Owns the bookmark slices this screen needs (Decision 14) — parsing and
 * ordering live in [BookmarkRef], not in the composable.
 */
@HiltViewModel
class BookmarksViewModel @Inject constructor(
    private val preferences: UserPreferencesRepository
) : ViewModel() {

    private val _loaded = MutableStateFlow(false)
    private val _surahBookmarks = MutableStateFlow<List<BookmarkRef>>(emptyList())
    private val _ayahBookmarks = MutableStateFlow<List<BookmarkRef>>(emptyList())

    val uiState: StateFlow<BookmarksUiState> = combine(
        _loaded,
        _surahBookmarks,
        _ayahBookmarks
    ) { loaded, surahBookmarks, ayahBookmarks ->
        BookmarksUiState(
            isLoading = !loaded,
            surahBookmarks = surahBookmarks,
            ayahBookmarks = ayahBookmarks
        )
    }.stateIn(viewModelScope, SharingStarted.Eagerly, BookmarksUiState())

    init {
        viewModelScope.launch {
            preferences.bookmarkedSurahs.collect { keys ->
                _surahBookmarks.value = keys.mapNotNull(BookmarkRef::parseSurah).sortedBy { it.surahId }
                _loaded.value = true
            }
        }
        viewModelScope.launch {
            preferences.bookmarkedAyat.collect { keys ->
                _ayahBookmarks.value = keys.mapNotNull(BookmarkRef::parseAyah).sortedWith(BookmarkRef.AYAH_ORDER)
                _loaded.value = true
            }
        }
    }

    fun onEvent(event: BookmarksEvent) {
        when (event) {
            is BookmarksEvent.ToggleSurahBookmark -> toggleSurahBookmark(event.surahId)
            is BookmarksEvent.ToggleAyahBookmark -> toggleAyahBookmark(event.surahId, event.ayah)
        }
    }

    private fun toggleSurahBookmark(surahId: Int) {
        viewModelScope.launch { preferences.toggleSurahBookmark(surahId) }
    }

    private fun toggleAyahBookmark(surahId: Int, ayah: Int) {
        viewModelScope.launch { preferences.toggleAyahBookmark(surahId, ayah) }
    }
}