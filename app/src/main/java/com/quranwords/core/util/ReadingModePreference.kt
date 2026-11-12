package com.quranwords.core.util

/**
 * Reading layout mode for a surah.
 *
 * [SCROLL] (default) = one continuous vertical flow scrolled up/down, as it
 * has always been. [PAGES] = mushaf-style book: the surah's mushaf pages are
 * flipped horizontally right/left (RTL) like a paper book — Compose's Pager
 * mirrors the flip with the layout direction automatically.
 *
 * The mode also decides the adjacent-surah boundary behavior: in PAGES mode,
 * flipping past the surah's last/first page continues into the neighbour
 * surah with a horizontal slide; in SCROLL mode the vertical pull and its
 * vertical slide stay as they are.
 */
object ReadingModePreference {
    const val SCROLL = 0
    const val PAGES = 1
    const val DEFAULT = SCROLL

    fun isValid(value: Int): Boolean = value == SCROLL || value == PAGES

    fun fromValue(value: Int): Int = if (isValid(value)) value else DEFAULT
}
