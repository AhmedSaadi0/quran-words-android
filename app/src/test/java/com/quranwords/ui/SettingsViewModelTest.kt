package com.quranwords.ui

import com.quranwords.core.util.DARK_MODE_DARK
import com.quranwords.core.util.DARK_MODE_LIGHT
import com.quranwords.core.util.DARK_MODE_SYSTEM
import com.quranwords.core.util.LanguageManager
import com.quranwords.core.util.nextDarkMode
import com.quranwords.fake.FakeUserPreferences
import com.quranwords.ui.settings.SettingsEvent
import com.quranwords.ui.settings.SettingsViewModel
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
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@OptIn(ExperimentalCoroutinesApi::class)
@RunWith(RobolectricTestRunner::class)
@Config(sdk = [36])
class SettingsViewModelTest {

    private val dispatcher = UnconfinedTestDispatcher()

    @Before
    fun setUp() { Dispatchers.setMain(dispatcher) }

    @After
    fun tearDown() { Dispatchers.resetMain() }

    private fun viewModel(prefs: FakeUserPreferences) = SettingsViewModel(prefs, LanguageManager())

    @Test
    fun toggle_event_cycles_all_three_modes_and_dialog_event_sets_exact_mode() = runTest {
        assertEquals(DARK_MODE_DARK, nextDarkMode(DARK_MODE_SYSTEM))
        assertEquals(DARK_MODE_LIGHT, nextDarkMode(DARK_MODE_DARK))
        assertEquals(DARK_MODE_SYSTEM, nextDarkMode(DARK_MODE_LIGHT))

        val prefs = FakeUserPreferences()
        val vm = viewModel(prefs)
        advanceUntilIdle()

        vm.onEvent(SettingsEvent.ToggleDarkMode)
        advanceUntilIdle()
        assertEquals(DARK_MODE_DARK, prefs.darkModeState.value)

        vm.onEvent(SettingsEvent.DarkModeChanged(DARK_MODE_LIGHT))
        advanceUntilIdle()
        assertEquals(DARK_MODE_LIGHT, prefs.darkModeState.value)

        vm.onEvent(SettingsEvent.ToggleDynamicColor)
        advanceUntilIdle()
        assertEquals(true, prefs.dynamicColorState.value)
    }
}
