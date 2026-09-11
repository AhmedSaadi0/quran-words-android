package io.github.ahmedsaadi0.quranwords.ui

import app.cash.turbine.test
import io.github.ahmedsaadi0.quranwords.domain.model.RootItem
import io.github.ahmedsaadi0.quranwords.fake.FakeQuranRepository
import io.github.ahmedsaadi0.quranwords.ui.roots.RootsListEvent
import io.github.ahmedsaadi0.quranwords.ui.roots.RootsListViewModel
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
        RootItem(id = 1, root = "كتب", glossAr = "كتب الكتاب"),
        RootItem(id = 2, root = "علم", glossAr = "علم الشيء"),
        RootItem(id = 3, root = "رحم", glossAr = "رحمة بالله")
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
}