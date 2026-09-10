package io.github.ahmedsaadi0.quranwords.ui.search

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dagger.hilt.android.lifecycle.HiltViewModel
import io.github.ahmedsaadi0.quranwords.core.util.DatabaseConstants
import io.github.ahmedsaadi0.quranwords.core.util.Result
import io.github.ahmedsaadi0.quranwords.core.util.runCatchingResult
import io.github.ahmedsaadi0.quranwords.domain.model.SearchResult
import io.github.ahmedsaadi0.quranwords.domain.repository.QuranRepository
import javax.inject.Inject
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

@HiltViewModel
class SearchViewModel @Inject constructor(
    private val repository: QuranRepository
) : ViewModel() {

    private val pageSize: Int = DatabaseConstants.SEARCH_PAGE_SIZE

    private val _query = MutableStateFlow("")
    val query: StateFlow<String> = _query.asStateFlow()

    private val _results = MutableStateFlow(SearchResult())
    val results: StateFlow<SearchResult> = _results.asStateFlow()

    private val _isSearching = MutableStateFlow(false)
    val isSearching: StateFlow<Boolean> = _isSearching.asStateFlow()

    private val _isLoadingMore = MutableStateFlow(false)
    val isLoadingMore: StateFlow<Boolean> = _isLoadingMore.asStateFlow()

    private val _error = MutableStateFlow<String?>(null)

    private val rootsHasMore = MutableStateFlow(false)
    private val masadirHasMore = MutableStateFlow(false)
    private val derivativesHasMore = MutableStateFlow(false)
    private val ayatHasMore = MutableStateFlow(false)

    private var rootsOffset: Int = 0
    private var masadirOffset: Int = 0
    private var derivativesOffset: Int = 0
    private var ayatOffset: Int = 0

    private var searchJob: Job? = null

    /**
     * Single-collection UiState; granular flows above stay as source of truth
     * (RootDetail adapter precedent, Decision 14).
     */
    val uiState: StateFlow<SearchUiState> = combine(
        _query,
        _results,
        _isSearching,
        _isLoadingMore,
        _error
    ) { query, results, isSearching, isLoadingMore, error ->
        SearchUiState(
            query = query,
            results = results,
            isSearching = isSearching,
            isLoadingMore = isLoadingMore,
            error = error
        )
    }.stateIn(viewModelScope, SharingStarted.Eagerly, SearchUiState())

    fun onEvent(event: SearchEvent) {
        when (event) {
            is SearchEvent.QueryChanged -> onQueryChanged(event.query)
            is SearchEvent.NearingEnd -> loadMoreIfNeeded(event.tab, event.lastVisibleIndex)
            SearchEvent.Retry -> retry()
        }
    }

    fun onQueryChanged(newQuery: String) {
        _query.value = newQuery
        searchJob?.cancel()
        if (newQuery.isBlank()) {
            resetPagination()
            _results.value = SearchResult()
            _isSearching.value = false
            _error.value = null
            return
        }
        searchJob = viewModelScope.launch {
            delay(300)
            _isSearching.value = true
            resetPagination()
            when (val result = runCatchingResult { repository.searchAll(newQuery) }) {
                is Result.Success -> {
                    _error.value = null
                    val firstPage = result.data
                    _results.value = firstPage
                    rootsOffset = firstPage.roots.size
                    masadirOffset = firstPage.masadir.size
                    derivativesOffset = firstPage.derivatives.size
                    ayatOffset = firstPage.ayat.size
                    rootsHasMore.value = firstPage.roots.size == pageSize
                    masadirHasMore.value = firstPage.masadir.size == pageSize
                    derivativesHasMore.value = firstPage.derivatives.size == pageSize
                    ayatHasMore.value = firstPage.ayat.size == pageSize
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

    private fun retry() {
        val q = _query.value
        if (q.isBlank()) return
        onQueryChanged(q)
    }

    private fun resetPagination() {
        rootsOffset = 0
        masadirOffset = 0
        derivativesOffset = 0
        ayatOffset = 0
        rootsHasMore.value = false
        masadirHasMore.value = false
        derivativesHasMore.value = false
        ayatHasMore.value = false
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

    /**
     * One parameterized pager for all tabs — replaces the four copy-pasted
     * loadMoreX() methods. Each branch fetches its typed page, appends it and
     * advances its own offset; the common exit sets hasMore and surfaces errors.
     */
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
                        val next = repository.searchAyatPaged(q, pageSize, ayatOffset)
                        if (next.isNotEmpty()) {
                            _results.value = _results.value.copy(ayat = _results.value.ayat + next)
                            ayatOffset += next.size
                        }
                        next.size
                    }
                }
            }
            when (fetched) {
                is Result.Success -> {
                    _error.value = null
                    setHasMore(tab, fetched.data == pageSize)
                }
                is Result.Error -> {
                    setHasMore(tab, false)
                    _error.value = fetched.message
                }
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