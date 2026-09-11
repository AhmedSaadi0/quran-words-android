package io.github.ahmedsaadi0.quranwords.fake

import io.github.ahmedsaadi0.quranwords.domain.model.Ayah
import io.github.ahmedsaadi0.quranwords.domain.model.AyahOccurrenceModel
import io.github.ahmedsaadi0.quranwords.domain.model.DerivativeModel
import io.github.ahmedsaadi0.quranwords.domain.model.MasdarModel
import io.github.ahmedsaadi0.quranwords.domain.model.RootDetail
import io.github.ahmedsaadi0.quranwords.domain.model.RootItem
import io.github.ahmedsaadi0.quranwords.domain.model.RootWordModel
import io.github.ahmedsaadi0.quranwords.domain.model.SearchResult
import io.github.ahmedsaadi0.quranwords.domain.model.Surah
import io.github.ahmedsaadi0.quranwords.domain.repository.UserPreferencesRepository
import io.github.ahmedsaadi0.quranwords.domain.repository.QuranRepository
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.flowOf

/** StateFlow-backed fake of the preferences contract for VM tests. */
class FakeUserPreferences(
    initialSurahBookmarks: Set<String> = emptySet(),
    initialAyahBookmarks: Set<String> = emptySet(),
    initialLastReadSurah: Int = 1,
    initialLastReadAyah: Int = 1
) : UserPreferencesRepository {

    val surahBookmarkState = MutableStateFlow(initialSurahBookmarks)
    val ayahBookmarkState = MutableStateFlow(initialAyahBookmarks)
    val lastReadSurahState = MutableStateFlow(initialLastReadSurah)
    val lastReadAyahState = MutableStateFlow(initialLastReadAyah)
    val fontSizeState = MutableStateFlow(24f)
    val darkModeState = MutableStateFlow(0)
    val dynamicColorState = MutableStateFlow(false)
    val languageState = MutableStateFlow("system")

    override val language: Flow<String> = languageState
    override val fontSize: Flow<Float> = fontSizeState
    override val darkModeSetting: Flow<Int> = darkModeState
    override val dynamicColorEnabled: Flow<Boolean> = dynamicColorState
    override val lastReadSurah: Flow<Int> = lastReadSurahState
    override val lastReadAyah: Flow<Int> = lastReadAyahState
    override val bookmarkedSurahs: Flow<Set<String>> = surahBookmarkState
    override val bookmarkedAyat: Flow<Set<String>> = ayahBookmarkState
    override val dbVersionCode: Flow<Int> = MutableStateFlow(0)
    override val dbVersionName: Flow<String> = MutableStateFlow("")
    override val dismissedDbVersionCode: Flow<Int> = MutableStateFlow(0)

    override suspend fun setInstalledDbVersion(code: Int, name: String) = Unit
    override suspend fun setDismissedDbVersion(code: Int) = Unit
    override suspend fun setLastDbCheckAt(timestamp: Long) = Unit

    override suspend fun setFontSize(size: Float) {
        fontSizeState.value = size
    }

    override suspend fun setDarkModeSetting(mode: Int) {
        darkModeState.value = mode
    }

    override suspend fun setLanguage(tag: String) {
        languageState.value = tag
    }

    override suspend fun setDynamicColorEnabled(enabled: Boolean) {
        dynamicColorState.value = enabled
    }

    override suspend fun setLastRead(surahId: Int, ayahNum: Int) {
        lastReadSurahState.value = surahId
        lastReadAyahState.value = ayahNum
    }

    override suspend fun toggleSurahBookmark(surahId: Int) {
        val key = surahId.toString()
        surahBookmarkState.value =
            if (key in surahBookmarkState.value) surahBookmarkState.value - key
            else surahBookmarkState.value + key
    }

    override suspend fun toggleAyahBookmark(surahId: Int, ayahNum: Int) {
        val key = "$surahId:$ayahNum"
        ayahBookmarkState.value =
            if (key in ayahBookmarkState.value) ayahBookmarkState.value - key
            else ayahBookmarkState.value + key
    }
}

/**
 * Configurable fake of the Quran repository: empty by default, with optional
 * roots/search fixtures and fault injection for error paths.
 */
class FakeQuranRepository(
    val roots: List<RootItem> = emptyList(),
    val searchResult: SearchResult = SearchResult(),
    val searchPageSize: Int = 20,
    var failGetAllRoots: Boolean = false,
    var failSearch: Boolean = false
) : QuranRepository {

    override fun getSurahs(): Flow<List<Surah>> = flowOf(emptyList())
    override suspend fun getSurahById(id: Int): Surah? = null
    override fun getAyatBySurah(surahId: Int): Flow<List<Ayah>> = flowOf(emptyList())
    override suspend fun getAyatBySurahPaged(surahId: Int, limit: Int, offset: Int): List<Ayah> = emptyList()
    override suspend fun getAyahWithWords(surahId: Int, ayahNum: Int): Ayah? = null

    override suspend fun getRootsPaged(limit: Int, offset: Int): List<RootItem> =
        roots.drop(offset).take(limit)

    override suspend fun getAllRoots(): List<RootItem> {
        if (failGetAllRoots) throw IllegalStateException("db not ready")
        return roots
    }

    override suspend fun getRootDetail(rootId: Int): RootDetail? = null
    override suspend fun getRootByText(rootText: String): RootDetail? = null
    override suspend fun getRootOccurrencesPaged(rootId: Int, limit: Int, offset: Int): List<AyahOccurrenceModel> = emptyList()
    override suspend fun getAllRootOccurrences(rootId: Int): List<AyahOccurrenceModel> = emptyList()
    override suspend fun getRootOccurrencesCount(rootId: Int): Int = 0
    override suspend fun getRootWords(rootId: Int): List<RootWordModel> = emptyList()
    override suspend fun getWordOccurrencesPaged(rootId: Int, wordIds: List<Int>, limit: Int, offset: Int): List<AyahOccurrenceModel> = emptyList()
    override suspend fun getAllWordOccurrences(rootId: Int, wordIds: List<Int>): List<AyahOccurrenceModel> = emptyList()
    override suspend fun getAllOccurrencesForWords(rootId: Int, wordIds: List<Int>): List<AyahOccurrenceModel> = emptyList()

    override suspend fun searchAll(query: String): SearchResult {
        if (failSearch) throw IllegalStateException("search failed")
        return searchResult
    }

    override suspend fun searchRootsPaged(query: String, limit: Int, offset: Int): List<RootItem> {
        if (failSearch) throw IllegalStateException("search failed")
        return searchResult.roots.drop(offset).take(limit)
    }

    override suspend fun searchMasadirPaged(query: String, limit: Int, offset: Int): List<MasdarModel> {
        if (failSearch) throw IllegalStateException("search failed")
        return searchResult.masadir.drop(offset).take(limit)
    }

    override suspend fun searchDerivativesPaged(query: String, limit: Int, offset: Int): List<DerivativeModel> {
        if (failSearch) throw IllegalStateException("search failed")
        return searchResult.derivatives.drop(offset).take(limit)
    }

    override suspend fun searchAyatPaged(query: String, limit: Int, offset: Int): List<Ayah> {
        if (failSearch) throw IllegalStateException("search failed")
        return searchResult.ayat.drop(offset).take(limit)
    }

    override suspend fun getPagesForSurah(surahId: Int): List<Int> = emptyList()
    override fun isDatabaseReady(): Boolean = true
    override fun closeDb() = Unit
}