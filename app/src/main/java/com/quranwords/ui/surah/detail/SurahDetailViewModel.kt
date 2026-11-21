package com.quranwords.ui.surah.detail

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dagger.hilt.android.lifecycle.HiltViewModel
import com.quranwords.core.util.AyahRef
import com.quranwords.core.util.FontSizePreference
import com.quranwords.core.util.PillTransparencyPreference
import com.quranwords.core.util.ReadingModePreference
import com.quranwords.core.util.Result
import com.quranwords.core.util.orEmptyIfError
import com.quranwords.core.util.runCatchingResult
import com.quranwords.domain.model.Ayah
import com.quranwords.domain.model.WordToken
import com.quranwords.domain.repository.QuranRepository
import com.quranwords.domain.repository.UserPreferencesRepository
import com.quranwords.ui.theme.QuranFont
import javax.inject.Inject
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

/**
 * Owns the surah reader: vertical mode is surah-scoped, Pages mode paginates
 * real mushaf pages 1..N with a bounded lazy window. Selection/bookmarks are
 * keyed by [AyahRef] so surahs sharing a page never collide.
 */
@HiltViewModel
class SurahDetailViewModel @Inject constructor(
    private val repository: QuranRepository,
    private val preferences: UserPreferencesRepository,
    private val savedStateHandle: SavedStateHandle = SavedStateHandle()
) : ViewModel() {

    private val initialImmersive = savedStateHandle.get<Boolean>(KEY_INITIAL_IMMERSIVE) ?: false
    private val pageCache = MushafPageCache()
    private var currentSurahId: Int = -1
    private var lastWrittenLastRead: Pair<Int, Int>? = null

    private val _core = MutableStateFlow(
        ReaderCoreState(
            isImmersive = savedStateHandle.get<Boolean>(KEY_IMMERSIVE_STATE) ?: initialImmersive,
            fontSheetVisible = savedStateHandle.get<Boolean>(KEY_FONT_SHEET_VISIBLE) ?: false,
            pendingPage = savedStateHandle.get<Int>(KEY_PENDING_PAGE)
        )
    )
    private val pageLoader = SurahReaderPageLoader(
        repository, viewModelScope, pageCache, { _core.value }, { transform -> _core.update(transform) }
    )
    private val _effect = MutableSharedFlow<SurahDetailEffect>(extraBufferCapacity = 8)
    val effect: SharedFlow<SurahDetailEffect> = _effect

    private val fontState = MutableStateFlow(24f)
    private val pillTransparencyState = MutableStateFlow(PillTransparencyPreference.DEFAULT)
    private val pageModeState = MutableStateFlow(ReadingModePreference.DEFAULT)
    private val quranFontState = MutableStateFlow(QuranFont.KFGQPC_HAFS_1441)
    private val bookmarkAyahState = MutableStateFlow<Set<String>>(emptySet())
    private val dbReadyState = MutableStateFlow(repository.isDatabaseReady())

    /** Single authoritative state for the reader (P2-1/P2-2). */
    val uiState: StateFlow<SurahDetailUiState> = run {
        val prefsFlow = combine(fontState, quranFontState, bookmarkAyahState) { size, font, bookmarks ->
            ReaderPrefsA(size, font, bookmarks)
        }
        combine(prefsFlow, dbReadyState, pillTransparencyState, pageModeState, _core) {
                prefs, dbReady, pill, mode, state ->
            SurahDetailUiState(
                surah = state.surah,
                ayat = state.ayat,
                totalPages = state.totalPages,
                entryPage = state.entryPage,
                currentPage = state.currentPage,
                pagesByNumber = state.pagesByNumber,
                failedPages = state.failedPages,
                isPagesReady = state.isPagesReady,
                activeSurah = state.activeSurah,
                activeSurahId = state.activeSurahId,
                pageAyahs = state.pageAyahs,
                isImmersive = state.isImmersive,
                isLoading = state.isLoading,
                selectedWord = state.selectedWord,
                selectedWordAyah = state.selectedWordAyah,
                wordAction = state.wordAction,
                pendingPage = state.pendingPage,
                fontSheetVisible = state.fontSheetVisible,
                aiExtended = state.aiExtended,
                aiModel = state.aiModel,
                aiGeneratedAt = state.aiGeneratedAt,
                pulseAyah = state.pulseAyah,
                isSelectionMode = state.selection.isSelectionMode,
                selectedAyahs = state.selection.selected,
                surahPages = state.surahPages,
                error = state.error,
                fontSize = prefs.fontSize,
                quranFont = prefs.quranFont,
                bookmarkedAyat = prefs.bookmarks,
                isDbReady = dbReady,
                pillTransparency = pill,
                readingMode = mode
            )
        }
    }.stateIn(viewModelScope, SharingStarted.Eagerly, SurahDetailUiState())

    init {
        viewModelScope.launch { preferences.fontSize.collectLatest { fontState.value = it } }
        viewModelScope.launch { preferences.pillTransparency.collectLatest { pillTransparencyState.value = it } }
        viewModelScope.launch { preferences.readingMode.collectLatest { pageModeState.value = it } }
        viewModelScope.launch { preferences.quranFontKey.collectLatest { quranFontState.value = QuranFont.fromKey(it) } }
        viewModelScope.launch { preferences.bookmarkedAyat.collectLatest { bookmarkAyahState.value = it } }
    }
    fun onEvent(event: SurahDetailEvent) {
        when (event) {
            is SurahDetailEvent.Load -> loadSurah(event.surahId, event.targetAyah, event.immediate)
            is SurahDetailEvent.WordSelected -> selectWord(event.word, event.ayah)
            is SurahDetailEvent.CopyWord -> Unit // platform event: Route owns ShareHandler
            is SurahDetailEvent.SetImmersive -> {
                _core.update { it.copy(isImmersive = event.value) }
                savedStateHandle[KEY_IMMERSIVE_STATE] = event.value
            }
            is SurahDetailEvent.SetPulseAyah -> _core.update { it.copy(pulseAyah = event.ref) }
            is SurahDetailEvent.SetWordAction -> _core.update { it.copy(wordAction = event.target) }
            is SurahDetailEvent.SetPendingPage -> {
                _core.update { it.copy(pendingPage = event.page) }
                if (event.page == null) savedStateHandle.remove<Int>(KEY_PENDING_PAGE)
                else savedStateHandle[KEY_PENDING_PAGE] = event.page
            }
            is SurahDetailEvent.SetFontSheetVisible -> {
                _core.update { it.copy(fontSheetVisible = event.visible) }
                savedStateHandle[KEY_FONT_SHEET_VISIBLE] = event.visible
            }
            SurahDetailEvent.DismissWord -> clearSelectedWord()
            is SurahDetailEvent.AyahVisible -> updateLastRead(event.ref)
            is SurahDetailEvent.PageChanged -> pageLoader.onPageChanged(event.page)
            is SurahDetailEvent.SetEntryPage -> pageLoader.setEntryPage(event.page)
            is SurahDetailEvent.RetryPage -> pageLoader.retryPage(event.page)
            is SurahDetailEvent.ToggleAyahBookmark ->
                viewModelScope.launch { preferences.toggleAyahBookmark(event.ref.surahId, event.ref.ayah) }
            is SurahDetailEvent.SetFontSize ->
                viewModelScope.launch { preferences.setFontSize(FontSizePreference.clamp(event.size)) }
            is SurahDetailEvent.SetPillTransparency ->
                viewModelScope.launch {
                    preferences.setPillTransparency(PillTransparencyPreference.clamp(event.transparency))
                }
            is SurahDetailEvent.SetReadingMode ->
                viewModelScope.launch { preferences.setReadingMode(ReadingModePreference.fromValue(event.mode)) }
            is SurahDetailEvent.EnterSelection -> _core.update { it.copy(selection = it.selection.enter(event.ref)) }
            is SurahDetailEvent.ToggleAyahSelection -> _core.update { it.copy(selection = it.selection.toggle(event.ref)) }
            is SurahDetailEvent.RangeSelect -> _core.update { it.copy(selection = it.selection.rangeTo(event.ref)) }
            SurahDetailEvent.ClearSelection -> _core.update { it.copy(selection = it.selection.clear()) }
            SurahDetailEvent.BookmarkSelection -> bookmarkSelection()
            SurahDetailEvent.CopySelection, SurahDetailEvent.ShareSelection -> Unit
        }
    }

    private fun bookmarkSelection() {
        val selected = _core.value.selection.selected
        if (selected.isEmpty()) return
        _core.update { it.copy(selection = it.selection.clear()) }
        val keys = selected.map { "${it.surahId}:${it.ayah}" }.toSet()
        viewModelScope.launch {
            when (val result = runCatchingResult { preferences.setAyahBookmarked(keys, true) }) {
                is Result.Success -> _effect.tryEmit(SurahDetailEffect.BookmarkSelectionSucceeded(selected.size))
                is Result.Error -> {
                    _core.update { it.copy(error = result.message) }
                    _effect.tryEmit(SurahDetailEffect.BookmarkSelectionFailed(result.message))
                }
            }
        }
    }
    fun loadSurah(surahId: Int, targetAyah: Int = 1, immediate: Boolean = false) {
        if (currentSurahId == surahId && _core.value.surah?.id == surahId && _core.value.ayat.isNotEmpty()) return

        currentSurahId = surahId
        pageLoader.reset()
        _core.update {
            it.copy(
                isLoading = true, ayat = emptyList(),
                totalPages = 0, entryPage = 0, currentPage = 0, pagesByNumber = emptyMap(),
                failedPages = emptySet(),
                isPagesReady = false, activeSurah = null, activeSurahId = 0, pageAyahs = emptyList(),
                pulseAyah = null, error = null, selection = com.quranwords.core.util.AyahSelectionState()
            )
        }

        viewModelScope.launch {
            if (!immediate && targetAyah == 1) delay(200)
            val result = runCatchingResult {
                val surah = repository.getSurahById(surahId)
                val allAyat = repository.getAyatBySurahFull(surahId)
                val maxPage = repository.getMaxPageNumber().coerceAtLeast(1)
                val target = allAyat.firstOrNull { it.ayah == targetAyah }
                val firstPage = allAyat.firstOrNull()?.pageNumber

                val paged = target?.pageNumber != null || firstPage != null
                val (pageCount, entry, initialPages) = if (paged) {
                    val page = target?.pageNumber ?: firstPage ?: 1
                    val wanted = linkedMapOf<Int, List<Ayah>>()
                    val ordered = listOf(page) + ((page - PREFETCH_RADIUS)..(page + PREFETCH_RADIUS))
                        .filter { it in 1..maxPage && it != page }
                    ordered.forEach { p ->
                        val list = repository.getAyatOfPageFull(p).orEmptyIfError()
                        if (list.isNotEmpty()) wanted[p] = list
                    }
                    Triple(maxPage, page, wanted)
                } else {
                    val chunks = allAyat.chunked(10)
                    val targetIndex = chunks.indexOfFirst { chunk -> chunk.any { it.ayah == targetAyah } }
                    Triple(chunks.size, (if (targetIndex >= 0) targetIndex else 0) + 1,
                        chunks.mapIndexed { index, chunk -> index + 1 to chunk }.toMap())
                }
                pageLoader.prime(initialPages, entry)
                val activeId = pageLoader[entry].orEmpty().firstOrNull()?.surah ?: surahId
                LoadedReader(surah, allAyat, pageCount, entry, activeId, pageLoader[entry].orEmpty())
            }
            when (result) {
                is Result.Success -> {
                    val (surah, allAyat, pageCount, entry, activeId, entryAyat) = result.data
                    _core.update {
                        it.copy(
                            surah = surah,
                            ayat = allAyat,
                            totalPages = pageCount,
                            entryPage = entry,
                            currentPage = entry,
                            pagesByNumber = pageLoader.snapshot(),
                            isPagesReady = pageLoader.has(entry),
                            activeSurah = surahFor(activeId, surah),
                            activeSurahId = activeId,
                            pageAyahs = entryAyat,
                            isLoading = false
                        )
                    }
                }
                is Result.Error -> _core.update { it.copy(isLoading = false, error = result.message) }
            }
        }

        viewModelScope.launch {
            when (val result = runCatchingResult { repository.getPagesForSurah(surahId) }) {
                is Result.Success -> _core.update { it.copy(surahPages = result.data) }
                is Result.Error -> _core.update { state ->
                    if (state.error == null) state.copy(error = result.message) else state
                }
            }
        }
    }
    fun updateLastRead(ref: AyahRef) {
        val key = ref.surahId to ref.ayah
        if (ref.surahId <= 0 || lastWrittenLastRead == key) return
        lastWrittenLastRead = key
        viewModelScope.launch { preferences.setLastRead(ref.surahId, ref.ayah) }
    }

    private fun selectWord(word: WordToken, ayah: Ayah) {
        _core.update {
            it.copy(selectedWord = word, selectedWordAyah = ayah, aiExtended = null, aiModel = null, aiGeneratedAt = null)
        }
        val rootId = word.rootId
        if (rootId != null && rootId > 0) {
            viewModelScope.launch {
                when (val result = runCatchingResult { repository.getRootDetail(rootId) }) {
                    is Result.Success -> {
                        val detail = result.data
                        _core.update {
                            it.copy(
                                aiExtended = detail?.aiExtended?.takeIf { ext -> ext.sections().any { it.isNotBlank() } },
                                aiModel = detail?.aiModel,
                                aiGeneratedAt = detail?.aiGeneratedAt
                            )
                        }
                    }
                    is Result.Error -> Unit
                }
            }
        }
    }

    fun clearSelectedWord() {
        _core.update {
            it.copy(selectedWord = null, selectedWordAyah = null, aiExtended = null, aiModel = null, aiGeneratedAt = null)
        }
    }

    fun getFormattedSelection(): Result<String> = formatSelection(_core.value)

    fun getFormattedSingle(ayah: Ayah): String = formatSingle(ayah, _core.value.surah)
}
