package io.github.ahmedsaadi0.quranwords.ui.mushaf.model

import io.github.ahmedsaadi0.quranwords.core.util.SurahMeta
import io.github.ahmedsaadi0.quranwords.domain.model.Ayah

/**
 * Render-ready Mushaf page. [segments] splits the page body at surah
 * boundaries so ornamental banners can be composed between justified text
 * runs — a single AnnotatedString cannot embed them.
 */
data class MushafPageUi(
    val page: Int,
    val ayat: List<Ayah>,
    val segments: List<MushafSegment>
)

sealed interface MushafSegment {
    data class TextRun(val ayat: List<Ayah>) : MushafSegment
    data class SurahStart(
        val surahId: Int,
        val nameAr: String,
        val revelationType: String,
        val ayahCount: Int,
        /** False for Al-Fatihah (basmalah is its ayah 1) and At-Tawbah (none). */
        val showBasmalah: Boolean
    ) : MushafSegment
}

/**
 * Pure page builder (no Android dependencies, fully unit-tested). Emits a
 * [SurahStart] whenever a surah begins — at the top of the page or inline
 * mid-page — followed by its ayat as a [TextRun].
 */
fun buildMushafPage(
    page: Int,
    ayat: List<Ayah>,
    surahMeta: (Int) -> SurahMeta?
): MushafPageUi {
    val segments = mutableListOf<MushafSegment>()
    val run = mutableListOf<Ayah>()
    var previousSurah = -1

    fun flushRun() {
        if (run.isNotEmpty()) {
            segments.add(MushafSegment.TextRun(run.toList()))
            run.clear()
        }
    }

    for (ayah in ayat) {
        if (ayah.ayah == 1 && ayah.surah != previousSurah) {
            flushRun()
            val meta = surahMeta(ayah.surah)
            segments.add(
                MushafSegment.SurahStart(
                    surahId = ayah.surah,
                    nameAr = meta?.nameAr ?: "",
                    revelationType = meta?.revelationType ?: "",
                    ayahCount = meta?.ayahCount ?: 0,
                    showBasmalah = ayah.surah != 1 && ayah.surah != 9
                )
            )
        }
        run.add(ayah)
        previousSurah = ayah.surah
    }
    flushRun()

    return MushafPageUi(page = page, ayat = ayat, segments = segments)
}
