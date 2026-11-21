package com.quranwords.ui

import androidx.lifecycle.SavedStateHandle
import com.quranwords.domain.model.AyahOccurrenceModel
import com.quranwords.domain.model.RootDetail
import com.quranwords.domain.model.RootItem
import com.quranwords.fake.FakeQuranRepository
import com.quranwords.fake.FakeUserPreferences
import com.quranwords.ui.roots.detail.RootDetailEvent
import com.quranwords.ui.roots.detail.RootDetailViewModel
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
import org.junit.Assert.assertNull
import org.junit.Before
import org.junit.Test

private fun occurrence(num: Int) = AyahOccurrenceModel(
    surahId = 2, surahNameAr = "البقرة", ayahNum = num,
    textUthmani = "آية", matchedWordText = "كلمة"
)

@OptIn(ExperimentalCoroutinesApi::class)
class RootDetailErrorTest {

    private val dispatcher = UnconfinedTestDispatcher()

    @Before
    fun setUp() { Dispatchers.setMain(dispatcher) }

    @After
    fun tearDown() { Dispatchers.resetMain() }

    private class ThrowingRepository : FakeQuranRepository() {
        var failDetail = true

        override suspend fun getRootDetail(rootId: Int): Nothing? {
            if (failDetail) throw IllegalStateException("detail failed")
            return null
        }
    }

    private class PagingRepository : FakeQuranRepository() {
        var failOccurrences = true
        var calls = 0
            private set

        override suspend fun getRootDetail(rootId: Int): RootDetail = RootDetail(
            item = RootItem(id = rootId, root = "كتب", occurrencesCount = 31),
            ayatOccurrences = listOf(occurrence(1))
        )

        override suspend fun getRootOccurrencesPaged(rootId: Int, limit: Int, offset: Int): List<AyahOccurrenceModel> {
            calls++
            if (failOccurrences) throw IllegalStateException("paging failed")
            return listOf(occurrence(offset + 1))
        }
    }

    @Test
    fun paging_error_and_retry() = runTest {
        val repo = PagingRepository()
        val vm = RootDetailViewModel(repo, FakeUserPreferences(), SavedStateHandle())
        vm.onEvent(RootDetailEvent.Load(1))
        advanceUntilIdle()

        vm.onEvent(RootDetailEvent.AyatNearingEnd(0))
        advanceUntilIdle()
        assertEquals("paging failed", vm.uiState.value.error)
        assertEquals(1, repo.calls)
        assertEquals(1, vm.uiState.value.ayat.occurrences.size)

        repo.failOccurrences = false
        vm.onEvent(RootDetailEvent.Retry)
        advanceUntilIdle()

        assertEquals(null, vm.uiState.value.error)
        assertEquals(2, repo.calls)
        assertEquals(2, vm.uiState.value.ayat.occurrences.size)
    }

    @Test
    fun detail_load_error_is_exposed_and_retry_recovers() = runTest {
        val repo = ThrowingRepository()
        val vm = RootDetailViewModel(repo, FakeUserPreferences(), SavedStateHandle())

        vm.onEvent(RootDetailEvent.Load(1))
        advanceUntilIdle()

        assertEquals("detail failed", vm.uiState.value.error)
        assertFalse(vm.uiState.value.isLoading)

        repo.failDetail = false
        vm.onEvent(RootDetailEvent.Retry)
        advanceUntilIdle()

        assertNull(vm.uiState.value.error)
        assertFalse(vm.uiState.value.isLoading)
    }
}
