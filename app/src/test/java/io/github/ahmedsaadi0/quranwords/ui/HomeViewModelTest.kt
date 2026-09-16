package io.github.ahmedsaadi0.quranwords.ui

import app.cash.turbine.test
import io.github.ahmedsaadi0.quranwords.fake.FakeQuranRepository
import io.github.ahmedsaadi0.quranwords.fake.FakeUserPreferences
import io.github.ahmedsaadi0.quranwords.ui.home.HomeViewModel
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Before
import org.junit.Test

@OptIn(ExperimentalCoroutinesApi::class)
class HomeViewModelTest {

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
    fun `emits initial last-read from preferences`() = runTest {
        val vm = HomeViewModel(
            FakeQuranRepository(),
            FakeUserPreferences(initialLastReadSurah = 2, initialLastReadAyah = 142)
        )
        advanceUntilIdle()

        vm.uiState.test {
            val state = awaitItem()
            assertEquals(2, state.lastReadSurah)
            assertEquals(142, state.lastReadAyah)
        }
    }

    @Test
    fun `last-read updates reactively without reload`() = runTest {
        val prefs = FakeUserPreferences(initialLastReadSurah = 1, initialLastReadAyah = 1)
        val vm = HomeViewModel(FakeQuranRepository(), prefs)
        advanceUntilIdle()

        vm.uiState.test {
            val initial = awaitItem()
            assertEquals(1, initial.lastReadSurah)
            assertEquals(1, initial.lastReadAyah)

            prefs.setLastRead(2, 50)
            val updated = awaitItem()
            assertEquals(2, updated.lastReadSurah)
            assertEquals(50, updated.lastReadAyah)
        }
    }
}
