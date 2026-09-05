package io.github.ahmedsaadi0.quranwords

import androidx.lifecycle.SavedStateHandle
import io.github.ahmedsaadi0.quranwords.domain.model.Ayah
import io.github.ahmedsaadi0.quranwords.domain.model.AyahOccurrenceModel
import io.github.ahmedsaadi0.quranwords.domain.model.RootDetail
import io.github.ahmedsaadi0.quranwords.domain.model.RootItem
import io.github.ahmedsaadi0.quranwords.domain.model.RootWordModel
import io.github.ahmedsaadi0.quranwords.domain.model.SearchResult
import io.github.ahmedsaadi0.quranwords.domain.model.Surah
import io.github.ahmedsaadi0.quranwords.domain.repository.QuranRepository
import io.github.ahmedsaadi0.quranwords.ui.navigation.Screen
import io.github.ahmedsaadi0.quranwords.ui.viewmodel.RootViewModel
import io.github.ahmedsaadi0.quranwords.ui.viewmodel.WordAyatViewModel
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test

private class FakeQuranRepository(
    val words: List<RootWordModel> = listOf(
        RootWordModel(10, "كَتَبَ", 5),
        RootWordModel(11, "كِتَاب", 3),
        RootWordModel(12, "كَاتِب", 1)
    ),
    val wordOccurrences: Map<Int, List<AyahOccurrenceModel>> = mapOf(
        10 to listOf(
            AyahOccurrenceModel(2, "البقرة", 183, "نص 183", "كَتَبَ"),
            AyahOccurrenceModel(2, "البقرة", 282, "نص 282", "كَتَبَ"),
            AyahOccurrenceModel(4, "النساء", 103, "نص مشترك", "كَتَبَ")
        ),
        11 to listOf(
            AyahOccurrenceModel(2, "البقرة", 2, "نص الكتاب", "كِتَاب"),
            AyahOccurrenceModel(4, "النساء", 103, "نص مشترك", "كِتَاب")
        )
    )
) : QuranRepository {
    override fun getSurahs(): Flow<List<Surah>> = flowOf(emptyList())
    override suspend fun getSurahById(id: Int): Surah? = null
    override fun getAyatBySurah(surahId: Int): Flow<List<Ayah>> = flowOf(emptyList())
    override suspend fun getAyatBySurahPaged(surahId: Int, limit: Int, offset: Int): List<Ayah> = emptyList()
    override suspend fun getAyahWithWords(surahId: Int, ayahNum: Int): Ayah? = null
    override suspend fun getRootsPaged(limit: Int, offset: Int): List<RootItem> = emptyList()
    override suspend fun getRootDetail(rootId: Int): RootDetail? = RootDetail(
        item = RootItem(rootId, "كتب", occurrencesCount = 5)
    )
    override suspend fun getRootOccurrencesPaged(rootId: Int, limit: Int, offset: Int): List<AyahOccurrenceModel> = emptyList()
    override suspend fun getAllRootOccurrences(rootId: Int): List<AyahOccurrenceModel> = emptyList()
    override suspend fun getRootOccurrencesCount(rootId: Int): Int = 0
    override suspend fun getRootByText(rootText: String): RootDetail? = null
    override suspend fun searchAll(query: String): SearchResult = SearchResult()
    override suspend fun getPagesForSurah(surahId: Int): List<Int> = emptyList()
    override fun isDatabaseReady(): Boolean = true

    override suspend fun getRootWords(rootId: Int): List<RootWordModel> = words

    override suspend fun getWordOccurrencesPaged(rootId: Int, wordId: Int, limit: Int, offset: Int): List<AyahOccurrenceModel> {
        val all = wordOccurrences[wordId] ?: emptyList()
        if (offset >= all.size) return emptyList()
        return all.drop(offset).take(limit)
    }

    override suspend fun getAllWordOccurrences(rootId: Int, wordId: Int): List<AyahOccurrenceModel> =
        wordOccurrences[wordId] ?: emptyList()

    override suspend fun getAllOccurrencesForWords(rootId: Int, wordIds: List<Int>): List<AyahOccurrenceModel> {
        // Mirror SQL: GROUP BY ayah (dedupe), ORDER BY surah, ayah
        return wordIds.flatMap { wordOccurrences[it] ?: emptyList() }
            .distinctBy { it.surahId to it.ayahNum }
            .sortedWith(compareBy({ it.surahId }, { it.ayahNum }))
    }
}

@OptIn(ExperimentalCoroutinesApi::class)
class RootWordsTest {

    private val testDispatcher = UnconfinedTestDispatcher()

    @Before
    fun setup() {
        Dispatchers.setMain(testDispatcher)
    }

    @After
    fun tearDown() {
        Dispatchers.resetMain()
    }

    @Test
    fun `WordAyat route format is correct`() {
        assertEquals("word_ayat/3/10", Screen.WordAyat.createRoute(3, 10))
    }

    @Test
    fun `root words are most frequent first`() = runTest {
        val repo = FakeQuranRepository()
        val words = repo.getRootWords(1)
        assertEquals(listOf(10, 11, 12), words.map { it.wordId })
        assertTrue(words[0].occurrencesCount >= words[1].occurrencesCount)
    }

    @Test
    fun `multi-word occurrences dedupe shared ayat`() = runTest {
        val repo = FakeQuranRepository()
        // word 10 has {2:183, 2:282, 4:103}, word 11 has {2:2, 4:103} -> union is 4 distinct ayat
        val combined = repo.getAllOccurrencesForWords(1, listOf(10, 11))
        assertEquals(4, combined.size)
        assertEquals(1, combined.count { it.surahId == 4 && it.ayahNum == 103 })
        // sorted by surah, ayah
        val keys = combined.map { it.surahId to it.ayahNum }
        assertEquals(keys.sortedWith(compareBy({ it.first }, { it.second })), keys)
    }

    @Test
    fun `empty word selection returns empty`() = runTest {
        val repo = FakeQuranRepository()
        assertTrue(repo.getAllOccurrencesForWords(1, emptyList()).isEmpty())
    }

    @Test
    fun `RootViewModel word selection transitions`() = runTest {
        val vm = RootViewModel(FakeQuranRepository(), SavedStateHandle())
        advanceUntilIdle()

        assertFalse(vm.isWordSelectionMode.value)
        vm.enterWordSelectionMode(10)
        assertTrue(vm.isWordSelectionMode.value)
        assertEquals(setOf(10), vm.selectedWordIds.value)

        vm.toggleWordSelection(11)
        assertEquals(setOf(10, 11), vm.selectedWordIds.value)

        vm.toggleWordSelection(10)
        assertEquals(setOf(11), vm.selectedWordIds.value)

        vm.clearWordSelection()
        assertTrue(vm.selectedWordIds.value.isEmpty())
        assertFalse(vm.isWordSelectionMode.value)
    }

    @Test
    fun `RootViewModel selectAllWords selects every loaded word`() = runTest {
        val vm = RootViewModel(FakeQuranRepository(), SavedStateHandle())
        vm.loadRootDetail(1)
        advanceUntilIdle()
        vm.selectAllWords()
        assertEquals(setOf(10, 11, 12), vm.selectedWordIds.value)
        assertTrue(vm.isWordSelectionMode.value)
    }

    @Test
    fun `RootViewModel loads words on detail load`() = runTest {
        val vm = RootViewModel(FakeQuranRepository(), SavedStateHandle())
        vm.loadRootDetail(1)
        advanceUntilIdle()
        assertEquals(3, vm.rootWords.value.size)
        assertEquals("كَتَبَ", vm.rootWords.value.first().text)
    }

    @Test
    fun `WordAyatViewModel paginates word occurrences`() = runTest {
        val vm = WordAyatViewModel(FakeQuranRepository())
        vm.loadWord(1, 10)
        advanceUntilIdle()
        assertEquals("كَتَبَ", vm.wordText.value)
        assertEquals(3, vm.occurrences.value.size)
        assertFalse(vm.hasMore.value)
        val formatted = vm.getAllFormatted()
        assertTrue(formatted.contains("[سورة البقرة: 183]"))
        assertTrue(formatted.contains("[سورة البقرة: 282]"))
        assertTrue(formatted.contains("[سورة النساء: 103]"))
    }
}
