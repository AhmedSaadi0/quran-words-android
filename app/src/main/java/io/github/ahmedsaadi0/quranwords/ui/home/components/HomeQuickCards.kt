package io.github.ahmedsaadi0.quranwords.ui.home.components

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Bookmark
import androidx.compose.material.icons.outlined.BookmarkBorder
import androidx.compose.material.icons.outlined.Star
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.pluralStringResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import io.github.ahmedsaadi0.quranwords.R
import io.github.ahmedsaadi0.quranwords.core.util.SurahMetadata
import io.github.ahmedsaadi0.quranwords.ui.theme.ShapeMedium

/** Continue-reading card (Decision 13: 🔖 → vector). */
@Composable
fun ContinueReadingCard(
    lastReadSurah: Int,
    lastReadAyah: Int,
    onNavigateToSurahDetail: (Int, Int) -> Unit,
    modifier: Modifier = Modifier
) {
    val lastSurahMeta = SurahMetadata.SURAHS.firstOrNull { it.id == lastReadSurah }
        ?: SurahMetadata.SURAHS[0]
    Card(
        modifier = modifier
            .fillMaxWidth()
            .clip(ShapeMedium)
            .border(1.dp, MaterialTheme.colorScheme.outlineVariant, ShapeMedium)
            .clickable { onNavigateToSurahDetail(lastReadSurah, lastReadAyah) }
            .testTag("continue_reading_card"),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surface
        ),
        elevation = CardDefaults.cardElevation(defaultElevation = 0.5.dp)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            horizontalArrangement = Arrangement.spacedBy(12.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Box(
                modifier = Modifier
                    .size(46.dp)
                    .clip(CircleShape)
                    .background(MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.5f)),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = Icons.Filled.Bookmark,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.primary
                )
            }
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = stringResource(R.string.home_continue_title),
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onSurface,
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis
                )
                // Surah name is Arabic reference data (never translated);
                // only the surrounding chrome template is localized.
                Text(
                    text = stringResource(
                        R.string.home_continue_template,
                        lastSurahMeta.nameAr,
                        lastReadAyah
                    ),
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
            }
        }
    }
}

/** Bookmarks quick-access card (Decision 13: ⭐/🔖 → vectors). */
@Composable
fun BookmarksQuickCard(
    bookmarkedSurahs: Set<String>,
    bookmarkedAyat: Set<String>,
    onNavigateToBookmarks: () -> Unit,
    modifier: Modifier = Modifier
) {
    val hasBookmarks = bookmarkedSurahs.isNotEmpty() || bookmarkedAyat.isNotEmpty()
    Card(
        modifier = modifier
            .fillMaxWidth()
            .clip(ShapeMedium)
            .border(1.dp, MaterialTheme.colorScheme.outlineVariant, ShapeMedium)
            .clickable { onNavigateToBookmarks() }
            .testTag("bookmarks_quick_card"),
        colors = CardDefaults.cardColors(
            containerColor = if (hasBookmarks) {
                MaterialTheme.colorScheme.tertiaryContainer.copy(alpha = 0.35f)
            } else {
                MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f)
            }
        )
    ) {
        // Tag retained on the content row so the navigation entry point stays testable.
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp)
                .testTag("open_bookmarks_btn"),
            horizontalArrangement = Arrangement.spacedBy(12.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Box(
                modifier = Modifier
                    .size(46.dp)
                    .clip(CircleShape)
                    .background(
                        if (hasBookmarks) MaterialTheme.colorScheme.tertiaryContainer
                        else MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.5f)
                    ),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = if (hasBookmarks) Icons.Outlined.Star else Icons.Outlined.BookmarkBorder,
                    contentDescription = null,
                    tint = if (hasBookmarks) MaterialTheme.colorScheme.tertiary
                    else MaterialTheme.colorScheme.primary
                )
            }
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = stringResource(R.string.home_bookmarks_title),
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onSurface,
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis
                )
                Text(
                    text = if (hasBookmarks) {
                        val surahsCount = bookmarkedSurahs.size
                        val ayatCount = bookmarkedAyat.size
                        val surahsText = pluralStringResource(
                            R.plurals.home_bookmarks_surahs,
                            surahsCount,
                            surahsCount
                        )
                        val ayatText = pluralStringResource(
                            R.plurals.home_bookmarks_ayahs,
                            ayatCount,
                            ayatCount
                        )
                        "$surahsText • $ayatText ${stringResource(R.string.home_bookmarks_saved_suffix)}"
                    } else {
                        stringResource(R.string.home_bookmarks_empty)
                    },
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
            }
        }
    }
}