package io.github.ahmedsaadi0.quranwords.core.util

import io.github.ahmedsaadi0.quranwords.data.util.SurahMeta
import io.github.ahmedsaadi0.quranwords.domain.model.Surah

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
