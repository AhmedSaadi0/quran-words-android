package io.github.ahmedsaadi0.quranwords.ui

import app.cash.turbine.test
import io.github.ahmedsaadi0.quranwords.domain.model.Ayah
import io.github.ahmedsaadi0.quranwords.domain.model.RootDetail
import io.github.ahmedsaadi0.quranwords.domain.model.RootItem
import io.github.ahmedsaadi0.quranwords.domain.model.WordToken
import io.github.ahmedsaadi0.quranwords.fake.FakeQuranRepository
import io.github.ahmedsaadi0.quranwords.fake.FakeUserPreferences
import io.github.ahmedsaadi0.quranwords.ui.mushaf.MushafReaderEvent
import io.github.ahmedsaadi0.quranwords.ui.mushaf.MushafReaderViewModel
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
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test

@OptIn(ExperimentalCoroutinesApi::class)
class MushafReaderViewModelTest {

  private val testDispatcher = UnconfinedTestDispatcher()

  @Before
  fun setup() {
    Dispatchers.setMain(testDispatcher)
  }

  @After
  fun tearDown() {
    Dispatchers.resetMain()
  }

  private fun word(wordAyahId: Int, position: Int, text: String, rootId: Int? = null) = WordToken(
    wordId = wordAyahId,
    wordAyahId = wordAyahId,
    position = position,
    text = text,
    textClean = text,
    translation = "",
    rootId = rootId,
    rootText = if (rootId != null) "كتب" else null,
    pos = null,
    posNameAr = null,
    form = null,
    formNameAr = null,
    aspect = null,
    mood = null,
    voice = null,
    person = null,
    gender = null,
    number = null,
    grammaticalCase = null,
    state = null,
    derivation = null,
    special = null,
    segments = null
  )

  private fun pageAyah(surah: Int, num: Int, page: Int, words: List<WordToken> = emptyList()) = Ayah(
    id = page * 100 + num,
    surah = surah,
    ayah = num,
    textUthmani = "t$num",
    textUthmaniPlain = "p$num",
    textImlaei = "i$num",
    wordCount = words.size,
    words = words,
    juz = 1,
    hizb = 1,
    rubElHizb = 1,
    pageNumber = page
  )

  private fun tenPages(): Map<Int, List<Ayah>> =
    (1..10).associateWith { page -> listOf(pageAyah(surah = 2, num = page, page = page)) }

  @Test
  fun `openAtPage loads preload window and resolves initial state`() = runTest {
    val vm = MushafReaderViewModel(FakeQuranRepository(pages = tenPages()), FakeUserPreferences())
    advanceUntilIdle()

    vm.onEvent(MushafReaderEvent.OpenAtPage(5))
    advanceUntilIdle()

    val state = vm.uiState.value
    assertEquals(5, state.initialPage)
    assertEquals(5, state.currentPage)
    assertFalse(state.isInitialLoad)
    assertEquals(setOf(3, 4, 5, 6, 7), state.pages.keys)
    assertEquals(604, state.totalPages)
    assertTrue(state.loadingPages.isEmpty())
  }

  @Test
  fun `openAtPage clamps out-of-range page`() = runTest {
    val vm = MushafReaderViewModel(
      FakeQuranRepository(pages = tenPages(), mushafPageCount = 10),
      FakeUserPreferences()
    )
    advanceUntilIdle()

    vm.onEvent(MushafReaderEvent.OpenAtPage(99))
    advanceUntilIdle()

    val state = vm.uiState.value
    assertEquals(10, state.initialPage)
    assertEquals(10, state.totalPages)
    assertTrue(state.pages.keys.all { it in 8..10 })
  }

  @Test
  fun `pageSettled preloads forward evicts far pages and writes last-read`() = runTest {
    val prefs = FakeUserPreferences()
    val vm = MushafReaderViewModel(FakeQuranRepository(pages = tenPages(), mushafPageCount = 10), prefs)
    advanceUntilIdle()

    vm.onEvent(MushafReaderEvent.OpenAtPage(3))
    advanceUntilIdle()
    vm.onEvent(MushafReaderEvent.PageSettled(8))
    advanceUntilIdle()

    val state = vm.uiState.value
    assertEquals(8, state.currentPage)
    assertTrue(state.pages.keys.containsAll(setOf(6, 7, 8, 9, 10)))
    assertFalse(state.pages.containsKey(1))
    assertTrue(state.pages.containsKey(2))
    assertEquals(2, prefs.lastReadSurahState.value)
    assertEquals(8, prefs.lastReadAyahState.value)
  }

  @Test
  fun `pageTapped toggles immersive bars`() = runTest {
    val vm = MushafReaderViewModel(FakeQuranRepository(), FakeUserPreferences())
    advanceUntilIdle()

    vm.uiState.test {
      assertFalse(awaitItem().isImmersive)
      vm.onEvent(MushafReaderEvent.PageTapped)
      assertTrue(awaitItem().isImmersive)
      vm.onEvent(MushafReaderEvent.PageTapped)
      assertFalse(awaitItem().isImmersive)
    }
  }

  @Test
  fun `wordTapped selects word and loads ai summary`() = runTest {
    val pages = mapOf(5 to listOf(pageAyah(2, 5, 5, listOf(word(501, 1, "كلمة", rootId = 7)))))
    val detail = RootDetail(item = RootItem(id = 7, root = "كتب"), aiSummary = "sum", aiModel = "m", aiGeneratedAt = "d")
    val vm = MushafReaderViewModel(FakeQuranRepository(pages = pages, rootDetail = detail), FakeUserPreferences())
    advanceUntilIdle()

    vm.onEvent(MushafReaderEvent.OpenAtPage(5))
    advanceUntilIdle()
    vm.onEvent(MushafReaderEvent.WordTapped(wordAyahId = 501, position = 1))
    advanceUntilIdle()

    val state = vm.uiState.value
    assertEquals("كلمة", state.selectedWord?.text)
    assertEquals(5, state.selectedWordAyah?.ayah)
    assertEquals("sum", state.aiSummary)
    assertFalse(state.isAiLoading)

    vm.onEvent(MushafReaderEvent.DismissWord)
    assertNull(vm.uiState.value.selectedWord)
  }

  @Test
  fun `markerTapped toggles ayah bookmark`() = runTest {
    val prefs = FakeUserPreferences()
    val vm = MushafReaderViewModel(FakeQuranRepository(pages = tenPages()), prefs)
    advanceUntilIdle()

    vm.onEvent(MushafReaderEvent.MarkerTapped(surahId = 2, ayah = 5))
    advanceUntilIdle()

    assertTrue(prefs.ayahBookmarkState.value.contains("2:5"))
  }

  @Test
  fun `openAtAyah resolves page from repository`() = runTest {
    val pages = mapOf(7 to listOf(pageAyah(2, 5, 7)))
    val vm = MushafReaderViewModel(FakeQuranRepository(pages = pages), FakeUserPreferences())
    advanceUntilIdle()

    vm.onEvent(MushafReaderEvent.OpenAtAyah(surahId = 2, ayah = 5))
    advanceUntilIdle()

    assertEquals(7, vm.uiState.value.initialPage)
    assertTrue(vm.uiState.value.pages.containsKey(7))
  }

  @Test
  fun `openAtAyah with unknown ayah sets error`() = runTest {
    val vm = MushafReaderViewModel(FakeQuranRepository(), FakeUserPreferences())
    advanceUntilIdle()

    vm.onEvent(MushafReaderEvent.OpenAtAyah(surahId = 2, ayah = 999))
    advanceUntilIdle()

    assertNotNull(vm.uiState.value.error)
    assertEquals(1, vm.uiState.value.initialPage)
  }

  @Test
  fun `failed page load surfaces error`() = runTest {
    val repo = FakeQuranRepository(pages = tenPages())
    repo.failPageLoad = true
    val vm = MushafReaderViewModel(repo, FakeUserPreferences())
    advanceUntilIdle()

    vm.onEvent(MushafReaderEvent.OpenAtPage(5))
    advanceUntilIdle()

    assertNotNull(vm.uiState.value.error)
    assertTrue(vm.uiState.value.pages.isEmpty())
  }
}
