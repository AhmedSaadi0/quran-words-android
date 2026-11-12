package com.quranwords.data.remote

import android.util.Log
import androidx.activity.result.ActivityResultLauncher
import androidx.activity.result.IntentSenderRequest
import com.google.android.gms.tasks.Task
import com.google.android.play.core.appupdate.AppUpdateInfo
import com.google.android.play.core.appupdate.AppUpdateManager
import com.google.android.play.core.appupdate.AppUpdateOptions
import com.google.android.play.core.install.InstallState
import com.google.android.play.core.install.InstallStateUpdatedListener
import com.google.android.play.core.install.model.AppUpdateType
import com.google.android.play.core.install.model.InstallStatus
import com.google.android.play.core.install.model.UpdateAvailability
import com.quranwords.core.util.Result
import com.quranwords.core.util.runCatchingResult
import com.quranwords.domain.model.AppUpdateResult
import com.quranwords.domain.model.UpdateFailureReason
import com.quranwords.domain.repository.AppUpdateRepository
import com.quranwords.ui.update.AppUpdateFlowLauncher
import javax.inject.Inject
import javax.inject.Singleton
import kotlin.coroutines.resume
import kotlin.coroutines.resumeWithException
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow
import kotlinx.coroutines.suspendCancellableCoroutine

/**
 * Single Play Core touchpoint for in-app updates (Decision #29):
 * - checkUpdateAvailability: cold callbackFlow — the install-state listener is
 *   registered exactly once per subscription and unregistered in `awaitClose`, so
 *   collecting under repeatOnLifecycle(STARTED) never leaks a listener and every
 *   resume re-runs a fresh availability check (downloads that finished while the
 *   app was backgrounded are re-detected immediately).
 * - startFlexibleUpdate: launches Play's flexible sheet THROUGH the
 *   ActivityResultLauncher owned by the composition — this layer never touches an
 *   Activity, and every attempt fetches a fresh AppUpdateInfo (they are single-use).
 * Failures map to AppUpdateResult.Failed — no silent catches (AGENTS §13).
 *
 * Note: Play app-update 2.1 returns GMS `com.google.android.gms.tasks.Task`
 * (not `com.google.android.play.core.tasks.Task`).
 */
@Singleton
class PlayAppUpdateDataSource @Inject constructor(
    private val appUpdateManager: AppUpdateManager,
) : AppUpdateRepository, AppUpdateFlowLauncher {

    override fun checkUpdateAvailability(): Flow<AppUpdateResult> = callbackFlow {
        val listener = InstallStateUpdatedListener { state ->
            state.toAppUpdateResultOrNull()?.let(::trySend)
        }
        appUpdateManager.registerListener(listener)

        try {
            trySend(appUpdateManager.appUpdateInfo.await().toAppUpdateResult())
        } catch (cancellation: CancellationException) {
            throw cancellation
        } catch (throwable: Exception) {
            Log.w(TAG, "Availability check failed", throwable)
            trySend(AppUpdateResult.Failed(UpdateFailureReason.CHECK_FAILED))
        }

        awaitClose { appUpdateManager.unregisterListener(listener) }
    }

    override suspend fun startFlexibleUpdate(
        launcher: ActivityResultLauncher<IntentSenderRequest>,
    ): Boolean = try {
        // AppUpdateInfo instances are single-use: always fetch a fresh one for retries.
        val info = appUpdateManager.appUpdateInfo.await()
        val options = AppUpdateOptions.newBuilder(AppUpdateType.FLEXIBLE).build()
        val startable = info.updateAvailability() == UpdateAvailability.UPDATE_AVAILABLE &&
            info.isUpdateTypeAllowed(AppUpdateType.FLEXIBLE)
        if (startable) appUpdateManager.startUpdateFlowForResult(info, launcher, options) else false
    } catch (cancellation: CancellationException) {
        throw cancellation
    } catch (throwable: Exception) {
        Log.w(TAG, "Start flexible update failed", throwable)
        false
    }

    override suspend fun completeUpdate(): Result<Unit> =
        when (val outcome = runCatchingResult<Unit> { appUpdateManager.completeUpdate().await() }) {
            is Result.Success -> outcome
            is Result.Error -> {
                Log.w(TAG, "Complete update failed", outcome.cause)
                Result.Error("complete_update_failed", outcome.cause)
            }
        }

    // ---------- mapping ----------

    private fun AppUpdateInfo.toAppUpdateResult(): AppUpdateResult {
        if (updateAvailability() == UpdateAvailability.UPDATE_NOT_AVAILABLE) {
            return AppUpdateResult.NotAvailable
        }
        return when (installStatus()) {
            InstallStatus.DOWNLOADED -> AppUpdateResult.Downloaded
            InstallStatus.PENDING, InstallStatus.DOWNLOADING, InstallStatus.INSTALLING ->
                AppUpdateResult.InProgress(progressPercent(bytesDownloaded(), totalBytesToDownload()))
            InstallStatus.CANCELED -> AppUpdateResult.Available
            InstallStatus.FAILED -> AppUpdateResult.Failed(UpdateFailureReason.DOWNLOAD_FAILED)
            else -> AppUpdateResult.Available // fresh update, or a developer-triggered flow in progress
        }
    }

    private fun InstallState.toAppUpdateResultOrNull(): AppUpdateResult? = when (installStatus()) {
        InstallStatus.PENDING -> AppUpdateResult.InProgress(0)
        InstallStatus.DOWNLOADING, InstallStatus.INSTALLING ->
            AppUpdateResult.InProgress(progressPercent(bytesDownloaded(), totalBytesToDownload()))
        InstallStatus.DOWNLOADED -> AppUpdateResult.Downloaded
        InstallStatus.INSTALLED -> AppUpdateResult.NotAvailable
        InstallStatus.FAILED -> AppUpdateResult.Failed(UpdateFailureReason.DOWNLOAD_FAILED)
        InstallStatus.CANCELED -> AppUpdateResult.Available
        else -> null // unknown status: leave the current state untouched
    }

    private fun progressPercent(downloaded: Long, total: Long): Int =
        if (total > 0) ((downloaded * 100) / total).toInt().coerceIn(0, 100) else 0

    private suspend fun <T> Task<T>.await(): T = suspendCancellableCoroutine { continuation ->
        addOnSuccessListener { value -> continuation.resume(value) }
        addOnFailureListener { error -> continuation.resumeWithException(error) }
    }

    private companion object {
        const val TAG = "PlayAppUpdateDS"
    }
}
