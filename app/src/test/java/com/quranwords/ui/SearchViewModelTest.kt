package com.quranwords.ui

import app.cash.turbine.test
import com.quranwords.core.util.DatabaseConstants
import com.quranwords.domain.model.Ayah
import com.quranwords.domain.model.RootItem
import com.quranwords.domain.model.SearchResult
import com.quranwords.fake.FakeQuranRepository
import com.quranwords.ui.search.SearchEvent
import com.quranwords.ui.search.SearchTab
import com.quranwords.ui.search.SearchViewModel
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

    private fun viewModel(repo: FakeQuranRepository) =
        SearchViewModel(repo, com.quranwords.domain.usecase.SearchUseCase(repo), com.quranwords.fake.FakeUserPreferences())

    @Test
    fun `query debounce delays the search until 200ms pass`() = runTest {
        val vm = viewModel(repository())
        vm.onEvent(SearchEvent.QueryChanged("كتب"))

        // Before the debounce window elapses nothing was searched yet.
        advanceTimeBy(199)
        runCurrent()
        assertEquals(0, vm.uiState.value.results.roots.size)

        advanceTimeBy(1)
        advanceUntilIdle()
        assertEquals(pageSize, vm.uiState.value.results.roots.size)
    }

    @Test
    fun `first page sets per-tab hasMore and near-end loads the next page`() = runTest {
        val vm = viewModel(repository(size = pageSize + 3))
        vm.onEvent(SearchEvent.QueryChanged("كتب"))
        advanceTimeBy(200)
        advanceUntilIdle()
        assertEquals(pageSize, vm.uiState.value.results.roots.size)

        vm.onEvent(SearchEvent.NearingEnd(SearchTab.ROOTS, lastVisibleIndex = pageSize - 1))
        advanceUntilIdle()
        assertEquals(pageSize + 3, vm.uiState.value.results.roots.size)
    }

    private class PagingFailureRepository(size: Int) : FakeQuranRepository(
        searchResult = SearchResult(roots = (1..size).map { RootItem(it, "جذر$it") })
    ) {
        var failPage = false

        override suspend fun searchRootsPaged(query: String, limit: Int, offset: Int): List<RootItem> {
            if (offset > 0 && failPage) throw IllegalStateException("page failed")
            return super.searchRootsPaged(query, limit, offset)
        }
    }

    @Test
    fun paging_error_preserves_results_and_offset() = runTest {
        val repo = PagingFailureRepository(pageSize + 3)
        val vm = viewModel(repo)
        vm.onEvent(SearchEvent.QueryChanged("كتب"))
        advanceTimeBy(200)
        advanceUntilIdle()

        repo.failPage = true
        vm.onEvent(SearchEvent.NearingEnd(SearchTab.ROOTS, lastVisibleIndex = pageSize - 1))
        advanceUntilIdle()

        assertEquals(pageSize, vm.uiState.value.results.roots.size)
        assertEquals("page failed", vm.uiState.value.pagingError)
        assertNull(vm.uiState.value.error)

        repo.failPage = false
        vm.onEvent(SearchEvent.LoadMore(SearchTab.ROOTS))
        advanceUntilIdle()
        assertEquals(pageSize + 3, vm.uiState.value.results.roots.size)
        assertNull(vm.uiState.value.pagingError)
    }

    @Test
    fun recent_recorded_once_per_navigation() = runTest {
        val prefs = com.quranwords.fake.FakeUserPreferences()
        val repo = repository()
        val vm = SearchViewModel(repo, com.quranwords.domain.usecase.SearchUseCase(repo), prefs)

        vm.onEvent(SearchEvent.RecordRecent("كتب"))
        advanceUntilIdle()

        assertEquals(listOf("كتب"), prefs.recentSearchesState.value)
    }

    @Test
    fun `blank query resets results`() = runTest {
        val vm = viewModel(repository())
        vm.onEvent(SearchEvent.QueryChanged("كتب"))
        advanceTimeBy(200)
        advanceUntilIdle()

        vm.onEvent(SearchEvent.QueryChanged(""))
        advanceUntilIdle()
        assertTrue(vm.uiState.value.results.roots.isEmpty())
        assertNull(vm.uiState.value.error)
    }

    @Test
    fun `search failure surfaces an error and retry recovers`() = runTest {
        val repo = FakeQuranRepository(failSearch = true)
        val vm = viewModel(repo)
        vm.onEvent(SearchEvent.QueryChanged("كتب"))
        advanceTimeBy(200)
        advanceUntilIdle()
        assertNotNull(vm.uiState.value.error)

        repo.failSearch = false
        vm.onEvent(SearchEvent.Retry)
        advanceTimeBy(200)
        advanceUntilIdle()

        vm.uiState.test {
            val recovered = awaitItem()
            assertNull(recovered.error)
            assertTrue(recovered.results.roots.isNotEmpty())
        }
    }
}