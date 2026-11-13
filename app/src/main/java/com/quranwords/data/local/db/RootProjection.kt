package com.quranwords.data.local.db

/**
 * Single source of truth for the roots projection (AGENTS §8.4).
 *
 * Every roots query — list, paged, search, top-roots, detail — selects the
 * SAME columns in the SAME order and maps them through the same cursor
 * mapper, so the read paths can never drift apart (the model column
 * `root_ai_summary.model` was the first casualty of duplicated projections).
 *
 * Pure Kotlin (no Android imports) so it stays unit-testable.
 */
object RootProjection {

    const val COLUMNS = "r.id, r.root, rg.gloss_ar, rg.gloss_en, ra.summary_ar, ra.model, " +
        "r.masadir_count, r.derivatives_count, r.occurrences_count"

    const val FROM = "FROM roots r " +
        "LEFT JOIN root_glosses rg ON rg.root_id = r.id " +
        "LEFT JOIN root_ai_summary ra ON ra.root_id = r.id"

    /** Column indexes of [COLUMNS] — shared by the cursor mapper. */
    object Col {
        const val ID = 0
        const val ROOT = 1
        const val GLOSS_AR = 2
        const val GLOSS_EN = 3
        const val SUMMARY_AR = 4
        const val MODEL = 5
        const val MASADIR_COUNT = 6
        const val DERIVATIVES_COUNT = 7
        const val OCCURRENCES_COUNT = 8
    }
}
