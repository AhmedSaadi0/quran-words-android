package com.quranwords.ui.surah.components

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Bookmark
import androidx.compose.material.icons.outlined.BookmarkBorder
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.pluralStringResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.quranwords.R
import com.quranwords.core.util.isMeccan
import com.quranwords.domain.model.Surah

/** Horizontal inset of the row divider: aligned with the name column. */
private val ROW_DIVIDER_INSET = 20.dp + OctagramBadgeSize + 16.dp

/**
 * Flat quran.com-style surah row: an octagram number medallion, the localized
 * surah name over a muted meta line (other name • revelation • ayah count),
 * a ghost bookmark action, and a hairline divider — no card, border or
 * elevation. The primary name follows the app locale (Arabic name in Arabic,
 * transliteration in English), like the quran.com index.
 */
@Composable
fun SurahRow(
    surah: Surah,
    isBookmarked: Boolean,
    onClick: () -> Unit,
    onBookmarkClick: (() -> Unit)? = null,
    modifier: Modifier = Modifier
) {
    val isArabic = (LocalConfiguration.current.locales.get(0)?.language ?: "ar").startsWith("ar")
    Column(modifier = modifier.fillMaxWidth().testTag("surah_item_${surah.id}")) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .clickable(onClick = onClick)
                .padding(horizontal = 20.dp, vertical = 12.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            OctagramNumberBadge(
                number = stringResource(R.string.number_plain, surah.id),
                modifier = Modifier.size(OctagramBadgeSize)
            )
            Spacer(modifier = Modifier.width(16.dp))
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    // Surah names are Arabic reference data (never translated).
                    text = if (isArabic) surah.nameAr else surah.nameEn,
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.SemiBold,
                    color = MaterialTheme.colorScheme.onSurface
                )
                Text(
                    text = listOf(
                        if (isArabic) surah.nameEn else surah.nameAr,
                        if (surah.isMeccan) stringResource(R.string.revelation_meccan)
                        else stringResource(R.string.revelation_medinan),
                        pluralStringResource(R.plurals.ayah_count, surah.ayahCount, surah.ayahCount)
                    ).joinToString(" • "),
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
            if (onBookmarkClick != null) {
                // Ghost icon (no chip): quran.com-clean trailing action, still
                // a full 48dp touch target via IconButton's default size.
                IconButton(
                    onClick = onBookmarkClick,
                    modifier = Modifier.testTag("bookmark_surah_${surah.id}")
                ) {
                    Icon(
                        imageVector = if (isBookmarked) Icons.Filled.Bookmark else Icons.Outlined.BookmarkBorder,
                        contentDescription = stringResource(
                            if (isBookmarked) R.string.cd_unbookmark_surah else R.string.cd_bookmark_surah
                        ),
                        tint = if (isBookmarked) MaterialTheme.colorScheme.primary
                        else MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
        }
        HorizontalDivider(
            modifier = Modifier.padding(start = ROW_DIVIDER_INSET),
            color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f)
        )
    }
}
