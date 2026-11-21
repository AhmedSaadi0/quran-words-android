package com.quranwords.core.util

/**
 * Converts Latin digits to Eastern Arabic digits (e.g. 604 -> ٦٠٤) for
 * authentic Quranic markers (﴿٦٠٤﴾). Pure arithmetic mapping,
 * locale-independent; non-digit characters pass through untouched.
 */
fun Int.toEasternArabicDigits(): String = toString().toEasternArabicDigits()

/** Converts digits and grouping commas in an already formatted number. */
fun String.toEasternArabicDigits(): String = map { c ->
    when (c) {
        in '0'..'9' -> '٠' + (c - '0')
        ',' -> '٬'
        else -> c
    }
}.joinToString("")
