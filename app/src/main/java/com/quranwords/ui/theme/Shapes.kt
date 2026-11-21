package com.quranwords.ui.theme

import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Shapes
import androidx.compose.ui.unit.dp
// M3 unified shape scale - 12 / 16 / 20 as requested
val AppShapes = Shapes(
    extraSmall = RoundedCornerShape(8.dp),
    small = RoundedCornerShape(12.dp),      // Small cards, chips, badges
    medium = RoundedCornerShape(16.dp),     // Standard cards, dialogs
    large = RoundedCornerShape(20.dp),      // Hero banners, header cards
    extraLarge = RoundedCornerShape(28.dp)
)

// Aliases for semantic use — derive from AppShapes so radii stay in sync.
val ShapeExtraSmall = AppShapes.extraSmall
val ShapeSmall = AppShapes.small
val ShapeMedium = AppShapes.medium
val ShapeLarge = AppShapes.large
val ShapeExtraLarge = AppShapes.extraLarge
