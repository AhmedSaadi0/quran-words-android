package io.github.ahmedsaadi0.quranwords.ui.theme

import androidx.compose.ui.text.font.Font
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import io.github.ahmedsaadi0.quranwords.R

private fun singleCutWithHafsFallback(resId: Int): FontFamily = FontFamily(
    Font(resId = resId, weight = FontWeight.Normal),
    Font(resId = resId, weight = FontWeight.Medium),
    Font(resId = resId, weight = FontWeight.Bold),
    // Per-glyph fallback: rare Quranic marks missing from a cut render from
    // the 1441H Hafs font instead of tofu boxes.
    Font(resId = R.font.kfgqpc_hafs_1441, weight = FontWeight.Normal),
    Font(resId = R.font.kfgqpc_hafs_1441, weight = FontWeight.Medium),
    Font(resId = R.font.kfgqpc_hafs_1441, weight = FontWeight.Bold)
)

/**
 * Selectable Quran text typefaces. [key] is the persisted DataStore value;
 * [lineHeightMultiplier] scales `fontSize` into `lineHeight` per cut so tall
 * diacritic stacks never clip. Unknown/stale keys resolve to the default.
 */
enum class QuranFont(
    val key: String,
    val arabicName: String,
    val lineHeightMultiplier: Float,
    val fontFamily: FontFamily
) {
    KFGQPC_HAFS_1441(
        key = "kfgqpc_hafs_1441",
        arabicName = "خط مصحف المدينة ١٤٤١",
        lineHeightMultiplier = 2.2f,
        fontFamily = UthmanicHafs1441
    ),
    AMIRI_QURAN(
        key = "amiri_quran",
        arabicName = "أميري قرآن",
        lineHeightMultiplier = 2.6f,
        fontFamily = singleCutWithHafsFallback(R.font.amiri_quran)
    ),
    SCHEHERAZADE_NEW(
        key = "scheherazade_new",
        arabicName = "شهرزاد الجديد",
        lineHeightMultiplier = 2.3f,
        fontFamily = singleCutWithHafsFallback(R.font.scheherazade_new)
    ),
    NOTO_NASKH_ARABIC(
        key = "noto_naskh_arabic",
        arabicName = "نوتو نسخ",
        lineHeightMultiplier = 2.4f,
        fontFamily = singleCutWithHafsFallback(R.font.noto_naskh_arabic)
    );

    companion object {
        fun fromKey(key: String?): QuranFont =
            entries.find { it.key == key } ?: KFGQPC_HAFS_1441
    }
}
