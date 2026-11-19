package com.quranwords.ui.update

import com.quranwords.domain.model.DbInstalledVersion
import com.quranwords.domain.model.DbReleaseInfo
import com.quranwords.domain.model.DbUpdateState
import com.quranwords.domain.repository.DbCheckResult
import com.quranwords.domain.repository.DbUpdateRepository
import com.quranwords.domain.usecase.CheckDbUpdateUseCase
import com.quranwords.ui.appstate.DbUpdateViewModel
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
class DbUpdateViewModelTest {

    private val dispatcher = UnconfinedTestDispatcher()

    @Before
    fun setUp() { Dispatchers.setMain(dispatcher) }

    @After
    fun tearDown() { Dispatchers.resetMain() }

    private class FailingRepository : DbUpdateRepository {
        override suspend fun getInstalledVersion(): DbInstalledVersion = DbInstalledVersion(1, "v1")
        override suspend fun setInstalledVersion(code: Int, name: String) = Unit
        override suspend fun getDismissedVersionCode(): Int = 0
        override suspend fun setDismissedVersionCode(code: Int) = Unit
        override suspend fun getLatestRelease(): DbCheckResult<DbReleaseInfo> = DbCheckResult.Error("boom")
        override suspend fun checkForUpdate(): DbCheckResult<DbUpdateState> = DbCheckResult.Error("boom")
    }

    @Test
    fun error_state_is_exposed_for_retry() = runTest {
        val repo = FailingRepository()
        val vm = DbUpdateViewModel(CheckDbUpdateUseCase(repo), repo)

        vm.checkOnce()
        advanceUntilIdle()

        assertEquals(DbUpdateState.CheckFailed("boom"), vm.state.value)
        assertEquals(false, vm.isChecking.value)
    }
}
