package io.github.ahmedsaadi0.quranwords.core.util

/**
 * Typed reference to a bookmarked surah or ayah (UI_REFACTOR_PLAN Phase 7).
 * Replaces raw DataStore key parsing (`"5"`, `"2:282"`) that used to live in
 * the composable via `split(":")` + `toIntOrNull() ?: 1` fallbacks.
 */
data class BookmarkRef(val surahId: Int, val ayah: Int? = null) {

    /** DataStore key: surah bookmarks are `"id"`, ayah bookmarks `"id:ayah"`. */
    val key: String = if (ayah == null) surahId.toString() else "$surahId:$ayah"

    companion object {

        /** Parses a surah bookmark key; null for malformed input (no silent 1 fallback). */
        fun parseSurah(key: String): BookmarkRef? =
            key.toIntOrNull()?.let { BookmarkRef(it) }

        /** Parses an ayah bookmark key (`"surah:ayah"`); null for malformed input. */
        fun parseAyah(key: String): BookmarkRef? {
            val parts = key.split(":")
            val surahId = parts.getOrNull(0)?.toIntOrNull() ?: return null
            val ayahNum = parts.getOrNull(1)?.toIntOrNull() ?: return null
            return BookmarkRef(surahId, ayahNum)
        }

        /** Canonical display order: surah then ayah. */
        val AYAH_ORDER: Comparator<BookmarkRef> = compareBy({ it.surahId }, { it.ayah ?: 0 })
    }
}