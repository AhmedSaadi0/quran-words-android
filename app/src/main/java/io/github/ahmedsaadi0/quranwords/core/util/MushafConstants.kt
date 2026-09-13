package io.github.ahmedsaadi0.quranwords.core.util

/** QPC Mushaf constants — single source, no magic numbers in UI/data code. */
object MushafConstants {
    const val FIRST_PAGE = 1
    const val LAST_PAGE = 604
    const val TOTAL_PAGES = 604

    /** Madinah Mushaf grid: at most 15 printed lines per page. */
    const val LINES_PER_PAGE = 15
    const val MIN_LINE = 1
    const val MAX_LINE = 15

    /** Official QCF v2 per-page TTF CDN (verified TTF magic + cmap coverage). */
    const val PAGE_FONT_URL_TEMPLATE =
        "https://verses.quran.foundation/fonts/quran/hafs/v2/ttf/p%d.ttf"

    /** In-memory font cache: current page ± 3 (Phase 2 MushafFontManager). */
    const val FONT_CACHE_SIZE = 7

    fun isValidPage(page: Int): Boolean = page in FIRST_PAGE..LAST_PAGE

    fun requireValidPage(page: Int) {
        require(isValidPage(page)) { "Mushaf page out of range 1..604: $page" }
    }

    fun fontFileName(page: Int): String {
        requireValidPage(page)
        return "p%d.ttf".format(page)
    }

    /**
     * Dedicated Basmalah font (official quran.com `bismillah.ttf`, renders the
     * single `U+FDFD` ligature). Lives in the same `fonts/qpc` lane as pages.
     */
    const val BASMALAH_FONT_FILE = "bismillah.ttf"

    fun fontUrl(page: Int): String {
        requireValidPage(page)
        return PAGE_FONT_URL_TEMPLATE.format(page)
    }

    private val EASTERN_DIGITS = charArrayOf('٠', '١', '٢', '٣', '٤', '٥', '٦', '٧', '٨', '٩')

    /** Page footer numeral, e.g. 604 -> "٦٠٤". */
    fun toEasternArabic(number: Int): String =
        number.toString().map { c ->
            if (c in '0'..'9') EASTERN_DIGITS[c - '0'] else c
        }.joinToString("")
}
