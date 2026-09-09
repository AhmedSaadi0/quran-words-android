package com.quranwords.core.util

object DatabaseConstants {
    const val DB_NAME = "quran_words.db"
    const val DB_MIN_READY_SIZE = 50_000_000L
    const val DB_BASELINE_VERSION_CODE = 1
    const val DB_BASELINE_VERSION_NAME = "v1"

    const val MANIFEST_URL =
        "https://raw.githubusercontent.com/AhmedSaadi0/quran-words/main/data/db-manifest.json"

    const val ZIP_ENTRY_NAME = "quran_words.db"

    const val SQLITE_HEADER = "SQLite format 3\u0000"

    const val SEARCH_PAGE_SIZE = 20
}
