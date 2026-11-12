package com.quranwords.ui.update

import android.app.Activity
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.quranwords.core.util.Result
import com.quranwords.domain.model.AppUpdateResult
import com.quranwords.domain.repository.AppUpdateRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import javax.inject.Inject
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

/**
 * Banner state for Play in-app updates (Decision #29). The flow is collected by the
 * UI inside repeatOnLifecycle(STARTED) — see [observeUpdates]; the ViewModel never
 * touches Play Core and only reads Activity result constants, never a Context.
 */
@HiltViewModel
class UpdateViewModel @Inject constructor(
    private val repository: AppUpdateRepository,
) : ViewModel() {

    private val _uiState = MutableStateFlow(UpdateUiState())
    val uiState: StateFlow<UpdateUiState> = _uiState.asStateFlow()

    /**
     * Drives the banner from Play update states. Runs the cold repository flow until
     * cancelled — i.e. until the host lifecycle leaves STARTED — so every resume
     * re-queries Play fresh: downloads that finished while the app was backgrounded
     * surface as "restart to complete" immediately.
     */
    suspend fun observeUpdates(): Unit = coroutineScope {
        repository.checkUpdateAvailability().collect { result ->
            _uiState.update { current -> current.withUpdateResult(result) }
        }
    }

    fun onEvent(event: UpdateEvent) {
        when (event) {
            UpdateEvent.Dismiss -> _uiState.update {
                it.copy(isDismissed = true, banner = it.banner.hideIfDismissible())
            }

            UpdateEvent.RestartApp -> viewModelScope.launch {
                if (repository.completeUpdate() is Result.Error) {
                    _uiState.update { it.copy(banner = UpdateBannerState.Failed) }
                }
            }

            UpdateEvent.FlowNotStarted -> _uiState.update {
                it.copy(banner = UpdateBannerState.Failed)
            }

            is UpdateEvent.UpdateSheetResult -> updateWithSheetResult(event.resultCode)
        }
    }

    private fun updateWithSheetResult(resultCode: Int) {
        when (resultCode) {
            Activity.RESULT_OK -> _uiState.update { state ->
                // Listener refinements arrive immediately after; optimism avoids re-taps.
                state.copy(banner = state.banner.optimisticDownloading())
            }

            Activity.RESULT_CANCELED -> Unit // unchanged; the next resume re-checks

            else -> _uiState.update { it.copy(banner = UpdateBannerState.Failed) }
        }
    }

    private fun UpdateUiState.withUpdateResult(result: AppUpdateResult): UpdateUiState {
        val banner: UpdateBannerState = when (result) {
            AppUpdateResult.NotAvailable -> UpdateBannerState.Hidden
            AppUpdateResult.Available -> if (isDismissed) UpdateBannerState.Hidden else UpdateBannerState.Available
            is AppUpdateResult.InProgress -> UpdateBannerState.Downloading(result.percent)
            AppUpdateResult.Downloaded -> UpdateBannerState.RestartRequired
            is AppUpdateResult.Failed -> if (isDismissed) UpdateBannerState.Hidden else UpdateBannerState.Failed
        }
        // NotAvailable also clears the session dismissal so a future update is visible again.
        return if (result is AppUpdateResult.NotAvailable) {
            copy(banner = banner, isDismissed = false)
        } else {
            copy(banner = banner)
        }
    }

    private fun UpdateBannerState.hideIfDismissible(): UpdateBannerState =
        if (this is UpdateBannerState.Available || this is UpdateBannerState.Failed) UpdateBannerState.Hidden else this

    private fun UpdateBannerState.optimisticDownloading(): UpdateBannerState =
        if (this is UpdateBannerState.Available) UpdateBannerState.Downloading(0) else this
}
