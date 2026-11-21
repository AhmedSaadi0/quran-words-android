package com.quranwords

import androidx.compose.runtime.Composable
import androidx.compose.ui.platform.LocalLayoutDirection
import androidx.compose.ui.unit.LayoutDirection
import androidx.compose.ui.test.junit4.ComposeContentTestRule
import com.quranwords.ui.theme.MyApplicationTheme

/** Renders composables under the app theme with an explicit reading direction. */
fun ComposeContentTestRule.setThemedContent(
    rtl: Boolean = false,
    content: @Composable () -> Unit
) = setContent {
    MyApplicationTheme {
        androidx.compose.runtime.CompositionLocalProvider(
            LocalLayoutDirection provides if (rtl) LayoutDirection.Rtl else LayoutDirection.Ltr
        ) {
            content()
        }
    }
}
