package com.quranwords.ui.surah.detail

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dagger.hilt.android.lifecycle.HiltViewModel
import com.quranwords.core.util.FontSizePreference
import com.quranwords.core.util.QuranCopyFormatter
import com.quranwords.core.util.Result
import com.quranwords.core.util.SelectionState
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
        _selectedWord,
        _selectedWordAyah,
        _aiSummary,
        _aiExtended,
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
            selectedWord = values[3] as WordToken?,
            selectedWordAyah = values[4] as Ayah?,
            aiSummary = values[5] as String?,
            aiExtended = values[6] as AiExtendedSummary?,
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
     * Extends the selection from the anchor to [ayah] (union). The whole surah
     * is always loaded, so both ends render immediately — no paging behind.
     */
    private fun rangeSelect(ayah: Int) {
        _selection.update { it.rangeTo(ayah) }
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
     * Loads the whole surah in one shot. Surah text is finite static data (286
     * ayat at most), so there is no paging state to maintain, no offset to
     * track, and no page loop that can deadlock: either the full list lands or
     * the screen shows its failure branch.
     *
     * @param targetAyah ayah being entered at; kept so the screen can place the
     *   list at it the moment the data lands.
     * @param immediate true for adjacent-surah switches: the vertical slide is
     * already running, so content must land inside the animation instead of
     * after the enter-animation warm-up delay.
     */
    fun loadSurah(surahId: Int, targetAyah: Int = 1, immediate: Boolean = false) {
        // Guard: if the surah is already loaded with ayat, keep state
        if (currentSurahId == surahId && _surah.value?.id == surahId && _ayat.value.isNotEmpty()) {
            return
        }

        currentSurahId = surahId
        _isLoading.value = true
        _ayat.value = emptyList()
        _error.value = null

        viewModelScope.launch {
            // Warm-up only when the entry content IS the first page. A deep link
            // to a mid-surah ayah must query at 0ms, not after the transition.
            if (!immediate && targetAyah == 1) delay(200)
            val result = runCatchingResult {
                val surah = repository.getSurahById(surahId)
                surah to repository.getAyatBySurahFull(surahId)
            }
            when (result) {
                is Result.Success -> {
                    val (surah, allAyat) = result.data
                    _surah.value = surah
                    _ayat.value = allAyat
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
