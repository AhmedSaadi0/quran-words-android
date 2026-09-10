package io.github.ahmedsaadi0.quranwords.ui.surah

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dagger.hilt.android.lifecycle.HiltViewModel
import io.github.ahmedsaadi0.quranwords.core.util.ArabicNormalizer
import io.github.ahmedsaadi0.quranwords.core.util.RevelationFilter
import io.github.ahmedsaadi0.quranwords.core.util.isMeccan
import io.github.ahmedsaadi0.quranwords.data.repository.UserPreferencesRepository
import io.github.ahmedsaadi0.quranwords.domain.model.Surah
import io.github.ahmedsaadi0.quranwords.domain.repository.QuranRepository
import javax.inject.Inject
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

@HiltViewModel
class SurahIndexViewModel @Inject constructor(
    private val repository: QuranRepository,
    private val preferences: UserPreferencesRepository
) : ViewModel() {

    private val _surahs = MutableStateFlow<List<Surah>>(emptyList())
    private val _query = MutableStateFlow("")
    private val _filter = MutableStateFlow(RevelationFilter.ALL)
    private val _bookmarkedSurahIds = MutableStateFlow<Set<Int>>(emptySet())
    private val _error = MutableStateFlow<String?>(null)

    init {
        viewModelScope.launch {
            repository.getSurahs()
                .catch { throwable ->
                    _error.value = throwable.message ?: "Unexpected error"
                }
                .collect { surahs ->
                    _surahs.value = surahs
                }
        }
        viewModelScope.launch {
            preferences.bookmarkedSurahs.collectLatest { keys ->
                _bookmarkedSurahIds.value = keys.mapNotNull { it.toIntOrNull() }.toSet()
            }
        }
    }

    /**
     * Filtering (revelation type + Arabic-normalized name search) lives here
     * per AGENTS §9 — no normalization logic in the composable.
     * Loading is derived from emptiness: the corpus always emits 114 rows.
     */
    val uiState: StateFlow<SurahIndexUiState> = combine(
        _query,
        _filter,
        _bookmarkedSurahIds,
        _surahs,
        _error
    ) { query, filter, bookmarkedSurahIds, surahs, error ->
        val queryNorm = ArabicNormalizer.normalizeAr(query)
        val filtered = surahs.filter { surah ->
            val matchesFilter = when (filter) {
                RevelationFilter.ALL -> true
                RevelationFilter.MECCAN -> surah.isMeccan
                RevelationFilter.MEDINAN -> !surah.isMeccan
            }
            val matchesQuery = queryNorm.isBlank() ||
                ArabicNormalizer.normalizeAr(surah.nameAr).contains(queryNorm) ||
                surah.nameEn.contains(query, ignoreCase = true) ||
                surah.id.toString() == query.trim()
            matchesFilter && matchesQuery
        }
        SurahIndexUiState(
            isLoading = surahs.isEmpty(),
            query = query,
            filter = filter,
            filteredSurahs = filtered,
            bookmarkedSurahIds = bookmarkedSurahIds,
            error = error
        )
    }.stateIn(viewModelScope, SharingStarted.Eagerly, SurahIndexUiState())

    fun onEvent(event: SurahIndexEvent) {
        when (event) {
            is SurahIndexEvent.QueryChanged -> _query.value = event.query
            is SurahIndexEvent.FilterChanged -> _filter.value = event.filter
            is SurahIndexEvent.ToggleBookmark -> toggleSurahBookmark(event.surahId)
        }
    }

    private fun toggleSurahBookmark(surahId: Int) {
        viewModelScope.launch { preferences.toggleSurahBookmark(surahId) }
    }
}