package com.quranwords.ui.surah.components

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.quranwords.R
import com.quranwords.core.util.SurahMetadata

/** Horizontal inset of the row divider: aligned with the title column. */
private val ROW_DIVIDER_INSET = 20.dp + OctagramBadgeSize + 16.dp

/**
 * Flat quran.com-style juz row: an octagram number medallion, the localized
 * juz label over a muted "الفاتحة 1 – البقرة 141" range line, and a hairline
 * divider — no card, border or elevation. Tapping jumps to the juz's first
 * ayah.
 */
@Composable
fun JuzRow(
    juzId: Int,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    val range = SurahMetadata.juzRange(juzId)
    Column(modifier = modifier.fillMaxWidth().testTag("juz_item_${juzId}")) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .clickable(onClick = onClick)
                .padding(horizontal = 20.dp, vertical = 12.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            OctagramNumberBadge(
                number = stringResource(R.string.number_plain, juzId),
                modifier = Modifier.size(OctagramBadgeSize)
            )
            Spacer(modifier = Modifier.width(16.dp))
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = stringResource(R.string.juz_label, juzId),
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.SemiBold,
                    color = MaterialTheme.colorScheme.onSurface
                )
                Text(
                    text = range?.localizedRange() ?: "",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }
        HorizontalDivider(
            modifier = Modifier.padding(start = ROW_DIVIDER_INSET),
            color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f)
        )
    }
}

