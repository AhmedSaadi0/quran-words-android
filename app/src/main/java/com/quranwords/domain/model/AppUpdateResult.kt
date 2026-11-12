package com.quranwords.domain.model

/**
 * Pure domain state of a Play in-app update (Decision #29). Doubles as the failure
 * carrier so failures surface explicitly instead of being hidden (AGENTS §13).
 * No Android or Play Core types are allowed here — the domain stays framework-free.
 */
sealed interface AppUpdateResult {
    /** Device is on the latest version, or Play cannot serve updates for this install. */
    data object NotAvailable : AppUpdateResult

    /** A newer versionCode is published; the flexible flow may be started. */
    data object Available : AppUpdateResult

    /** Flexible download is running; [percent] is 0..100 (0 when unknown). */
    data class InProgress(val percent: Int = 0) : AppUpdateResult

    /** Download finished; the app must be restarted to complete the installation. */
    data object Downloaded : AppUpdateResult

    /** The availability check or the flexible flow failed and may be retried. */
    data class Failed(val reason: UpdateFailureReason) : AppUpdateResult
}

enum class UpdateFailureReason {
    /** Play reported the flow/type is not allowed for this install right now. */
    NOT_ALLOWED,

    /** The availability check itself failed (connectivity, Play service error). */
    CHECK_FAILED,

    /** The download or install failed during the flexible flow. */
    DOWNLOAD_FAILED,

    /** Low-level cause not mapped to a more specific reason. */
    UNKNOWN,
}
