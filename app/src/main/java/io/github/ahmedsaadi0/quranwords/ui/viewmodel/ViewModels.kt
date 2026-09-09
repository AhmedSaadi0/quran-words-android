package io.github.ahmedsaadi0.quranwords.ui.viewmodel

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dagger.hilt.android.lifecycle.HiltViewModel
import io.github.ahmedsaadi0.quranwords.core.util.AppLanguage
import io.github.ahmedsaadi0.quranwords.core.util.LanguageManager
import io.github.ahmedsaadi0.quranwords.data.remote.DatabaseDownloadManager
import io.github.ahmedsaadi0.quranwords.data.remote.DownloadState
import io.github.ahmedsaadi0.quranwords.data.repository.UserPreferencesRepository
import io.github.ahmedsaadi0.quranwords.domain.model.Ayah
import io.github.ahmedsaadi0.quranwords.domain.model.RootDetail
import io.github.ahmedsaadi0.quranwords.domain.model.RootItem
import io.github.ahmedsaadi0.quranwords.domain.model.SearchResult
import io.github.ahmedsaadi0.quranwords.domain.model.Surah
import io.github.ahmedsaadi0.quranwords.domain.model.WordToken
import io.github.ahmedsaadi0.quranwords.domain.repository.QuranRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class MainViewModel @Inject constructor(
    private val repository: QuranRepository,
    private val preferences: UserPreferencesRepository,
    private val languageManager: LanguageManager,
    val downloadManager: DatabaseDownloadManager
) : ViewModel() {

    private val _isDbReady = MutableStateFlow(repository.isDatabaseReady())
    val isDbReady: StateFlow<Boolean> = _isDbReady.asStateFlow()

    private val _fontSize = MutableStateFlow(24f)
    val fontSize: StateFlow<Float> = _fontSize.asStateFlow()

    private val _darkModeSetting = MutableStateFlow(0)
    val darkModeSetting: StateFlow<Int> = _darkModeSetting.asStateFlow()

    private val _dynamicColorEnabled = MutableStateFlow(false)
    val dynamicColorEnabled: StateFlow<Boolean> = _dynamicColorEnabled.asStateFlow()

    private val _colorMode = MutableStateFlow(0)
    val colorMode: StateFlow<Int> = _colorMode.asStateFlow()

    private val _bookmarkedSurahs = MutableStateFlow<Set<String>>(emptySet())
    val bookmarkedSurahs: StateFlow<Set<String>> = _bookmarkedSurahs.asStateFlow()

    private val _bookmarkedAyat = MutableStateFlow<Set<String>>(emptySet())
    val bookmarkedAyat: StateFlow<Set<String>> = _bookmarkedAyat.asStateFlow()

    private val _lastReadSurah = MutableStateFlow(1)
    val lastReadSurah: StateFlow<Int> = _lastReadSurah.asStateFlow()

    private val _lastReadAyah = MutableStateFlow(1)
    val lastReadAyah: StateFlow<Int> = _lastReadAyah.asStateFlow()

    private val _language = MutableStateFlow(AppLanguage.SYSTEM)
    val language: StateFlow<String> = _language.asStateFlow()

    init {
        viewModelScope.launch {
            preferences.fontSize.collectLatest { _fontSize.value = it }
        }
        viewModelScope.launch {
            preferences.darkModeSetting.collectLatest { _darkModeSetting.value = it }
        }
        viewModelScope.launch {
            preferences.dynamicColorEnabled.collectLatest { _dynamicColorEnabled.value = it }
        }
        viewModelScope.launch {
            preferences.colorMode.collectLatest { _colorMode.value = it }
        }
        viewModelScope.launch {
            preferences.bookmarkedSurahs.collectLatest { _bookmarkedSurahs.value = it }
        }
        viewModelScope.launch {
            preferences.bookmarkedAyat.collectLatest { _bookmarkedAyat.value = it }
        }
        viewModelScope.launch {
            preferences.lastReadSurah.collectLatest { _lastReadSurah.value = it }
        }
        viewModelScope.launch {
            preferences.lastReadAyah.collectLatest { _lastReadAyah.value = it }
        }
        viewModelScope.launch {
            preferences.language.collectLatest {
                _language.value = it
                languageManager.apply(it)
            }
        }
    }

    fun refreshDbStatus() {
        _isDbReady.value = repository.isDatabaseReady()
    }

    fun setFontSize(size: Float) {
        viewModelScope.launch { preferences.setFontSize(size.coerceIn(1f, 48f)) }
    }

    fun toggleDarkMode() {
        viewModelScope.launch {
            val next = when (_darkModeSetting.value) {
                1 -> 2 // from light to dark
                2 -> 1 // from dark to light
                else -> 2 // from system to dark
            }
            preferences.setDarkModeSetting(next)
        }
    }

    fun setDarkModeSetting(mode: Int) {
        viewModelScope.launch { preferences.setDarkModeSetting(mode) }
    }

    fun setLanguage(tag: String) {
        viewModelScope.launch { preferences.setLanguage(tag) }
    }

    fun setColorMode(mode: Int) {
        viewModelScope.launch { preferences.setColorMode(mode) }
    }

    fun toggleDynamicColor() {
        viewModelScope.launch { preferences.setDynamicColorEnabled(!_dynamicColorEnabled.value) }
    }

    fun setDynamicColorEnabled(enabled: Boolean) {
        viewModelScope.launch { preferences.setDynamicColorEnabled(enabled) }
    }

    fun toggleSurahBookmark(surahId: Int) {
        viewModelScope.launch { preferences.toggleSurahBookmark(surahId) }
    }

    fun toggleAyahBookmark(surahId: Int, ayahNum: Int) {
        viewModelScope.launch { preferences.toggleAyahBookmark(surahId, ayahNum) }
    }

    fun updateLastRead(surahId: Int, ayahNum: Int) {
        viewModelScope.launch { preferences.setLastRead(surahId, ayahNum) }
    }

    fun isSurahBookmarked(surahId: Int): Boolean = _bookmarkedSurahs.value.contains(surahId.toString())
    fun isAyahBookmarked(surahId: Int, ayahNum: Int): Boolean = _bookmarkedAyat.value.contains("$surahId:$ayahNum")
}

@HiltViewModel
class HomeViewModel @Inject constructor(
    private val repository: QuranRepository,
    private val preferences: UserPreferencesRepository
) : ViewModel() {
    private val _featuredRoots = MutableStateFlow<List<RootItem>>(emptyList())
    val featuredRoots: StateFlow<List<RootItem>> = _featuredRoots.asStateFlow()

    private val _lastReadSurah = MutableStateFlow(1)
    val lastReadSurah: StateFlow<Int> = _lastReadSurah.asStateFlow()

    private val _lastReadAyah = MutableStateFlow(1)
    val lastReadAyah: StateFlow<Int> = _lastReadAyah.asStateFlow()

    init {
        loadData()
    }

    fun loadData() {
        viewModelScope.launch {
            _featuredRoots.value = repository.getRootsPaged(8, 0)
        }
        viewModelScope.launch {
            _lastReadSurah.value = preferences.lastReadSurah.first()
            _lastReadAyah.value = preferences.lastReadAyah.first()
        }
    }
}

@HiltViewModel
class SurahViewModel @Inject constructor(
    private val repository: QuranRepository
) : ViewModel() {
    private val _surahs = MutableStateFlow<List<Surah>>(emptyList())
    val surahs: StateFlow<List<Surah>> = _surahs.asStateFlow()

    private val _filterType = MutableStateFlow("all") // "all", "meccan", "medinan"
    val filterType: StateFlow<String> = _filterType.asStateFlow()

    private val _searchQuery = MutableStateFlow("")
    val searchQuery: StateFlow<String> = _searchQuery.asStateFlow()

    init {
        viewModelScope.launch {
            repository.getSurahs().collectLatest {
                _surahs.value = it
            }
        }
    }

    fun setFilter(type: String) {
        _filterType.value = type
    }

    fun setSearchQuery(query: String) {
        _searchQuery.value = query
    }
}


@HiltViewModel
class SurahDetailViewModel @Inject constructor(
    private val repository: QuranRepository,
    private val preferences: UserPreferencesRepository
) : ViewModel() {
    private val _surah = MutableStateFlow<Surah?>(null)
    val surah: StateFlow<Surah?> = _surah.asStateFlow()

    private val _ayat = MutableStateFlow<List<Ayah>>(emptyList())
    val ayat: StateFlow<List<Ayah>> = _ayat.asStateFlow()

    private val _selectedWord = MutableStateFlow<WordToken?>(null)
    val selectedWord: StateFlow<WordToken?> = _selectedWord.asStateFlow()

    private val _selectedWordAyah = MutableStateFlow<Ayah?>(null)
    val selectedWordAyah: StateFlow<Ayah?> = _selectedWordAyah.asStateFlow()

    private val _isLoading = MutableStateFlow(true)
    val isLoading: StateFlow<Boolean> = _isLoading.asStateFlow()

    private val _isLoadingMore = MutableStateFlow(false)
    val isLoadingMore: StateFlow<Boolean> = _isLoadingMore.asStateFlow()

    private val _aiSummary = MutableStateFlow<String?>(null)
    val aiSummary: StateFlow<String?> = _aiSummary.asStateFlow()

    private val _aiModel = MutableStateFlow<String?>(null)
    val aiModel: StateFlow<String?> = _aiModel.asStateFlow()

    private val _aiGeneratedAt = MutableStateFlow<String?>(null)
    val aiGeneratedAt: StateFlow<String?> = _aiGeneratedAt.asStateFlow()

    private val _isAiLoading = MutableStateFlow(false)
    val isAiLoading: StateFlow<Boolean> = _isAiLoading.asStateFlow()

    // Multi-ayah copy selection (Option A — contextual TopBar)
    private val _selectedAyahs = MutableStateFlow<Set<Int>>(emptySet())
    val selectedAyahs: StateFlow<Set<Int>> = _selectedAyahs.asStateFlow()

    private val _isSelectionMode = MutableStateFlow(false)
    val isSelectionMode: StateFlow<Boolean> = _isSelectionMode.asStateFlow()

    private val _surahPages = MutableStateFlow<List<Int>>(emptyList())
    val surahPages: StateFlow<List<Int>> = _surahPages.asStateFlow()

    private var currentSurahId: Int = -1
    private var currentOffset: Int = 0
    private var hasMore: Boolean = true
    private val pageSize: Int = 20

    fun loadSurah(surahId: Int) {
        // 🛡️ صمام الأمان: إذا كانت السورة محملة بالفعل وقائمة الآيات غير فارغة، احتفظ بها ولا تمسح الذاكرة
        if (currentSurahId == surahId && _surah.value?.id == surahId && _ayat.value.isNotEmpty()) {
            return
        }

        currentSurahId = surahId
        currentOffset = 0
        hasMore = true
        _isLoading.value = true
        _ayat.value = emptyList()

        viewModelScope.launch {
            // Delay until enter animation finishes (250ms + buffer) to avoid jank
            kotlinx.coroutines.delay(300)
            _surah.value = repository.getSurahById(surahId)
            val firstPage = repository.getAyatBySurahPaged(surahId, pageSize, 0)
            _ayat.value = firstPage
            currentOffset = firstPage.size
            val total = _surah.value?.ayahCount ?: 0
            hasMore = firstPage.size == pageSize && currentOffset < total
            _isLoading.value = false
        }

        viewModelScope.launch {
            try {
                _surahPages.value = repository.getPagesForSurah(surahId)
            } catch (_: Exception) {
                _surahPages.value = emptyList()
            }
        }
    }

    fun loadMoreIfNeeded(lastVisibleIndex: Int) {
        if (_isLoading.value || _isLoadingMore.value || !hasMore) return
        // Trigger when within 5 items from end (account for Basmalah offset handled in UI, but approximate)
        if (lastVisibleIndex >= _ayat.value.size - 5) {
            loadNextPage()
        }
    }

    private fun loadNextPage() {
        if (_isLoadingMore.value || !hasMore) return
        viewModelScope.launch {
            _isLoadingMore.value = true
            // Small debounce to avoid rapid triggers
            kotlinx.coroutines.delay(80)
            val nextPage = repository.getAyatBySurahPaged(currentSurahId, pageSize, currentOffset)
            if (nextPage.isNotEmpty()) {
                _ayat.value = _ayat.value + nextPage
                currentOffset += nextPage.size
                val total = _surah.value?.ayahCount ?: Int.MAX_VALUE
                hasMore = nextPage.size == pageSize && currentOffset < total
            } else {
                hasMore = false
            }
            _isLoadingMore.value = false
        }
    }

    suspend fun ensureAyahLoaded(targetAyah: Int) {
        // Keep loading pages until targetAyah is in list or no more
        while (hasMore && _ayat.value.none { it.ayah == targetAyah }) {
            val nextPage = repository.getAyatBySurahPaged(currentSurahId, pageSize, currentOffset)
            if (nextPage.isEmpty()) {
                hasMore = false
                break
            }
            _ayat.value = _ayat.value + nextPage
            currentOffset += nextPage.size
            val total = _surah.value?.ayahCount ?: Int.MAX_VALUE
            hasMore = nextPage.size == pageSize && currentOffset < total
            // Small yield to not block UI
            kotlinx.coroutines.delay(10)
        }
    }

    suspend fun ensurePageLoaded(targetPage: Int) {
        while (hasMore && _ayat.value.none { it.pageNumber == targetPage }) {
            // debounce to avoid race
            if (_isLoadingMore.value) {
                kotlinx.coroutines.delay(80)
                continue
            }
            _isLoadingMore.value = true
            kotlinx.coroutines.delay(80)
            val nextPage = repository.getAyatBySurahPaged(currentSurahId, pageSize, currentOffset)
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
            kotlinx.coroutines.delay(10)
        }
    }

    fun updateLastRead(surahId: Int, ayahNum: Int) {
        viewModelScope.launch { preferences.setLastRead(surahId, ayahNum) }
    }

    fun selectWord(word: WordToken, ayah: Ayah) {
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
                try {
                    val detail = repository.getRootDetail(rootId)
                    _aiSummary.value = detail?.aiSummary
                    _aiModel.value = detail?.aiModel
                    _aiGeneratedAt.value = detail?.aiGeneratedAt
                } catch (_: Exception) {
                    // keep null, UI shows fallback
                } finally {
                    _isAiLoading.value = false
                }
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

    // Copy selection
    fun enterSelectionMode(initialAyah: Int) {
        _isSelectionMode.value = true
        _selectedAyahs.value = setOf(initialAyah)
    }

    fun toggleAyahSelection(ayahNum: Int) {
        val current = _selectedAyahs.value.toMutableSet()
        if (current.contains(ayahNum)) current.remove(ayahNum) else current.add(ayahNum)
        _selectedAyahs.value = current
        if (current.isEmpty()) {
            _isSelectionMode.value = false
        } else if (!_isSelectionMode.value) {
            _isSelectionMode.value = true
        }
    }

    fun selectAllAyahs() {
        _selectedAyahs.value = _ayat.value.map { it.ayah }.toSet()
        _isSelectionMode.value = _selectedAyahs.value.isNotEmpty()
    }

    fun clearSelection() {
        _selectedAyahs.value = emptySet()
        _isSelectionMode.value = false
    }

    fun getFormattedSelection(): String {
        val surahVal = _surah.value
        val selectedNums = _selectedAyahs.value
        if (selectedNums.isEmpty()) return ""
        val selectedList = _ayat.value.filter { it.ayah in selectedNums }.sortedBy { it.ayah }
        if (selectedList.isEmpty()) return ""
        return io.github.ahmedsaadi0.quranwords.core.util.QuranCopyFormatter.formatMultiple(selectedList, surahVal)
    }

    fun getFormattedSingle(ayah: Ayah): String {
        return io.github.ahmedsaadi0.quranwords.core.util.QuranCopyFormatter.formatSingle(ayah, _surah.value)
    }
}

@HiltViewModel
class RootViewModel @Inject constructor(
    private val repository: QuranRepository,
    private val savedStateHandle: SavedStateHandle
) : ViewModel() {
    private val _roots = MutableStateFlow<List<RootItem>>(emptyList())
    val roots: StateFlow<List<RootItem>> = _roots.asStateFlow()

    private val _rootDetail = MutableStateFlow<RootDetail?>(null)
    val rootDetail: StateFlow<RootDetail?> = _rootDetail.asStateFlow()

    private val _isLoading = MutableStateFlow(false)
    val isLoading: StateFlow<Boolean> = _isLoading.asStateFlow()

    private val _query = MutableStateFlow("")
    val query: StateFlow<String> = _query.asStateFlow()

    // Paginated ayat occurrences for the current root
    private val _occurrences = MutableStateFlow<List<io.github.ahmedsaadi0.quranwords.domain.model.AyahOccurrenceModel>>(emptyList())
    val occurrences: StateFlow<List<io.github.ahmedsaadi0.quranwords.domain.model.AyahOccurrenceModel>> = _occurrences.asStateFlow()

    private val _occurrencesHasMore = MutableStateFlow(true)
    val occurrencesHasMore: StateFlow<Boolean> = _occurrencesHasMore.asStateFlow()

    private val _isOccurrencesLoadingMore = MutableStateFlow(false)
    val isOccurrencesLoadingMore: StateFlow<Boolean> = _isOccurrencesLoadingMore.asStateFlow()

    private var currentRootIdForOcc: Int? = null
    private var occOffset: Int = 0
    private val occPageSize: Int = 30
    private var occTotalCount: Int = 0

    private val _isCopyingAll = MutableStateFlow(false)
    val isCopyingAll: StateFlow<Boolean> = _isCopyingAll.asStateFlow()

    // Distinct Quran words for the current root (grouped by words.id, most frequent first)
    private val _rootWords = MutableStateFlow<List<io.github.ahmedsaadi0.quranwords.domain.model.RootWordModel>>(emptyList())
    val rootWords: StateFlow<List<io.github.ahmedsaadi0.quranwords.domain.model.RootWordModel>> = _rootWords.asStateFlow()

    private val _isWordsLoading = MutableStateFlow(false)
    val isWordsLoading: StateFlow<Boolean> = _isWordsLoading.asStateFlow()

    // Multi-word selection (long-press in Words tab)
    private val _selectedWordIds = MutableStateFlow<Set<Int>>(emptySet())
    val selectedWordIds: StateFlow<Set<Int>> = _selectedWordIds.asStateFlow()

    private val _isWordSelectionMode = MutableStateFlow(false)
    val isWordSelectionMode: StateFlow<Boolean> = _isWordSelectionMode.asStateFlow()

    init {
        // Restore pagination across process death if available
        savedStateHandle.get<Int>("currentRootIdForOcc")?.let { currentRootIdForOcc = it }
        savedStateHandle.get<Int>("occOffset")?.let { occOffset = it }
        savedStateHandle.get<Int>("occTotalCount")?.let { occTotalCount = it }
        loadRoots()
    }

    fun loadRoots() {
        viewModelScope.launch {
            _isLoading.value = true
            try {
                _roots.value = repository.getAllRoots()
            } catch (_: Exception) {
                _roots.value = emptyList()
            } finally {
                _isLoading.value = false
            }
        }
    }

    fun loadRootDetail(rootId: Int) {
        // Guard: if same root already loaded and data exists, keep current pagination/scroll state
        if (rootId == currentRootIdForOcc
            && _rootDetail.value?.item?.id == rootId
            && !_isLoading.value
            && _occurrences.value.isNotEmpty()
        ) return
        // Capture saved pagination for process-death restoration before overwriting
        val previousSavedRoot = savedStateHandle.get<Int>("currentRootIdForOcc")
        val savedOffset = savedStateHandle.get<Int>("occOffset") ?: 0
        val restoreTarget = if (previousSavedRoot == rootId && savedOffset > 0) savedOffset else 0
        // Persist current root for process death
        savedStateHandle["currentRootIdForOcc"] = rootId
        clearWordSelection()
        clearMeaningSelection()
        viewModelScope.launch {
            _isLoading.value = true
            currentRootIdForOcc = rootId
            occOffset = 0
            _occurrences.value = emptyList()
            _occurrencesHasMore.value = true
            occTotalCount = 0
            _rootWords.value = emptyList()
            val detail = repository.getRootDetail(rootId)
            _rootDetail.value = detail
            if (detail != null) {
                _occurrences.value = detail.ayatOccurrences
                occOffset = detail.ayatOccurrences.size
                occTotalCount = detail.item.occurrencesCount
                _occurrencesHasMore.value = occOffset < occTotalCount
                savedStateHandle["occOffset"] = occOffset
                savedStateHandle["occTotalCount"] = occTotalCount
                // Process-death restoration: fetch additional pages up to previously saved offset
                if (restoreTarget > occOffset && restoreTarget <= occTotalCount) {
                    while (occOffset < restoreTarget && _occurrencesHasMore.value) {
                        val next = repository.getRootOccurrencesPaged(rootId, occPageSize, occOffset)
                        if (next.isEmpty()) {
                            _occurrencesHasMore.value = false
                            break
                        }
                        _occurrences.value = _occurrences.value + next
                        occOffset += next.size
                        savedStateHandle["occOffset"] = occOffset
                        _occurrencesHasMore.value = occOffset < occTotalCount
                    }
                }
            }
            _isLoading.value = false
            loadRootWords(rootId)
        }
    }

    fun loadRootWords(rootId: Int) {
        viewModelScope.launch {
            _isWordsLoading.value = true
            try {
                _rootWords.value = repository.getRootWords(rootId)
            } catch (_: Exception) {
                _rootWords.value = emptyList()
            } finally {
                _isWordsLoading.value = false
            }
        }
    }

    fun enterWordSelectionMode(wordId: Int) {
        _isWordSelectionMode.value = true
        _selectedWordIds.value = setOf(wordId)
    }

    fun toggleWordSelection(wordId: Int) {
        val current = _selectedWordIds.value.toMutableSet()
        if (current.contains(wordId)) current.remove(wordId) else current.add(wordId)
        _selectedWordIds.value = current
        if (current.isEmpty()) {
            _isWordSelectionMode.value = false
        } else if (!_isWordSelectionMode.value) {
            _isWordSelectionMode.value = true
        }
    }

    fun selectAllWords() {
        _selectedWordIds.value = _rootWords.value.map { it.wordId }.toSet()
        _isWordSelectionMode.value = _selectedWordIds.value.isNotEmpty()
    }

    fun clearWordSelection() {
        _selectedWordIds.value = emptySet()
        _isWordSelectionMode.value = false
    }

    // Multi-meaning selection (long-press in Meanings tab) — mirrors words selection
    private val _selectedMeaningIds = MutableStateFlow<Set<Int>>(emptySet())
    val selectedMeaningIds: StateFlow<Set<Int>> = _selectedMeaningIds.asStateFlow()

    private val _isMeaningSelectionMode = MutableStateFlow(false)
    val isMeaningSelectionMode: StateFlow<Boolean> = _isMeaningSelectionMode.asStateFlow()

    fun enterMeaningSelectionMode(meaningId: Int) {
        _isMeaningSelectionMode.value = true
        _selectedMeaningIds.value = setOf(meaningId)
    }

    fun toggleMeaningSelection(meaningId: Int) {
        val current = _selectedMeaningIds.value.toMutableSet()
        if (current.contains(meaningId)) current.remove(meaningId) else current.add(meaningId)
        _selectedMeaningIds.value = current
        if (current.isEmpty()) {
            _isMeaningSelectionMode.value = false
        } else if (!_isMeaningSelectionMode.value) {
            _isMeaningSelectionMode.value = true
        }
    }

    fun selectAllMeanings() {
        _selectedMeaningIds.value =
            _rootDetail.value?.meanings?.map { it.id }?.toSet() ?: emptySet()
        _isMeaningSelectionMode.value = _selectedMeaningIds.value.isNotEmpty()
    }

    fun clearMeaningSelection() {
        _selectedMeaningIds.value = emptySet()
        _isMeaningSelectionMode.value = false
    }

    fun loadMoreOccurrencesIfNeeded(lastVisibleIndex: Int) {
        if (_isOccurrencesLoadingMore.value || !_occurrencesHasMore.value) return
        // lastVisibleIndex is index inside occurrences list (0-based)
        if (lastVisibleIndex >= _occurrences.value.size - 4) {
            loadMoreOccurrences()
        }
    }

    fun loadMoreOccurrences() {
        val rootId = currentRootIdForOcc ?: return
        if (_isOccurrencesLoadingMore.value || !_occurrencesHasMore.value) return
        viewModelScope.launch {
            _isOccurrencesLoadingMore.value = true
            kotlinx.coroutines.delay(80)
            val next = repository.getRootOccurrencesPaged(rootId, occPageSize, occOffset)
            if (next.isNotEmpty()) {
                _occurrences.value = _occurrences.value + next
                occOffset += next.size
                savedStateHandle["occOffset"] = occOffset
                _occurrencesHasMore.value = occOffset < occTotalCount
            } else {
                _occurrencesHasMore.value = false
            }
            _isOccurrencesLoadingMore.value = false
        }
    }

    // Called from UI when ayat tab's LazyColumn nears bottom (with header offset already subtracted)
    fun ensureOccurrencesLoadedForCount() {
        // No-op if already has data; used if total count was unknown
        if (_occurrences.value.isEmpty() && occTotalCount > 0) {
            loadMoreOccurrences()
        }
    }

    fun setQuery(q: String) {
        _query.value = q
    }

    /**
     * Fetches ALL occurrences for current root (single query, bypasses pagination)
     * and returns formatted text for copy/share (Format B1).
     * Exposes loading via [isCopyingAll].
     */
    suspend fun getAllOccurrencesFormatted(): String {
        val rootId = currentRootIdForOcc ?: return ""
        if (_isCopyingAll.value) return ""
        _isCopyingAll.value = true
        return try {
            val all = repository.getAllRootOccurrences(rootId)
            io.github.ahmedsaadi0.quranwords.core.util.QuranCopyFormatter.formatOccurrences(all)
        } catch (_: Exception) {
            ""
        } finally {
            _isCopyingAll.value = false
        }
    }

    /**
     * Fetches ALL ayat for the currently selected words (deduplicated, sorted)
     * and returns formatted text for copy/share. Exposes loading via [isCopyingAll].
     */
    suspend fun getSelectedWordsOccurrencesFormatted(): String {
        val rootId = currentRootIdForOcc ?: return ""
        val wordIds = _selectedWordIds.value.toList()
        if (wordIds.isEmpty() || _isCopyingAll.value) return ""
        _isCopyingAll.value = true
        return try {
            val all = repository.getAllOccurrencesForWords(rootId, wordIds)
            io.github.ahmedsaadi0.quranwords.core.util.QuranCopyFormatter.formatOccurrences(all)
        } catch (_: Exception) {
            ""
        } finally {
            _isCopyingAll.value = false
        }
    }

    suspend fun getSelectedWordsOccurrencesCount(): Int {
        val rootId = currentRootIdForOcc ?: return 0
        val wordIds = _selectedWordIds.value.toList()
        if (wordIds.isEmpty()) return 0
        return try {
            repository.getAllOccurrencesForWords(rootId, wordIds).size
        } catch (_: Exception) {
            0
        }
    }

    /**
     * Formats ALL lexicon meanings for the current root for copy/share.
     * Meanings are fully loaded with the detail (no pagination).
     * Exposes loading via [isCopyingAll].
     */
    suspend fun getAllMeaningsFormatted(): String {
        val detail = _rootDetail.value ?: return ""
        if (_isCopyingAll.value) return ""
        _isCopyingAll.value = true
        return try {
            io.github.ahmedsaadi0.quranwords.core.util.QuranCopyFormatter.formatMeanings(
                detail.item.root,
                detail.meanings
            )
        } catch (_: Exception) {
            ""
        } finally {
            _isCopyingAll.value = false
        }
    }

    /**
     * Formats only the selected lexicon meanings for copy/share.
     * Exposes loading via [isCopyingAll].
     */
    suspend fun getSelectedMeaningsFormatted(): String {
        val detail = _rootDetail.value ?: return ""
        val ids = _selectedMeaningIds.value
        if (ids.isEmpty() || _isCopyingAll.value) return ""
        _isCopyingAll.value = true
        return try {
            io.github.ahmedsaadi0.quranwords.core.util.QuranCopyFormatter.formatMeanings(
                detail.item.root,
                detail.meanings.filter { it.id in ids }
            )
        } catch (_: Exception) {
            ""
        } finally {
            _isCopyingAll.value = false
        }
    }
}

@HiltViewModel
class WordAyatViewModel @Inject constructor(
    private val repository: QuranRepository
) : ViewModel() {
    private val _wordText = MutableStateFlow("")
    val wordText: StateFlow<String> = _wordText.asStateFlow()

    private val _occurrences = MutableStateFlow<List<io.github.ahmedsaadi0.quranwords.domain.model.AyahOccurrenceModel>>(emptyList())
    val occurrences: StateFlow<List<io.github.ahmedsaadi0.quranwords.domain.model.AyahOccurrenceModel>> = _occurrences.asStateFlow()

    private val _hasMore = MutableStateFlow(true)
    val hasMore: StateFlow<Boolean> = _hasMore.asStateFlow()

    private val _isLoading = MutableStateFlow(true)
    val isLoading: StateFlow<Boolean> = _isLoading.asStateFlow()

    private val _isLoadingMore = MutableStateFlow(false)
    val isLoadingMore: StateFlow<Boolean> = _isLoadingMore.asStateFlow()

    private val _isCopyingAll = MutableStateFlow(false)
    val isCopyingAll: StateFlow<Boolean> = _isCopyingAll.asStateFlow()

    private val _totalCount = MutableStateFlow(0)
    val totalCount: StateFlow<Int> = _totalCount.asStateFlow()

    private var currentRootId: Int? = null
    private var currentWordId: Int? = null
    private var offset: Int = 0
    private val pageSize: Int = 30

    fun loadWord(rootId: Int, wordId: Int) {
        if (rootId == currentRootId && wordId == currentWordId && _occurrences.value.isNotEmpty()) return
        currentRootId = rootId
        currentWordId = wordId
        offset = 0
        _occurrences.value = emptyList()
        _hasMore.value = true
        _isLoading.value = true
        _wordText.value = ""
        _totalCount.value = 0
        viewModelScope.launch {
            try {
                val words = repository.getRootWords(rootId)
                val matched = words.firstOrNull { it.wordId == wordId }
                _wordText.value = matched?.text ?: ""
                _totalCount.value = matched?.occurrencesCount ?: 0
                val first = repository.getWordOccurrencesPaged(rootId, wordId, pageSize, 0)
                _occurrences.value = first
                offset = first.size
                if (_totalCount.value < first.size) {
                    _totalCount.value = first.size
                }
                _hasMore.value = if (_totalCount.value > 0) {
                    offset < _totalCount.value
                } else {
                    first.size == pageSize
                }
            } catch (_: Exception) {
                _occurrences.value = emptyList()
                _hasMore.value = false
            } finally {
                _isLoading.value = false
            }
        }
    }

    fun loadMoreIfNeeded(lastVisibleIndex: Int) {
        if (_isLoadingMore.value || !_hasMore.value) return
        if (lastVisibleIndex >= _occurrences.value.size - 4) {
            loadMore()
        }
    }

    private fun loadMore() {
        val rootId = currentRootId ?: return
        val wordId = currentWordId ?: return
        if (_isLoadingMore.value || !_hasMore.value) return
        viewModelScope.launch {
            _isLoadingMore.value = true
            kotlinx.coroutines.delay(80)
            try {
                val next = repository.getWordOccurrencesPaged(rootId, wordId, pageSize, offset)
                if (next.isNotEmpty()) {
                    _occurrences.value = _occurrences.value + next
                    offset += next.size
                    if (_totalCount.value < _occurrences.value.size) {
                        _totalCount.value = _occurrences.value.size
                    }
                    _hasMore.value = if (_totalCount.value > 0) {
                        offset < _totalCount.value
                    } else {
                        next.size == pageSize
                    }
                } else {
                    _hasMore.value = false
                }
            } catch (_: Exception) {
                _hasMore.value = false
            } finally {
                _isLoadingMore.value = false
            }
        }
    }

    suspend fun getAllFormatted(): String {
        val rootId = currentRootId ?: return ""
        val wordId = currentWordId ?: return ""
        if (_isCopyingAll.value) return ""
        _isCopyingAll.value = true
        return try {
            val all = repository.getAllWordOccurrences(rootId, wordId)
            io.github.ahmedsaadi0.quranwords.core.util.QuranCopyFormatter.formatOccurrences(all)
        } catch (_: Exception) {
            ""
        } finally {
            _isCopyingAll.value = false
        }
    }
}

@HiltViewModel
class SearchViewModel @Inject constructor(
    private val repository: QuranRepository
) : ViewModel() {
    private val pageSize: Int = io.github.ahmedsaadi0.quranwords.core.util.DatabaseConstants.SEARCH_PAGE_SIZE

    private val _query = MutableStateFlow("")
    val query: StateFlow<String> = _query.asStateFlow()

    private val _results = MutableStateFlow(SearchResult())
    val results: StateFlow<SearchResult> = _results.asStateFlow()

    private val _isSearching = MutableStateFlow(false)
    val isSearching: StateFlow<Boolean> = _isSearching.asStateFlow()

    private val _isLoadingMore = MutableStateFlow(false)
    val isLoadingMore: StateFlow<Boolean> = _isLoadingMore.asStateFlow()

    private val _rootsHasMore = MutableStateFlow(false)
    val rootsHasMore: StateFlow<Boolean> = _rootsHasMore.asStateFlow()

    private val _masadirHasMore = MutableStateFlow(false)
    val masadirHasMore: StateFlow<Boolean> = _masadirHasMore.asStateFlow()

    private val _derivativesHasMore = MutableStateFlow(false)
    val derivativesHasMore: StateFlow<Boolean> = _derivativesHasMore.asStateFlow()

    private val _ayatHasMore = MutableStateFlow(false)
    val ayatHasMore: StateFlow<Boolean> = _ayatHasMore.asStateFlow()

    private var rootsOffset: Int = 0
    private var masadirOffset: Int = 0
    private var derivativesOffset: Int = 0
    private var ayatOffset: Int = 0

    private var searchJob: kotlinx.coroutines.Job? = null

    fun onQueryChanged(newQuery: String) {
        _query.value = newQuery
        searchJob?.cancel()
        if (newQuery.isBlank()) {
            resetPagination()
            _results.value = SearchResult()
            _isSearching.value = false
            return
        }
        searchJob = viewModelScope.launch {
            kotlinx.coroutines.delay(300)
            _isSearching.value = true
            resetPagination()
            try {
                val firstPage = repository.searchAll(newQuery)
                _results.value = firstPage
                rootsOffset = firstPage.roots.size
                masadirOffset = firstPage.masadir.size
                derivativesOffset = firstPage.derivatives.size
                ayatOffset = firstPage.ayat.size
                _rootsHasMore.value = firstPage.roots.size == pageSize
                _masadirHasMore.value = firstPage.masadir.size == pageSize
                _derivativesHasMore.value = firstPage.derivatives.size == pageSize
                _ayatHasMore.value = firstPage.ayat.size == pageSize
            } catch (_: Exception) {
                resetPagination()
                _results.value = SearchResult()
            } finally {
                _isSearching.value = false
            }
        }
    }

    private fun resetPagination() {
        rootsOffset = 0
        masadirOffset = 0
        derivativesOffset = 0
        ayatOffset = 0
        _rootsHasMore.value = false
        _masadirHasMore.value = false
        _derivativesHasMore.value = false
        _ayatHasMore.value = false
        _isLoadingMore.value = false
    }

    fun loadMoreIfNeeded(tab: Int, lastVisibleIndex: Int) {
        if (_isSearching.value || _isLoadingMore.value) return
        val size = when (tab) {
            0 -> _results.value.roots.size
            1 -> _results.value.masadir.size
            2 -> _results.value.derivatives.size
            3 -> _results.value.ayat.size
            else -> return
        }
        val hasMore = when (tab) {
            0 -> _rootsHasMore.value
            1 -> _masadirHasMore.value
            2 -> _derivativesHasMore.value
            3 -> _ayatHasMore.value
            else -> false
        }
        if (!hasMore) return
        if (lastVisibleIndex < size - 5) return
        when (tab) {
            0 -> loadMoreRoots()
            1 -> loadMoreMasadir()
            2 -> loadMoreDerivatives()
            3 -> loadMoreAyat()
        }
    }

    private fun loadMoreRoots() {
        val q = _query.value
        if (q.isBlank() || _isLoadingMore.value || !_rootsHasMore.value) return
        viewModelScope.launch {
            _isLoadingMore.value = true
            kotlinx.coroutines.delay(80)
            try {
                val next = repository.searchRootsPaged(q, pageSize, rootsOffset)
                if (next.isNotEmpty()) {
                    _results.value = _results.value.copy(roots = _results.value.roots + next)
                    rootsOffset += next.size
                    _rootsHasMore.value = next.size == pageSize
                } else {
                    _rootsHasMore.value = false
                }
            } catch (_: Exception) {
                _rootsHasMore.value = false
            } finally {
                _isLoadingMore.value = false
            }
        }
    }

    private fun loadMoreMasadir() {
        val q = _query.value
        if (q.isBlank() || _isLoadingMore.value || !_masadirHasMore.value) return
        viewModelScope.launch {
            _isLoadingMore.value = true
            kotlinx.coroutines.delay(80)
            try {
                val next = repository.searchMasadirPaged(q, pageSize, masadirOffset)
                if (next.isNotEmpty()) {
                    _results.value = _results.value.copy(masadir = _results.value.masadir + next)
                    masadirOffset += next.size
                    _masadirHasMore.value = next.size == pageSize
                } else {
                    _masadirHasMore.value = false
                }
            } catch (_: Exception) {
                _masadirHasMore.value = false
            } finally {
                _isLoadingMore.value = false
            }
        }
    }

    private fun loadMoreDerivatives() {
        val q = _query.value
        if (q.isBlank() || _isLoadingMore.value || !_derivativesHasMore.value) return
        viewModelScope.launch {
            _isLoadingMore.value = true
            kotlinx.coroutines.delay(80)
            try {
                val next = repository.searchDerivativesPaged(q, pageSize, derivativesOffset)
                if (next.isNotEmpty()) {
                    _results.value = _results.value.copy(derivatives = _results.value.derivatives + next)
                    derivativesOffset += next.size
                    _derivativesHasMore.value = next.size == pageSize
                } else {
                    _derivativesHasMore.value = false
                }
            } catch (_: Exception) {
                _derivativesHasMore.value = false
            } finally {
                _isLoadingMore.value = false
            }
        }
    }

    private fun loadMoreAyat() {
        val q = _query.value
        if (q.isBlank() || _isLoadingMore.value || !_ayatHasMore.value) return
        viewModelScope.launch {
            _isLoadingMore.value = true
            kotlinx.coroutines.delay(80)
            try {
                val next = repository.searchAyatPaged(q, pageSize, ayatOffset)
                if (next.isNotEmpty()) {
                    _results.value = _results.value.copy(ayat = _results.value.ayat + next)
                    ayatOffset += next.size
                    _ayatHasMore.value = next.size == pageSize
                } else {
                    _ayatHasMore.value = false
                }
            } catch (_: Exception) {
                _ayatHasMore.value = false
            } finally {
                _isLoadingMore.value = false
            }
        }
    }
}

@HiltViewModel
class DatabaseSetupViewModel @Inject constructor(
    private val downloadManager: DatabaseDownloadManager,
    private val dbUpdateRepository: io.github.ahmedsaadi0.quranwords.domain.repository.DbUpdateRepository,
    private val quranRepository: QuranRepository
) : ViewModel() {

    private val _downloadState = MutableStateFlow<DownloadState>(
        if (downloadManager.isDatabaseReady()) DownloadState.Completed() else DownloadState.Idle
    )
    val downloadState: StateFlow<DownloadState> = _downloadState.asStateFlow()

    private val _latestRelease =
        MutableStateFlow<io.github.ahmedsaadi0.quranwords.domain.model.DbReleaseInfo?>(null)
    val latestRelease: StateFlow<io.github.ahmedsaadi0.quranwords.domain.model.DbReleaseInfo?> =
        _latestRelease.asStateFlow()

    private val _installedVersion =
        MutableStateFlow(io.github.ahmedsaadi0.quranwords.domain.model.DbInstalledVersion())
    val installedVersion: StateFlow<io.github.ahmedsaadi0.quranwords.domain.model.DbInstalledVersion> =
        _installedVersion.asStateFlow()

    private val _isCheckingUpdate = MutableStateFlow(false)
    val isCheckingUpdate: StateFlow<Boolean> = _isCheckingUpdate.asStateFlow()

    init {
        viewModelScope.launch {
            _installedVersion.value = dbUpdateRepository.getInstalledVersion()
        }
    }

    fun checkForUpdate() {
        if (_isCheckingUpdate.value) return
        viewModelScope.launch {
            _isCheckingUpdate.value = true
            try {
                _installedVersion.value = dbUpdateRepository.getInstalledVersion()
                when (val r = dbUpdateRepository.getLatestRelease()) {
                    is io.github.ahmedsaadi0.quranwords.domain.repository.DbCheckResult.Success ->
                        _latestRelease.value = r.data
                    is io.github.ahmedsaadi0.quranwords.domain.repository.DbCheckResult.Error -> Unit
                }
            } finally {
                _isCheckingUpdate.value = false
            }
        }
    }

    fun startDownload() {
        viewModelScope.launch {
            val info = resolveReleaseForDownload() ?: return@launch
            downloadManager.downloadRelease(info).collectLatest { state ->
                _downloadState.value = state
                if (state is DownloadState.Completed && state.versionCode > 0) {
                    quranRepository.closeDb()
                    dbUpdateRepository.setInstalledVersion(state.versionCode, state.versionName)
                    _installedVersion.value = dbUpdateRepository.getInstalledVersion()
                }
            }
        }
    }

    private suspend fun resolveReleaseForDownload(): io.github.ahmedsaadi0.quranwords.domain.model.DbReleaseInfo? {
        _latestRelease.value?.let { return it }
        return when (val r = dbUpdateRepository.getLatestRelease()) {
            is io.github.ahmedsaadi0.quranwords.domain.repository.DbCheckResult.Success -> {
                _latestRelease.value = r.data
                r.data
            }
            is io.github.ahmedsaadi0.quranwords.domain.repository.DbCheckResult.Error -> {
                _downloadState.value = DownloadState.Error(
                    io.github.ahmedsaadi0.quranwords.data.remote.DownloadError.MANIFEST_FAILED,
                    r.message
                )
                null
            }
        }
    }

    fun importDatabase(uri: android.net.Uri) {
        viewModelScope.launch {
            downloadManager.importDatabase(uri).collectLatest { state ->
                _downloadState.value = state
                if (state is DownloadState.Completed) {
                    quranRepository.closeDb()
                    _installedVersion.value = dbUpdateRepository.getInstalledVersion()
                }
            }
        }
    }

    fun isReady(): Boolean = downloadManager.isDatabaseReady()
}

@HiltViewModel
class DbUpdateViewModel @Inject constructor(
    private val checkUpdate: io.github.ahmedsaadi0.quranwords.domain.usecase.CheckDbUpdateUseCase,
    private val dbUpdateRepository: io.github.ahmedsaadi0.quranwords.domain.repository.DbUpdateRepository
) : ViewModel() {
    private val _state =
        MutableStateFlow<io.github.ahmedsaadi0.quranwords.domain.model.DbUpdateState>(
            io.github.ahmedsaadi0.quranwords.domain.model.DbUpdateState.Unknown
        )
    val state: StateFlow<io.github.ahmedsaadi0.quranwords.domain.model.DbUpdateState> =
        _state.asStateFlow()

    private val _isChecking = MutableStateFlow(false)
    val isChecking: StateFlow<Boolean> = _isChecking.asStateFlow()

    fun checkOnce() {
        if (_isChecking.value) return
        viewModelScope.launch {
            _isChecking.value = true
            try {
                when (val r = checkUpdate()) {
                    is io.github.ahmedsaadi0.quranwords.domain.repository.DbCheckResult.Success ->
                        _state.value = r.data
                    is io.github.ahmedsaadi0.quranwords.domain.repository.DbCheckResult.Error ->
                        _state.value = io.github.ahmedsaadi0.quranwords.domain.model.DbUpdateState.Unknown
                }
            } finally {
                _isChecking.value = false
            }
        }
    }

    fun dismiss(info: io.github.ahmedsaadi0.quranwords.domain.model.DbReleaseInfo) {
        viewModelScope.launch {
            dbUpdateRepository.setDismissedVersionCode(info.versionCode)
            _state.value = io.github.ahmedsaadi0.quranwords.domain.model.DbUpdateState.UpToDate
        }
    }
}
