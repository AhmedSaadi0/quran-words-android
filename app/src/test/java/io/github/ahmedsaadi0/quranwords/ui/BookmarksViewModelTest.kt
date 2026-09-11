package io.github.ahmedsaadi0.quranwords.ui

import app.cash.turbine.test
import io.github.ahmedsaadi0.quranwords.core.util.BookmarkRef
import io.github.ahmedsaadi0.quranwords.fake.FakeUserPreferences
import io.github.ahmedsaadi0.quranwords.ui.bookmarks.BookmarksEvent
import io.github.ahmedsaadi0.quranwords.ui.bookmarks.BookmarksViewModel
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
class BookmarksViewModelTest {

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
    fun `parses and sorts bookmarks by surah then ayah`() = runTest {
        val vm = BookmarksViewModel(
            FakeUserPreferences(
                initialSurahBookmarks = setOf("10", "2"),
                initialAyahBookmarks = setOf("2:282", "1:7")
            )
        )
        advanceUntilIdle()

        vm.uiState.test {
            val state = awaitItem()
            assertFalse(state.isLoading)
            assertEquals(
                listOf(BookmarkRef(2), BookmarkRef(10)),
                state.surahBookmarks
            )
            assertEquals(
                listOf(BookmarkRef(1, 7), BookmarkRef(2, 282)),
                state.ayahBookmarks
            )
            assertFalse(state.isEmpty)
        }
    }

    @Test
    fun `empty preferences produce loaded empty state`() = runTest {
        val vm = BookmarksViewModel(FakeUserPreferences())
        advanceUntilIdle()

        vm.uiState.test {
            val state = awaitItem()
            assertTrue(state.isEmpty)
            assertFalse(state.isLoading)
        }
    }

    @Test
    fun `toggle ayah bookmark removes the key`() = runTest {
        val prefs = FakeUserPreferences(initialAyahBookmarks = setOf("2:282"))
        val vm = BookmarksViewModel(prefs)
        advanceUntilIdle()

        vm.onEvent(BookmarksEvent.ToggleAyahBookmark(surahId = 2, ayah = 282))
        advanceUntilIdle()

        vm.uiState.test {
            assertTrue(awaitItem().ayahBookmarks.isEmpty())
        }
        assertEquals(emptySet<String>(), prefs.ayahBookmarkState.value)
    }

    @Test
    fun `toggle surah bookmark adds a key`() = runTest {
        val prefs = FakeUserPreferences()
        val vm = BookmarksViewModel(prefs)
        advanceUntilIdle()

        vm.onEvent(BookmarksEvent.ToggleSurahBookmark(surahId = 3))
        advanceUntilIdle()

        vm.uiState.test {
            assertEquals(listOf(BookmarkRef(3)), awaitItem().surahBookmarks)
        }
    }
}