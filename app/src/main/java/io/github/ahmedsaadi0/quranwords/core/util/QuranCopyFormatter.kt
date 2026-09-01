package io.github.ahmedsaadi0.quranwords.core.util

import io.github.ahmedsaadi0.quranwords.domain.model.Ayah
import io.github.ahmedsaadi0.quranwords.domain.model.AyahOccurrenceModel
import io.github.ahmedsaadi0.quranwords.domain.model.Surah

object QuranCopyFormatter {

    /**
     * Single ayah:
     * {textUthmani} ﴿{ayahNum}﴾
     * [سورة {surahName}: {ayahNum}]
     */
    fun formatSingle(ayah: Ayah, surah: Surah?): String {
        val surahName = surah?.nameAr ?: "السورة"
        return buildString {
            append(ayah.textUthmani)
            append(" ﴿${ayah.ayah}﴾")
            append("\n")
            append("[سورة $surahName: ${ayah.ayah}]")
        }
    }

    /**
     * Multiple ayat:
     * Contiguous -> [سورة {name}: {start} - {end}]
     * Non-contiguous -> [سورة {name}: {n1}، {n2}، {n3}]
     */
    fun formatMultiple(ayahs: List<Ayah>, surah: Surah?): String {
        if (ayahs.isEmpty()) return ""
        if (ayahs.size == 1) return formatSingle(ayahs.first(), surah)
        val sorted = ayahs.sortedBy { it.ayah }
        val surahName = surah?.nameAr ?: "السورة"

        val body = sorted.joinToString("\n") { "${it.textUthmani} ﴿${it.ayah}﴾" }

        val suffix = if (isContiguous(sorted.map { it.ayah })) {
            "[سورة $surahName: ${sorted.first().ayah} - ${sorted.last().ayah}]"
        } else {
            val list = sorted.joinToString("، ") { it.ayah.toString() }
            "[سورة $surahName: $list]"
        }

        return "$body\n$suffix"
    }

    fun formatSelected(
        allAyat: List<Ayah>,
        selectedAyahNumbers: Set<Int>,
        surah: Surah?
    ): String {
        val selected = allAyat.filter { it.ayah in selectedAyahNumbers }
        return formatMultiple(selected, surah)
    }

    private fun isContiguous(numbers: List<Int>): Boolean {
        if (numbers.size < 2) return true
        val sorted = numbers.sorted()
        for (i in 1 until sorted.size) {
            if (sorted[i] != sorted[i - 1] + 1) return false
        }
        return true
    }

    /**
     * Occurrences (cross-surah) — Format B1:
     * Each ayah on its own block:
     * {textUthmani} ﴿{ayahNum}﴾
     * [سورة {surahNameAr}: {ayahNum}]
     * Blocks separated by double newlines.
     */
    fun formatOccurrences(occurrences: List<AyahOccurrenceModel>): String {
        if (occurrences.isEmpty()) return ""
        return occurrences.joinToString("\n\n") { occ ->
            buildString {
                append(occ.textUthmani)
                append(" ﴿${occ.ayahNum}﴾")
                append("\n")
                append("[سورة ${occ.surahNameAr}: ${occ.ayahNum}]")
            }
        }
    }
}
