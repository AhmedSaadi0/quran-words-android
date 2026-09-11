package io.github.ahmedsaadi0.quranwords.ui.mushaf.components

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.sp
import io.github.ahmedsaadi0.quranwords.core.util.toEasternArabicDigits
import io.github.ahmedsaadi0.quranwords.ui.theme.AmiriQuran

/**
 * Centered Eastern-Arabic page number at the bottom of the frame.
 */
@Composable
fun MushafPageFooter(
    page: Int,
    modifier: Modifier = Modifier
) {
    Box(
        modifier = modifier
            .fillMaxWidth()
            .testTag("mushaf_page_footer"),
        contentAlignment = Alignment.Center
    ) {
        Text(
            text = page.toEasternArabicDigits(),
            fontFamily = AmiriQuran,
            fontWeight = FontWeight.Bold,
            fontSize = 16.sp,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
    }
}
