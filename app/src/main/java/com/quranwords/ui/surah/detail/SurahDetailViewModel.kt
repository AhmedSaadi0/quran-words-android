package com.quranwords.ui.surah.detail

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dagger.hilt.android.lifecycle.HiltViewModel
import com.quranwords.core.util.FontSizePreference
import com.quranwords.core.util.QuranCopyFormatter
import com.quranwords.core.util.Result
import com.quranwords.core.util.SelectionState
import com.quranwords.core.util.SurahMetadata
import com.quranwords.core.util.runCatchingResult
import com.quranwords.domain.repository.UserPreferencesRepository
import com.quranwords.domain.model.Ayah
import com.quranwords.domain.model.AiExtendedSummary
import com.quranwords.domain.model.Surah
import com.quranwords.domain.model.WordToken
import com.quranwords.domain.repository.QuranRepository
import com.quranwords.ui.theme.QuranFont
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
    private val _aiExtended = MutableStateFlow<AiExtendedSummary?>(null)
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
    private val _hasMore = MutableStateFlow(true)
    private val pageSize: Int = 20
    private var lastWrittenLastRead: Pair<Int, Int>? = null

    // Preference slices kept as separate flows so the combine above stays readable.
    private val fontState = MutableStateFlow(24f)
    private val quranFontState = MutableStateFlow(QuranFont.KFGQPC_HAFS_1441)
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
        _aiExtended,
        _aiModel,
        _aiGeneratedAt,
        _isAiLoading,
        _selection,
        _surahPages,
        _error,
        _hasMore
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
            aiExtended = values[7] as AiExtendedSummary?,
            aiModel = values[8] as String?,
            aiGeneratedAt = values[9] as String?,
            isAiLoading = values[10] as Boolean,
            isSelectionMode = (values[11] as SelectionState).isSelectionMode,
            selectedAyahs = (values[11] as SelectionState).selectedIds,
            surahPages = values[12] as List<Int>,
            error = values[13] as String?,
            hasMoreAyat = values[14] as Boolean
        )
    }.stateIn(viewModelScope, SharingStarted.Eagerly, SurahDetailUiState())

    init {
        viewModelScope.launch {
            preferences.fontSize.collectLatest { fontState.value = it }
        }
        viewModelScope.launch {
            preferences.quranFontKey.collectLatest { quranFontState.value = QuranFont.fromKey(it) }
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
        uiState, fontState, quranFontState, bookmarkAyahState, dbReadyState
    ) { values ->
        @Suppress("UNCHECKED_CAST")
        (values[0] as SurahDetailUiState).copy(
            fontSize = values[1] as Float,
            quranFont = values[2] as QuranFont,
            bookmarkedAyat = values[3] as Set<String>,
            isDbReady = values[4] as Boolean
        )
    }.stateIn(viewModelScope, SharingStarted.Eagerly, SurahDetailUiState())

    fun onEvent(event: SurahDetailEvent) {
        when (event) {
            is SurahDetailEvent.Load -> loadSurah(event.surahId, event.targetAyah, event.immediate)
            is SurahDetailEvent.WordSelected -> selectWord(event.word, event.ayah)
            SurahDetailEvent.DismissWord -> clearSelectedWord()
            is SurahDetailEvent.AyahVisible -> updateLastRead(currentSurahId, event.ayahNum)
            is SurahDetailEvent.NearingEnd -> loadMoreIfNeeded(event.lastVisibleIndex)
            is SurahDetailEvent.EnsureAyahLoaded -> viewModelScope.launch { ensureAyahLoaded(event.ayah) }
            is SurahDetailEvent.EnsurePageLoaded -> viewModelScope.launch { ensurePageLoaded(event.page) }
            is SurahDetailEvent.ToggleAyahBookmark ->
                viewModelScope.launch { preferences.toggleAyahBookmark(event.surahId, event.ayah) }
            is SurahDetailEvent.SetFontSize ->
                viewModelScope.launch { preferences.setFontSize(FontSizePreference.clamp(event.size)) }
            is SurahDetailEvent.EnterSelection -> _selection.update { it.enter(event.ayah) }
            is SurahDetailEvent.ToggleAyahSelection -> _selection.update { it.toggle(event.ayah) }
            is SurahDetailEvent.RangeSelect -> rangeSelect(event.ayah)
            SurahDetailEvent.ClearSelection -> _selection.update { it.clear() }
            SurahDetailEvent.BookmarkSelection -> bookmarkSelection()
            // Platform events (copy/share selection) are handled by the Route.
            SurahDetailEvent.CopySelection, SurahDetailEvent.ShareSelection -> Unit
        }
    }

    /**
     * Extends the selection from the anchor to [ayah] (union). Selection ids
     * apply instantly even beyond the loaded pages; the far end is then paged
     * in behind so its wash renders as pages arrive.
     */
    private fun rangeSelect(ayah: Int) {
        val anchor = _selection.value.anchorId
        _selection.update { it.rangeTo(ayah) }
        val farEnd = maxOf(anchor ?: ayah, ayah)
        viewModelScope.launch { ensureAyahLoaded(farEnd) }
    }

    private fun bookmarkSelection() {
        val surahId = currentSurahId
        val selected = _selection.value.selectedIds
        if (surahId <= 0 || selected.isEmpty()) return
        // Optimistic clear: selection is UI state, persistence continues in
        // the background — the bar dismisses instantly on tap.
        _selection.update { it.clear() }
        viewModelScope.launch {
            selected.forEach { ayahNum -> preferences.toggleAyahBookmark(surahId, ayahNum) }
        }
    }

    /**
     * @param targetAyah ayah being entered at. When it is beyond the first page,
     *   the initial query reaches **through** it (plus a small runway) in a
     *   single round-trip: the target block is present in the very first
     *   composition, so the reader never sees ayah 1 and no scroll hunt runs.
     * @param immediate true for adjacent-surah switches: the vertical slide is
     * already running, so first-page content must land inside the animation
     * instead of after the enter-animation warm-up delay.
     */
    fun loadSurah(surahId: Int, targetAyah: Int = 1, immediate: Boolean = false) {
        // Guard: if the surah is already loaded with ayat, keep state
        if (currentSurahId == surahId && _surah.value?.id == surahId && _ayat.value.isNotEmpty()) {
            return
        }

        currentSurahId = surahId
        currentOffset = 0
        _hasMore.value = true
        _isLoading.value = true
        _ayat.value = emptyList()
        _error.value = null

        viewModelScope.launch {
            // Warm-up only when the entry content IS the first page. A deep link
            // to a mid-surah ayah must query at 0ms, not after the transition.
            if (!immediate && targetAyah == 1) delay(200)
            val result = runCatchingResult {
                val surah = repository.getSurahById(surahId)
                // Fall back to the bundled reference data: a missing or
                // zero-count surah row would otherwise collapse the window to a
                // single ayah and turn a deep link into a paging hunt.
                val total = surah?.ayahCount?.takeIf { it > 0 }
                    ?: SurahMetadata.SURAHS.getOrNull(surahId - 1)?.ayahCount
                    ?: 0
                surah to repository.getAyatBySurahPaged(
                    surahId,
                    initialAyatLimit(targetAyah, pageSize, total),
                    0
                )
            }
            when (result) {
                is Result.Success -> {
                    val (surah, initialAyat) = result.data
                    _surah.value = surah
                    _ayat.value = initialAyat
                    currentOffset = initialAyat.size
                    // Compare loaded count against the surah total: the first
                    // fetch is deliberately larger than one page on deep links,
                    // so a page-sized check would wrongly report "no more".
                    _hasMore.value = currentOffset < (surah?.ayahCount ?: 0)
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
        if (_isLoading.value || _isLoadingMore.value || !_hasMore.value) return
        // Trigger when within 5 items from end (Basmalah offset handled in UI)
        if (lastVisibleIndex >= _ayat.value.size - 5) {
            loadNextPage()
        }
    }

    private fun loadNextPage() {
        if (_isLoadingMore.value || !_hasMore.value) return
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
                        appendAyat(nextPage)
                        val total = _surah.value?.ayahCount ?: Int.MAX_VALUE
                        _hasMore.value = nextPage.size == pageSize && currentOffset < total
                    } else {
                        _hasMore.value = false
                    }
                }
                is Result.Error -> _hasMore.value = false
            }
            _isLoadingMore.value = false
        }
    }

    /**
     * Appends paged ayat deduplicated by ayah number so concurrent
     * [loadNextPage]/[ensureAyahLoaded]/[ensurePageLoaded] fetches at a stale
     * offset can never duplicate rows (which previously produced duplicate
     * `mushaf_page_*` LazyColumn keys). [currentOffset] advances only by the
     * actually-added count so a duplicate fetch never skips verses.
     */
    private fun appendAyat(next: List<Ayah>) {
        if (next.isEmpty()) return
        val current = _ayat.value
        val deduped = (current + next).distinctBy { it.ayah }
        _ayat.value = deduped
        currentOffset += deduped.size - current.size
    }

    suspend fun ensureAyahLoaded(targetAyah: Int) {
        // Keep loading pages until targetAyah is in list or no more
        while (_hasMore.value && _ayat.value.none { it.ayah == targetAyah }) {
            val result = runCatchingResult {
                repository.getAyatBySurahPaged(currentSurahId, pageSize, currentOffset)
            }
            val nextPage = (result as? Result.Success)?.data ?: emptyList()
            if (nextPage.isEmpty()) {
                _hasMore.value = false
                break
            }
            appendAyat(nextPage)
            val total = _surah.value?.ayahCount ?: Int.MAX_VALUE
            _hasMore.value = nextPage.size == pageSize && currentOffset < total
            // Small yield to not block UI
            delay(10)
        }
    }

    suspend fun ensurePageLoaded(targetPage: Int) {
        while (_hasMore.value && _ayat.value.none { it.pageNumber == targetPage }) {
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
                _hasMore.value = false
                _isLoadingMore.value = false
                break
            }
            appendAyat(nextPage)
            val total = _surah.value?.ayahCount ?: Int.MAX_VALUE
            _hasMore.value = nextPage.size == pageSize && currentOffset < total
            _isLoadingMore.value = false
            delay(10)
        }
    }

    fun updateLastRead(surahId: Int, ayahNum: Int) {
        if (surahId <= 0) return
        val key = surahId to ayahNum
        if (lastWrittenLastRead == key) return
        lastWrittenLastRead = key
        viewModelScope.launch { preferences.setLastRead(surahId, ayahNum) }
    }

    private fun selectWord(word: WordToken, ayah: Ayah) {
        _selectedWord.value = word
        _selectedWordAyah.value = ayah
        // Fetch AI summary via repository (UDF: ViewModel owns data, not Composable)
        _aiSummary.value = null
        _aiExtended.value = null
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
                        _aiExtended.value = detail?.aiExtended
                            ?.takeIf { ext -> ext.sections().any { it.isNotBlank() } }
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
        _aiExtended.value = null
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

/**
 * Extra ayat fetched past a deep-link target so the reader lands with a little
 * forward context instead of the target being the very last loaded row.
 */
private const val DEEP_LINK_RUNWAY = 10

/**
 * How many ayat the very first query must fetch for an entry at [targetAyah].
 *
 * A target inside the first page needs no special handling. A deeper target is
 * fetched **through** in one round-trip — [runway] ayat of forward context on
 * top — so the target block already exists in the first composition: no ayah-1
 * frame, no `ensureAyahLoaded` page loop, no animated scroll. Never exceeds the
 * surah, so "jump to the last ayah" degrades to loading the whole surah once.
 *
 * Top-level and pure so [SurahDeepLinkWindowTest] can pin the rule.
 */
internal fun initialAyatLimit(
    targetAyah: Int,
    pageSize: Int,
    totalAyah: Int,
    runway: Int = DEEP_LINK_RUNWAY
): Int = if (targetAyah > pageSize) {
    minOf(totalAyah, targetAyah + runway).coerceAtLeast(1)
} else {
    pageSize
}
