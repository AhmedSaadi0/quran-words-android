package io.github.ahmedsaadi0.quranwords.ui.roots

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dagger.hilt.android.lifecycle.HiltViewModel
import io.github.ahmedsaadi0.quranwords.core.util.ArabicNormalizer
import io.github.ahmedsaadi0.quranwords.core.util.Result
import io.github.ahmedsaadi0.quranwords.core.util.runCatchingResult
import io.github.ahmedsaadi0.quranwords.domain.model.RootItem
import io.github.ahmedsaadi0.quranwords.domain.repository.QuranRepository
import javax.inject.Inject
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

@HiltViewModel
class RootsListViewModel @Inject constructor(
    private val repository: QuranRepository
) : ViewModel() {

    private val _roots = MutableStateFlow<List<RootItem>>(emptyList())
    private val _isLoading = MutableStateFlow(true)
    private val _query = MutableStateFlow("")
    private val _error = MutableStateFlow<String?>(null)

    init {
        loadRoots()
    }

    /**
     * Arabic-normalized search filtering lives here (AGENTS §9), not in the
     * composable — matches root text or Arabic gloss.
     */
    val uiState: StateFlow<RootsListUiState> = combine(
        _isLoading,
        _query,
        _error,
        _roots
    ) { isLoading, query, error, roots ->
        val normalized = ArabicNormalizer.normalizeAr(query)
        val filtered = if (normalized.isBlank()) {
            roots
        } else {
            roots.filter {
                ArabicNormalizer.normalizeAr(it.root).contains(normalized) ||
                    (it.glossAr != null && ArabicNormalizer.normalizeAr(it.glossAr).contains(normalized))
            }
        }
        RootsListUiState(
            isLoading = isLoading,
            query = query,
            filteredRoots = filtered,
            totalCount = roots.size,
            error = error
        )
    }.stateIn(viewModelScope, SharingStarted.Eagerly, RootsListUiState())

    fun onEvent(event: RootsListEvent) {
        when (event) {
            is RootsListEvent.QueryChanged -> _query.value = event.query
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
}