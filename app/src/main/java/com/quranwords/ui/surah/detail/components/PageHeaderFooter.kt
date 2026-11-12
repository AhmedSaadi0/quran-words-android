package com.quranwords.ui.surah.detail.components

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.quranwords.R
import com.quranwords.core.util.SurahMetadata
import com.quranwords.core.util.toEasternArabicDigits

/**
 * Mushaf page header (Full Plan v2): "سورة X | الجزء Y (• الحزب H) • صفحة Z"
 * above each page block — Lexicon's header in flat tokens, no image frame.
 * [hizb] is passed only on pages opening a hizb, where the removed
 * juz/hizb separator used to appear.
 */
@Composable
fun PageHeaderRow(
    surahId: Int,
    juz: Int?,
    hizb: Int? = null,
    pageNumber: Int?,
    modifier: Modifier = Modifier
) {
    val surahName = SurahMetadata.SURAHS.getOrNull(surahId - 1)?.nameAr ?: ""
    Row(
        modifier = modifier.fillMaxWidth().padding(horizontal = 6.dp, vertical = 4.dp).testTag("page_header_$pageNumber"),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(
            text = stringResource(R.string.bookmarks_surah_item, surahName),
            fontSize = 11.5.sp,
            fontWeight = FontWeight.Bold,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
        Text(
            text = when {
                juz != null && hizb != null && pageNumber != null ->
                    stringResource(R.string.page_header_juz, juz.toEasternArabicDigits()) + " • " +
                        stringResource(R.string.hizb_label, hizb) + " • " +
                        stringResource(R.string.page_header_page, pageNumber.toEasternArabicDigits())
                juz != null && pageNumber != null -> stringResource(R.string.page_header_juz, juz.toEasternArabicDigits()) + " • " + stringResource(R.string.page_header_page, pageNumber.toEasternArabicDigits())
                juz != null -> stringResource(R.string.page_header_juz, juz.toEasternArabicDigits())
                pageNumber != null -> stringResource(R.string.page_header_page, pageNumber.toEasternArabicDigits())
                else -> ""
            },
            fontSize = 11.5.sp,
            fontWeight = FontWeight.Bold,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
    }
}
