package com.quranwords.ui.theme

import android.os.Build
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.dynamicDarkColorScheme
import androidx.compose.material3.dynamicLightColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.compositionLocalOf
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
/** Temporary long-press word focus — separate from selection/bookmark washes. */
val LocalWordFocusHighlight = compositionLocalOf { WordFocusHighlightLight }

private val DarkColorScheme = darkColorScheme(
    primary = NaturalPrimaryDark, // #A4AD96
    onPrimary = NaturalOlive900,
    primaryContainer = NaturalPrimaryContainerDark,
    onPrimaryContainer = NaturalOlive100, // #DDE5D1
    secondary = NaturalSecondaryDark,
    onSecondary = NaturalOlive900,
    secondaryContainer = NaturalSecondaryContainerDark,
    onSecondaryContainer = NaturalOlive100,
    tertiary = NaturalAmberLight,
    onTertiary = NaturalAmberDark,
    tertiaryContainer = NaturalAmberContainerDark,
    onTertiaryContainer = NaturalAmberContainer,
    background = NaturalBackgroundDark, // #121411
    onBackground = NaturalTextPrimaryDark,
    surface = NaturalSurfaceDark, // #191D17
    onSurface = NaturalTextPrimaryDark,
    surfaceVariant = NaturalSurfaceVariantDark,
    onSurfaceVariant = NaturalTextSecondaryDark,
    outline = NaturalBorderDark,
    outlineVariant = NaturalOutlineVariantDark
)

private val LightColorScheme = lightColorScheme(
    primary = NaturalOlive600, // #56624B
    onPrimary = Color.White,
    primaryContainer = NaturalOlive100, // #DDE5D1
    onPrimaryContainer = NaturalTextPrimaryLight, // #1A1C18
    secondary = NaturalOlive700, // #3F4E36
    onSecondary = Color.White,
    secondaryContainer = NaturalSecondaryContainerLight,
    onSecondaryContainer = NaturalTextPrimaryLight,
    tertiary = NaturalAmber, // #8C6E2D
    onTertiary = Color.White,
    tertiaryContainer = NaturalAmberContainer,
    onTertiaryContainer = NaturalAmberDark,
    background = NaturalBackgroundLight, // #F7F8F2
    onBackground = NaturalTextPrimaryLight, // #1A1C18
    surface = NaturalSurfaceLight, // #FFFFFF
    onSurface = NaturalTextPrimaryLight, // #1A1C18
    surfaceVariant = NaturalBorderLight, // #E1E3D9
    onSurfaceVariant = NaturalTextSecondaryLight, // #43483E
    outline = NaturalOutlineLight,
    outlineVariant = NaturalBorderLight // #E1E3D9
)

/**
 * Natural Tones theme with optional M3 dynamic colour on Android 12+.
 * Dynamic colours are harmonized toward the brand olive; otherwise the
 * light/dark Natural schemes below are used directly.
 */
@Composable
fun MyApplicationTheme(
    darkTheme: Boolean = isSystemInDarkTheme(),
    useDynamicColor: Boolean = false,
    content: @Composable () -> Unit
) {
    val context = LocalContext.current
    val colorScheme = when {
        useDynamicColor && Build.VERSION.SDK_INT >= Build.VERSION_CODES.S -> {
            val dynamicScheme = if (darkTheme) dynamicDarkColorScheme(context) else dynamicLightColorScheme(context)
            // Harmonize dynamic primary with brand olive to retain identity (25% blend)
            dynamicScheme.copy(
                primary = harmonizeWithBrand(dynamicScheme.primary),
                primaryContainer = harmonizeWithBrand(dynamicScheme.primaryContainer, 0.15f),
                secondary = harmonizeWithBrand(dynamicScheme.secondary, 0.20f)
            )
        }
        darkTheme -> DarkColorScheme
        else -> LightColorScheme
    }
    CompositionLocalProvider(
        LocalWordFocusHighlight provides if (darkTheme) WordFocusHighlightDark else WordFocusHighlightLight
    ) {
        MaterialTheme(
            colorScheme = colorScheme,
            typography = AppTypography,
            shapes = AppShapes,
            content = content
        )
    }
}
