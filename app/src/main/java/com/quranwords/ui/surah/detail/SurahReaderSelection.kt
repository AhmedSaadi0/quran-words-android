package com.quranwords.ui.surah.detail

import com.quranwords.core.util.AyahRef
import com.quranwords.core.util.QuranCopyFormatter
import com.quranwords.core.util.Result
import com.quranwords.core.util.SurahMetadata
import com.quranwords.core.util.runCatchingResult
import com.quranwords.domain.model.Ayah
import com.quranwords.domain.model.Surah

internal fun surahFor(id: Int, loaded: Surah?): Surah? {
    if (id <= 0) return null
    if (loaded?.id == id) return loaded
    val meta = SurahMetadata.SURAHS.getOrNull(id - 1) ?: return null
    return Surah(meta.id, meta.nameAr, meta.nameEn, meta.ayahCount, meta.revelationType, meta.juzStart)
}

internal fun findAyah(state: ReaderCoreState, ref: AyahRef): Ayah? {
    fun List<Ayah>.find() = firstOrNull { it.surah == ref.surahId && it.ayah == ref.ayah }
    return state.pageAyahs.find()
        ?: state.pagesByNumber.values.firstNotNullOfOrNull { it.find() }
        ?: state.ayat.find()
}

/** Formats selected ayat grouped by surah, each with its own Surah header. */
internal fun formatSelection(state: ReaderCoreState): Result<String> {
    val bySurah = state.selection.selected.groupBy { it.surahId }
    if (bySurah.isEmpty()) return Result.Success("")
    return runCatchingResult {
        bySurah.mapNotNull { (surahId, refs) ->
            val ayat = refs.mapNotNull { findAyah(state, it) }.sortedBy { it.ayah }
            if (ayat.isEmpty()) null
            else QuranCopyFormatter.formatMultiple(ayat, surahFor(surahId, state.surah))
        }.joinToString("\n\n")
    }
}

internal fun formatSingle(ayah: Ayah, loaded: Surah?): String =
    QuranCopyFormatter.formatSingle(ayah, surahFor(ayah.surah, loaded))
