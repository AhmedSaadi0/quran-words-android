package com.quranwords.ui.update

import androidx.activity.result.ActivityResultLauncher
import androidx.activity.result.IntentSenderRequest

/**
 * Contract OWNED by the UI layer and implemented by the data layer (Decision #29,
 * dependency inversion): the composition owns the registered ActivityResultLauncher,
 * the data layer owns the Play Core knowledge, and no Activity context crosses
 * either side. This is the only boundary where an Android launch primitive flows.
 */
interface AppUpdateFlowLauncher {
    /**
     * Fetches a fresh AppUpdateInfo, validates that a flexible flow may start, and
     * starts Play's update sheet through the provided launcher.
     *
     * @return true when the sheet was actually started.
     */
    suspend fun startFlexibleUpdate(launcher: ActivityResultLauncher<IntentSenderRequest>): Boolean
}
