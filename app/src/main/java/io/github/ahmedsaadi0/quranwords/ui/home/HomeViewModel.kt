package io.github.ahmedsaadi0.quranwords.ui.home

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dagger.hilt.android.lifecycle.HiltViewModel
import io.github.ahmedsaadi0.quranwords.core.util.Result
import io.github.ahmedsaadi0.quranwords.core.util.runCatchingResult
import io.github.ahmedsaadi0.quranwords.data.repository.UserPreferencesRepository
import io.github.ahmedsaadi0.quranwords.domain.model.RootItem
import io.github.ahmedsaadi0.quranwords.domain.repository.QuranRepository
import javax.inject.Inject
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

@HiltViewModel
class HomeViewModel @Inject constructor(
    private val repository: QuranRepository,
    private val preferences: UserPreferencesRepository
) : ViewModel() {

    private val _featuredRoots = MutableStateFlow<List<RootItem>>(emptyList())
    val featuredRoots: StateFlow<List<RootItem>> = _featuredRoots.asStateFlow()

    private val _isDbReady = MutableStateFlow(repository.isDatabaseReady())
    val isDbReady: StateFlow<Boolean> = _isDbReady.asStateFlow()

    private val _lastReadSurah = MutableStateFlow(1)
    private val _lastReadAyah = MutableStateFlow(1)
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
        viewModelScope.launch {
            _lastReadSurah.value = preferences.lastReadSurah.first()
            _lastReadAyah.value = preferences.lastReadAyah.first()
        }
    }

    /**
     * Single-collection UiState; granular flows above stay as source of truth
     * (Decision 14).
     */
    val uiState: StateFlow<HomeUiState> = combine(
        _featuredRoots,
        _isDbReady,
        _lastReadSurah,
        _lastReadAyah,
        _bookmarkedSurahs,
        _bookmarkedAyat
    ) { values ->
        @Suppress("UNCHECKED_CAST")
        HomeUiState(
            featuredRoots = values[0] as List<RootItem>,
            isDbReady = values[1] as Boolean,
            lastReadSurah = values[2] as Int,
            lastReadAyah = values[3] as Int,
            bookmarkedSurahs = values[4] as Set<String>,
            bookmarkedAyat = values[5] as Set<String>
        )
    }.stateIn(viewModelScope, SharingStarted.Eagerly, HomeUiState())
}