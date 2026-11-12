package com.quranwords.ui.roots.detail.components

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.statusBars
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.outlined.ReportProblem
import androidx.compose.material3.Badge
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ScrollableTabRow
import androidx.compose.material3.Tab
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.quranwords.R
import com.quranwords.ui.roots.detail.util.RootDetailTab

/**
 * Static (non-collapsing) header for RootDetail: pinned top bar + tabs.
 *
 * The AI summary lives in the AI_SUMMARY tab cards (Decision #33), so the
 * scroll-linked collapse machinery (CollapsingHeaderState, nested scroll,
 * fling continuation) is gone entirely.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun RootDetailHeader(
    rootText: String,
    tabCounts: Map<RootDetailTab, Int>,
    pagerTabs: List<RootDetailTab>,
    selectedTabIndex: Int,
    onNavigateBack: () -> Unit,
    onReportClick: () -> Unit,
    onTabClick: (Int) -> Unit,
    modifier: Modifier = Modifier,
) {
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
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(4.dp)
            ) {
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
        }

        // Pinned tabs
        RootDetailTabRow(
            pagerTabs = pagerTabs,
            tabCounts = tabCounts,
            selectedTabIndex = selectedTabIndex,
            onTabClick = onTabClick
        )
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
