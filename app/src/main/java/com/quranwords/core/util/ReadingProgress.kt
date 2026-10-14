package com.quranwords.core.util

import com.quranwords.core.util.SurahMetadata.JUZ_LIST
import com.quranwords.core.util.SurahMetadata.SURAHS

/**
 * Pure Quran position math (AGENTS §14: static reference data, no DB / no
 * Android deps). Juz boundaries come from [SurahMetadata.JUZ_LIST] (verified
 * read-only from `ayat.juz` — most juz start mid-surah) combined with
 * canonical surah ayah counts via prefix sums. Unit-tested in `ReadingProgressTest`.
 */
object QuranIndex {

    /** CUM[i] = total ayat of surahs 1..i; CUM[0] = 0. */
    private val CUM: IntArray = IntArray(SURAHS.size + 1).also { cum ->
        SURAHS.forEachIndexed { index, surah -> cum[index + 1] = cum[index] + surah.ayahCount }
    }

    /** Total ayat of the mushaf (6236) — sanity anchor for tests. */
    val TOTAL_AYAT: Int = CUM.last()

    /** 1-based global ayah index of (surahId, ayah) in 1..[TOTAL_AYAT]; 0 when invalid. */
    fun globalAyahIndex(surahId: Int, ayah: Int): Int {
        if (surahId !in 1..SURAHS.size) return 0
        val base = CUM[surahId - 1]
        return if (ayah in 1..SURAHS[surahId - 1].ayahCount) base + ayah else 0
    }

    /** Global 1-based index where [juz] starts; 0 when out of range. */
    fun juzStartIndex(juz: Int): Int {
        val meta = JUZ_LIST.getOrNull(juz - 1) ?: return 0
        return globalAyahIndex(meta.startSurahId, meta.startAyah)
    }

    /** Number of ayat in [juz] (next start − this start; last juz → TOTAL + 1 − start). */
    fun juzLength(juz: Int): Int {
        if (juz !in 1..JUZ_LIST.size) return 0
        val start = juzStartIndex(juz)
        val endExclusive = if (juz == JUZ_LIST.size) TOTAL_AYAT + 1 else juzStartIndex(juz + 1)
        return endExclusive - start
    }

    /** Juz containing the given 1-based global index (clamped into 1..30). */
    fun juzAt(globalIndex: Int): Int {
        val g = globalIndex.coerceIn(1, TOTAL_AYAT)
        var low = 0
        var high = JUZ_LIST.size - 1
        var result = 0
        while (low <= high) {
            val mid = (low + high) / 2
            if (juzStartIndex(mid + 1) <= g) {
                result = mid + 1
                low = mid + 1
            } else {
                high = mid - 1
            }
        }
        return result.coerceAtLeast(1)
    }
}

/**
 * Snapshot of the reader's position for the top-bar rings: the mushaf page
 * currently at the viewport top with its position within the surah, plus the
 * current juz and how far the reader has advanced inside it.
 *
 * Both fractions GROW as the reader advances (arc fills up).
 */
data class ReadingProgress(
    val currentPage: Int,
    val totalPages: Int,
    val pageFraction: Float,
    val juz: Int,
    val juzFraction: Float
) {
    companion object {
        /**
         * @param blockFirstAyah first ayah of the currently visible flow block
         * @param blockAyahCount number of ayat in that block (≥ 1)
         * @param scrolledFraction 0..1 fraction of the block scrolled past the viewport top
         * @param currentPage mushaf page owning the viewport top (null hides the rings)
         * @param surahPages all mushaf pages of this surah (VM-loaded, may be empty)
         */
        fun of(
            surahId: Int,
            blockFirstAyah: Int,
            blockAyahCount: Int,
            scrolledFraction: Float,
            currentPage: Int?,
            surahPages: List<Int>
        ): ReadingProgress? {
            if (surahId !in 1..SURAHS.size || blockFirstAyah < 1 || blockAyahCount < 1) return null
            val page = currentPage ?: return null
            if (QuranIndex.globalAyahIndex(surahId, blockFirstAyah) == 0) return null

            val pageIdx = surahPages.indexOf(page)
            val totalPages = surahPages.size
            val pageFraction = (if (pageIdx >= 0 && totalPages > 0) {
                (pageIdx + 1f) / totalPages
            } else {
                0f
            }).coerceIn(0f, 1f)

            // Fractional ayah position interpolates the smooth juz progress
            // even though the page ring advances in whole-page steps.
            val fraction = scrolledFraction.coerceIn(0f, 1f)
            val ayahF = blockFirstAyah + fraction * blockAyahCount
            val globalF = QuranIndex.globalAyahIndex(surahId, 1) + (ayahF - 1f)
            val juz = QuranIndex.juzAt(globalF.toInt())
            val juzStart = QuranIndex.juzStartIndex(juz)
            val juzLen = QuranIndex.juzLength(juz)
            val juzFraction = (if (juzLen <= 1) {
                1f
            } else {
                (globalF - juzStart) / (juzLen - 1)
            }).coerceIn(0f, 1f)

            return ReadingProgress(
                currentPage = page,
                totalPages = totalPages,
                pageFraction = pageFraction,
                juz = juz,
                juzFraction = juzFraction
            )
        }
    }
}

/** Target of an adjacent-surah switch; `null` at the Quran's boundaries. */
data class AdjacentSurahTarget(
    val surahId: Int,
    val ayah: Int,
    val fromNext: Boolean
)

/**
 * Surah continued by scrolling past an edge: forward → next surah at ayah 1,
 * backward → previous surah at its last ayah (you were reading upward).
 */
fun adjacentSurahTarget(surahId: Int, forward: Boolean): AdjacentSurahTarget? {
    val targetId = if (forward) surahId + 1 else surahId - 1
    if (targetId !in 1..SurahMetadata.SURAHS.size) return null
    val targetAyah = if (forward) 1 else SurahMetadata.SURAHS[targetId - 1].ayahCount
    return AdjacentSurahTarget(surahId = targetId, ayah = targetAyah, fromNext = forward)
}
