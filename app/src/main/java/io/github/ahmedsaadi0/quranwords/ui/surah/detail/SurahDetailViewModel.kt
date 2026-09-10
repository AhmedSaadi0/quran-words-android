package io.github.ahmedsaadi0.quranwords.ui.surah.detail

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dagger.hilt.android.lifecycle.HiltViewModel
import io.github.ahmedsaadi0.quranwords.core.util.QuranCopyFormatter
import io.github.ahmedsaadi0.quranwords.core.util.Result
import io.github.ahmedsaadi0.quranwords.core.util.SelectionState
import io.github.ahmedsaadi0.quranwords.core.util.runCatchingResult
import io.github.ahmedsaadi0.quranwords.data.repository.UserPreferencesRepository
import io.github.ahmedsaadi0.quranwords.domain.model.Ayah
import io.github.ahmedsaadi0.quranwords.domain.model.Surah
import io.github.ahmedsaadi0.quranwords.domain.model.WordToken
import io.github.ahmedsaadi0.quranwords.domain.repository.QuranRepository
import javax.inject.Inject
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

/**
 * Owns everything the surah detail screen needs (Decision 14): ayat paging,
 * word selection + AI summary, ayah multi-selection, font size and bookmarks
 * slices — the screen no longer receives a shared MainViewModel.
 */
@HiltViewModel
class SurahDetailViewModel @Inject constructor(
    private val repository: QuranRepository,
    private val preferences: UserPreferencesRepository
) : ViewModel() {

    private val _surah = MutableStateFlow<Surah?>(null)
    private val _ayat = MutableStateFlow<List<Ayah>>(emptyList())
    val ayat: StateFlow<List<Ayah>> = _ayat.asStateFlow()

    private val _selectedWord = MutableStateFlow<WordToken?>(null)
    private val _selectedWordAyah = MutableStateFlow<Ayah?>(null)

    private val _isLoading = MutableStateFlow(true)
    private val _isLoadingMore = MutableStateFlow(false)

    private val _aiSummary = MutableStateFlow<String?>(null)
    private val _aiModel = MutableStateFlow<String?>(null)
    private val _aiGeneratedAt = MutableStateFlow<String?>(null)
    private val _isAiLoading = MutableStateFlow(false)

    // Multi-ayah copy selection (contextual TopBar)
    private val _selection = MutableStateFlow(SelectionState())
    val selection: StateFlow<SelectionState> = _selection.asStateFlow()

    private val _surahPages = MutableStateFlow<List<Int>>(emptyList())
    private val _error = MutableStateFlow<String?>(null)

    private var currentSurahId: Int = -1
    private var currentOffset: Int = 0
    private var hasMore: Boolean = true
    private val pageSize: Int = 20

    // Preference slices kept as separate flows so the combine above stays readable.
    private val fontState = MutableStateFlow(24f)
    private val bookmarkSurahState = MutableStateFlow<Set<String>>(emptySet())
    private val bookmarkAyahState = MutableStateFlow<Set<String>>(emptySet())
    private val dbReadyState = MutableStateFlow(repository.isDatabaseReady())

    val uiState: StateFlow<SurahDetailUiState> = combine(
        _surah,
        _ayat,
        _isLoading,
        _isLoadingMore,
        _selectedWord,
        _selectedWordAyah,
        _aiSummary,
        _aiModel,
        _aiGeneratedAt,
        _isAiLoading,
        _selection,
        _surahPages,
        _error
    ) { values ->
        @Suppress("UNCHECKED_CAST")
        SurahDetailUiState(
            surah = values[0] as Surah?,
            ayat = values[1] as List<Ayah>,
            isLoading = values[2] as Boolean,
            isLoadingMore = values[3] as Boolean,
            selectedWord = values[4] as WordToken?,
            selectedWordAyah = values[5] as Ayah?,
            aiSummary = values[6] as String?,
            aiModel = values[7] as String?,
            aiGeneratedAt = values[8] as String?,
            isAiLoading = values[9] as Boolean,
            isSelectionMode = (values[10] as SelectionState).isSelectionMode,
            selectedAyahs = (values[10] as SelectionState).selectedIds,
            surahPages = values[11] as List<Int>,
            error = values[12] as String?
        )
    }.stateIn(viewModelScope, SharingStarted.Eagerly, SurahDetailUiState())

    init {
        viewModelScope.launch {
            preferences.fontSize.collectLatest { fontState.value = it }
        }
        viewModelScope.launch {
            preferences.bookmarkedSurahs.collectLatest { bookmarkSurahState.value = it }
        }
        viewModelScope.launch {
            preferences.bookmarkedAyat.collectLatest { bookmarkAyahState.value = it }
        }
    }

    /**
     * Derived single-collection state including preference slices; consumers
     * (Route/Screen) collect only this.
     */
    val screenState: StateFlow<SurahDetailUiState> = combine(
        uiState, fontState, bookmarkSurahState, bookmarkAyahState, dbReadyState
    ) { base, fontSize, surahMarks, ayahMarks, dbReady ->
        base.copy(
            fontSize = fontSize,
            bookmarkedSurahs = surahMarks,
            bookmarkedAyat = ayahMarks,
            isDbReady = dbReady
        )
    }.stateIn(viewModelScope, SharingStarted.Eagerly, SurahDetailUiState())

    fun onEvent(event: SurahDetailEvent) {
        when (event) {
            is SurahDetailEvent.Load -> loadSurah(event.surahId)
            is SurahDetailEvent.WordSelected -> selectWord(event.word, event.ayah)
            SurahDetailEvent.DismissWord -> clearSelectedWord()
            is SurahDetailEvent.AyahVisible -> updateLastRead(currentSurahId, event.ayahNum)
            is SurahDetailEvent.NearingEnd -> loadMoreIfNeeded(event.lastVisibleIndex)
            is SurahDetailEvent.EnsureAyahLoaded -> viewModelScope.launch { ensureAyahLoaded(event.ayah) }
            is SurahDetailEvent.EnsurePageLoaded -> viewModelScope.launch { ensurePageLoaded(event.page) }
            is SurahDetailEvent.ToggleAyahBookmark ->
                viewModelScope.launch { preferences.toggleAyahBookmark(event.surahId, event.ayah) }
            is SurahDetailEvent.ToggleSurahBookmark ->
                viewModelScope.launch { preferences.toggleSurahBookmark(event.surahId) }
            is SurahDetailEvent.SetFontSize ->
                viewModelScope.launch { preferences.setFontSize(event.size.coerceIn(1f, 48f)) }
            is SurahDetailEvent.EnterSelection -> _selection.update { it.enter(event.ayah) }
            is SurahDetailEvent.ToggleAyahSelection -> _selection.update { it.toggle(event.ayah) }
            SurahDetailEvent.SelectAllAyahs -> _selection.update {
                it.selectAll(_ayat.value.map { ayah -> ayah.ayah })
            }
            SurahDetailEvent.ClearSelection -> _selection.update { it.clear() }
            // Platform events (copy/share selection) are handled by the Route.
            SurahDetailEvent.CopySelection, SurahDetailEvent.ShareSelection -> Unit
        }
    }

    fun loadSurah(surahId: Int) {
        // Guard: if the surah is already loaded with ayat, keep state
        if (currentSurahId == surahId && _surah.value?.id == surahId && _ayat.value.isNotEmpty()) {
            return
        }

        currentSurahId = surahId
        currentOffset = 0
        hasMore = true
        _isLoading.value = true
        _ayat.value = emptyList()
        _error.value = null

        viewModelScope.launch {
            // Delay until enter animation finishes (250ms + buffer) to avoid jank
            delay(300)
            val result = runCatchingResult {
                val surah = repository.getSurahById(surahId)
                val firstPage = repository.getAyatBySurahPaged(surahId, pageSize, 0)
                surah to firstPage
            }
            when (result) {
                is Result.Success -> {
                    val (surah, firstPage) = result.data
                    _surah.value = surah
                    _ayat.value = firstPage
                    currentOffset = firstPage.size
                    val total = surah?.ayahCount ?: 0
                    hasMore = firstPage.size == pageSize && currentOffset < total
                }
                is Result.Error -> _error.value = result.message
            }
            _isLoading.value = false
        }

        viewModelScope.launch {
            // Supplementary data: on failure the page header simply stays hidden.
            when (val result = runCatchingResult { repository.getPagesForSurah(surahId) }) {
                is Result.Success -> _surahPages.value = result.data
                is Result.Error -> _surahPages.value = emptyList()
            }
        }
    }

    fun loadMoreIfNeeded(lastVisibleIndex: Int) {
        if (_isLoading.value || _isLoadingMore.value || !hasMore) return
        // Trigger when within 5 items from end (Basmalah offset handled in UI)
        if (lastVisibleIndex >= _ayat.value.size - 5) {
            loadNextPage()
        }
    }

    private fun loadNextPage() {
        if (_isLoadingMore.value || !hasMore) return
        viewModelScope.launch {
            _isLoadingMore.value = true
            // Small debounce to avoid rapid triggers
            delay(80)
            when (val result = runCatchingResult {
                repository.getAyatBySurahPaged(currentSurahId, pageSize, currentOffset)
            }) {
                is Result.Success -> {
                    val nextPage = result.data
                    if (nextPage.isNotEmpty()) {
                        _ayat.value = _ayat.value + nextPage
                        currentOffset += nextPage.size
                        val total = _surah.value?.ayahCount ?: Int.MAX_VALUE
                        hasMore = nextPage.size == pageSize && currentOffset < total
                    } else {
                        hasMore = false
                    }
                }
                is Result.Error -> hasMore = false
            }
            _isLoadingMore.value = false
        }
    }

    suspend fun ensureAyahLoaded(targetAyah: Int) {
        // Keep loading pages until targetAyah is in list or no more
        while (hasMore && _ayat.value.none { it.ayah == targetAyah }) {
            val result = runCatchingResult {
                repository.getAyatBySurahPaged(currentSurahId, pageSize, currentOffset)
            }
            val nextPage = (result as? Result.Success)?.data ?: emptyList()
            if (nextPage.isEmpty()) {
                hasMore = false
                break
            }
            _ayat.value = _ayat.value + nextPage
            currentOffset += nextPage.size
            val total = _surah.value?.ayahCount ?: Int.MAX_VALUE
            hasMore = nextPage.size == pageSize && currentOffset < total
            // Small yield to not block UI
            delay(10)
        }
    }

    suspend fun ensurePageLoaded(targetPage: Int) {
        while (hasMore && _ayat.value.none { it.pageNumber == targetPage }) {
            if (_isLoadingMore.value) {
                delay(80)
                continue
            }
            _isLoadingMore.value = true
            delay(80)
            val result = runCatchingResult {
                repository.getAyatBySurahPaged(currentSurahId, pageSize, currentOffset)
            }
            val nextPage = (result as? Result.Success)?.data ?: emptyList()
            if (nextPage.isEmpty()) {
                hasMore = false
                _isLoadingMore.value = false
                break
            }
            _ayat.value = _ayat.value + nextPage
            currentOffset += nextPage.size
            val total = _surah.value?.ayahCount ?: Int.MAX_VALUE
            hasMore = nextPage.size == pageSize && currentOffset < total
            _isLoadingMore.value = false
            delay(10)
        }
    }

    fun updateLastRead(surahId: Int, ayahNum: Int) {
        if (surahId <= 0) return
        viewModelScope.launch { preferences.setLastRead(surahId, ayahNum) }
    }

    private fun selectWord(word: WordToken, ayah: Ayah) {
        _selectedWord.value = word
        _selectedWordAyah.value = ayah
        // Fetch AI summary via repository (UDF: ViewModel owns data, not Composable)
        _aiSummary.value = null
        _aiModel.value = null
        _aiGeneratedAt.value = null
        val rootId = word.rootId
        if (rootId != null && rootId > 0) {
            _isAiLoading.value = true
            viewModelScope.launch {
                // AI summary is supplementary: on failure the UI shows its fallback.
                when (val result = runCatchingResult { repository.getRootDetail(rootId) }) {
                    is Result.Success -> {
                        val detail = result.data
                        _aiSummary.value = detail?.aiSummary
                        _aiModel.value = detail?.aiModel
                        _aiGeneratedAt.value = detail?.aiGeneratedAt
                    }
                    is Result.Error -> Unit
                }
                _isAiLoading.value = false
            }
        } else {
            _isAiLoading.value = false
        }
    }

    fun clearSelectedWord() {
        _selectedWord.value = null
        _selectedWordAyah.value = null
        _aiSummary.value = null
        _aiModel.value = null
        _aiGeneratedAt.value = null
        _isAiLoading.value = false
    }

    /** Formats the selected ayat for copy/share (consumed by the Route's ShareHandler). */
    fun getFormattedSelection(): String {
        val surahVal = _surah.value
        val selectedNums = _selection.value.selectedIds
        if (selectedNums.isEmpty()) return ""
        val selectedList = _ayat.value.filter { it.ayah in selectedNums }.sortedBy { it.ayah }
        if (selectedList.isEmpty()) return ""
        return QuranCopyFormatter.formatMultiple(selectedList, surahVal)
    }

    fun getFormattedSingle(ayah: Ayah): String {
        return QuranCopyFormatter.formatSingle(ayah, _surah.value)
    }
}