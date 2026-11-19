package com.quranwords.ui

import com.quranwords.domain.model.AyahOccurrenceModel
import com.quranwords.domain.model.RootWordModel
import com.quranwords.fake.FakeQuranRepository
import com.quranwords.ui.roots.word.WordAyatEvent
import com.quranwords.ui.roots.word.WordAyatViewModel
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
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

@OptIn(ExperimentalCoroutinesApi::class)
class WordAyatViewModelTest {

    private val dispatcher = UnconfinedTestDispatcher()

    @Before
    fun setUp() { Dispatchers.setMain(dispatcher) }

    @After
    fun tearDown() { Dispatchers.resetMain() }

    private class PagingRepository : FakeQuranRepository() {
        var failNextPage = false

        override suspend fun getRootWords(rootId: Int): List<RootWordModel> =
            listOf(RootWordModel(wordId = 7, text = "كلمة", occurrencesCount = 90, wordIds = listOf(7)))

        override suspend fun getWordOccurrencesPaged(
            rootId: Int,
            wordIds: List<Int>,
            limit: Int,
            offset: Int
        ): List<AyahOccurrenceModel> {
            if (offset > 0 && failNextPage) throw IllegalStateException("page failed")
            return List(limit) { index ->
                AyahOccurrenceModel(1, "الفاتحة", offset + index + 1, "نص", "كلمة")
            }
        }
    }

    @Test
    fun paging_error_keeps_has_more_and_retry_loads_next_page() = runTest {
        val repo = PagingRepository()
        val vm = WordAyatViewModel(repo)
        vm.onEvent(WordAyatEvent.Load(1, 7))
        advanceUntilIdle()
        assertTrue(vm.uiState.value.hasMore)

        repo.failNextPage = true
        vm.onEvent(WordAyatEvent.AyatNearingEnd(25))
        advanceUntilIdle()
        assertEquals("page failed", vm.uiState.value.error)
        assertTrue(vm.uiState.value.hasMore)
        assertEquals(30, vm.uiState.value.occurrences.size)

        repo.failNextPage = false
        vm.onEvent(WordAyatEvent.Retry)
        advanceUntilIdle()
        assertEquals(null, vm.uiState.value.error)
        assertEquals(60, vm.uiState.value.occurrences.size)
        assertFalse(vm.uiState.value.isLoadingMore)
    }
}
