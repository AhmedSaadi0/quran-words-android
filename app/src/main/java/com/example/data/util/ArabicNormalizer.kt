package com.example.data.util

object ArabicNormalizer {
    private val DIACRITICS_REGEX = Regex("[\u0617-\u061A\u064B-\u0652\u0656-\u065F\u0670\u06D6-\u06DC\u06DF-\u06E4\u06E7\u06E8\u06EA-\u06ED\u0640]")

    fun stripDiacritics(text: String): String = DIACRITICS_REGEX.replace(text, "")

    fun normalizeAr(text: String): String {
        if (text.isBlank()) return ""
        var res = stripDiacritics(text)
        res = res.replace("ٱ", "ا")
            .replace("ـ", "")
            .replace("آ", "ا")
            .replace("أ", "ا")
            .replace("إ", "ا")
            .replace("ى", "ي")
            .replace("ؤ", "و")
            .replace("ئ", "ي")
            .replace("ة", "ه")
        return res.trim()
    }
}
