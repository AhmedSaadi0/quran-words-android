package com.quranwords.core.util

/** 0 = system, 1 = light, 2 = dark (same values as MainActivity/DataStore). */
const val DARK_MODE_SYSTEM = 0
const val DARK_MODE_LIGHT = 1
const val DARK_MODE_DARK = 2

/** One documented cycle for every theme control: system → dark → light → system. */
fun nextDarkMode(current: Int): Int = when (current) {
    DARK_MODE_DARK -> DARK_MODE_SYSTEM
    DARK_MODE_LIGHT -> DARK_MODE_DARK
    else -> DARK_MODE_DARK
}
