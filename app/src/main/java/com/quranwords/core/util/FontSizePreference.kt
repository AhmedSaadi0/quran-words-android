package com.quranwords.core.util

/**
 * Canonical bounds of the shared font-size preference.
 *
 * Lives in `core` (not `ui/theme`) because the preference is read and written
 * below the UI layer — `core/datastore` and the feature ViewModels clamp on
 * both read and write — while `ArabicTextScale` in `ui/theme` projects it onto
 * the type hierarchy. One source of truth, so the mushaf and the cards can
 * never clamp to different ranges (which previously let the reading view keep
 * shrinking below the point where cards froze at their floor).
 *
 * 18sp is the practical floor: Quranic text carries tashkeel, and no reader
 * needs it smaller.
 */
object FontSizePreference {
    const val DEFAULT = 24f
    const val MIN = 18f
    const val MAX = 48f

    fun clamp(value: Float): Float = value.coerceIn(MIN, MAX)
}