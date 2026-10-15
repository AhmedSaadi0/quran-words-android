package com.quranwords.ui.surah.detail.components

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.statusBars
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clipToBounds
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.layout.layout
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.pluralStringResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Constraints
import androidx.compose.ui.unit.dp
import com.quranwords.R
import com.quranwords.core.util.ReadingProgress
import com.quranwords.core.util.isMeccan
import com.quranwords.domain.model.Surah
import com.quranwords.ui.components.FontSizeControls
import com.quranwords.ui.components.SurahPagesHeader
import kotlin.math.roundToInt

/**
 * Surah header: pinned top bar (title + back + reading-progress rings) above
 * the collapsible area (font controls + page chips). Collapse physics live in
 * [SurahReadingScrollConnection], which the caller attaches via
 * `Modifier.nestedScroll(...)` to the common ancestor of this header and the
 * scrollable ayat list — attaching it here would never receive deltas because
 * this header has no scrollable child.
 *
 * [readingProgress] is invoked by [ReadingProgressRings] inside its own
 * composition so scroll-driven updates recompute only that row.
 */
@Composable
fun SurahDetailHeader(
    surah: Surah?,
    fontSize: Float,
    surahPages: List<Int>,
    /** Provided as a lambda so page changes recompose the header only — never the screen. */
    currentPage: () -> Int?,
    collapseState: SurahCollapsingHeaderState,
    readingProgress: () -> ReadingProgress?,
    onNavigateBack: () -> Unit,
    onFontSizeChange: (Float) -> Unit,
    onPageClick: (Int) -> Unit
) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .background(MaterialTheme.colorScheme.surface)
            .windowInsetsPadding(WindowInsets.statusBars)
    ) {
        // Pinned Top Bar Row (title + back + reading-progress rings)
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .height(64.dp)
                .padding(horizontal = 8.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(4.dp),
                modifier = Modifier.weight(1f)
            ) {
                IconButton(
                    onClick = onNavigateBack,
                    modifier = Modifier.testTag("back_button")
                ) {
                    Icon(
                        imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                        contentDescription = stringResource(R.string.cd_back)
                    )
                }
                Column(verticalArrangement = Arrangement.Center) {
                    Text(
                        // Surah name is Arabic reference data (never translated).
                        text = surah?.let { stringResource(R.string.bookmarks_surah_item, it.nameAr) }
                            ?: stringResource(R.string.surah_unnamed),
                        style = MaterialTheme.typography.titleLarge,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onSurface,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                    surah?.let { s ->
                        Text(
                            text = "${if (s.isMeccan) stringResource(R.string.revelation_meccan) else stringResource(R.string.revelation_medinan)}"
                                + " • ${pluralStringResource(R.plurals.ayah_count, s.ayahCount, s.ayahCount)}",
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            maxLines = 1
                        )
                    }
                }
            }

            // Reading position: whole-surah ring + current-juz ring with the
            // juz number inside it (bookmark action removed from this bar).
            ReadingProgressRings(readingProgress = readingProgress)
        }

        // Collapsible area: alpha fades + height/offset collapse (no layout-write)
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .clipToBounds()
                .graphicsLayer {
                    alpha = collapseState.collapseProgress
                }
                .layout { measurable, constraints ->
                    val placeable = measurable.measure(
                        constraints.copy(
                            minHeight = 0,
                            maxHeight = Constraints.Infinity
                        )
                    )
                    val naturalHeight = placeable.height
                    if (collapseState.collapsibleHeightPx != naturalHeight && naturalHeight > 0) {
                        collapseState.collapsibleHeightPx = naturalHeight
                    }
                    val currentHeight = (naturalHeight + collapseState.headerOffsetPx.roundToInt())
                        .coerceIn(0, naturalHeight)
                    layout(placeable.width, currentHeight) {
                        placeable.placeRelative(0, collapseState.headerOffsetPx.roundToInt())
                    }
                }
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 12.dp, vertical = 6.dp),
                verticalArrangement = Arrangement.spacedBy(6.dp)
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.Center,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    FontSizeControls(
                        fontSize = fontSize,
                        onFontSizeChange = onFontSizeChange
                    )
                }

                if (surahPages.isNotEmpty()) {
                    SurahPagesHeader(
                        pages = surahPages,
                        currentPage = currentPage(),
                        onPageClick = onPageClick,
                        modifier = Modifier.fillMaxWidth()
                    )
                }
            }
        }
    }
}
