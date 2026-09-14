package io.github.ahmedsaadi0.quranwords.core.util

private const val MADDAH = '\u0653'
private const val DAGGER_ALIF = '\u0670'
private const val TATWEEL = '\u0640'
private const val SMALL_LOW_MEEM = '\u06ED'
private const val SMALL_HIGH_ROUNDED_ZERO = '\u06DF'
private const val SUKUN = '\u0652'
private const val ZWNJ = '\u200C'
private const val ZWSP = '\u200B'

private fun isPauseMark(c: Char?): Boolean = c != null && c in '\u06D6'..'\u06DC'

/**
 * Repository-level Uthmanic sanitizer: rewrites raw DB text into a mark order
 * the KFGQPC 1441H font (HarfBuzz) attaches cleanly, without dotted circles.
 *
 * Rules (audited against `quran_words.db`):
 * 1. Maddah + Dagger Alif → Dagger Alif + Maddah (canonical order; idempotent).
 * 2. Drop Tatweel directly before a Dagger Alif (`ـٰ` → `ٰ`), including when a
 *    Maddah sits between them (`ـٰٓ` input order).
 * 3. Strip Small Low Meem (`U+06ED`, e.g. `هُدًۭى` → `هُدًى`) so word tokens
 *    converge with `ayat.text_uthmani`, which never carries it (2197 tokens).
 * 4. Trim a token-internal space before pause marks (`U+06D6`–`U+06DC`),
 *    e.g. `رَّبِّهِمْ ۖ` → `رَّبِّهِمْۖ`. Leading spaces are kept.
 * 5. Strip zero-width artifacts (`U+200C`, `U+200B`; 0 rows in DB, defensive).
 * 6. Normalize Small High Rounded Zero (`U+06DF`) to standard Sukun (`U+0652`):
 *    the KFGQPC font has no `U+06DF` mark anchors on `و`/`ا` (dotted circle),
 *    while Sukun renders as the correct Quranic circular zero. True Quranic
 *    Sukun (`U+06E1`) is untouched.
 *
 * Deliberately preserves: base consonants, harakat, `U+06E1`, `U+06E2`, and
 * every search column (`*_plain`, `text_imlaei`) — both `U+06DF` and `U+0652`
 * are stripped by [ArabicNormalizer], so search symmetry is unaffected. Pure, single-pass, allocation-free when
 * the input is already clean (returns the same instance).
 */
fun sanitizeUthmanicText(text: String): String {
    var out: StringBuilder? = null
    var cleanUntil = 0
    var i = 0
    val n = text.length
    while (i < n) {
        val c = text[i]
        val next: Char? = if (i + 1 < n) text[i + 1] else null
        when {
            // 1. Canonicalize mark order.
            c == MADDAH && next == DAGGER_ALIF -> {
                out = (out ?: StringBuilder(n)).append(text, cleanUntil, i)
                    .append(DAGGER_ALIF).append(MADDAH)
                i += 2
                cleanUntil = i
            }
            // 2. Tatweel before dagger (with or without an intervening maddah).
            c == TATWEEL && (next == DAGGER_ALIF ||
                (next == MADDAH && i + 2 < n && text[i + 2] == DAGGER_ALIF)) -> {
                out = (out ?: StringBuilder(n)).append(text, cleanUntil, i)
                i += 1
                cleanUntil = i
            }
            // 3 + 5. Drop detached/superfluous marks.
            c == SMALL_LOW_MEEM || c == ZWNJ || c == ZWSP -> {
                out = (out ?: StringBuilder(n)).append(text, cleanUntil, i)
                i += 1
                cleanUntil = i
            }
            // 6. Rounded zero on silent letters -> standard sukun (font anchor).
            c == SMALL_HIGH_ROUNDED_ZERO -> {
                out = (out ?: StringBuilder(n)).append(text, cleanUntil, i)
                    .append(SUKUN)
                i += 1
                cleanUntil = i
            }
            // 4. Token-internal space before a pause mark.
            c == ' ' && isPauseMark(next) && i > 0 && text[i - 1] != ' ' -> {
                out = (out ?: StringBuilder(n)).append(text, cleanUntil, i)
                i += 1
                cleanUntil = i
            }
            else -> i += 1
        }
    }
    val builder = out ?: return text
    return builder.append(text, cleanUntil, n).toString()
}
