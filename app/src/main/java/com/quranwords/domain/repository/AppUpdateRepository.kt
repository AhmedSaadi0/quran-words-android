package com.quranwords.domain.repository

import com.quranwords.core.util.Result
import com.quranwords.domain.model.AppUpdateResult
import kotlinx.coroutines.flow.Flow

/**
 * Play in-app updates contract (Decision #29). Pure Kotlin — the ActivityResult
 * launch port lives in `ui/update/[AppUpdateFlowLauncher]` and is implemented by
 * the same data source as this interface (DIP: both contracts on one implementation).
 */
interface AppUpdateRepository {
    /**
     * Cold flow: every subscription performs a fresh Play availability check and
     * stays subscribed to install-state updates until cancellation. Collected under
     * `repeatOnLifecycle(STARTED)`, each resume re-detects `InstallStatus.DOWNLOADED`
     * that finished while the app was backgrounded.
     *
     * [AppUpdateResult.Failed] is the failure carrier — failures are never hidden.
     */
    fun checkUpdateAvailability(): Flow<AppUpdateResult>

    /** Completes a downloaded flexible update; Play restarts the app with the new build. */
    suspend fun completeUpdate(): Result<Unit>
}
