package com.quranwords.ui

import com.quranwords.domain.model.DbInstalledVersion
import com.quranwords.domain.model.DbReleaseInfo
import com.quranwords.domain.model.DbUpdateState
import com.quranwords.domain.model.DownloadPhase
import com.quranwords.domain.model.DownloadState
import com.quranwords.domain.repository.DatabaseDownloadRepository
import com.quranwords.domain.repository.DbCheckResult as RepoCheckResult
import com.quranwords.domain.repository.DbUpdateRepository
import com.quranwords.fake.FakeQuranRepository
import com.quranwords.ui.setup.DatabaseSetupViewModel
import com.quranwords.ui.setup.SetupDownload
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Before
import org.junit.Test

@OptIn(ExperimentalCoroutinesApi::class)
class DatabaseSetupViewModelTest {

    private val dispatcher = UnconfinedTestDispatcher()

    @Before
    fun setUp() { Dispatchers.setMain(dispatcher) }

    @After
    fun tearDown() { Dispatchers.resetMain() }

    private class FakeDownloadRepository(
        private val ready: Boolean = false
    ) : DatabaseDownloadRepository {
        override fun isDatabaseReady(): Boolean = ready
        override fun downloadRelease(info: DbReleaseInfo) = flow {
            emit(DownloadState.Progress(10, 100L, 1000L, 50L, DownloadPhase.DOWNLOAD))
            emit(DownloadState.Completed(info.versionCode, info.versionName))
        }
        override fun importDatabase(uri: String) = flow { emit(DownloadState.Completed()) }
    }

    private class FakeUpdateRepository : DbUpdateRepository {
        var fail = false
        override suspend fun getInstalledVersion(): DbInstalledVersion = DbInstalledVersion(1, "v1")
        override suspend fun setInstalledVersion(code: Int, name: String) = Unit
        override suspend fun getDismissedVersionCode(): Int = 0
        override suspend fun setDismissedVersionCode(code: Int) = Unit
        override suspend fun getLatestRelease(): RepoCheckResult<DbReleaseInfo> =
            if (fail) RepoCheckResult.Error("manifest boom") else RepoCheckResult.Success(
                DbReleaseInfo(2, "v2", "https://x/db.zip")
            )
        override suspend fun checkForUpdate(): RepoCheckResult<DbUpdateState> =
            RepoCheckResult.Success(DbUpdateState.Unknown)
    }

    private fun viewModel(update: FakeUpdateRepository) = DatabaseSetupViewModel(
        FakeDownloadRepository(), update, FakeQuranRepository()
    )

    @Test
    fun progress_and_completed_states_map_to_ui_without_data_types() = runTest {
        val vm = viewModel(FakeUpdateRepository())
        vm.onEvent(com.quranwords.ui.setup.SetupEvent.StartDownload)
        advanceUntilIdle()

        val state = vm.uiState.value
        assertEquals(SetupDownload.Completed(2, "v2"), state.download)
    }

    @Test
    fun manifest_check_error_is_exposed_and_retry_clears_it() = runTest {
        val update = FakeUpdateRepository().apply { fail = true }
        val vm = viewModel(update)

        vm.onEvent(com.quranwords.ui.setup.SetupEvent.CheckForUpdate)
        advanceUntilIdle()
        assertEquals("manifest boom", vm.uiState.value.manifestError)

        update.fail = false
        vm.onEvent(com.quranwords.ui.setup.SetupEvent.CheckForUpdate)
        advanceUntilIdle()
        assertNull(vm.uiState.value.manifestError)
    }
}
