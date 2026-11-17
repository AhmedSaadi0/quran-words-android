package com.quranwords.ui.surah.detail

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dagger.hilt.android.lifecycle.HiltViewModel
import com.quranwords.core.util.AyahRef
import com.quranwords.core.util.AyahSelectionState
import com.quranwords.core.util.FontSizePreference
import com.quranwords.core.util.PillTransparencyPreference
import com.quranwords.core.util.QuranCopyFormatter
import com.quranwords.core.util.ReadingModePreference
import com.quranwords.core.util.Result
import com.quranwords.core.util.SurahMetadata
import com.quranwords.core.util.orEmptyIfError
import com.quranwords.core.util.runCatchingResult
import com.quranwords.domain.model.Ayah
import com.quranwords.domain.model.AiExtendedSummary
import com.quranwords.domain.model.Surah
import com.quranwords.domain.model.WordToken
import com.quranwords.domain.repository.QuranRepository
import com.quranwords.domain.repository.UserPreferencesRepository
import com.quranwords.ui.theme.QuranFont
import javax.inject.Inject
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

/** Route input (written only by navigation) and mutable user state are kept
 * in SEPARATE keys — rotation must not reset a toggled chrome choice. */
private const val KEY_INITIAL_IMMERSIVE = "initialImmersive"
private const val KEY_IMMERSIVE_STATE = "immersiveState"
private const val PAGE_CACHE_CAPACITY = 18
private const val PREFETCH_RADIUS = 2

/** Everything resolved during initial Load, emitted in one atomic update. */
private data class LoadedReader(
    val surah: Surah?,
    val ayat: List<Ayah>,
    val pageRangeAyat: List<Ayah>,
    val pageCount: Int,
    val entry: Int,
    val activeId: Int,
    val entryAyat: List<Ayah>
)

/** One atomic state object keeps the many reader slices in sync. */
private data class ReaderCoreState(
    val surah: Surah? = null,
    val ayat: List<Ayah> = emptyList(),
    val pageRangeAyat: List<Ayah> = emptyList(),
    val totalPages: Int = 0,
    val entryPage: Int = 0,
    val currentPage: Int = 0,
    val pagesByNumber: Map<Int, List<Ayah>> = emptyMap(),
    val isPagesReady: Boolean = false,
    val activeSurah: Surah? = null,
    val activeSurahId: Int = 0,
    val pageAyahs: List<Ayah> = emptyList(),
    val isImmersive: Boolean = false,
    val isLoading: Boolean = true,
    val selectedWord: WordToken? = null,
    val selectedWordAyah: Ayah? = null,
    val aiExtended: AiExtendedSummary? = null,
    val aiModel: String? = null,
    val aiGeneratedAt: String? = null,
    val pulseAyah: AyahRef? = null,
    val selection: AyahSelectionState = AyahSelectionState(),
    val surahPages: List<Int> = emptyList(),
    val error: String? = null
)

/** Bounded access-order cache: recently viewed pages survive rapid flipping. */
private class PageCache : LinkedHashMap<Int, List<Ayah>>(PAGE_CACHE_CAPACITY + 2, 0.75f, true) {
    override fun removeEldestEntry(eldest: MutableMap.MutableEntry<Int, List<Ayah>>?): Boolean =
        size > PAGE_CACHE_CAPACITY
}

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
    private val pageCache = PageCache()
    private var prefetchJob: Job? = null
    private var currentSurahId: Int = -1
    private var lastWrittenLastRead: Pair<Int, Int>? = null

    private val _core = MutableStateFlow(
        ReaderCoreState(isImmersive = savedStateHandle.get<Boolean>(KEY_IMMERSIVE_STATE) ?: initialImmersive)
    )

    private val fontState = MutableStateFlow(24f)
    private val pillTransparencyState = MutableStateFlow(PillTransparencyPreference.DEFAULT)
    private val pageModeState = MutableStateFlow(ReadingModePreference.DEFAULT)
    private val quranFontState = MutableStateFlow(QuranFont.KFGQPC_HAFS_1441)
    private val bookmarkAyahState = MutableStateFlow<Set<String>>(emptySet())
    private val dbReadyState = MutableStateFlow(repository.isDatabaseReady())

    val ayat: StateFlow<List<Ayah>> = _core.map { it.ayat }
        .stateIn(viewModelScope, SharingStarted.Eagerly, emptyList())
    val selection: StateFlow<AyahSelectionState> = _core.map { it.selection }
        .stateIn(viewModelScope, SharingStarted.Eagerly, AyahSelectionState())

    val uiState: StateFlow<SurahDetailUiState> = _core.map { state ->
        SurahDetailUiState(
            surah = state.surah,
            ayat = state.ayat,
            pageRangeAyat = state.pageRangeAyat,
            totalPages = state.totalPages,
            entryPage = state.entryPage,
            currentPage = state.currentPage,
            pagesByNumber = state.pagesByNumber,
            isPagesReady = state.isPagesReady,
            activeSurah = state.activeSurah,
            activeSurahId = state.activeSurahId,
            pageAyahs = state.pageAyahs,
            isImmersive = state.isImmersive,
            isLoading = state.isLoading,
            selectedWord = state.selectedWord,
            selectedWordAyah = state.selectedWordAyah,
            aiExtended = state.aiExtended,
            aiModel = state.aiModel,
            aiGeneratedAt = state.aiGeneratedAt,
            pulseAyah = state.pulseAyah,
            isSelectionMode = state.selection.isSelectionMode,
            selectedAyahs = state.selection.selected,
            surahPages = state.surahPages,
            error = state.error
        )
    }.stateIn(viewModelScope, SharingStarted.Eagerly, SurahDetailUiState())

    init {
        viewModelScope.launch { preferences.fontSize.collectLatest { fontState.value = it } }
        viewModelScope.launch { preferences.pillTransparency.collectLatest { pillTransparencyState.value = it } }
        viewModelScope.launch { preferences.readingMode.collectLatest { pageModeState.value = it } }
        viewModelScope.launch { preferences.quranFontKey.collectLatest { quranFontState.value = QuranFont.fromKey(it) } }
        viewModelScope.launch { preferences.bookmarkedAyat.collectLatest { bookmarkAyahState.value = it } }
    }

    val screenState: StateFlow<SurahDetailUiState> = combine(
        uiState, fontState, quranFontState, bookmarkAyahState, dbReadyState,
        pillTransparencyState, pageModeState
    ) { values ->
        @Suppress("UNCHECKED_CAST")
        (values[0] as SurahDetailUiState).copy(
            fontSize = values[1] as Float,
            quranFont = values[2] as QuranFont,
            bookmarkedAyat = values[3] as Set<String>,
            isDbReady = values[4] as Boolean,
            pillTransparency = values[5] as Float,
            readingMode = values[6] as Int
        )
    }.stateIn(viewModelScope, SharingStarted.Eagerly, SurahDetailUiState())

    fun onEvent(event: SurahDetailEvent) {
        when (event) {
            is SurahDetailEvent.Load -> loadSurah(event.surahId, event.targetAyah, event.immediate)
            is SurahDetailEvent.WordSelected -> selectWord(event.word, event.ayah)
            is SurahDetailEvent.CopyWord -> Unit // platform event: Route owns ShareHandler
            is SurahDetailEvent.SetImmersive -> {
                _core.update { it.copy(isImmersive = event.value) }
                // Separate key: never overwrite the route's initialImmersive.
                savedStateHandle[KEY_IMMERSIVE_STATE] = event.value
            }
            is SurahDetailEvent.SetPulseAyah -> _core.update { it.copy(pulseAyah = event.ref) }
            SurahDetailEvent.DismissWord -> clearSelectedWord()
            is SurahDetailEvent.AyahVisible -> updateLastRead(event.ref)
            is SurahDetailEvent.PageChanged -> onPageChanged(event.page)
            is SurahDetailEvent.SetEntryPage -> setEntryPage(event.page)
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
        viewModelScope.launch {
            selected.forEach { ref -> preferences.toggleAyahBookmark(ref.surahId, ref.ayah) }
        }
    }

    fun loadSurah(surahId: Int, targetAyah: Int = 1, immediate: Boolean = false) {
        if (currentSurahId == surahId && _core.value.surah?.id == surahId && _core.value.ayat.isNotEmpty()) return

        currentSurahId = surahId
        prefetchJob?.cancel()
        pageCache.clear()
        _core.update {
            it.copy(
                isLoading = true, ayat = emptyList(), pageRangeAyat = emptyList(),
                totalPages = 0, entryPage = 0, currentPage = 0, pagesByNumber = emptyMap(),
                isPagesReady = false, activeSurah = null, activeSurahId = 0, pageAyahs = emptyList(),
                pulseAyah = null, error = null, selection = AyahSelectionState()
            )
        }

        viewModelScope.launch {
            if (!immediate && targetAyah == 1) delay(200)
            val result = runCatchingResult {
                val surah = repository.getSurahById(surahId)
                // Scroll mode stays strictly surah-scoped.
                val allAyat = repository.getAyatBySurahFull(surahId)
                val maxPage = repository.getMaxPageNumber().coerceAtLeast(1)
                val target = allAyat.firstOrNull { it.ayah == targetAyah }
                val firstPage = allAyat.firstOrNull()?.pageNumber
                val lastPage = allAyat.lastOrNull()?.pageNumber
                val rangeAyat = if (firstPage != null && lastPage != null) {
                    repository.getAyatByPageRangeFull(firstPage, lastPage).orEmptyIfError()
                } else emptyList()

                val paged = target?.pageNumber != null || firstPage != null
                val (pageCount, entry, initialPages) = if (paged) {
                    val page = target?.pageNumber ?: firstPage ?: 1
                    val wanted = linkedMapOf<Int, List<Ayah>>()
                    // Guardrail 1: preload entry + neighbours BEFORE the ready state.
                    val ordered = listOf(page) + ((page - PREFETCH_RADIUS)..(page + PREFETCH_RADIUS))
                        .filter { it in 1..maxPage && it != page }
                    ordered.forEach { p ->
                        val list = repository.getAyatOfPageFull(p).orEmptyIfError()
                        if (list.isNotEmpty()) wanted[p] = list
                    }
                    Triple(maxPage, page, wanted)
                } else {
                    // Unpaged legacy corpus: synthetic pages of 10 ayat.
                    val chunks = allAyat.chunked(10)
                    val targetIndex = chunks.indexOfFirst { chunk -> chunk.any { it.ayah == targetAyah } }
                    Triple(chunks.size, (if (targetIndex >= 0) targetIndex else 0) + 1,
                        chunks.mapIndexed { index, chunk -> index + 1 to chunk }.toMap())
                }
                pageCache.putAll(initialPages)
                val entryAyat = initialPages[entry].orEmpty()
                val activeId = entryAyat.firstOrNull()?.surah ?: surahId
                LoadedReader(surah, allAyat, rangeAyat, pageCount, entry, activeId, entryAyat)
            }
            when (result) {
                is Result.Success -> {
                    val (surah, allAyat, rangeAyat, pageCount, entry, activeId, entryAyat) = result.data
                    _core.update {
                        it.copy(
                            surah = surah,
                            ayat = allAyat,
                            pageRangeAyat = rangeAyat,
                            totalPages = pageCount,
                            entryPage = entry,
                            currentPage = entry,
                            pagesByNumber = LinkedHashMap(pageCache),
                            isPagesReady = pageCache.containsKey(entry),
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
                is Result.Error -> _core.update { it.copy(surahPages = emptyList()) }
            }
        }
    }

    /** Called when the unified pager settles on a real mushaf page. */
    fun onPageChanged(page: Int) {
        if (page < 1) return
        val ayat = pageCache[page].orEmpty()
        val activeId = ayat.firstOrNull()?.surah ?: _core.value.activeSurahId
        _core.update {
            it.copy(
                currentPage = page,
                pageAyahs = ayat,
                activeSurahId = activeId,
                activeSurah = if (activeId > 0) surahFor(activeId, it.surah) else it.activeSurah
            )
        }
        prefetchJob?.cancel()
        prefetchJob = viewModelScope.launch {
            val total = _core.value.totalPages
            val wanted = (page - PREFETCH_RADIUS..page + PREFETCH_RADIUS)
                .filter { it in 1..total && !pageCache.containsKey(it) }
            var changed = false
            for (p in wanted) {
                val result = runCatchingResult { repository.getAyatOfPageFull(p).orEmptyIfError() }
                if (result is Result.Success && result.data.isNotEmpty()) {
                    pageCache[p] = result.data
                    changed = true
                }
            }
            if (changed) _core.update { it.copy(pagesByNumber = LinkedHashMap(pageCache)) }
        }
    }

    /** Mode flip from scroll → pages: re-anchor and re-preload that page before
     * the pager mounts, so it starts on the reader's current position. */
    fun setEntryPage(page: Int) {
        if (page < 1) return
        _core.update { it.copy(isPagesReady = false, entryPage = page, currentPage = page) }
        viewModelScope.launch {
            if (!pageCache.containsKey(page)) {
                val loaded = runCatchingResult { repository.getAyatOfPageFull(page).orEmptyIfError() }
                if (loaded is Result.Success && loaded.data.isNotEmpty()) pageCache[page] = loaded.data
            }
            val ayat = pageCache[page].orEmpty()
            val activeId = ayat.firstOrNull()?.surah ?: _core.value.activeSurahId
            _core.update {
                it.copy(
                    pagesByNumber = LinkedHashMap(pageCache),
                    pageAyahs = ayat,
                    isPagesReady = pageCache.containsKey(page),
                    activeSurahId = activeId,
                    activeSurah = if (activeId > 0) surahFor(activeId, it.surah) else it.activeSurah
                )
            }
        }
    }

    private fun surahFor(id: Int, loaded: Surah?): Surah? {
        if (id <= 0) return null
        if (loaded?.id == id) return loaded
        val meta = SurahMetadata.SURAHS.getOrNull(id - 1) ?: return null
        return Surah(meta.id, meta.nameAr, meta.nameEn, meta.ayahCount, meta.revelationType, meta.juzStart)
    }

    private fun findAyah(ref: AyahRef): Ayah? {
        val state = _core.value
        fun List<Ayah>.find() = firstOrNull { it.surah == ref.surahId && it.ayah == ref.ayah }
        return state.pageAyahs.find()
            ?: state.pagesByNumber.values.firstNotNullOfOrNull { it.find() }
            ?: state.ayat.find()
            ?: state.pageRangeAyat.find()
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

    /** Formats selected ayat grouped by surah, each with its own Surah header. */
    fun getFormattedSelection(): String {
        val selected = _core.value.selection.selected
        if (selected.isEmpty()) return ""
        return selected.groupBy { it.surahId }.mapNotNull { (surahId, refs) ->
            val ayat = refs.mapNotNull { findAyah(it) }.sortedBy { it.ayah }
            if (ayat.isEmpty()) null else QuranCopyFormatter.formatMultiple(ayat, surahFor(surahId, _core.value.surah))
        }.joinToString("\n\n")
    }

    fun getFormattedSingle(ayah: Ayah): String =
        QuranCopyFormatter.formatSingle(ayah, surahFor(ayah.surah, _core.value.surah))
}
