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
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import io.github.ahmedsaadi0.quranwords.R
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

    // Resolve localized parts in @Composable context first: stringResource
    // cannot be called from inside the buildString lambda below.
    val quarter = rubElHizb?.let { ((it - 1) % 4) + 1 }
    val juzText = if (isJuzStart && juz != null) stringResource(R.string.juz_label, juz) else null
    val hizbText = if (hizb != null) stringResource(R.string.hizb_label, hizb) else null
    val quarterText = quarter?.let { stringResource(R.string.quarter_label, it) }
    val text = buildString {
        if (juzText != null) {
            append(juzText)
            if (isHizbStart && hizbText != null) {
                append(" • $hizbText")
            }
            // Quarter only if juz start coincides with hizb but rub is not 1 of that hizb
            // Still show quarter for full Mushaf accuracy when rub present and not aligned
            if (quarter != null && quarter != 1) {
                append(" • $quarterText")
            }
        } else if (isHizbStart && hizbText != null) {
            append(hizbText)
            // Show quarter 2..4 for hizb boundaries; clean Hizb start shows Hizb only
            if (quarter != null && quarter != 1) {
                append(" • $quarterText")
            }
        } else if (quarterText != null) {
            // Rub-only (quarter inside hizb) — rare, but handle
            if (hizbText != null) {
                append("$hizbText • $quarterText")
            } else {
                append(quarterText)
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
