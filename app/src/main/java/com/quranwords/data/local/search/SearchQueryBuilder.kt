package com.quranwords.data.local.search

/**
 * Safe dynamic query builder (Full Plan v2, Fix #2).
 *
 * Every LIKE term goes through bind args — user input NEVER enters the
 * SQL string — so multi-word AND queries are 100% injection-proof.
 * The Room DAO wraps the built SQL in a SimpleSQLiteQuery; the
 * repository reaches the same SQL/args via android rawQuery.
 * Pure + unit-testable.
 */
data class BuiltQuery(val sql: String, val args: Array<Any>)

object SearchQueryBuilder {
    /** Escapes LIKE wildcards so % / _ in user input cannot fan out. */
    fun escapeLike(term: String): String =
        term.replace("\\", "\\\\").replace("%", "\\%").replace("_", "\\_")

    private fun likeArg(term: String) = "%${escapeLike(term)}%"
    private val WORD_SPLIT = Regex("\\s+")

    fun ayatSearchQuery(
        trimmed: String,
        normalized: String,
        limit: Int,
        offset: Int,
        surahFilter: Int? = null
    ): BuiltQuery {
        val words = normalized.split(WORD_SPLIT).filter { it.length >= 2 }
        val sql = StringBuilder(
            "SELECT id, surah, ayah, text_uthmani, text_uthmani_plain, text_imlaei, " +
                "word_count, juz, hizb, rub_el_hizb, page_number FROM ayat WHERE "
        )
        val args = mutableListOf<Any>()
        if (surahFilter != null) {
            sql.append("surah = ? AND ")
            args.add(surahFilter.toString())
        }
        if (words.size > 1) {
            // Multi-word AND — each word as bind arg (never concatenated).
            sql.append(words.joinToString(" AND ") {
                "(text_uthmani_plain LIKE ? OR text_imlaei LIKE ?)"
            })
            for (w in words) {
                args.add(likeArg(w))
                args.add(likeArg(w))
            }
        } else {
            sql.append("(text_uthmani_plain LIKE ? OR text_imlaei LIKE ?)")
            args.add(likeArg(normalized))
            args.add(likeArg(trimmed))
        }
        sql.append(" ORDER BY surah ASC, ayah ASC LIMIT ? OFFSET ?")
        args.add(limit.toString())
        args.add(offset.toString())
        return BuiltQuery(sql.toString(), args.toTypedArray())
    }

    /** Words search: clean/plain/translation + English LIKE (D5). */
    fun wordsSearchQuery(trimmed: String, normalized: String, limit: Int, offset: Int): BuiltQuery =
        BuiltQuery(
            "SELECT w.id, w.text, w.text_clean, w.text_plain, w.translation " +
                "FROM words w WHERE " +
                "(w.text_plain LIKE ? OR w.text_clean LIKE ? OR w.text LIKE ? " +
                "OR w.translation LIKE ?) LIMIT ? OFFSET ?",
            arrayOf(
                likeArg(normalized),
                likeArg(normalized),
                likeArg(trimmed),
                likeArg(trimmed),
                limit.toString(),
                offset.toString()
            )
        )

    fun topRootsQuery(limit: Int): BuiltQuery =
        BuiltQuery(
            "SELECT r.id, r.root, rg.gloss_ar, rg.gloss_en, ra.summary_ar, " +
                "r.masadir_count, r.derivatives_count, r.occurrences_count FROM roots r " +
                "LEFT JOIN root_glosses rg ON rg.root_id = r.id " +
                "LEFT JOIN root_ai_summary ra ON ra.root_id = r.id " +
                "ORDER BY r.occurrences_count DESC LIMIT ?",
            arrayOf(limit.toString())
        )
}
