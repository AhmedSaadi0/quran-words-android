package io.github.ahmedsaadi0.quranwords.ui.surah.detail

import io.github.ahmedsaadi0.quranwords.fake.FakeQuranRepository
import io.github.ahmedsaadi0.quranwords.fake.FakeUserPreferences
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
class SurahDetailViewModelTest {

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
    fun `bookmark selection clears selection immediately and persists keys`() = runTest {
        val prefs = FakeUserPreferences()
        val vm = SurahDetailViewModel(FakeQuranRepository(), prefs)
        vm.onEvent(SurahDetailEvent.Load(2))
        advanceUntilIdle()

        vm.onEvent(SurahDetailEvent.EnterSelection(3))
        vm.onEvent(SurahDetailEvent.ToggleAyahSelection(5))
        assertTrue(vm.selection.value.isSelectionMode)

        vm.onEvent(SurahDetailEvent.BookmarkSelection)

        // Optimistic clear: synchronous, before the background writes finish.
        assertEquals(emptySet<Int>(), vm.selection.value.selectedIds)
        assertFalse(vm.selection.value.isSelectionMode)

        advanceUntilIdle()
        assertEquals(setOf("2:3", "2:5"), prefs.ayahBookmarkState.value)
    }

    @Test
    fun `bookmark selection with empty selection does nothing`() = runTest {
        val prefs = FakeUserPreferences()
        val vm = SurahDetailViewModel(FakeQuranRepository(), prefs)
        vm.onEvent(SurahDetailEvent.Load(2))
        advanceUntilIdle()

        vm.onEvent(SurahDetailEvent.BookmarkSelection)
        advanceUntilIdle()

        assertEquals(emptySet<String>(), prefs.ayahBookmarkState.value)
        assertFalse(vm.selection.value.isSelectionMode)
    }

    @Test
    fun `clear selection event empties an active selection`() = runTest {
        val vm = SurahDetailViewModel(FakeQuranRepository(), FakeUserPreferences())

        vm.onEvent(SurahDetailEvent.EnterSelection(1))
        vm.onEvent(SurahDetailEvent.ToggleAyahSelection(2))
        assertEquals(setOf(1, 2), vm.selection.value.selectedIds)

        vm.onEvent(SurahDetailEvent.ClearSelection)

        assertEquals(emptySet<Int>(), vm.selection.value.selectedIds)
        assertFalse(vm.selection.value.isSelectionMode)
    }

    @Test
    fun `range select extends forward from the anchor immediately`() = runTest {
        val vm = SurahDetailViewModel(FakeQuranRepository(), FakeUserPreferences())
        vm.onEvent(SurahDetailEvent.Load(2))
        advanceUntilIdle()

        vm.onEvent(SurahDetailEvent.EnterSelection(3))
        vm.onEvent(SurahDetailEvent.RangeSelect(7))

        // Ids apply instantly, even beyond loaded pages.
        assertEquals(setOf(3, 4, 5, 6, 7), vm.selection.value.selectedIds)
        assertTrue(vm.selection.value.isSelectionMode)
        advanceUntilIdle()
    }

    @Test
    fun `range select extends backward and keeps prior taps`() = runTest {
        val vm = SurahDetailViewModel(FakeQuranRepository(), FakeUserPreferences())
        vm.onEvent(SurahDetailEvent.Load(2))
        advanceUntilIdle()

        vm.onEvent(SurahDetailEvent.EnterSelection(10))
        vm.onEvent(SurahDetailEvent.ToggleAyahSelection(4))
        vm.onEvent(SurahDetailEvent.RangeSelect(7))

        assertEquals(setOf(4, 7, 8, 9, 10), vm.selection.value.selectedIds)
        advanceUntilIdle()
    }
}
