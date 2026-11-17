package com.quranwords.ui.surah.detail

import androidx.lifecycle.SavedStateHandle
import com.quranwords.core.util.AyahRef
import com.quranwords.domain.model.Ayah
import com.quranwords.fake.FakeQuranRepository
import com.quranwords.fake.FakeUserPreferences
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
    fun setup() { Dispatchers.setMain(testDispatcher) }

    @After
    fun tearDown() { Dispatchers.resetMain() }

    private fun pageAyah(surah: Int, num: Int, page: Int?) = Ayah(
        id = surah * 1000 + num, surah = surah, ayah = num,
        textUthmani = "نص", textUthmaniPlain = "نص", textImlaei = "نص",
        wordCount = 1, pageNumber = page
    )

    @Test
    fun `bookmark selection clears selection immediately and persists pair keys`() = runTest {
        val prefs = FakeUserPreferences()
        val vm = SurahDetailViewModel(FakeQuranRepository(), prefs)
        vm.onEvent(SurahDetailEvent.Load(2))
        advanceUntilIdle()

        vm.onEvent(SurahDetailEvent.EnterSelection(AyahRef(2, 3)))
        vm.onEvent(SurahDetailEvent.ToggleAyahSelection(AyahRef(2, 5)))
        assertTrue(vm.selection.value.isSelectionMode)

        vm.onEvent(SurahDetailEvent.BookmarkSelection)
        assertEquals(emptySet<AyahRef>(), vm.selection.value.selected)
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
        vm.onEvent(SurahDetailEvent.EnterSelection(AyahRef(2, 1)))
        vm.onEvent(SurahDetailEvent.ToggleAyahSelection(AyahRef(2, 2)))
        assertEquals(setOf(AyahRef(2, 1), AyahRef(2, 2)), vm.selection.value.selected)
        vm.onEvent(SurahDetailEvent.ClearSelection)
        assertEquals(emptySet<AyahRef>(), vm.selection.value.selected)
        assertFalse(vm.selection.value.isSelectionMode)
    }

    @Test
    fun `selection distinguishes identical ayah numbers in different surahs`() = runTest {
        val vm = SurahDetailViewModel(FakeQuranRepository(), FakeUserPreferences())
        vm.onEvent(SurahDetailEvent.EnterSelection(AyahRef(113, 1)))
        vm.onEvent(SurahDetailEvent.ToggleAyahSelection(AyahRef(114, 1)))
        assertEquals(setOf(AyahRef(113, 1), AyahRef(114, 1)), vm.selection.value.selected)
        // Range stays inside the anchor's surah (114): 113:1 is untouched.
        vm.onEvent(SurahDetailEvent.RangeSelect(AyahRef(114, 3)))
        assertEquals(
            setOf(AyahRef(113, 1), AyahRef(114, 1), AyahRef(114, 2), AyahRef(114, 3)),
            vm.selection.value.selected
        )
    }

    @Test
    fun `range select extends forward from the anchor`() = runTest {
        val vm = SurahDetailViewModel(FakeQuranRepository(), FakeUserPreferences())
        vm.onEvent(SurahDetailEvent.Load(2))
        advanceUntilIdle()
        vm.onEvent(SurahDetailEvent.EnterSelection(AyahRef(2, 3)))
        vm.onEvent(SurahDetailEvent.RangeSelect(AyahRef(2, 7)))
        assertEquals((3..7).mapTo(mutableSetOf()) { AyahRef(2, it) }, vm.selection.value.selected)
        assertTrue(vm.selection.value.isSelectionMode)
    }

    @Test
    fun `range select extends backward and keeps prior taps`() = runTest {
        val vm = SurahDetailViewModel(FakeQuranRepository(), FakeUserPreferences())
        vm.onEvent(SurahDetailEvent.Load(2))
        advanceUntilIdle()
        vm.onEvent(SurahDetailEvent.EnterSelection(AyahRef(2, 10)))
        vm.onEvent(SurahDetailEvent.ToggleAyahSelection(AyahRef(2, 4)))
        vm.onEvent(SurahDetailEvent.RangeSelect(AyahRef(2, 7)))
        assertEquals(
            setOf(AyahRef(2, 4), AyahRef(2, 7), AyahRef(2, 8), AyahRef(2, 9), AyahRef(2, 10)),
            vm.selection.value.selected
        )
    }

    @Test
    fun `entry page resolves from target ayah and preloads its window before ready`() = runTest {
        val active = listOf(pageAyah(4, 1, 50), pageAyah(4, 2, 51))
        val repo = FakeQuranRepository(
            ayatBySurah = mapOf(4 to active),
            ayatByPage = mapOf(
                50 to listOf(pageAyah(3, 199, 50), active.first()),
                51 to listOf(active.last(), pageAyah(5, 1, 51))
            )
        )
        val vm = SurahDetailViewModel(repo, FakeUserPreferences())
        vm.onEvent(SurahDetailEvent.Load(4, targetAyah = 2))
        advanceUntilIdle()

        val state = vm.screenState.value
        assertEquals(604, state.totalPages)
        assertEquals(51, state.entryPage)
        assertEquals(51, state.currentPage)
        assertTrue(state.isPagesReady)
        assertTrue(state.pagesByNumber.containsKey(51))
        assertTrue(repo.pageRangeFetchCalls.contains(49 to 49))
        assertTrue(repo.pageRangeFetchCalls.contains(51 to 51))
        assertTrue(repo.pageRangeFetchCalls.contains(50 to 51))
    }

    @Test
    fun `single-page Juz Amma surah keeps neighbours in both directions`() = runTest {
        val active = listOf(pageAyah(113, 1, 604), pageAyah(113, 2, 604))
        val repo = FakeQuranRepository(
            ayatBySurah = mapOf(113 to active),
            ayatByPage = mapOf(604 to listOf(pageAyah(112, 4, 604), pageAyah(114, 1, 604)) + active)
        )
        val vm = SurahDetailViewModel(repo, FakeUserPreferences())
        vm.onEvent(SurahDetailEvent.Load(113))
        advanceUntilIdle()

        val state = vm.screenState.value
        assertEquals(listOf(112, 113, 113, 114), state.pageRangeAyat.map { it.surah })
        assertEquals(604, state.entryPage)
        assertEquals(listOf(112, 113, 113, 114), state.pageAyahs.map { it.surah })
    }

    @Test
    fun `page fetch failure keeps scroll mode usable`() = runTest {
        val active = listOf(pageAyah(4, 1, 50), pageAyah(4, 2, 51))
        val repo = FakeQuranRepository(ayatBySurah = mapOf(4 to active), failGetAyatOfPage = true)
        val vm = SurahDetailViewModel(repo, FakeUserPreferences())
        vm.onEvent(SurahDetailEvent.Load(4))
        advanceUntilIdle()

        val state = vm.screenState.value
        assertEquals(emptyList<Ayah>(), state.pageRangeAyat)
        assertEquals(listOf(1, 2), state.ayat.map { it.ayah })
        assertFalse(state.isPagesReady)
    }

    @Test
    fun `unpaged surah uses synthetic pages without DB page queries`() = runTest {
        val repo = FakeQuranRepository(
            ayatBySurah = mapOf(4 to listOf(pageAyah(4, 1, null), pageAyah(4, 2, null)))
        )
        val vm = SurahDetailViewModel(repo, FakeUserPreferences())
        vm.onEvent(SurahDetailEvent.Load(4))
        advanceUntilIdle()

        val state = vm.screenState.value
        assertTrue(repo.pageRangeFetchCalls.isEmpty())
        assertEquals(1, state.totalPages)
        assertEquals(1, state.entryPage)
        assertTrue(state.pagesByNumber[1]?.size == 2)
    }

    @Test
    fun `page change prefetches the window around the settled page`() = runTest {
        val repo = FakeQuranRepository(
            ayatBySurah = mapOf(4 to listOf(pageAyah(4, 1, 50))),
            ayatByPage = (45..55).associateWith { page -> listOf(pageAyah(4, 1, page)) }
        )
        val vm = SurahDetailViewModel(repo, FakeUserPreferences())
        vm.onEvent(SurahDetailEvent.Load(4, targetAyah = 1))
        advanceUntilIdle()
        repo.pageRangeFetchCalls.clear()

        vm.onEvent(SurahDetailEvent.PageChanged(54))
        advanceUntilIdle()
        val fetched = repo.pageRangeFetchCalls.map { it.first }.toSet()
        // 52 was already in the entry window; only genuinely missing pages load.
        assertTrue(fetched.containsAll(setOf(53, 54, 55, 56)))
        assertFalse(fetched.contains(48))
    }

    @Test
    fun `immersive state comes from the route and is not reset by load`() = runTest {
        val repo = FakeQuranRepository(ayatBySurah = mapOf(4 to listOf(pageAyah(4, 1, 50))))
        val handle = SavedStateHandle(mapOf("initialImmersive" to true))
        val vm = SurahDetailViewModel(repo, FakeUserPreferences(), handle)

        assertTrue(vm.screenState.value.isImmersive)
        vm.onEvent(SurahDetailEvent.Load(4))
        advanceUntilIdle()
        assertTrue(vm.screenState.value.isImmersive)

        vm.onEvent(SurahDetailEvent.SetImmersive(false))
        assertFalse(vm.screenState.value.isImmersive)
        // Rotation-safe: the route input key is never overwritten by a toggle.
        assertEquals(true, handle.get<Boolean>("initialImmersive"))
        assertEquals(false, handle.get<Boolean>("immersiveState"))
    }

    @Test
    fun `mode flip re-anchors the page window before mounting the pager`() = runTest {
        val repo = FakeQuranRepository(
            ayatBySurah = mapOf(4 to listOf(pageAyah(4, 1, 50))),
            ayatByPage = mapOf(60 to listOf(pageAyah(5, 1, 60)))
        )
        val vm = SurahDetailViewModel(repo, FakeUserPreferences())
        vm.onEvent(SurahDetailEvent.Load(4))
        advanceUntilIdle()
        assertFalse(vm.screenState.value.isPagesReady)

        vm.onEvent(SurahDetailEvent.SetEntryPage(60))
        assertTrue(vm.screenState.value.entryPage == 60)
        assertFalse(vm.screenState.value.isPagesReady) // remount gate
        advanceUntilIdle()
        val state = vm.screenState.value
        assertTrue(state.isPagesReady)
        assertEquals(listOf(5), state.pageAyahs.map { it.surah })
    }
}
