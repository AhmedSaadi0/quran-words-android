package com.quranwords.core.util

/**
 * Canonical bounds of the reading-chrome pill transparency preference.
 *
 * The value stored is the TRANSPARENCY (0 = fully opaque, MAX = most
 * transparent); the pill's drawn alpha is `1 - transparency`. It applies
 * ONLY while the status bar is hidden (immersive reading): with the bars
 * visible the pill is always fully opaque, regardless of this setting.
 *
 * Lives in `core` (not `ui`) because the preference is read and written
 * below the UI layer — `core/datastore` and the feature ViewModel clamp on
 * both read and write, mirroring [FontSizePreference].
 *
 * 0.8 is the practical ceiling: beyond it the pill's rings and their labels
 * stop being readable over any mushaf page.
 */
object PillTransparencyPreference {
    const val DEFAULT = 0.4f
    const val MIN = 0f
    const val MAX = 0.8f

    /** Pill alpha for a stored transparency value (`1 - transparency`). */
    fun alphaFor(transparency: Float): Float = 1f - clamp(transparency)

    fun clamp(value: Float): Float = value.coerceIn(MIN, MAX)
}
