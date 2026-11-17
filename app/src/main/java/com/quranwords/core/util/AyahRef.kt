package com.quranwords.core.util

/**
 * Identity of one ayah: surah + ayah number. Ayah numbers restart per surah,
 * so a bare `Int` collides on a shared mushaf page (e.g. 113:1 vs 114:1).
 * The [key] matches the persisted bookmark format (`"surah:ayah"`).
 */
data class AyahRef(val surahId: Int, val ayah: Int) {
    val key: String get() = "$surahId:$ayah"

    companion object {
        fun parse(key: String): AyahRef? {
            val parts = key.split(":")
            val surahId = parts.getOrNull(0)?.toIntOrNull() ?: return null
            val ayah = parts.getOrNull(1)?.toIntOrNull() ?: return null
            return AyahRef(surahId, ayah)
        }
    }
}
