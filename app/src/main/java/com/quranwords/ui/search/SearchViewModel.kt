package com.quranwords.ui.search

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.quranwords.core.util.ArabicNormalizer
import com.quranwords.core.util.DatabaseConstants
import com.quranwords.core.util.Result
import com.quranwords.core.util.runCatchingResult
import com.quranwords.domain.model.RootItem
import com.quranwords.domain.model.SearchResult
import com.quranwords.domain.repository.QuranRepository
import com.quranwords.domain.repository.UserPreferencesRepository
import com.quranwords.domain.usecase.SearchUseCase
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class SearchViewModel @Inject constructor(
    private val repository: QuranRepository,
    private val searchUseCase: SearchUseCase,
    private val preferences: UserPreferencesRepository
) : ViewModel() {
    private val pageSize: Int = DatabaseConstants.SEARCH_PAGE_SIZE

    private val _query = MutableStateFlow("")
    private val _results = MutableStateFlow(SearchResult())
    private val _scope = MutableStateFlow(SearchScope.ALL)
    private val _lexiconSubScope = MutableStateFlow(LexiconSubScope.ROOTS)
    private val _surahFilter = MutableStateFlow<Int?>(null)
    private val _recentSearches = MutableStateFlow<List<String>>(emptyList())
    private val _topRoots = MutableStateFlow<List<RootItem>>(emptyList())
    private val _bookmarkedAyat = MutableStateFlow<Set<String>>(emptySet())
    private val _isSearching = MutableStateFlow(false)
    private val _isLoadingMore = MutableStateFlow(false)
    private val _error = MutableStateFlow<String?>(null)

    private val rootsHasMore = MutableStateFlow(false)
    private val masadirHasMore = MutableStateFlow(false)
    private val derivativesHasMore = MutableStateFlow(false)
    private val ayatHasMore = MutableStateFlow(false)
    private val wordsHasMore = MutableStateFlow(false)

    private var rootsOffset = 0
    private var masadirOffset = 0
    private var derivativesOffset = 0
    private var ayatOffset = 0
    private var wordsOffset = 0
    private var searchJob: Job? = null

    init {
        viewModelScope.launch {
            preferences.recentSearches.collect { _recentSearches.value = it }
        }
        viewModelScope.launch {
            preferences.bookmarkedAyat.collect { _bookmarkedAyat.value = it }
        }
        viewModelScope.launch {
            // Explorer cache: single ordered query, never a full scan (Fix #4).
            _topRoots.value = runCatchingResult { repository.getTopRoots(50) }.let {
                when (it) {
                    is Result.Success -> it.data
                    is Result.Error -> emptyList()
                }
            }
        }
    }

    val uiState: StateFlow<SearchUiState> = combine(
        _query, _results, _scope, _lexiconSubScope, _surahFilter,
        _recentSearches, _topRoots, _bookmarkedAyat, _isSearching, _isLoadingMore, _error
    ) { flows ->
        @Suppress("UNCHECKED_CAST")
        val query = flows[0] as String
        val results = flows[1] as SearchResult
        val scope = flows[2] as SearchScope
        val sub = flows[3] as LexiconSubScope
        val filter = flows[4] as Int?
        val recents = flows[5] as List<String>
        @Suppress("UNCHECKED_CAST")
        val top = flows[6] as List<RootItem>
        val bookmarks = flows[7] as Set<String>
        val searching = flows[8] as Boolean
        val loadingMore = flows[9] as Boolean
        val error = flows[10] as String?
        SearchUiState(
            query = query, results = results, scope = scope, lexiconSubScope = sub,
            surahFilter = filter, recentSearches = recents, topRoots = top, bookmarkedAyat = bookmarks,
            isSearching = searching, isLoadingMore = loadingMore, error = error
        )
    }.stateIn(viewModelScope, SharingStarted.Eagerly, SearchUiState())

    fun onEvent(event: SearchEvent) {
        when (event) {
            is SearchEvent.QueryChanged -> onQueryChanged(event.query)
            is SearchEvent.NearingEnd -> loadMoreIfNeeded(event.tab, event.lastVisibleIndex)
            is SearchEvent.ScopeChanged -> _scope.value = event.scope
            is SearchEvent.LexiconSubScopeChanged -> _lexiconSubScope.value = event.subScope
            is SearchEvent.SurahFilterChanged -> { _surahFilter.value = event.surahId; runSearch(_query.value) }
            is SearchEvent.RecentSearchSelected -> onQueryChanged(event.query)
            is SearchEvent.RecentSearchRemoved -> viewModelScope.launch { preferences.removeRecentSearch(event.query) }
            SearchEvent.ClearRecents -> viewModelScope.launch { preferences.clearRecentSearches() }
            SearchEvent.Retry -> retry()
        }
    }

    fun onQueryChanged(newQuery: String) {
        _query.value = newQuery
        if (newQuery.isBlank()) {
            searchJob?.cancel()
            resetPagination()
            _results.value = SearchResult()
            _isSearching.value = false
            _error.value = null
            return
        }
        runSearch(newQuery)
    }

    private fun runSearch(newQuery: String) {
        searchJob?.cancel()
        searchJob = viewModelScope.launch {
            delay(200) // 200ms unified debounce (narrower than legacy 300ms)
            if (ArabicNormalizer.normalizeAr(newQuery).isBlank()) return@launch
            _isSearching.value = true
            resetPagination()
            val result: Result<SearchResult> = searchUseCase(newQuery, _surahFilter.value)
            when (result) {
                is Result.Success -> {
                    _error.value = null
                    _results.value = result.data
                    rootsOffset = result.data.roots.size
                    masadirOffset = result.data.masadir.size
                    derivativesOffset = result.data.derivatives.size
                    ayatOffset = result.data.ayat.size
                    wordsOffset = result.data.words.size
                    rootsHasMore.value = result.data.roots.size == pageSize
                    masadirHasMore.value = result.data.masadir.size == pageSize
                    derivativesHasMore.value = result.data.derivatives.size == pageSize
                    ayatHasMore.value = result.data.ayat.size == pageSize
                    wordsHasMore.value = result.data.words.size == pageSize
                }
                is Result.Error -> {
                    resetPagination()
                    _results.value = SearchResult()
                    _error.value = result.message
                }
            }
            _isSearching.value = false
        }
    }

    fun recordRecent(query: String) {
        viewModelScope.launch { preferences.addRecentSearch(query) }
    }

    fun toggleAyahBookmark(surahId: Int, ayahNum: Int) {
        viewModelScope.launch { preferences.toggleAyahBookmark(surahId, ayahNum) }
    }

    private fun retry() {
        val q = _query.value
        if (q.isBlank()) return
        runSearch(q)
    }

    private fun resetPagination() {
        rootsOffset = 0; masadirOffset = 0; derivativesOffset = 0; ayatOffset = 0; wordsOffset = 0
        rootsHasMore.value = false; masadirHasMore.value = false
        derivativesHasMore.value = false; ayatHasMore.value = false; wordsHasMore.value = false
        _isLoadingMore.value = false
    }

    private fun hasMoreFor(tab: SearchTab): Boolean = when (tab) {
        SearchTab.ROOTS -> rootsHasMore.value
        SearchTab.MASADIR -> masadirHasMore.value
        SearchTab.DERIVATIVES -> derivativesHasMore.value
        SearchTab.AYAT -> ayatHasMore.value
    }

    private fun sizeFor(tab: SearchTab): Int = when (tab) {
        SearchTab.ROOTS -> _results.value.roots.size
        SearchTab.MASADIR -> _results.value.masadir.size
        SearchTab.DERIVATIVES -> _results.value.derivatives.size
        SearchTab.AYAT -> _results.value.ayat.size
    }

    fun loadMoreIfNeeded(tab: SearchTab, lastVisibleIndex: Int) {
        if (_isSearching.value || _isLoadingMore.value) return
        if (!hasMoreFor(tab)) return
        if (lastVisibleIndex < sizeFor(tab) - 5) return
        loadMore(tab)
    }

    private fun loadMore(tab: SearchTab) {
        val q = _query.value
        if (q.isBlank() || _isLoadingMore.value || !hasMoreFor(tab)) return
        viewModelScope.launch {
            _isLoadingMore.value = true
            delay(80)
            val fetched = runCatchingResult {
                when (tab) {
                    SearchTab.ROOTS -> {
                        val next = repository.searchRootsPaged(q, pageSize, rootsOffset)
                        if (next.isNotEmpty()) {
                            _results.value = _results.value.copy(roots = _results.value.roots + next)
                            rootsOffset += next.size
                        }
                        next.size
                    }
                    SearchTab.MASADIR -> {
                        val next = repository.searchMasadirPaged(q, pageSize, masadirOffset)
                        if (next.isNotEmpty()) {
                            _results.value = _results.value.copy(masadir = _results.value.masadir + next)
                            masadirOffset += next.size
                        }
                        next.size
                    }
                    SearchTab.DERIVATIVES -> {
                        val next = repository.searchDerivativesPaged(q, pageSize, derivativesOffset)
                        if (next.isNotEmpty()) {
                            _results.value = _results.value.copy(derivatives = _results.value.derivatives + next)
                            derivativesOffset += next.size
                        }
                        next.size
                    }
                    SearchTab.AYAT -> {
                        val next = repository.searchAyatPaged(q, pageSize, ayatOffset, _surahFilter.value)
                        if (next.isNotEmpty()) {
                            _results.value = _results.value.copy(ayat = _results.value.ayat + next)
                            ayatOffset += next.size
                        }
                        next.size
                    }
                }
            }
            when (fetched) {
                is Result.Success -> { _error.value = null; setHasMore(tab, fetched.data == pageSize) }
                is Result.Error -> { setHasMore(tab, false); _error.value = fetched.message }
            }
            _isLoadingMore.value = false
        }
    }

    private fun setHasMore(tab: SearchTab, hasMore: Boolean) {
        when (tab) {
            SearchTab.ROOTS -> rootsHasMore.value = hasMore
            SearchTab.MASADIR -> masadirHasMore.value = hasMore
            SearchTab.DERIVATIVES -> derivativesHasMore.value = hasMore
            SearchTab.AYAT -> ayatHasMore.value = hasMore
        }
    }
}
