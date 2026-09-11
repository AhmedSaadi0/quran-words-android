package io.github.ahmedsaadi0.quranwords

import androidx.lifecycle.SavedStateHandle
import io.github.ahmedsaadi0.quranwords.domain.model.Ayah
import io.github.ahmedsaadi0.quranwords.domain.model.AyahOccurrenceModel
import io.github.ahmedsaadi0.quranwords.domain.model.DerivativeModel
import io.github.ahmedsaadi0.quranwords.domain.model.MasdarModel
import io.github.ahmedsaadi0.quranwords.domain.model.RootDetail
import io.github.ahmedsaadi0.quranwords.domain.model.RootItem
import io.github.ahmedsaadi0.quranwords.domain.model.RootMeaningModel
import io.github.ahmedsaadi0.quranwords.domain.model.RootWordModel
import io.github.ahmedsaadi0.quranwords.domain.model.SearchResult
import io.github.ahmedsaadi0.quranwords.domain.model.Surah
import io.github.ahmedsaadi0.quranwords.domain.repository.QuranRepository
import io.github.ahmedsaadi0.quranwords.ui.roots.detail.RootDetailEvent
import io.github.ahmedsaadi0.quranwords.ui.roots.detail.RootDetailViewModel
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

private class FakeMeaningsRepository(
    val meanings: List<RootMeaningModel> = listOf(
        RootMeaningModel(1, "تعريف أول", "لسان العرب"),
        RootMeaningModel(2, "تعريف ثان", "الصحاح"),
        RootMeaningModel(3, "تعريف ثالث", "المقاييس")
    )
) : QuranRepository {
    override fun getSurahs(): Flow<List<Surah>> = flowOf(emptyList())
    override suspend fun getSurahById(id: Int): Surah? = null
    override fun getAyatBySurah(surahId: Int): Flow<List<Ayah>> = flowOf(emptyList())
    override suspend fun getAyatBySurahPaged(surahId: Int, limit: Int, offset: Int): List<Ayah> = emptyList()
    override suspend fun getAyahWithWords(surahId: Int, ayahNum: Int): Ayah? = null
    override suspend fun getRootsPaged(limit: Int, offset: Int): List<RootItem> = emptyList()
    override suspend fun getAllRoots(): List<RootItem> = emptyList()
    override suspend fun getRootDetail(rootId: Int): RootDetail? = RootDetail(
        item = RootItem(rootId, "كتب", occurrencesCount = 5),
        meanings = meanings
    )
    override suspend fun getRootOccurrencesPaged(rootId: Int, limit: Int, offset: Int): List<AyahOccurrenceModel> = emptyList()
    override suspend fun getAllRootOccurrences(rootId: Int): List<AyahOccurrenceModel> = emptyList()
    override suspend fun getRootOccurrencesCount(rootId: Int): Int = 0
    override suspend fun getRootWords(rootId: Int): List<RootWordModel> = emptyList()
    override suspend fun getWordOccurrencesPaged(rootId: Int, wordId: Int, limit: Int, offset: Int): List<AyahOccurrenceModel> = emptyList()
    override suspend fun getAllWordOccurrences(rootId: Int, wordId: Int): List<AyahOccurrenceModel> = emptyList()
    override suspend fun getAllOccurrencesForWords(rootId: Int, wordIds: List<Int>): List<AyahOccurrenceModel> = emptyList()
    override suspend fun getRootByText(rootText: String): RootDetail? = null
    override suspend fun searchAll(query: String): SearchResult = SearchResult()
    override suspend fun searchRootsPaged(query: String, limit: Int, offset: Int): List<RootItem> = emptyList()
    override suspend fun searchMasadirPaged(query: String, limit: Int, offset: Int): List<MasdarModel> = emptyList()
    override suspend fun searchDerivativesPaged(query: String, limit: Int, offset: Int): List<DerivativeModel> = emptyList()
    override suspend fun searchAyatPaged(query: String, limit: Int, offset: Int): List<Ayah> = emptyList()
    override suspend fun getPagesForSurah(surahId: Int): List<Int> = emptyList()
    override suspend fun getAyatByPage(page: Int): List<Ayah> = emptyList()
    override suspend fun getMushafPageCount(): Int = 0
    override fun isDatabaseReady(): Boolean = true
    override fun closeDb() = Unit
}

@OptIn(ExperimentalCoroutinesApi::class)
class RootMeaningsTest {

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
    fun `meaning selection transitions mirror words`() = runTest {
        val vm = RootDetailViewModel(FakeMeaningsRepository(), SavedStateHandle())
        advanceUntilIdle()

        assertFalse(vm.meaningSelection.value.isSelectionMode)
        vm.onEvent(RootDetailEvent.EnterMeaningSelection(1))
        assertTrue(vm.meaningSelection.value.isSelectionMode)
        assertEquals(setOf(1), vm.meaningSelection.value.selectedIds)

        vm.onEvent(RootDetailEvent.ToggleMeaning(2))
        assertEquals(setOf(1, 2), vm.meaningSelection.value.selectedIds)

        vm.onEvent(RootDetailEvent.ToggleMeaning(1))
        assertEquals(setOf(2), vm.meaningSelection.value.selectedIds)

        vm.onEvent(RootDetailEvent.ClearMeaningSelection)
        assertTrue(vm.meaningSelection.value.selectedIds.isEmpty())
        assertFalse(vm.meaningSelection.value.isSelectionMode)
    }

    @Test
    fun `selectAllMeanings selects every loaded meaning`() = runTest {
        val vm = RootDetailViewModel(FakeMeaningsRepository(), SavedStateHandle())
        vm.loadRootDetail(1)
        advanceUntilIdle()
        vm.onEvent(RootDetailEvent.SelectAllMeanings)
        assertEquals(setOf(1, 2, 3), vm.meaningSelection.value.selectedIds)
        assertTrue(vm.meaningSelection.value.isSelectionMode)
    }

    @Test
    fun `getAllMeaningsFormatted covers every meaning`() = runTest {
        val vm = RootDetailViewModel(FakeMeaningsRepository(), SavedStateHandle())
        vm.loadRootDetail(1)
        advanceUntilIdle()
        val formatted = vm.getAllMeaningsFormatted()
        assertTrue(formatted.contains("[معاني الجذر: كتب]"))
        assertTrue(formatted.contains("▪ لسان العرب"))
        assertTrue(formatted.contains("▪ الصحاح"))
        assertTrue(formatted.contains("▪ المقاييس"))
    }

    @Test
    fun `getSelectedMeaningsFormatted filters by selection`() = runTest {
        val vm = RootDetailViewModel(FakeMeaningsRepository(), SavedStateHandle())
        vm.loadRootDetail(1)
        advanceUntilIdle()
        vm.onEvent(RootDetailEvent.EnterMeaningSelection(2))
        val formatted = vm.getSelectedMeaningsFormatted()
        assertTrue(formatted.contains("▪ الصحاح"))
        assertFalse(formatted.contains("▪ لسان العرب"))
        assertFalse(formatted.contains("▪ المقاييس"))
    }

    @Test
    fun `empty selection returns empty`() = runTest {
        val vm = RootDetailViewModel(FakeMeaningsRepository(), SavedStateHandle())
        vm.loadRootDetail(1)
        advanceUntilIdle()
        assertEquals("", vm.getSelectedMeaningsFormatted())
    }
}
