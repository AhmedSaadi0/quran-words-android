package com.quranwords.ui.theme

import androidx.compose.runtime.Immutable
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.unit.TextUnit
import androidx.compose.ui.unit.sp
import com.quranwords.core.util.FontSizePreference
import kotlin.math.max

/**
 * Typography metrics for Quranic verses and the lexical cards that quote or
 * explain them. One token, one contract — see [ArabicTextScale].
 */
@Immutable
data class CardTextScale(
    val verseTextSize: TextUnit,
    val verseLineHeight: TextUnit,
    val wordTitleSize: TextUnit,
    val wordTitleLineHeight: TextUnit,
    val explanationBodySize: TextUnit,
    val explanationLineHeight: TextUnit,
    val metadataLabelSize: TextUnit
)

/**
 * Single source of truth mapping the shared font-size preference onto every text
 * role of a reading surface (AGENTS §10.4: no hardcoded sizes per screen).
 *
 * The preference is a **Quran text size in sp**, anchored to the mushaf — the
 * surface users calibrate against. Every other role derives from the delta
 * against that anchor, so the visual hierarchy holds at any slider position:
 *
 * | Role | Size | Line height |
 * |---|---|---|
 * | Verse (Quranic text) | `24 + Δ` — identical to the mushaf | × per-cut Quran multiplier |
 * | Word / token headline | `20 + 0.85Δ`, capped at [MAX_WORD_TITLE_SIZE] | ×[ARABIC_LINE_HEIGHT_RATIO] |
 * | Explanation / meaning body | `max(16, 17 + 0.75Δ)` | ×[ARABIC_LINE_HEIGHT_RATIO] |
 * | Metadata / badges / labels | `max(13, 14 + 0.50Δ)` | theme default |
 *
 * Replaces the legacy linear `fontSize / 24f` multiplier, which pinned Arabic
 * body copy to Material's 14sp Latin default — microscopic once tashkeel is
 * accounted for, and the reason cards barely grew while verses jumped to 32sp+
 * for elderly and low-vision readers.
 */
object ArabicTextScale {

    /** Preference value at which the scale matches its design baseline. */
    const val DEFAULT_QURAN_SIZE = FontSizePreference.DEFAULT

    /**
     * Shared bounds of the preference (also enforced by the slider and the
     * DataStore via [FontSizePreference]). No reader needs Quran text with
     * tashkeel below 18sp, and the mushaf and the cards must clamp to the same
     * range — otherwise a small preference would freeze the cards while still
     * shrinking the reading view.
     */
    const val MIN_QURAN_SIZE = FontSizePreference.MIN
    const val MAX_QURAN_SIZE = FontSizePreference.MAX

    /** Arabic tashkeel needs generous leading; Quranic marks need more still. */
    private const val ARABIC_LINE_HEIGHT_RATIO = 1.7f
    private const val QURAN_LINE_HEIGHT_RATIO = 2.2f

    /** Keeps a bold headline (plus system font scale) inside narrow cards. */
    private const val MAX_WORD_TITLE_SIZE = 36f

    private const val WORD_TITLE_BASE = 20f
    private const val WORD_TITLE_DELTA_RATIO = 0.85f
    private const val EXPLANATION_BASE = 17f
    private const val EXPLANATION_FLOOR = 16f
    private const val EXPLANATION_DELTA_RATIO = 0.75f
    private const val LABEL_BASE = 14f
    private const val LABEL_FLOOR = 13f
    private const val LABEL_DELTA_RATIO = 0.5f

    /**
     * Projects the raw preference onto every text role.
     *
     * Pure by design (no `@Composable`): it reads no composition state, so it
     * is directly unit-testable and cannot force a recomposition on its own.
     *
     * @param quranLineHeightMultiplier per-cut line rhythm of the active
     *   [QuranFont]; pass `quranFont.lineHeightMultiplier` so cards keep the
     *   same vertical rhythm as the mushaf.
     */
    fun calculate(
        userFontSize: Float,
        quranLineHeightMultiplier: Float = QURAN_LINE_HEIGHT_RATIO
    ): CardTextScale {
        val clamped = userFontSize.coerceIn(MIN_QURAN_SIZE, MAX_QURAN_SIZE)
        val delta = clamped - DEFAULT_QURAN_SIZE

        // 1. Verse: absolute 1:1 match with the mushaf reading view.
        // 2. Word headline: prominent but capped so badges stay on screen.
        // 3. Explanation body: accessible 17sp floor, accelerating for low vision.
        // 4. Metadata labels and badges: grow slowest, floored for legibility.
        val verse = clamped
        val wordTitle = (WORD_TITLE_BASE + delta * WORD_TITLE_DELTA_RATIO).coerceAtMost(MAX_WORD_TITLE_SIZE)
        val explanation = max(EXPLANATION_FLOOR, EXPLANATION_BASE + delta * EXPLANATION_DELTA_RATIO)
        val label = max(LABEL_FLOOR, LABEL_BASE + delta * LABEL_DELTA_RATIO)

        return CardTextScale(
            verseTextSize = verse.sp,
            verseLineHeight = (verse * quranLineHeightMultiplier).sp,
            wordTitleSize = wordTitle.sp,
            wordTitleLineHeight = (wordTitle * ARABIC_LINE_HEIGHT_RATIO).sp,
            explanationBodySize = explanation.sp,
            explanationLineHeight = (explanation * ARABIC_LINE_HEIGHT_RATIO).sp,
            metadataLabelSize = label.sp
        )
    }

    /** [base] with size and leading overridden by the explanation role. */
    fun TextStyle.asExplanation(scale: CardTextScale): TextStyle = copy(
        fontSize = scale.explanationBodySize,
        lineHeight = scale.explanationLineHeight
    )

    /** [base] with size overridden by the metadata role, keeping its leading. */
    fun TextStyle.asMetadataLabel(scale: CardTextScale): TextStyle =
        copy(fontSize = scale.metadataLabelSize)

    /** Clamps a raw preference into the range every reading surface honours. */
    fun clamp(userFontSize: Float): Float = FontSizePreference.clamp(userFontSize)
}