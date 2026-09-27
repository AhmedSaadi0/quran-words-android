package com.quranwords.core.util

import com.quranwords.core.util.SurahMeta
import com.quranwords.domain.model.Surah

/**
 * Single normalized check for the Meccan/Medinan classification.
 *
 * The database column stays a raw Arabic String (read-only corpus, no migration);
 * UI must use [isMeccan] instead of `revelationType.contains("مكية")` so labels
 * keep working once they are localized.
 */
val Surah.isMeccan: Boolean
    get() = revelationType == "مكية"

val SurahMeta.isMeccan: Boolean
    get() = revelationType == "مكية"

/** Surah-index filter chips (Phase 6) — replaces the raw `"all"/"meccan"/"medinan"` strings. */
enum class RevelationFilter {
    ALL,
    MECCAN,
    MEDINAN
}
