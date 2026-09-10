package io.github.ahmedsaadi0.quranwords.ui.roots.detail.components

import androidx.compose.foundation.background
import androidx.compose.foundation.gestures.Orientation
import androidx.compose.foundation.gestures.draggable
import androidx.compose.foundation.gestures.rememberDraggableState
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBars
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.foundation.layout.wrapContentHeight
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.outlined.ContentCopy
import androidx.compose.material.icons.outlined.ReportProblem
import androidx.compose.material.icons.outlined.Share
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Badge
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ScrollableTabRow
import androidx.compose.material3.Tab
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.SideEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clipToBounds
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.layout.onSizeChanged
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.dp
import io.github.ahmedsaadi0.quranwords.R
import io.github.ahmedsaadi0.quranwords.ui.roots.detail.util.RootDetailTab
import kotlin.math.roundToInt

/**
 * Collapsing header for RootDetail (Phase 5a).
 *
 * Owns ONLY layout/draw reads of [CollapsingHeaderState]:
 * - natural height via `onSizeChanged` (no state-write inside `layout`),
 * - collapse via fixed height + `offset` + `graphicsLayer(alpha)`,
 * - same pixel physics as legacy (`offset` clamped, progress for alpha).
 *
 * All text is precomputed by the ViewModel ([RootDetailUiState.subtitleText],
 * [RootDetailUiState.aiMetaLine]) — no date cleaning or string building here.
 * The parent never reads [state]'s offset/height — it only passes the stable
 * state object + `nestedScrollConnection` — so drag events recompose this
 * header alone, not the pager.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun RootDetailHeader(
    state: CollapsingHeaderState,
    rootText: String,
    subtitleText: String?,
    hasSubtitle: Boolean,
    aiMetaLine: String?,
    hasAiMeta: Boolean,
    tabCounts: Map<RootDetailTab, Int>,
    pagerTabs: List<RootDetailTab>,
    selectedTabIndex: Int,
    onNavigateBack: () -> Unit,
    onReportClick: () -> Unit,
    onCopyAiSummary: () -> Unit,
    onShareAiSummary: () -> Unit,
    onTabClick: (Int) -> Unit,
    modifier: Modifier = Modifier,
) {

    val density = LocalDensity.current
    val currentHeightDp = with(density) {
        state.currentHeightPx.toDp()
    }

    var showFullSummary by remember { mutableStateOf(false) }

    // 40% viewport cap so tabs + list head stay grabbable at any font scale.
    // Screen height approximates the Scaffold content (no bottom bar on detail
    // routes); recomputed automatically on rotation via LocalConfiguration.
    val capDp = LocalConfiguration.current.screenHeightDp.dp * 0.4f
    SideEffect {
        state.onViewportCapChanged(with(density) { capDp.roundToPx() })
    }

    Column(
        modifier = modifier
            .fillMaxWidth()
            .background(MaterialTheme.colorScheme.surface)
            .windowInsetsPadding(WindowInsets.statusBars)
    ) {
        // Pinned Top Bar Row (Always 64dp)
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .height(64.dp)
                .padding(horizontal = 16.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(4.dp)
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
                Text(
                    text = stringResource(R.string.root_title_template, rootText),
                    style = MaterialTheme.typography.titleLarge,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onSurface,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
            }
            IconButton(
                onClick = onReportClick,
                modifier = Modifier.testTag("report_help_button")
            ) {
                Icon(
                    imageVector = Icons.Outlined.ReportProblem,
                    contentDescription = stringResource(R.string.cd_report),
                    tint = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }

        // Collapsible subtitle: fixed-height clip + offset (no layout-write).
        // Capped to 40% of the viewport so tabs stay grabbable at any font
        // scale; direct draggable drive fixes touch starvation when the
        // summary fills the screen (plain Columns dispatch no nested scroll).
        if (hasSubtitle || hasAiMeta) {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .then(
                        if (state.subtitleHeightPx > 0) {
                            Modifier.height(minOf(currentHeightDp, capDp))
                        } else {
                            Modifier
                        }
                    )
                    .clipToBounds()
                    .graphicsLayer {
                        alpha = state.collapseProgress
                    }
                    .draggable(
                        orientation = Orientation.Vertical,
                        enabled = !state.isSelectionMode && state.subtitleHeightPx > 0,
                        state = rememberDraggableState { delta -> state.onHandledDrag(delta) }
                    )
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .offset {
                            IntOffset(0, state.headerOffsetPx.roundToInt())
                        }
                        .wrapContentHeight(align = Alignment.Top, unbounded = true)
                        .onSizeChanged { size ->
                            state.onNaturalHeightMeasured(size.height)
                        }
                        .padding(horizontal = 16.dp, vertical = 8.dp),
                    verticalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    if (hasSubtitle) {
                        Text(
                            text = subtitleText ?: "",
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurface
                        )
                    }
                    if (hasAiMeta && !aiMetaLine.isNullOrBlank()) {
                        Text(
                            text = aiMetaLine,
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                    }
                    if (hasSubtitle) {
                        Row(
                            horizontalArrangement = Arrangement.spacedBy(0.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            IconButton(
                                onClick = onCopyAiSummary,
                                modifier = Modifier
                                    .size(28.dp)
                                    .testTag("copy_ai_summary_btn")
                            ) {
                                Icon(
                                    imageVector = Icons.Outlined.ContentCopy,
                                    contentDescription = stringResource(R.string.cd_copy_ai_summary),
                                    tint = MaterialTheme.colorScheme.onSurfaceVariant,
                                    modifier = Modifier.size(16.dp)
                                )
                            }
                            IconButton(
                                onClick = onShareAiSummary,
                                modifier = Modifier
                                    .size(28.dp)
                                    .testTag("share_ai_summary_btn")
                            ) {
                                Icon(
                                    imageVector = Icons.Outlined.Share,
                                    contentDescription = stringResource(R.string.share_ai_summary),
                                    tint = MaterialTheme.colorScheme.onSurfaceVariant,
                                    modifier = Modifier.size(16.dp)
                                )
                            }
                        }
                    }
                    if (state.isCapped && hasSubtitle) {
                        TextButton(
                            onClick = { showFullSummary = true },
                            modifier = Modifier.testTag("read_more_summary_btn")
                        ) {
                            Text(
                                text = stringResource(R.string.read_more),
                                style = MaterialTheme.typography.labelMedium,
                                fontWeight = FontWeight.Bold
                            )
                        }
                    }
                }
            }
        }

        // Pinned tabs
        RootDetailTabRow(
            pagerTabs = pagerTabs,
            tabCounts = tabCounts,
            selectedTabIndex = selectedTabIndex,
            onTabClick = onTabClick
        )

        if (showFullSummary && hasSubtitle) {
            AlertDialog(
                onDismissRequest = { showFullSummary = false },
                title = {
                    Text(
                        text = stringResource(R.string.root_title_template, rootText),
                        style = MaterialTheme.typography.titleLarge,
                        fontWeight = FontWeight.Bold
                    )
                },
                text = {
                    Column(
                        modifier = Modifier.verticalScroll(rememberScrollState())
                    ) {
                        Text(
                            text = subtitleText ?: "",
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurface
                        )
                        if (hasAiMeta && !aiMetaLine.isNullOrBlank()) {
                            Text(
                                text = aiMetaLine,
                                style = MaterialTheme.typography.labelSmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }
                },
                confirmButton = {
                    TextButton(onClick = { showFullSummary = false }) {
                        Text(stringResource(R.string.common_close))
                    }
                }
            )
        }
    }
}

@Composable
fun RootDetailTabRow(
    pagerTabs: List<RootDetailTab>,
    tabCounts: Map<RootDetailTab, Int>,
    selectedTabIndex: Int,
    onTabClick: (Int) -> Unit,
    modifier: Modifier = Modifier,
) {
    ScrollableTabRow(
        selectedTabIndex = selectedTabIndex,
        modifier = modifier,
        containerColor = MaterialTheme.colorScheme.surface,
        edgePadding = 16.dp,
        divider = {}
    ) {
        val tabs = pagerTabs.map { tab ->
            stringResource(tab.titleRes) to (tabCounts[tab] ?: 0)
        }
        tabs.forEachIndexed { index, (title, count) ->
            val selected = selectedTabIndex == index
            Tab(
                selected = selected,
                onClick = { onTabClick(index) },
                modifier = Modifier.testTag("root_tab_$index"),
                text = {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        Text(
                            title,
                            fontWeight = FontWeight.Bold,
                            maxLines = 1
                        )
                        Badge(
                            containerColor = if (selected) MaterialTheme.colorScheme.primary
                            else MaterialTheme.colorScheme.surfaceVariant,
                            contentColor = if (selected) MaterialTheme.colorScheme.onPrimary
                            else MaterialTheme.colorScheme.onSurfaceVariant
                        ) {
                            Text(
                                text = count.toString(),
                                style = MaterialTheme.typography.labelSmall,
                                fontWeight = FontWeight.Bold
                            )
                        }
                    }
                }
            )
        }
    }
}
