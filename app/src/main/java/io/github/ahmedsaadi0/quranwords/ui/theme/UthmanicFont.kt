package io.github.ahmedsaadi0.quranwords.ui.theme

import androidx.compose.ui.text.font.Font
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import io.github.ahmedsaadi0.quranwords.R

/**
 * Official King Fahd Glorious Quran Printing Complex (KFGQPC) HAFS Uthmanic
 * Script — 1441H Madinah Mushaf edition.
 *
 * Defined with Normal weight only so the text engine synthesizes bolding
 * (Synthetic Bold) when FontWeight.SemiBold or FontWeight.Bold is requested.
 */
val UthmanicHafs1441 = FontFamily(
    Font(resId = R.font.kfgqpc_hafs_1441, weight = FontWeight.Normal)
)