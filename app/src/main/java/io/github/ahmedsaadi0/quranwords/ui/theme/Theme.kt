package io.github.ahmedsaadi0.quranwords.ui.theme

import android.os.Build
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.dynamicDarkColorScheme
import androidx.compose.material3.dynamicLightColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext

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
    outlineVariant = Color(0xFF293023)
)

private val LightColorScheme = lightColorScheme(
    primary = NaturalOlive600, // #56624B
    onPrimary = Color.White,
    primaryContainer = NaturalOlive100, // #DDE5D1
    onPrimaryContainer = NaturalTextPrimaryLight, // #1A1C18
    secondary = NaturalOlive700, // #3F4E36
    onSecondary = Color.White,
    secondaryContainer = Color(0xFFE5EBDB),
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
 * Three-state theme: System / Natural / Dynamic
 * - colorMode: 0 = system (follow darkTheme), 1 = force light Natural, 2 = force dark Natural
 * - useDynamicColor: true = use M3 dynamic on 12+ harmonized with brand
 * Combined: when useDynamicColor true and Android 12+, dynamic scheme is used (harmonized).
 * Otherwise fall back to Natural Tones.
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
    MaterialTheme(
        colorScheme = colorScheme,
        typography = Typography,
        shapes = AppShapes,
        content = content
    )
}
