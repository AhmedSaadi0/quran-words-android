package io.github.ahmedsaadi0.quranwords.ui.theme

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.Immutable
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp

/**
 * Dedicated Mushaf reader tokens — the only place holding print-paper colors.
 *
 * App chrome keeps using Natural Tones ([Color.kt]); Quran ink/paper live here
 * because pure `onSurface #1A1C18` on white reads as harsh UI black, not book
 * ink. Warm paper + soft charcoal-brown ink is the Madinah-page look.
 *
 * PAPER is active (light follows system dark via [PaperDark]).
 * SEPIA + manual NIGHT override arrive with the reader settings sheet
 * (DataStore `mushaf_theme`); their palettes are defined now so no file is
 * touched twice.
 *
 * Gold stays on headers, surah frames and divider ornaments only — never on
 * ayah markers or small text (contrast). QCF glyph/metric spacing is never
 * altered here.
 */
@Immutable
data class MushafPalette(
    val paper: Color,
    val paperEdge: Color,
    val outer: Color,
    val outerScrim: Color,
    val ink: Color,
    val inkSoft: Color,
    val inkFaded: Color,
    val gold: Color,
    val goldSoft: Color,
    val selectedWord: Color,
    val divider: Color
)

object MushafTokens {
    val PaperLight = MushafPalette(
        paper = Color(0xFFFDF8EC),
        paperEdge = Color(0xFFF3EAD3),
        outer = Color(0xFFE9E4D4),
        outerScrim = Color(0xFFDCD5C0),
        ink = Color(0xFF2B251A),
        inkSoft = Color(0xFF2B251A).copy(alpha = 0.88f),
        inkFaded = Color(0xFF6B6252),
        gold = NaturalAmber,
        goldSoft = NaturalAmberContainer,
        selectedWord = Color(0xFF56624B).copy(alpha = 0.22f),
        divider = Color(0xFF2B251A).copy(alpha = 0.10f)
    )

    val PaperDark = MushafPalette(
        paper = Color(0xFF171B15),
        paperEdge = Color(0xFF10130F),
        outer = Color(0xFF0E100E),
        outerScrim = Color(0xFF080908),
        ink = Color(0xFFE9E1C8),
        inkSoft = Color(0xFFE9E1C8).copy(alpha = 0.88f),
        inkFaded = Color(0xFF9A917C),
        gold = NaturalAmberLight,
        goldSoft = NaturalAmberContainerDark,
        selectedWord = Color(0xFFA4AD96).copy(alpha = 0.25f),
        divider = Color(0xFFE9E1C8).copy(alpha = 0.12f)
    )

    /** Reserved for the Phase-3 reader settings sheet. */
    val SepiaLight = MushafPalette(
        paper = Color(0xFFF6ECD4),
        paperEdge = Color(0xFFEADDBE),
        outer = Color(0xFFE4D5B5),
        outerScrim = Color(0xFFD5C5A3),
        ink = Color(0xFF43381F),
        inkSoft = Color(0xFF43381F).copy(alpha = 0.88f),
        inkFaded = Color(0xFF7A6B4D),
        gold = NaturalAmberDark,
        goldSoft = NaturalAmberContainer,
        selectedWord = Color(0xFF8C6E2D).copy(alpha = 0.20f),
        divider = Color(0xFF43381F).copy(alpha = 0.12f)
    )

    /** Overlay glass translucency: paper whispers instead of shouting. */
    const val CHROME_GLASS_ALPHA = 0.88f

    /** Vignette ceiling: warm depth, never dirt (~5% edge darkening). */
    const val VIGNETTE_MAX_ALPHA = 0.05f

    /** Lines with at most this many words render centered, never justified. */
    const val SHORT_LINE_WORD_THRESHOLD = 4

    val CardRadius = 20.dp
    val PagePaddingH = 20.dp
    val PagePaddingV = 16.dp
    val PageMaxWidth = 520.dp
    val CardElevation = 2.dp
}

/**
 * Current resolution: PAPER palette, dark variant follows the system theme.
 * A later phase adds an explicit `MushafReadingTheme` parameter
 * (DataStore-backed) which will activate [SepiaLight].
 */
@Composable
fun rememberMushafPalette(): MushafPalette =
    if (isSystemInDarkTheme()) MushafTokens.PaperDark else MushafTokens.PaperLight
