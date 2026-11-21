package com.quranwords.ui.theme

import org.junit.Assert.assertEquals
import org.junit.Test

class ThemeTokenTest {
    @Test
    fun shape_aliases_match_app_shapes() {
        assertEquals(AppShapes.extraSmall, ShapeExtraSmall)
        assertEquals(AppShapes.small, ShapeSmall)
        assertEquals(AppShapes.medium, ShapeMedium)
        assertEquals(AppShapes.large, ShapeLarge)
        assertEquals(AppShapes.extraLarge, ShapeExtraLarge)
    }
}
