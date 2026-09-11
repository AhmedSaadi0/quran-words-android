package io.github.ahmedsaadi0.quranwords.ui.surah.detail.components

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
import androidx.compose.material.icons.automirrored.filled.MenuBook
import androidx.compose.material.icons.filled.Bookmark
import androidx.compose.material.icons.outlined.BookmarkBorder
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clipToBounds
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.input.nestedscroll.NestedScrollConnection
import androidx.compose.ui.input.nestedscroll.NestedScrollSource
import androidx.compose.ui.layout.layout
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.pluralStringResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Constraints
import androidx.compose.ui.unit.dp
import io.github.ahmedsaadi0.quranwords.R
import io.github.ahmedsaadi0.quranwords.core.util.isMeccan
import io.github.ahmedsaadi0.quranwords.domain.model.Surah
import io.github.ahmedsaadi0.quranwords.ui.components.FontSizeControls
import io.github.ahmedsaadi0.quranwords.ui.components.SurahPagesHeader
import kotlin.math.roundToInt

/**
 * Surah header: pinned top bar (title + back + bookmark) above the
 * collapsible area (font controls + page chips). Collapse physics live in
 * [rememberNestedScrollCollapse], which the caller must attach via
 * `Modifier.nestedScroll(...)` to the common ancestor of this header and the
 * scrollable ayat list — attaching it here would never receive deltas because
 * this header has no scrollable child.
 */
@Composable
fun SurahDetailHeader(
    surah: Surah?,
    isBookmarked: Boolean,
    fontSize: Float,
    surahPages: List<Int>,
    currentPage: Int?,
    collapseState: SurahCollapsingHeaderState,
    onNavigateBack: () -> Unit,
    onToggleBookmark: () -> Unit,
    onFontSizeChange: (Float) -> Unit,
    onPageClick: (Int) -> Unit,
    onOpenMushaf: (() -> Unit)? = null
) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .background(MaterialTheme.colorScheme.surface)
            .windowInsetsPadding(WindowInsets.statusBars)
    ) {
        // Pinned Top Bar Row (dedicated to title + back + bookmark)
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

            Row(
                verticalAlignment = Alignment.CenterVertically
            ) {
                if (onOpenMushaf != null) {
                    IconButton(
                        onClick = onOpenMushaf,
                        modifier = Modifier.testTag("open_mushaf_reader")
                    ) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.MenuBook,
                            contentDescription = stringResource(R.string.cd_open_mushaf),
                            tint = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
                IconButton(
                    onClick = onToggleBookmark,
                    modifier = Modifier.testTag("bookmark_button")
                ) {
                    Icon(
                        imageVector = if (isBookmarked) Icons.Filled.Bookmark else Icons.Outlined.BookmarkBorder,
                        contentDescription = stringResource(R.string.cd_save_surah),
                        tint = if (isBookmarked) MaterialTheme.colorScheme.primary
                        else MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
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
                        currentPage = currentPage,
                        onPageClick = onPageClick,
                        modifier = Modifier.fillMaxWidth()
                    )
                }
            }
        }
    }
}

/**
 * Quick-return / enter-always collapse connection — physics preserved 1:1.
 * Must be hoisted by the screen and attached to the common ancestor of the
 * header and the scrollable list so scroll deltas reach [onPreScroll].
 */
@Composable
fun rememberNestedScrollCollapse(
    state: SurahCollapsingHeaderState,
    isSelectionActive: Boolean
): NestedScrollConnection = androidx.compose.runtime.remember(isSelectionActive) {
    object : NestedScrollConnection {
        override fun onPreScroll(available: Offset, source: NestedScrollSource): Offset {
            if (isSelectionActive) return Offset.Zero
            val delta = available.y
            if (state.collapsibleHeightPx > 0) {
                // Collapses immediately on scroll down, reappears on scroll up
                // from anywhere on the page.
                if ((delta < 0f && state.headerOffsetPx > -state.collapsibleHeightPx) ||
                    (delta > 0f && state.headerOffsetPx < 0f)
                ) {
                    val prevOffset = state.headerOffsetPx
                    state.headerOffsetPx = (state.headerOffsetPx + delta)
                        .coerceIn(-state.collapsibleHeightPx.toFloat(), 0f)
                    val consumed = state.headerOffsetPx - prevOffset
                    return Offset(0f, consumed)
                }
            }
            return Offset.Zero
        }
    }
}