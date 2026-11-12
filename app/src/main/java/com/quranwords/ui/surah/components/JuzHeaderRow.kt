package com.quranwords.ui.surah.components

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.quranwords.R
import com.quranwords.core.util.SurahMetadata
import com.quranwords.core.util.SurahMetadata.JuzRange

/**
 * Interleaved juz boundary band (Full Plan v2, D3): a full-bleed tinted strip
 * showing "الجزء N" and its "start – end" span, between the flat surah rows
 * of the quran.com-style index. Clicking jumps to the juz start. Pure chrome.
 */
@Composable
fun JuzHeaderRow(
    juzId: Int,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    val range = SurahMetadata.juzRange(juzId)
    Surface(
        modifier = modifier.fillMaxWidth()
            .clickable(onClick = onClick)
            .testTag("juz_header_${juzId}"),
        color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f)
    ) {
        Row(
            modifier = Modifier.fillMaxWidth().padding(horizontal = 20.dp, vertical = 9.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = stringResource(R.string.juz_label, juzId),
                fontSize = 13.5.sp,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            Text(
                text = range?.localizedRange() ?: "",
                fontSize = 13.5.sp,
                fontWeight = FontWeight.Medium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }
}

/** Shared with [JuzRow]: "الفاتحة 1 – البقرة 141" following the app locale. */
@Composable
internal fun JuzRange.localizedRange(): String {
    val isArabic = (LocalConfiguration.current.locales.get(0)?.language ?: "ar").startsWith("ar")
    val startName = SurahMetadata.SURAHS[startSurahId - 1].let { if (isArabic) it.nameAr else it.nameEn }
    val endName = SurahMetadata.SURAHS[endSurahId - 1].let { if (isArabic) it.nameAr else it.nameEn }
    return stringResource(R.string.juz_range_label, startName, startAyah, endName, endAyah)
}
