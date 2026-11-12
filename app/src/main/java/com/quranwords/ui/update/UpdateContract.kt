package com.quranwords.ui.update

/** What the app-update banner renders right now (Decision #29): the two user-facing
 *  states — "update available" and "restart to complete" — plus quiet download and
 *  failure states. */
sealed interface UpdateBannerState {
    /** Nothing to show: no update, up to date, or dismissed for the session. */
    data object Hidden : UpdateBannerState

    /** "New update available" + Update/Not-now — entry point for the flexible flow. */
    data object Available : UpdateBannerState

    /** Download running; [percent] is 0..100. */
    data class Downloading(val percent: Int = 0) : UpdateBannerState

    /** "Restart to complete" — flexible download finished. */
    data object RestartRequired : UpdateBannerState

    /** Transient failure message; auto-recovers on the next Play state. */
    data object Failed : UpdateBannerState
}

data class UpdateUiState(
    val banner: UpdateBannerState = UpdateBannerState.Hidden,
    /** Session-scoped dismissal; applies to the "available" and "failed" prompts only. */
    val isDismissed: Boolean = false,
)

sealed interface UpdateEvent {
    /** User tapped "restart now". */
    data object RestartApp : UpdateEvent

    /** User dismissed the "available" banner for this session. */
    data object Dismiss : UpdateEvent

    /** startFlexibleUpdate() returned false (stale info / flow not allowed). */
    data object FlowNotStarted : UpdateEvent

    /** Play sheet result delivered through the composition's ActivityResultLauncher. */
    data class UpdateSheetResult(val resultCode: Int) : UpdateEvent
}
