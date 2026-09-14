package io.github.ahmedsaadi0.quranwords.core.util

/**
 * Converts Latin digits to Eastern Arabic digits (e.g. 604 -> ٦٠٤) for
 * authentic Quranic markers (﴿٦٠٤﴾). Pure arithmetic mapping,
 * locale-independent; non-digit characters pass through untouched.
 */
fun Int.toEasternArabicDigits(): String =
    this.toString().map { c ->
        if (c in '0'..'9') '٠' + (c - '0') else c
    }.joinToString("")
