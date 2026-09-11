package io.github.ahmedsaadi0.quranwords.core.util

/**
 * Eastern Arabic numeral conversion for authentic Mushaf typography
 * (e.g. ayah end markers ﴿١٢﴾ and page footers). Pure Kotlin, no Android
 * dependencies.
 */
fun Int.toEasternArabicDigits(): String = toString().toEasternArabicDigits()

fun String.toEasternArabicDigits(): String = map {
    when (it) {
        '0' -> '٠'
        '1' -> '١'
        '2' -> '٢'
        '3' -> '٣'
        '4' -> '٤'
        '5' -> '٥'
        '6' -> '٦'
        '7' -> '٧'
        '8' -> '٨'
        '9' -> '٩'
        else -> it
    }
}.joinToString("")
