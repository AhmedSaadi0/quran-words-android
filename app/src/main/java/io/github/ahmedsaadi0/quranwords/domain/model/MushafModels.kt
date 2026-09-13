package io.github.ahmedsaadi0.quranwords.domain.model

/**
 * QPC page-by-page Mushaf layout models (pure Kotlin, no Android deps).
 *
 * Layout data lives in the supplementary `mushaf_word_location`,
 * `mushaf_special_lines` and `mushaf_page_meta` tables. Word taps resolve
 * through [MushafWord.wordAyahId] to the existing `WordToken` /
 * `MorphologyBottomSheet` flow — glyph positions are never used as identity.
 */
data class MushafWord(
    /** `word_ayah.id`, null for ayah-end markers which have no morphology. */
    val wordAyahId: Int?,
    /** QCF v2 per-word glyph string for the page font `p{page}.ttf`. */
    val codeV2: String,
    /** `word` or `end` (ayah marker). */
    val charType: String,
    val surah: Int,
    val ayah: Int,
    /** 1-based position inside the ayah (`end` markers use wordCount + 1). */
    val position: Int
) {
    val isAyahMarker: Boolean get() = charType == MushafLineType.END_MARKER
}

object MushafLineType {
    const val TEXT = "text"
    const val SURAH_HEADER = "surah_header"
    const val BASMALAH = "basmalah"

    /** Char types stored per glyph row. */
    const val WORD = "word"
    const val END_MARKER = "end"
}

data class MushafLine(
    val pageNumber: Int,
    /** Absolute printed line 1..15 (headers/basmalah consume line slots). */
    val lineNumber: Int,
    /** One of [MushafLineType].TEXT / SURAH_HEADER / BASMALAH. */
    val lineType: String,
    /** Set for header/basmalah lines only. */
    val surahId: Int?,
    /** Glyph words in reading order; empty for header/basmalah lines. */
    val words: List<MushafWord> = emptyList()
) {
    val isText: Boolean get() = lineType == MushafLineType.TEXT
}

data class MushafPage(
    val pageNumber: Int,
    /** Printed line sequence (short pages hold fewer than 15 lines). */
    val lines: List<MushafLine>,
    val surahStart: Int,
    val ayahStart: Int,
    val surahEnd: Int,
    val ayahEnd: Int,
    val juzNumber: Int?,
    /** Surah id -> Arabic name, resolved from the serving DB handle. */
    val surahNames: Map<Int, String> = emptyMap()
)

/** Raw row DTOs shared by the repository and the pure page assembler. */
data class MushafWordRow(
    val wordAyahId: Int?,
    val pageNumber: Int,
    val lineNumber: Int,
    val posInLine: Int,
    val codeV2: String,
    val charType: String,
    val surah: Int,
    val ayah: Int,
    val position: Int
)

data class MushafSpecialRow(
    val pageNumber: Int,
    val lineNumber: Int,
    val lineType: String,
    val surahId: Int
)

data class MushafPageMetaRow(
    val pageNumber: Int,
    val surahStart: Int,
    val ayahStart: Int,
    val surahEnd: Int,
    val ayahEnd: Int,
    val juzNumber: Int?,
    val minLine: Int,
    val maxLine: Int
)
