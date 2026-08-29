package io.github.ahmedsaadi0.quranwords.ui.viewmodel

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import io.github.ahmedsaadi0.quranwords.data.remote.DatabaseDownloadManager
import io.github.ahmedsaadi0.quranwords.data.remote.DownloadState
import io.github.ahmedsaadi0.quranwords.data.repository.QuranRepositoryImpl
import io.github.ahmedsaadi0.quranwords.data.repository.UserPreferencesRepository
import io.github.ahmedsaadi0.quranwords.data.util.QuranMetaConstants
import io.github.ahmedsaadi0.quranwords.domain.model.Ayah
import io.github.ahmedsaadi0.quranwords.domain.model.RootDetail
import io.github.ahmedsaadi0.quranwords.domain.model.RootItem
import io.github.ahmedsaadi0.quranwords.domain.model.SearchResult
import io.github.ahmedsaadi0.quranwords.domain.model.Surah
import io.github.ahmedsaadi0.quranwords.domain.model.WordToken
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch

class MainViewModel(application: Application) : AndroidViewModel(application) {
    val repository = QuranRepositoryImpl(application)
    val preferences = UserPreferencesRepository(application)
    val downloadManager = DatabaseDownloadManager(application)

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
    }

    fun refreshDbStatus() {
        _isDbReady.value = repository.isDatabaseReady()
    }

    fun setFontSize(size: Float) {
        viewModelScope.launch { preferences.setFontSize(size) }
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

class HomeViewModel(application: Application) : AndroidViewModel(application) {
    private val repository = QuranRepositoryImpl(application)
    private val preferences = UserPreferencesRepository(application)

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

class SurahViewModel(application: Application) : AndroidViewModel(application) {
    private val repository = QuranRepositoryImpl(application)

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

class SurahDetailViewModel(application: Application) : AndroidViewModel(application) {
    private val repository = QuranRepositoryImpl(application)
    private val preferences = UserPreferencesRepository(application)

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

    private var currentSurahId: Int = 1
    private var currentOffset: Int = 0
    private var hasMore: Boolean = true
    private val pageSize: Int = 20

    fun loadSurah(surahId: Int) {
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
        // Do not overwrite lastRead here; it will be updated via updateLastRead on scroll/enter
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

    fun updateLastRead(surahId: Int, ayahNum: Int) {
        viewModelScope.launch { preferences.setLastRead(surahId, ayahNum) }
    }

    fun selectWord(word: WordToken, ayah: Ayah) {
        _selectedWord.value = word
        _selectedWordAyah.value = ayah
    }

    fun clearSelectedWord() {
        _selectedWord.value = null
        _selectedWordAyah.value = null
    }
}

class RootViewModel(application: Application) : AndroidViewModel(application) {
    private val repository = QuranRepositoryImpl(application)

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

    init {
        loadRoots()
    }

    fun loadRoots() {
        viewModelScope.launch {
            _isLoading.value = true
            _roots.value = repository.getRootsPaged(50, 0)
            _isLoading.value = false
        }
    }

    fun loadRootDetail(rootId: Int) {
        viewModelScope.launch {
            _isLoading.value = true
            currentRootIdForOcc = rootId
            occOffset = 0
            _occurrences.value = emptyList()
            _occurrencesHasMore.value = true
            occTotalCount = 0
            val detail = repository.getRootDetail(rootId)
            _rootDetail.value = detail
            if (detail != null) {
                _occurrences.value = detail.ayatOccurrences
                occOffset = detail.ayatOccurrences.size
                occTotalCount = detail.item.occurrencesCount
                _occurrencesHasMore.value = occOffset < occTotalCount
            }
            _isLoading.value = false
        }
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
}

class SearchViewModel(application: Application) : AndroidViewModel(application) {
    private val repository = QuranRepositoryImpl(application)

    private val _query = MutableStateFlow("")
    val query: StateFlow<String> = _query.asStateFlow()

    private val _results = MutableStateFlow(SearchResult())
    val results: StateFlow<SearchResult> = _results.asStateFlow()

    private val _isSearching = MutableStateFlow(false)
    val isSearching: StateFlow<Boolean> = _isSearching.asStateFlow()

    fun onQueryChanged(newQuery: String) {
        _query.value = newQuery
        if (newQuery.isBlank()) {
            _results.value = SearchResult()
            return
        }
        viewModelScope.launch {
            _isSearching.value = true
            _results.value = repository.searchAll(newQuery)
            _isSearching.value = false
        }
    }
}

class DatabaseSetupViewModel(application: Application) : AndroidViewModel(application) {
    private val downloadManager = DatabaseDownloadManager(application)

    private val _downloadState = MutableStateFlow<DownloadState>(
        if (downloadManager.isDatabaseReady()) DownloadState.Completed else DownloadState.Idle
    )
    val downloadState: StateFlow<DownloadState> = _downloadState.asStateFlow()

    fun startDownload() {
        viewModelScope.launch {
            downloadManager.downloadDatabase().collectLatest { state ->
                _downloadState.value = state
            }
        }
    }

    fun importDatabase(uri: android.net.Uri) {
        viewModelScope.launch {
            downloadManager.importDatabase(uri).collectLatest { state ->
                _downloadState.value = state
            }
        }
    }

    fun isReady(): Boolean = downloadManager.isDatabaseReady()
}
