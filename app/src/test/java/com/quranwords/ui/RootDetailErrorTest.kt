package com.quranwords.ui

import androidx.lifecycle.SavedStateHandle
import com.quranwords.fake.FakeQuranRepository
import com.quranwords.fake.FakeUserPreferences
import com.quranwords.ui.roots.detail.RootDetailEvent
import com.quranwords.ui.roots.detail.RootDetailViewModel
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
import org.junit.Assert.assertNull
import org.junit.Before
import org.junit.Test

@OptIn(ExperimentalCoroutinesApi::class)
class RootDetailErrorTest {

    private val dispatcher = UnconfinedTestDispatcher()

    @Before
    fun setUp() { Dispatchers.setMain(dispatcher) }

    @After
    fun tearDown() { Dispatchers.resetMain() }

    private class ThrowingRepository : FakeQuranRepository() {
        var failDetail = true

        override suspend fun getRootDetail(rootId: Int): Nothing? {
            if (failDetail) throw IllegalStateException("detail failed")
            return null
        }
    }

    @Test
    fun detail_load_error_is_exposed_and_retry_recovers() = runTest {
        val repo = ThrowingRepository()
        val vm = RootDetailViewModel(repo, FakeUserPreferences(), SavedStateHandle())

        vm.onEvent(RootDetailEvent.Load(1))
        advanceUntilIdle()

        assertEquals("detail failed", vm.uiState.value.error)
        assertFalse(vm.uiState.value.isLoading)

        repo.failDetail = false
        vm.onEvent(RootDetailEvent.Retry)
        advanceUntilIdle()

        assertNull(vm.uiState.value.error)
        assertFalse(vm.uiState.value.isLoading)
    }
}
