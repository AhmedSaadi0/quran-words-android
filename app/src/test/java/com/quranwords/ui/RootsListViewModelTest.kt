package com.quranwords.ui

import app.cash.turbine.test
import com.quranwords.domain.model.RootItem
import com.quranwords.fake.FakeQuranRepository
import com.quranwords.ui.roots.RootsListEvent
import com.quranwords.ui.roots.RootsListViewModel
import com.quranwords.ui.roots.RootsSort
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
class RootsListViewModelTest {

    private val testDispatcher = UnconfinedTestDispatcher()

    private val roots = listOf(
        RootItem(id = 1, root = "كتب", glossAr = "كتب الكتاب", aiModel = "muse-spark-1.3"),
        RootItem(id = 2, root = "علم", glossAr = "علم الشيء", aiModel = "Muse Spark 1.3"),
        RootItem(id = 3, root = "رحم", glossAr = "رحمة بالله", aiModel = null)
    )

    @Before
    fun setup() {
        Dispatchers.setMain(testDispatcher)
    }

    @After
    fun tearDown() {
        Dispatchers.resetMain()
    }

    @Test
    fun `loads all roots and exposes total count`() = runTest {
        val vm = RootsListViewModel(FakeQuranRepository(roots = roots))
        advanceUntilIdle()

        vm.uiState.test {
            val state = awaitItem()
            assertFalse(state.isLoading)
            assertEquals(3, state.totalCount)
            assertEquals(3, state.filteredRoots.size)
            assertEquals(null, state.error)
        }
    }

    @Test
    fun `query filters roots with Arabic normalization`() = runTest {
        val vm = RootsListViewModel(FakeQuranRepository(roots = roots))
        advanceUntilIdle()

        // Diacritics + alef variants normalize to the plain root text.
        vm.onEvent(RootsListEvent.QueryChanged("عِلْم"))
        advanceUntilIdle()

        vm.uiState.test {
            val state = awaitItem()
            assertEquals(listOf(2), state.filteredRoots.map { it.id })
        }
    }

    @Test
    fun `query matches the Arabic gloss too`() = runTest {
        val vm = RootsListViewModel(FakeQuranRepository(roots = roots))
        advanceUntilIdle()

        vm.onEvent(RootsListEvent.QueryChanged("رحمة"))
        advanceUntilIdle()

        vm.uiState.test {
            assertEquals(listOf(3), awaitItem().filteredRoots.map { it.id })
        }
    }

    @Test
    fun `load failure surfaces an error and retry recovers`() = runTest {
        val repo = FakeQuranRepository(roots = roots, failGetAllRoots = true)
        val vm = RootsListViewModel(repo)
        advanceUntilIdle()

        vm.uiState.test {
            val failed = awaitItem()
            assertTrue(failed.error != null)
            assertTrue(failed.filteredRoots.isEmpty())
        }

        repo.failGetAllRoots = false
        vm.onEvent(RootsListEvent.Retry)
        advanceUntilIdle()

        vm.uiState.test {
            val recovered = awaitItem()
            assertEquals(null, recovered.error)
            assertEquals(3, recovered.totalCount)
        }
    }

    @Test
    fun `available models skip blank values and are counted`() = runTest {
        val vm = RootsListViewModel(FakeQuranRepository(roots = roots))
        advanceUntilIdle()

        vm.uiState.test {
            val state = awaitItem()
            assertEquals(listOf("Muse Spark 1.3", "muse-spark-1.3"), state.availableModels.map { it.model })
            assertEquals(listOf(1, 1), state.availableModels.map { it.count })
        }
    }

    @Test
    fun `query matches the model name case insensitively`() = runTest {
        val vm = RootsListViewModel(FakeQuranRepository(roots = roots))
        advanceUntilIdle()

        vm.onEvent(RootsListEvent.QueryChanged("muse"))
        advanceUntilIdle()

        vm.uiState.test {
            assertEquals(listOf(1, 2), awaitItem().filteredRoots.map { it.id })
        }
    }

    @Test
    fun `model chip toggles the filter and clears it again`() = runTest {
        val vm = RootsListViewModel(FakeQuranRepository(roots = roots))
        advanceUntilIdle()

        vm.onEvent(RootsListEvent.ModelFilterToggled("muse-spark-1.3"))
        advanceUntilIdle()

        vm.uiState.test {
            val filtered = awaitItem()
            assertEquals(setOf("muse-spark-1.3"), filtered.selectedModels)
            assertEquals(listOf(1), filtered.filteredRoots.map { it.id })
        }

        vm.onEvent(RootsListEvent.ModelFiltersCleared)
        advanceUntilIdle()

        vm.uiState.test {
            val cleared = awaitItem()
            assertTrue(cleared.selectedModels.isEmpty())
            assertEquals(3, cleared.filteredRoots.size)
        }
    }

    @Test
    fun `sort by model pushes missing models last`() = runTest {
        val vm = RootsListViewModel(FakeQuranRepository(roots = roots))
        advanceUntilIdle()

        vm.onEvent(RootsListEvent.SortChanged(RootsSort.ByModel))
        advanceUntilIdle()

        vm.uiState.test {
            val state = awaitItem()
            assertEquals(RootsSort.ByModel, state.sort)
            assertEquals(listOf(2, 1, 3), state.filteredRoots.map { it.id })
        }
    }
}