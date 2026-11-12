package com.quranwords.core.util

/**
 * Parses free-text verse references such as "2:255", "٢:٢٥٥", "البقرة 255",
 * "سورة الكهف 10", "آل عمران 18", "يس 1" into (surahId, ayah).
 *
 * Pure Kotlin — no Android deps — so it is fully unit-testable.
 *
 * Robustness rules (Full Plan v2, Fix #3):
 * 1. Western + Eastern (٠١٢٣٤٥٦٧٨٩) + Arabic-Indic Extended digits are
 *    canonicalized to Western before any regex work.
 * 2. separators accept `: ، ٫ - /` + arbitrary whitespace (incl. NBSP).
 * 3. Optional "سورة / سوره / سورة" prefix is stripped.
 * 4. Room for compound surah names ("آل عمران", "آل  عمران" with extra
 *    spaces, with/without همزة normalization) is handled by
 *    Longest-Prefix Match against [SurahMetadata.SURAHS] after
 *    [ArabicNormalizer.normalizeAr] — naive split on " " is wrong.
 * 5. A reference is accepted ONLY when a trailing ayah number exists.
 *    Bare phrases like "لا أقسم بهذا البلد" return null (no number).
 * 6. Surah id must be 1..114, ayah must be ≥1 and ≤ surah.ayahCount
 *    from metadata (canonical DB numbering wins only at VM read time;
 *    parser is metadata-gated so garbage never navigates).
 */
object VerseReferenceParser {
    private val EASTERN_MAP = mapOf(
        '٠' to '0', '١' to '1', '٢' to '2', '٣' to '3', '٤' to '4',
        '٥' to '5', '٦' to '6', '٧' to '7', '٨' to '8', '٩' to '9',
        '۰' to '0', '۱' to '1', '۲' to '2', '۳' to '3', '۴' to '4',
        '۵' to '5', '۶' to '6', '۷' to '7', '۸' to '8', '۹' to '9'
    )

    private const val NUM_RANGE = "(\\d{1,3})"
    // Trailing "<name> <number>" with flexible separators between name and number.
    private val NAME_NUM_REGEX = Regex("^(.+?)[\\s:\\u060c\\u066b\\-/,]+$NUM_RANGE\\s*$")
    // Pure numeric "2:255" / "2 255" / "2-255".
    private val NUM_NUM_REGEX = Regex("^$NUM_RANGE[\\s:\\u060c\\u066b\\-/,]+$NUM_RANGE\\s*$")
    private val SURAH_PREFIX = Regex("^\\s*سورة\\s+|^\\s*سوره\\s+|^\\s*surah\\s+", RegexOption.IGNORE_CASE)

    fun canonicalizeDigits(raw: String): String =
        raw.map { EASTERN_MAP[it] ?: it }.joinToString("")

    fun parse(rawQuery: String): Pair<Int, Int>? {
        val raw = canonicalizeDigits(rawQuery).trim()
        if (raw.isBlank()) return null
        // 1. numeric pair
        NUM_NUM_REGEX.find(raw)?.let { m ->
            val sId = m.groupValues[1].toIntOrNull() ?: return null
            val aNum = m.groupValues[2].toIntOrNull() ?: return null
            if (sId in 1..114 && aNum >= 1) {
                val meta = SurahMetadata.SURAHS.getOrNull(sId - 1) ?: return null
                if (aNum <= meta.ayahCount) return sId to aNum
            }
            return null
        }
        // 2. name + number (number MUST exist, else null per spec)
        val m = NAME_NUM_REGEX.find(raw) ?: return null
        val nameRaw = m.groupValues[1].trim()
        val aNum = m.groupValues[2].toIntOrNull() ?: return null
        if (aNum < 1) return null
        val nameStripped = SURAH_PREFIX.replace(nameRaw, "").trim()
        if (nameStripped.isBlank()) return null
        val normTarget = ArabicNormalizer.normalizeAr(nameStripped)
        if (normTarget.isBlank()) return null
        // Longest-prefix match: prefer the longest normalized surah name
        // that is a prefix of the query tail (or equals it) so that
        // "آل عمران" beats "عمران" if both ever existed.
        val best = SurahMetadata.SURAHS
            .map { it to ArabicNormalizer.normalizeAr(it.nameAr) }
            .filter { (_, normName) -> normName.isNotBlank() && (normTarget == normName || normTarget.startsWith(normName)) }
            .maxByOrNull { (_, normName) -> normName.length }
            ?.first
        if (best != null && aNum <= best.ayahCount) return best.id to aNum
        return null
    }
}
