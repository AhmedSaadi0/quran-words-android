package com.quranwords.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Slider
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.quranwords.core.util.FontSizePreference

@Composable
fun FontSizeControls(
    fontSize: Float,
    onFontSizeChange: (Float) -> Unit,
    modifier: Modifier = Modifier
) {
    Row(
        modifier = modifier
            .clip(RoundedCornerShape(20.dp))
            .background(MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f))
            .border(1.dp, MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f), RoundedCornerShape(20.dp))
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
                .size(32.dp)
                .testTag("decrease_font_size")
        ) {
            Text("A-", fontWeight = FontWeight.Bold, fontSize = 11.sp)
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
                .size(32.dp)
                .testTag("increase_font_size")
        ) {
            Text("A+", fontWeight = FontWeight.Bold, fontSize = 12.sp)
        }

        // Live preview chip with numeric value
        Box(
            modifier = Modifier
                .clip(RoundedCornerShape(8.dp))
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
                fontSize = 11.sp
            )
        }
    }
}
