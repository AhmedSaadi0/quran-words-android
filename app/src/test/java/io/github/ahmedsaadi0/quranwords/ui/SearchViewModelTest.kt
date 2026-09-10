package io.github.ahmedsaadi0.quranwords.ui

import app.cash.turbine.test
import io.github.ahmedsaadi0.quranwords.core.util.DatabaseConstants
import io.github.ahmedsaadi0.quranwords.domain.model.Ayah
import io.github.ahmedsaadi0.quranwords.domain.model.RootItem
import io.github.ahmedsaadi0.quranwords.domain.model.SearchResult
import io.github.ahmedsaadi0.quranwords.fake.FakeQuranRepository
import io.github.ahmedsaadi0.quranwords.ui.search.SearchEvent
import io.github.ahmedsaadi0.quranwords.ui.search.SearchTab
import io.github.ahmedsaadi0.quranwords.ui.search.SearchViewModel
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import kotlinx.coroutines.test.advanceTimeBy
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runCurrent
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test

@OptIn(ExperimentalCoroutinesApi::class)
class SearchViewModelTest {

    private val testDispatcher = UnconfinedTestDispatcher()
    private val pageSize = DatabaseConstants.SEARCH_PAGE_SIZE

    private val ayah = Ayah(
        id = 1, surah = 2, ayah = 255,
        textUthmani = "اللَّهُ لَا إِلَٰهَ إِلَّا هُوَ",
        textUthmaniPlain = "اللَّهُ لَا إِلَٰهَ إِلَّا هُوَ",
        textImlaei = "الله لا اله الا هو",
        wordCount = 5
    )

    @Before
    fun setup() {
        Dispatchers.setMain(testDispatcher)
    }

    @After
    fun tearDown() {
        Dispatchers.resetMain()
    }

    private fun repository(size: Int = pageSize + 3) = FakeQuranRepository(
        searchResult = SearchResult(
            roots = (1..size).map { RootItem(it, "جذر$it") },
            ayat = (1..size).map { ayah }
        )
    )

    @Test
    fun `query debounce delays the search until 300ms pass`() = runTest {
        val vm = SearchViewModel(repository())
        vm.onEvent(SearchEvent.QueryChanged("كتب"))

        // Before the debounce window elapses nothing was searched yet.
        advanceTimeBy(299)
        runCurrent()
        assertEquals(0, vm.uiState.value.results.roots.size)

        advanceTimeBy(1)
        advanceUntilIdle()
        assertEquals(pageSize, vm.uiState.value.results.roots.size)
    }

    @Test
    fun `first page sets per-tab hasMore and near-end loads the next page`() = runTest {
        val vm = SearchViewModel(repository(size = pageSize + 3))
        vm.onEvent(SearchEvent.QueryChanged("كتب"))
        advanceTimeBy(300)
        advanceUntilIdle()
        assertEquals(pageSize, vm.uiState.value.results.roots.size)

        vm.onEvent(SearchEvent.NearingEnd(SearchTab.ROOTS, lastVisibleIndex = pageSize - 1))
        advanceUntilIdle()
        assertEquals(pageSize + 3, vm.uiState.value.results.roots.size)
    }

    @Test
    fun `blank query resets results`() = runTest {
        val vm = SearchViewModel(repository())
        vm.onEvent(SearchEvent.QueryChanged("كتب"))
        advanceTimeBy(300)
        advanceUntilIdle()

        vm.onEvent(SearchEvent.QueryChanged(""))
        advanceUntilIdle()
        assertTrue(vm.uiState.value.results.roots.isEmpty())
        assertNull(vm.uiState.value.error)
    }

    @Test
    fun `search failure surfaces an error and retry recovers`() = runTest {
        val repo = FakeQuranRepository(failSearch = true)
        val vm = SearchViewModel(repo)
        vm.onEvent(SearchEvent.QueryChanged("كتب"))
        advanceTimeBy(300)
        advanceUntilIdle()
        assertNotNull(vm.uiState.value.error)

        repo.failSearch = false
        vm.onEvent(SearchEvent.Retry)
        advanceTimeBy(300)
        advanceUntilIdle()

        vm.uiState.test {
            val recovered = awaitItem()
            assertNull(recovered.error)
            assertTrue(recovered.results.roots.isNotEmpty())
        }
    }
}