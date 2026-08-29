package io.github.ahmedsaadi0.quranwords.ui.theme

import androidx.compose.ui.graphics.Color

// ==========================================
// Natural Tones Design Theme Palette
// Design tokens extracted from Natural Tones specification:
// - Background: #F7F8F2 (Soft warm chalk/bone natural linen)
// - Text/Primary: #1A1C18 (Deep earthy forest charcoal)
// - Text/Secondary: #43483E (Muted olive stone charcoal)
// - Sage Container: #DDE5D1 (Light organic sage tint)
// - Olive Accent: #56624B (Earthy deep moss/olive green)
// - Sage Highlight: #A4AD96 (Soft sage green highlight)
// - Natural Border: #E1E3D9 (Subtle organic border & divider)
// ==========================================

// Core Natural Light Tokens
val NaturalBackgroundLight = Color(0xFFF7F8F2)
val NaturalSurfaceLight = Color(0xFFFFFFFF)
val NaturalSurfaceVariantLight = Color(0xFFE1E3D9)
val NaturalBorderLight = Color(0xFFE1E3D9)
val NaturalOutlineLight = Color(0xFFD2D8C7)

val NaturalTextPrimaryLight = Color(0xFF1A1C18)
val NaturalTextSecondaryLight = Color(0xFF43483E)
val NaturalTextTertiaryLight = Color(0xFF737A6C)

// Natural Olive & Sage Scale
val NaturalOlive900 = Color(0xFF192314)
val NaturalOlive800 = Color(0xFF2B3824)
val NaturalOlive700 = Color(0xFF3F4E36)
val NaturalOlive600 = Color(0xFF56624B) // Key primary brand accent (#56624B)
val NaturalOlive500 = Color(0xFF6B785E)
val NaturalOlive400 = Color(0xFF869477)
val NaturalOlive300 = Color(0xFFA4AD96) // Key highlight accent (#A4AD96)
val NaturalOlive200 = Color(0xFFC4CCA6)
val NaturalOlive100 = Color(0xFFDDE5D1) // Key container accent (#DDE5D1)
val NaturalOlive50 = Color(0xFFF2F5EC)

// Earth & Quranic Antique Amber Scale (organic warm complement to olive)
val NaturalAmber = Color(0xFF8C6E2D)
val NaturalAmberDark = Color(0xFF634D1B)
val NaturalAmberLight = Color(0xFFB5934C)
val NaturalAmberContainer = Color(0xFFF3E8D0)
val NaturalAmberContainerDark = Color(0xFF382A0C)

// Dark Theme Natural Tones
val NaturalBackgroundDark = Color(0xFF121411)
val NaturalSurfaceDark = Color(0xFF191D17)
val NaturalSurfaceVariantDark = Color(0xFF232820)
val NaturalBorderDark = Color(0xFF363D31)
val NaturalTextPrimaryDark = Color(0xFFE2E5DC)
val NaturalTextSecondaryDark = Color(0xFFA2A999)
val NaturalPrimaryDark = Color(0xFFA4AD96)
val NaturalPrimaryContainerDark = Color(0xFF323B2B)
val NaturalSecondaryDark = Color(0xFFBCC6AF)
val NaturalSecondaryContainerDark = Color(0xFF3E4636)

// Compatibility Aliases for Existing Emerald & Gold References
val Emerald900 = NaturalOlive900
val Emerald800 = NaturalOlive800
val Emerald700 = NaturalOlive600 // #56624B
val Emerald600 = NaturalOlive600
val Emerald500 = NaturalOlive500
val Emerald400 = NaturalOlive400
val Emerald300 = NaturalOlive300 // #A4AD96
val Emerald200 = NaturalOlive200
val Emerald100 = NaturalOlive100 // #DDE5D1
val Emerald50 = NaturalOlive50

val QuranGold = NaturalAmber
val QuranGoldDark = NaturalAmberDark
val QuranGoldLight = NaturalAmberLight
val QuranGoldContainer = NaturalAmberContainer
val QuranGoldContainerDark = NaturalAmberContainerDark

// Surfaces & Backgrounds Aliases
val SandBackground = NaturalBackgroundLight
val SandSurface = NaturalSurfaceLight
val SandSurfaceVariant = NaturalBorderLight
val SandBorder = NaturalBorderLight

val DarkBackground = NaturalBackgroundDark
val DarkSurface = NaturalSurfaceDark
val DarkSurfaceVariant = NaturalSurfaceVariantDark
val DarkBorder = NaturalBorderDark

// Text Colors Aliases
val TextPrimaryLight = NaturalTextPrimaryLight
val TextSecondaryLight = NaturalTextSecondaryLight
val TextPrimaryDark = NaturalTextPrimaryDark
val TextSecondaryDark = NaturalTextSecondaryDark

// Word Morphology Highlighting (Harmonized with Natural Tones)
val PosNounColor = Color(0xFF285E53) // pine teal
val PosVerbColor = Color(0xFF8A4930) // terracotta clay
val PosPrepositionColor = Color(0xFF4B6434) // moss green
val PosPronounColor = Color(0xFF5E4E6E) // earthy heather
val PosAdjectiveColor = Color(0xFF886326) // warm amber
val PosDefaultColor = Color(0xFF56624B) // natural olive

// Harmonize helper - blend dynamic color with brand olive for identity retention
fun harmonizeWithBrand(color: Color, blendRatio: Float = 0.25f): Color {
    // Simple lerp blend: 25% brand olive + 75% dynamic color
    return Color(
        red = color.red * (1 - blendRatio) + NaturalOlive600.red * blendRatio,
        green = color.green * (1 - blendRatio) + NaturalOlive600.green * blendRatio,
        blue = color.blue * (1 - blendRatio) + NaturalOlive600.blue * blendRatio,
        alpha = color.alpha
    )
}

