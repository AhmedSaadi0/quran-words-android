package io.github.ahmedsaadi0.quranwords.domain.mushaf

import io.github.ahmedsaadi0.quranwords.core.util.MushafConstants
import io.github.ahmedsaadi0.quranwords.domain.model.MushafLine
import io.github.ahmedsaadi0.quranwords.domain.model.MushafLineType
import io.github.ahmedsaadi0.quranwords.domain.model.MushafPage
import io.github.ahmedsaadi0.quranwords.domain.model.MushafPageMetaRow
import io.github.ahmedsaadi0.quranwords.domain.model.MushafSpecialRow
import io.github.ahmedsaadi0.quranwords.domain.model.MushafWord
import io.github.ahmedsaadi0.quranwords.domain.model.MushafWordRow

/**
 * Pure assembly of a [MushafPage] from raw DB rows (no Android deps).
 *
 * Fail-fast: any line in 1..maxLine that holds neither glyph words nor a
 * special marker indicates corrupt layout data and throws — silent gaps
 * would shift the whole printed grid.
 */
object MushafPageAssembler {

    fun assemble(
        meta: MushafPageMetaRow,
        wordRows: List<MushafWordRow>,
        specials: List<MushafSpecialRow>
    ): MushafPage {
        require(meta.minLine in MushafConstants.MIN_LINE..MushafConstants.MAX_LINE) {
            "page ${meta.pageNumber}: minLine ${meta.minLine} out of range"
        }
        require(meta.maxLine in meta.minLine..MushafConstants.MAX_LINE) {
            "page ${meta.pageNumber}: maxLine ${meta.maxLine} out of range"
        }
        val strayWords = wordRows.filter { it.pageNumber != meta.pageNumber }
        require(strayWords.isEmpty()) {
            "page ${meta.pageNumber}: ${strayWords.size} rows belong to other pages"
        }

        val wordsByLine = wordRows.groupBy { it.lineNumber }
        val specialByLine = specials.associateBy { it.lineNumber }

        val lines = (MushafConstants.MIN_LINE..meta.maxLine).mapNotNull { lineNumber ->
            val special = specialByLine[lineNumber]
            val words = wordsByLine[lineNumber]
                ?.sortedBy { it.posInLine }
                ?.map {
                    MushafWord(
                        wordAyahId = it.wordAyahId,
                        codeV2 = it.codeV2,
                        charType = it.charType,
                        surah = it.surah,
                        ayah = it.ayah,
                        position = it.position
                    )
                }
            when {
                special != null && words.isNullOrEmpty() -> MushafLine(
                    pageNumber = meta.pageNumber,
                    lineNumber = lineNumber,
                    lineType = special.lineType,
                    surahId = special.surahId
                )
                !words.isNullOrEmpty() && special == null -> MushafLine(
                    pageNumber = meta.pageNumber,
                    lineNumber = lineNumber,
                    lineType = MushafLineType.TEXT,
                    surahId = null,
                    words = words
                )
                else -> error(
                    "page ${meta.pageNumber} line $lineNumber: " +
                        "special=$special words=${words?.size} — corrupt layout"
                )
            }
        }
        check(lines.isNotEmpty()) { "page ${meta.pageNumber}: no lines assembled" }
        return MushafPage(
            pageNumber = meta.pageNumber,
            lines = lines,
            surahStart = meta.surahStart,
            ayahStart = meta.ayahStart,
            surahEnd = meta.surahEnd,
            ayahEnd = meta.ayahEnd,
            juzNumber = meta.juzNumber
        )
    }
}
