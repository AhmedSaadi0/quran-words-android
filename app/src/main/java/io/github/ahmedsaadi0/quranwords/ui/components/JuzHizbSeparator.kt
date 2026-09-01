package io.github.ahmedsaadi0.quranwords.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import io.github.ahmedsaadi0.quranwords.ui.theme.ShapeSmall

@Composable
fun JuzHizbSeparator(
    juz: Int?,
    hizb: Int?,
    rubElHizb: Int?,
    isJuzStart: Boolean,
    isHizbStart: Boolean,
    modifier: Modifier = Modifier
) {
    // No separator if not a boundary (caller guards, but keep safe)
    if (!isJuzStart && !isHizbStart) return

    val text = buildString {
        if (isJuzStart && juz != null) {
            append("الجزء $juz")
            if (isHizbStart && hizb != null) {
                append(" • الحزب $hizb")
            }
            // Quarter only if juz start coincides with hizb but rub is not 1 of that hizb
            // Still show quarter for full Mushaf accuracy when rub present and not aligned
            if (rubElHizb != null) {
                val quarter = ((rubElHizb - 1) % 4) + 1
                if (quarter != 1) {
                    append(" • ربع $quarter")
                }
            }
        } else if (isHizbStart && hizb != null) {
            append("الحزب $hizb")
            if (rubElHizb != null) {
                val quarter = ((rubElHizb - 1) % 4) + 1
                // Show quarter 1..4 for hizb boundaries
                if (quarter != 1) {
                    append(" • ربع $quarter")
                } else if (hizb != null) {
                    // For clean Hizb start (quarter 1), just show Hizb
                }
            }
        } else if (rubElHizb != null) {
            // Rub-only (quarter inside hizb) — rare, but handle
            val quarter = ((rubElHizb - 1) % 4) + 1
            if (hizb != null) {
                append("الحزب $hizb • ربع $quarter")
            } else {
                append("ربع $quarter")
            }
        }
    }

    if (text.isBlank()) return

    // Subtle divider + pill — 28dp total height, does not disturb reading
    Row(
        modifier = modifier
            .fillMaxWidth()
            .padding(vertical = 4.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        HorizontalDivider(
            modifier = Modifier.weight(1f),
            thickness = 0.5.dp,
            color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.6f)
        )
        Box(
            modifier = Modifier
                .clip(ShapeSmall)
                .background(
                    if (isJuzStart) MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.45f)
                    else MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.55f)
                )
                .border(1.dp, MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.6f), ShapeSmall)
                .padding(horizontal = 10.dp, vertical = 4.dp),
            contentAlignment = Alignment.Center
        ) {
            Text(
                text = text,
                style = MaterialTheme.typography.labelSmall,
                fontWeight = FontWeight.Medium,
                fontSize = 11.sp,
                lineHeight = 14.sp,
                color = if (isJuzStart) MaterialTheme.colorScheme.primary
                else MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
        HorizontalDivider(
            modifier = Modifier.weight(1f),
            thickness = 0.5.dp,
            color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.6f)
        )
    }
}
