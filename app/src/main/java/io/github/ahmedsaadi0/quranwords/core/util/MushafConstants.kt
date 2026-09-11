package io.github.ahmedsaadi0.quranwords.core.util

/**
 * Mushaf (Madina codex) reader constants (AGENTS §14). The page count is read
 * from the database ([QuranRepository.getMushafPageCount]); this fallback keeps
 * the pager stable when the database is missing or unreadable.
 */
object MushafConstants {
    const val TOTAL_PAGES_FALLBACK = 604
}
