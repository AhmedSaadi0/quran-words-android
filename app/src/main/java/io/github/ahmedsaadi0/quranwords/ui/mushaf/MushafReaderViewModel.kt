package io.github.ahmedsaadi0.quranwords.ui.mushaf

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dagger.hilt.android.lifecycle.HiltViewModel
import io.github.ahmedsaadi0.quranwords.core.util.MushafConstants
import io.github.ahmedsaadi0.quranwords.core.util.Result
import io.github.ahmedsaadi0.quranwords.core.util.SurahMetadata
import io.github.ahmedsaadi0.quranwords.core.util.runCatchingResult
import io.github.ahmedsaadi0.quranwords.domain.model.Ayah
import io.github.ahmedsaadi0.quranwords.domain.model.WordToken
import io.github.ahmedsaadi0.quranwords.domain.repository.QuranRepository
import io.github.ahmedsaadi0.quranwords.domain.repository.UserPreferencesRepository
import io.github.ahmedsaadi0.quranwords.ui.mushaf.model.MushafPageUi
import io.github.ahmedsaadi0.quranwords.ui.mushaf.model.buildMushafPage
import javax.inject.Inject
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

/**
 * Mushaf page reader state owner. Keeps a sliding window of loaded pages
 * ([PRELOAD_RADIUS] eagerly, evicted beyond [CACHE_RADIUS]) around the
 * pager's settled position. The pager is the position authority; this VM owns
 * data, immersive bars state, last-read and the morphology sheet.
 */
@HiltViewModel
class MushafReaderViewModel @Inject constructor(
    private val repository: QuranRepository,
    private val preferences: UserPreferencesRepository
) : ViewModel() {

    companion object {
        const val PRELOAD_RADIUS = 2
        const val CACHE_RADIUS = 6
    }

    private val _pages = MutableStateFlow<Map<Int, MushafPageUi>>(emptyMap())
    private val _loadingPages = MutableStateFlow<Set<Int>>(emptySet())
    private val _currentPage = MutableStateFlow(1)
    private val _initialPage = MutableStateFlow<Int?>(null)
    private val _isInitialLoad = MutableStateFlow(true)
    private val _isImmersive = MutableStateFlow(false)

    private val _selectedWord = MutableStateFlow<WordToken?>(null)
    private val _selectedWordAyah = MutableStateFlow<Ayah?>(null)
    private val _aiSummary = MutableStateFlow<String?>(null)
    private val _aiModel = MutableStateFlow<String?>(null)
    private val _aiGeneratedAt = MutableStateFlow<String?>(null)
    private val _isAiLoading = MutableStateFlow(false)

    private val _totalPages = MutableStateFlow(MushafConstants.TOTAL_PAGES_FALLBACK)
    private val _error = MutableStateFlow<String?>(null)
    private val _dbReady = MutableStateFlow(repository.isDatabaseReady())

    private var opened = false

    val uiState: StateFlow<MushafReaderUiState> = combine(
        _pages,
        _loadingPages,
        _currentPage,
        _initialPage,
        _isInitialLoad,
        _isImmersive,
        _selectedWord,
        _selectedWordAyah,
        _aiSummary,
        _aiModel,
        _aiGeneratedAt,
        _isAiLoading,
        _totalPages,
        _error,
        _dbReady
    ) { args ->
        @Suppress("UNCHECKED_CAST")
        MushafReaderUiState(
            pages = args[0] as Map<Int, MushafPageUi>,
            loadingPages = args[1] as Set<Int>,
            currentPage = args[2] as Int,
            initialPage = args[3] as Int?,
            isInitialLoad = args[4] as Boolean,
            isImmersive = args[5] as Boolean,
            selectedWord = args[6] as WordToken?,
            selectedWordAyah = args[7] as Ayah?,
            aiSummary = args[8] as String?,
            aiModel = args[9] as String?,
            aiGeneratedAt = args[10] as String?,
            isAiLoading = args[11] as Boolean,
            totalPages = args[12] as Int,
            error = args[13] as String?,
            isDbReady = args[14] as Boolean
        )
    }.combine(preferences.bookmarkedAyat) { state, bookmarks ->
        state.copy(bookmarkedAyat = bookmarks)
    }.stateIn(viewModelScope, SharingStarted.Eagerly, MushafReaderUiState())

    fun onEvent(event: MushafReaderEvent) {
        when (event) {
            is MushafReaderEvent.OpenAtAyah -> openAtAyah(event.surahId, event.ayah)
            is MushafReaderEvent.OpenAtPage -> openAtPage(event.page)
            is MushafReaderEvent.PageSettled -> onPageSettled(event.page)
            MushafReaderEvent.PageTapped -> _isImmersive.update { !it }
            is MushafReaderEvent.WordTapped -> selectWord(event.wordAyahId, event.position)
            is MushafReaderEvent.MarkerTapped ->
                viewModelScope.launch { preferences.toggleAyahBookmark(event.surahId, event.ayah) }
            MushafReaderEvent.DismissWord -> clearSelectedWord()
        }
    }

    /** Cold open at a known page. No-op on repeat calls (back-nav safe). */
    private fun openAtPage(page: Int) {
        if (opened) return
        opened = true
        viewModelScope.launch {
            val total = loadTotalPages()
            val target = page.coerceIn(1, total)
            _currentPage.value = target
            _initialPage.value = target
            ensureWindow(target)
            _isInitialLoad.value = false
        }
    }

    /** Cold open at (surah, ayah): resolves its Mushaf page, then opens there. */
    private fun openAtAyah(surahId: Int, ayah: Int) {
        if (opened) return
        opened = true
        viewModelScope.launch {
            val total = loadTotalPages()
            val resolved = runCatchingResult { repository.getAyahWithWords(surahId, ayah) }
            val target = (resolved as? Result.Success)?.data?.pageNumber?.coerceIn(1, total)
            if (target == null) {
                _error.value = (resolved as? Result.Error)?.message ?: "Ayah not found"
                _initialPage.value = 1
                _isInitialLoad.value = false
                return@launch
            }
            _currentPage.value = target
            _initialPage.value = target
            ensureWindow(target)
            _isInitialLoad.value = false
        }
    }

    private fun onPageSettled(page: Int) {
        // Ignore pager positions before the cold-open target resolves; otherwise
        // the initial settled page (1) would briefly overwrite last-read.
        if (_initialPage.value == null) return
        val total = _totalPages.value
        if (page !in 1..total) return
        _currentPage.value = page
        ensureWindow(page)
        _pages.value[page]?.ayat?.firstOrNull()?.let { first ->
            viewModelScope.launch { preferences.setLastRead(first.surah, first.ayah) }
        }
    }

    private suspend fun loadTotalPages(): Int {
        val result = runCatchingResult { repository.getMushafPageCount() }
        val total = (result as? Result.Success)?.data?.takeIf { it > 0 }
            ?: MushafConstants.TOTAL_PAGES_FALLBACK
        _totalPages.value = total
        return total
    }

    private fun ensureWindow(center: Int) {
        val total = _totalPages.value
        val wanted = ((center - PRELOAD_RADIUS)..(center + PRELOAD_RADIUS))
            .filter { it in 1..total }
        _pages.update { current ->
            current.filterKeys { kotlin.math.abs(it - center) <= CACHE_RADIUS }
        }
        wanted
            .filter { it !in _pages.value && it !in _loadingPages.value }
            .forEach { loadPage(it) }
    }

    private fun loadPage(page: Int) {
        _loadingPages.update { it + page }
        viewModelScope.launch {
            when (val result = runCatchingResult { repository.getAyatByPage(page) }) {
                is Result.Success -> {
                    val pageUi = buildMushafPage(page, result.data) { id ->
                        SurahMetadata.SURAHS.firstOrNull { it.id == id }
                    }
                    _pages.update { it + (page to pageUi) }
                }
                is Result.Error -> _error.value = result.message
            }
            _loadingPages.update { it - page }
        }
    }

    private fun selectWord(wordAyahId: Int, position: Int) {
        var foundWord: WordToken? = null
        var foundAyah: Ayah? = null
        for (pageUi in _pages.value.values) {
            for (ayah in pageUi.ayat) {
                val word = ayah.words.firstOrNull { it.wordAyahId == wordAyahId && it.position == position }
                if (word != null) {
                    foundWord = word
                    foundAyah = ayah
                    break
                }
            }
            if (foundWord != null) break
        }
        if (foundWord == null) return
        _selectedWord.value = foundWord
        _selectedWordAyah.value = foundAyah
        _aiSummary.value = null
        _aiModel.value = null
        _aiGeneratedAt.value = null
        val rootId = foundWord.rootId
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

    private fun clearSelectedWord() {
        _selectedWord.value = null
        _selectedWordAyah.value = null
        _aiSummary.value = null
        _aiModel.value = null
        _aiGeneratedAt.value = null
        _isAiLoading.value = false
    }
}
