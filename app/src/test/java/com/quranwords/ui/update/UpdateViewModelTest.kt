package com.quranwords.ui.update

import android.app.Activity
import com.quranwords.core.util.Result
import com.quranwords.domain.model.AppUpdateResult
import com.quranwords.domain.model.UpdateFailureReason
import com.quranwords.domain.repository.AppUpdateRepository
import kotlinx.coroutines.BufferOverflow
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.test.TestScope
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
class UpdateViewModelTest {

    private val mainDispatcher = UnconfinedTestDispatcher()

    @Before
    fun setup() {
        Dispatchers.setMain(mainDispatcher)
    }

    @After
    fun tearDown() {
        Dispatchers.resetMain()
    }

    /** Latest-wins fake of Play update states (mirrors the conflated repository flow). */
    private class RecordingUpdateRepository : AppUpdateRepository {
        val emitted = MutableSharedFlow<AppUpdateResult>(
            replay = 1,
            onBufferOverflow = BufferOverflow.DROP_OLDEST,
        )
        var completeCalls = 0
        var completeOutcome: Result<Unit> = Result.Success(Unit)

        override fun checkUpdateAvailability(): Flow<AppUpdateResult> = emitted

        override suspend fun completeUpdate(): Result<Unit> {
            completeCalls++
            return completeOutcome
        }
    }

    private fun RecordingUpdateRepository.emit(result: AppUpdateResult) {
        emitted.tryEmit(result)
    }

    /** Stands in for the repeatOnLifecycle(STARTED) collection the handler performs. */
    private fun TestScope.observe(repository: RecordingUpdateRepository): UpdateViewModel {
        val viewModel = UpdateViewModel(repository)
        backgroundScope.launch(UnconfinedTestDispatcher(testScheduler)) {
            viewModel.observeUpdates()
        }
        return viewModel
    }

    @Test
    fun `available result shows the available banner`() = runTest {
        val repository = RecordingUpdateRepository()
        val viewModel = observe(repository)

        repository.emit(AppUpdateResult.Available)

        assertEquals(UpdateBannerState.Available, viewModel.uiState.value.banner)
    }

    @Test
    fun `in progress result shows the downloading banner with percent`() = runTest {
        val repository = RecordingUpdateRepository()
        val viewModel = observe(repository)

        repository.emit(AppUpdateResult.InProgress(72))

        assertEquals(UpdateBannerState.Downloading(72), viewModel.uiState.value.banner)
    }

    @Test
    fun `downloaded result shows the restart prompt`() = runTest {
        val repository = RecordingUpdateRepository()
        val viewModel = observe(repository)

        repository.emit(AppUpdateResult.Downloaded)

        assertEquals(UpdateBannerState.RestartRequired, viewModel.uiState.value.banner)
    }

    @Test
    fun `failed result shows the failed banner`() = runTest {
        val repository = RecordingUpdateRepository()
        val viewModel = observe(repository)

        repository.emit(AppUpdateResult.Failed(UpdateFailureReason.CHECK_FAILED))

        assertEquals(UpdateBannerState.Failed, viewModel.uiState.value.banner)
    }

    @Test
    fun `dismiss hides the failed banner and later failures stay hidden`() = runTest {
        val repository = RecordingUpdateRepository()
        val viewModel = observe(repository)

        repository.emit(AppUpdateResult.Failed(UpdateFailureReason.CHECK_FAILED))
        assertEquals(UpdateBannerState.Failed, viewModel.uiState.value.banner)

        viewModel.onEvent(UpdateEvent.Dismiss)
        assertEquals(UpdateBannerState.Hidden, viewModel.uiState.value.banner)

        // A fresh failure while dismissed must not re-show until NotAvailable resets the session.
        repository.emit(AppUpdateResult.Failed(UpdateFailureReason.DOWNLOAD_FAILED))
        assertEquals(UpdateBannerState.Hidden, viewModel.uiState.value.banner)
    }

    @Test
    fun `not available clears the dismissal so a future update is visible again`() = runTest {
        val repository = RecordingUpdateRepository()
        val viewModel = observe(repository)

        repository.emit(AppUpdateResult.Available)
        viewModel.onEvent(UpdateEvent.Dismiss)
        assertEquals(UpdateBannerState.Hidden, viewModel.uiState.value.banner)

        repository.emit(AppUpdateResult.NotAvailable)
        repository.emit(AppUpdateResult.Available)

        assertEquals(UpdateBannerState.Available, viewModel.uiState.value.banner)
    }

    @Test
    fun `dismiss hides the available banner but the restart prompt still shows`() = runTest {
        val repository = RecordingUpdateRepository()
        val viewModel = observe(repository)

        repository.emit(AppUpdateResult.Available)
        viewModel.onEvent(UpdateEvent.Dismiss)
        assertEquals(UpdateBannerState.Hidden, viewModel.uiState.value.banner)

        repository.emit(AppUpdateResult.Downloaded)

        assertEquals(UpdateBannerState.RestartRequired, viewModel.uiState.value.banner)
    }

    @Test
    fun `sheet ok result optimistically shows downloading then progress refines`() = runTest {
        val repository = RecordingUpdateRepository()
        val viewModel = observe(repository)
        repository.emit(AppUpdateResult.Available)

        viewModel.onEvent(UpdateEvent.UpdateSheetResult(Activity.RESULT_OK))
        assertEquals(UpdateBannerState.Downloading(0), viewModel.uiState.value.banner)

        repository.emit(AppUpdateResult.InProgress(42))
        assertEquals(UpdateBannerState.Downloading(42), viewModel.uiState.value.banner)
    }

    @Test
    fun `sheet canceled result keeps the available banner`() = runTest {
        val repository = RecordingUpdateRepository()
        val viewModel = observe(repository)
        repository.emit(AppUpdateResult.Available)

        viewModel.onEvent(UpdateEvent.UpdateSheetResult(Activity.RESULT_CANCELED))

        assertEquals(UpdateBannerState.Available, viewModel.uiState.value.banner)
    }

    @Test
    fun `sheet failure result shows the failed banner`() = runTest {
        val repository = RecordingUpdateRepository()
        val viewModel = observe(repository)
        repository.emit(AppUpdateResult.Available)

        // Same value space as ActivityResult.RESULT_IN_APP_UPDATE_FAILED.
        viewModel.onEvent(UpdateEvent.UpdateSheetResult(2))

        assertEquals(UpdateBannerState.Failed, viewModel.uiState.value.banner)
    }

    @Test
    fun `flow not started shows the failed banner`() = runTest {
        val repository = RecordingUpdateRepository()
        val viewModel = observe(repository)
        repository.emit(AppUpdateResult.Available)

        viewModel.onEvent(UpdateEvent.FlowNotStarted)

        assertEquals(UpdateBannerState.Failed, viewModel.uiState.value.banner)
    }

    @Test
    fun `restart event completes through the repository and keeps the prompt`() = runTest {
        val repository = RecordingUpdateRepository()
        val viewModel = observe(repository)
        repository.emit(AppUpdateResult.Downloaded)

        viewModel.onEvent(UpdateEvent.RestartApp)
        advanceUntilIdle()

        assertEquals(1, repository.completeCalls)
        assertEquals(UpdateBannerState.RestartRequired, viewModel.uiState.value.banner)
    }

    @Test
    fun `restart failure surfaces the failed banner`() = runTest {
        val repository = RecordingUpdateRepository()
        repository.completeOutcome = Result.Error("complete_update_failed")
        val viewModel = observe(repository)
        repository.emit(AppUpdateResult.Downloaded)

        viewModel.onEvent(UpdateEvent.RestartApp)
        advanceUntilIdle()

        assertEquals(UpdateBannerState.Failed, viewModel.uiState.value.banner)
    }
}
