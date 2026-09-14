package io.github.ahmedsaadi0.quranwords.ui.theme

import androidx.compose.ui.text.font.Font
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import io.github.ahmedsaadi0.quranwords.R

/**
 * Official King Fahd Glorious Quran Printing Complex (KFGQPC) HAFS Uthmanic
 * Script — 1441H Madinah Mushaf edition (TrueType, single file reused across
 * weights; the typeface ships only one cut).
 *
 * Source mirrors (identical bytes): `raflyfahrezi/KFGQPC-Uthmanic-Script-HAFS-Regular`
 * and `mustafa0x/qpc-fonts` (`various/UthmanicHafs1 Ver09.otf`). The upstream
 * file carries an `.otf` extension but contains TrueType (`glyf`) outlines, so
 * it is stored here with the mandated `.ttf` name unchanged byte-for-byte.
 */
val UthmanicHafs1441 = FontFamily(
    Font(resId = R.font.kfgqpc_hafs_1441, weight = FontWeight.Normal),
    Font(resId = R.font.kfgqpc_hafs_1441, weight = FontWeight.Medium),
    Font(resId = R.font.kfgqpc_hafs_1441, weight = FontWeight.Bold)
)
