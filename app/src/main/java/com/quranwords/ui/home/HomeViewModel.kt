package com.quranwords.ui.home

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dagger.hilt.android.lifecycle.HiltViewModel
import com.quranwords.core.util.Result
import com.quranwords.core.util.runCatchingResult
import com.quranwords.domain.repository.UserPreferencesRepository
import com.quranwords.domain.model.RootItem
import com.quranwords.domain.repository.QuranRepository
import javax.inject.Inject
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

@HiltViewModel
class HomeViewModel @Inject constructor(
    private val repository: QuranRepository,
    private val preferences: UserPreferencesRepository
) : ViewModel() {

    private val _featuredRoots = MutableStateFlow<List<RootItem>>(emptyList())
    private val _isDbReady = MutableStateFlow(repository.isDatabaseReady())

    private val _bookmarkedSurahs = MutableStateFlow<Set<String>>(emptySet())
    private val _bookmarkedAyat = MutableStateFlow<Set<String>>(emptySet())

    init {
        loadData()
        viewModelScope.launch {
            preferences.bookmarkedSurahs.collect { _bookmarkedSurahs.value = it }
        }
        viewModelScope.launch {
            preferences.bookmarkedAyat.collect { _bookmarkedAyat.value = it }
        }
    }

    fun refreshDbStatus() {
        _isDbReady.value = repository.isDatabaseReady()
    }

    fun loadData() {
        viewModelScope.launch {
            when (val result = runCatchingResult { repository.getRootsPaged(8, 0) }) {
                is Result.Success -> _featuredRoots.value = result.data
                is Result.Error -> Unit // featured roots are decorative; list stays empty
            }
        }
    }

    /**
     * Single-collection UiState; granular flows above stay as source of truth
     * (Decision 14). Last-read is combined reactively from DataStore so
     * returning to Home after reading updates the Continue card immediately.
     */
    private data class HomeFlowsA(
        val featuredRoots: List<RootItem>,
        val isDbReady: Boolean,
        val lastSurah: Int,
        val lastAyah: Int,
        val bookmarkedSurahs: Set<String>
    )

    /** Typed state assembly (P2-1/P2-2). */
    val uiState: StateFlow<HomeUiState> = combine(
        _featuredRoots, _isDbReady, preferences.lastReadSurah,
        preferences.lastReadAyah, _bookmarkedSurahs
    ) { roots, ready, lastSurah, lastAyah, bookmarkedSurahs ->
        HomeFlowsA(roots, ready, lastSurah, lastAyah, bookmarkedSurahs)
    }.combine(_bookmarkedAyat) { core, bookmarkedAyat ->
        HomeUiState(
            featuredRoots = core.featuredRoots,
            isDbReady = core.isDbReady,
            lastReadSurah = core.lastSurah,
            lastReadAyah = core.lastAyah,
            bookmarkedSurahs = core.bookmarkedSurahs,
            bookmarkedAyat = bookmarkedAyat
        )
    }.stateIn(viewModelScope, SharingStarted.Eagerly, HomeUiState())
}