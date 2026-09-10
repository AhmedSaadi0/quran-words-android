package io.github.ahmedsaadi0.quranwords.ui.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dagger.hilt.android.lifecycle.HiltViewModel
import io.github.ahmedsaadi0.quranwords.core.util.AppLanguage
import io.github.ahmedsaadi0.quranwords.core.util.LanguageManager
import io.github.ahmedsaadi0.quranwords.data.remote.DatabaseDownloadManager
import io.github.ahmedsaadi0.quranwords.data.remote.DownloadState
import io.github.ahmedsaadi0.quranwords.data.repository.UserPreferencesRepository
import io.github.ahmedsaadi0.quranwords.domain.model.Ayah
import io.github.ahmedsaadi0.quranwords.domain.model.RootItem
import io.github.ahmedsaadi0.quranwords.domain.model.Surah
import io.github.ahmedsaadi0.quranwords.domain.model.WordToken
import io.github.ahmedsaadi0.quranwords.domain.repository.QuranRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class MainViewModel @Inject constructor(
    private val repository: QuranRepository,
    private val preferences: UserPreferencesRepository,
    private val languageManager: LanguageManager
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

    /**
     * Single-collection app state (Decision 14). Screens migrate to this as
     * their features are refactored; the granular flows above stay until the
     * last consumer migrates.
     */
    val appState: StateFlow<AppPreferencesUiState> = combine(
        _fontSize,
        _darkModeSetting,
        _dynamicColorEnabled,
        _colorMode,
        _bookmarkedSurahs,
        _bookmarkedAyat,
        _lastReadSurah,
        _lastReadAyah,
        _language,
        _isDbReady
    ) { values ->
        @Suppress("UNCHECKED_CAST")
        AppPreferencesUiState(
            fontSize = values[0] as Float,
            darkModeSetting = values[1] as Int,
            dynamicColorEnabled = values[2] as Boolean,
            colorMode = values[3] as Int,
            bookmarkedSurahs = values[4] as Set<String>,
            bookmarkedAyat = values[5] as Set<String>,
            lastReadSurah = values[6] as Int,
            lastReadAyah = values[7] as Int,
            language = values[8] as String,
            isDbReady = values[9] as Boolean
        )
    }.stateIn(viewModelScope, SharingStarted.Eagerly, AppPreferencesUiState())

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
