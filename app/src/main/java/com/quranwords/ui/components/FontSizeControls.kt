package com.quranwords.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.material3.minimumInteractiveComponentSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Slider
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.quranwords.R
import com.quranwords.core.util.FontSizePreference
import com.quranwords.ui.theme.AppTextSize
import com.quranwords.ui.theme.ShapeLarge
import com.quranwords.ui.theme.ShapeSmall
@Composable
fun FontSizeControls(
    fontSize: Float,
    onFontSizeChange: (Float) -> Unit,
    modifier: Modifier = Modifier
) {
    Row(
        modifier = modifier
            .clip(ShapeLarge)
            .background(MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f))
            .border(1.dp, MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f), ShapeLarge)
            .padding(horizontal = 4.dp, vertical = 2.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(2.dp)
    ) {
        IconButton(
            onClick = {
                if (fontSize > FontSizePreference.MIN) {
                    onFontSizeChange((fontSize - 1f).coerceAtLeast(FontSizePreference.MIN))
                }
            },
            modifier = Modifier
                .minimumInteractiveComponentSize()
                .testTag("decrease_font_size")
        ) {
            Text(stringResource(R.string.font_decrease), fontWeight = FontWeight.Bold, fontSize = AppTextSize.Micro)
        }

        // Slider spans the same bounds as the preference clamp
        // (FontSizePreference), so the mushaf and the cards can never disagree
        // at the edges. Fixed width for TopAppBar actions overflow safety.
        Slider(
            value = fontSize.coerceIn(FontSizePreference.MIN, FontSizePreference.MAX),
            onValueChange = { onFontSizeChange(it) },
            valueRange = FontSizePreference.MIN..FontSizePreference.MAX,
            steps = (FontSizePreference.MAX - FontSizePreference.MIN).toInt() - 1,
            modifier = Modifier
                .weight(1f, fill = true)
                .testTag("font_size_slider")
        )

        IconButton(
            onClick = {
                if (fontSize < FontSizePreference.MAX) {
                    onFontSizeChange((fontSize + 1f).coerceAtMost(FontSizePreference.MAX))
                }
            },
            modifier = Modifier
                .minimumInteractiveComponentSize()
                .testTag("increase_font_size")
        ) {
            Text(stringResource(R.string.font_increase), fontWeight = FontWeight.Bold, fontSize = AppTextSize.Small)
        }

        // Live preview chip with numeric value
        Box(
            modifier = Modifier
                .clip(ShapeSmall)
                .background(MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.6f))
                .padding(horizontal = 6.dp, vertical = 3.dp)
                .testTag("font_size_preview"),
            contentAlignment = Alignment.Center
        ) {
            Text(
                text = "${fontSize.toInt()}",
                style = MaterialTheme.typography.labelSmall,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.primary,
                fontSize = AppTextSize.Micro
            )
        }
    }
}
