package io.github.ahmedsaadi0.quranwords.core.util

/**
 * Pure match-range computation for highlighting a matched word inside an
 * ayah text (extracted from the UI layer, UI_REFACTOR_PLAN Phase 3).
 * Returns 0-based inclusive [IntRange]s into [ayahText]; no Compose types —
 * the caller builds the AnnotatedString.
 *
 * Strategy (identical to the legacy implementation):
 * 1. blank match → no ranges;
 * 2. direct substring match (exact, with diacritics) → every occurrence;
 * 3. fallback token match → full tokens equal under diacritic-stripped or
 *    normalized comparison.
 */
object AyahHighlighter {

    fun matchRanges(ayahText: String, matchedWord: String): List<IntRange> {
        if (matchedWord.isBlank()) return emptyList()

        if (ayahText.contains(matchedWord)) {
            val ranges = mutableListOf<IntRange>()
            var startIndex = 0
            var foundIndex = ayahText.indexOf(matchedWord, startIndex)
            while (foundIndex != -1) {
                ranges += IntRange(foundIndex, foundIndex + matchedWord.length - 1)
                startIndex = foundIndex + matchedWord.length
                foundIndex = ayahText.indexOf(matchedWord, startIndex)
            }
            return ranges
        }

        val strippedTarget = ArabicNormalizer.stripDiacritics(matchedWord)
        val normalizedTarget = ArabicNormalizer.normalizeAr(matchedWord)
        val ranges = mutableListOf<IntRange>()
        var index = 0
        ayahText.split(" ").forEach { token ->
            val strippedToken = ArabicNormalizer.stripDiacritics(token)
            val normalizedToken = ArabicNormalizer.normalizeAr(token)
            val isMatch = token == matchedWord ||
                strippedToken == strippedTarget ||
                normalizedToken == normalizedTarget ||
                strippedToken == normalizedTarget ||
                normalizedToken == strippedTarget
            if (isMatch) ranges += IntRange(index, index + token.length - 1)
            index += token.length + 1
        }
        return ranges
    }
}