package com.quranwords.ui.roots

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dagger.hilt.android.lifecycle.HiltViewModel
import com.quranwords.core.util.Result
import com.quranwords.core.util.runCatchingResult
import com.quranwords.domain.model.RootItem
import com.quranwords.domain.repository.QuranRepository
import javax.inject.Inject
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

@HiltViewModel
class RootsListViewModel @Inject constructor(
    private val repository: QuranRepository
) : ViewModel() {

    private val _roots = MutableStateFlow<List<RootItem>>(emptyList())
    private val _isLoading = MutableStateFlow(true)
    private val _error = MutableStateFlow<String?>(null)

    /** Query + model chips + sort order — one flow so the state stays typed. */
    private val _filters = MutableStateFlow(RootsFilters())

    init {
        loadRoots()
    }

    /**
     * Arabic-normalized search filtering lives here (AGENTS §9), not in the
     * composable — matches the root text, the Arabic gloss or the AI model,
     * then narrows by the model chips and sorts.
     */
    val uiState: StateFlow<RootsListUiState> = combine(
        _isLoading,
        _error,
        _filters,
        _roots
    ) { isLoading, error, filters, roots ->
        RootsListUiState(
            isLoading = isLoading,
            query = filters.query,
            filteredRoots = RootsListFilter.filter(
                roots = roots,
                query = filters.query,
                selectedModels = filters.selectedModels,
                sort = filters.sort
            ),
            totalCount = roots.size,
            error = error,
            availableModels = RootsListFilter.modelOptions(roots),
            selectedModels = filters.selectedModels,
            sort = filters.sort
        )
    }.stateIn(viewModelScope, SharingStarted.Eagerly, RootsListUiState())

    fun onEvent(event: RootsListEvent) {
        when (event) {
            is RootsListEvent.QueryChanged -> _filters.update { it.copy(query = event.query) }
            is RootsListEvent.ModelFilterToggled -> _filters.update { filters ->
                val selected = filters.selectedModels.toMutableSet()
                if (!selected.add(event.model)) selected.remove(event.model)
                filters.copy(selectedModels = selected)
            }
            RootsListEvent.ModelFiltersCleared ->
                _filters.update { it.copy(selectedModels = emptySet()) }
            is RootsListEvent.SortChanged -> _filters.update { it.copy(sort = event.sort) }
            RootsListEvent.Retry -> loadRoots()
        }
    }

    private fun loadRoots() {
        _isLoading.value = true
        _error.value = null
        viewModelScope.launch {
            when (val result = runCatchingResult { repository.getAllRoots() }) {
                is Result.Success -> _roots.value = result.data
                is Result.Error -> _error.value = result.message
            }
            _isLoading.value = false
        }
    }

    private data class RootsFilters(
        val query: String = "",
        val selectedModels: Set<String> = emptySet(),
        val sort: RootsSort = RootsSort.Default
    )
}
