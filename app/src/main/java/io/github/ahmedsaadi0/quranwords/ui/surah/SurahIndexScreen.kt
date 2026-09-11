package io.github.ahmedsaadi0.quranwords.ui.surah

import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.MenuBook
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LargeTopAppBar
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Tab
import androidx.compose.material3.TabRow
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.input.nestedscroll.nestedScroll
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import io.github.ahmedsaadi0.quranwords.R
import io.github.ahmedsaadi0.quranwords.core.util.QuranStats
import io.github.ahmedsaadi0.quranwords.core.util.RevelationFilter
import io.github.ahmedsaadi0.quranwords.core.util.SurahMetadata
import io.github.ahmedsaadi0.quranwords.ui.components.SurahItemCard

@OptIn(ExperimentalMaterial3Api::class, ExperimentalFoundationApi::class)
@Composable
fun SurahIndexScreen(
    uiState: SurahIndexUiState,
    onEvent: (SurahIndexEvent) -> Unit,
    onNavigateBack: () -> Unit,
    onNavigateToSurahDetail: (Int) -> Unit
) {
    var selectedTabIndex by rememberSaveable { mutableIntStateOf(0) }
    val scrollBehavior = TopAppBarDefaults.exitUntilCollapsedScrollBehavior()

    Scaffold(
        modifier = Modifier.nestedScroll(scrollBehavior.nestedScrollConnection),
        topBar = {
            Column(modifier = Modifier.background(MaterialTheme.colorScheme.surface)) {
                LargeTopAppBar(
                    title = {
                        Column(verticalArrangement = Arrangement.spacedBy(2.dp)) {
                            Text(
                                text = stringResource(R.string.surah_index_title),
                                fontWeight = FontWeight.Bold
                            )
                            // Collapsible detail — visible only when expanded (earns the collapse)
                            if (scrollBehavior.state.collapsedFraction < 0.5f) {
                                Text(
                                    text = stringResource(
                                        R.string.surah_index_subtitle,
                                        QuranStats.TOTAL_SURAHS,
                                        QuranStats.MECCAN_SURAHS,
                                        QuranStats.MEDINAN_SURAHS,
                                        QuranStats.TOTAL_JUZ
                                    ),
                                    style = MaterialTheme.typography.labelSmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                        }
                    },
                    navigationIcon = {
                        IconButton(
                            onClick = onNavigateBack,
                            modifier = Modifier.testTag("back_button")
                        ) {
                            Icon(
                                imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                                contentDescription = stringResource(R.string.cd_back)
                            )
                        }
                    },
                    scrollBehavior = scrollBehavior,
                    colors = TopAppBarDefaults.largeTopAppBarColors(
                        containerColor = MaterialTheme.colorScheme.background,
                        scrolledContainerColor = MaterialTheme.colorScheme.surface,
                    )
                )

                // Pinned Tabs: remain visible when search collapses
                TabRow(
                    selectedTabIndex = selectedTabIndex,
                    containerColor = MaterialTheme.colorScheme.surface,
                    divider = {}
                ) {
                    Tab(
                        selected = selectedTabIndex == 0,
                        onClick = { selectedTabIndex = 0 },
                        text = {
                            Text(
                                text = stringResource(R.string.surah_index_tab_surahs, QuranStats.TOTAL_SURAHS),
                                fontWeight = FontWeight.Bold
                            )
                        },
                        modifier = Modifier.testTag("tab_surahs")
                    )
                    Tab(
                        selected = selectedTabIndex == 1,
                        onClick = { selectedTabIndex = 1 },
                        text = {
                            Text(
                                text = stringResource(R.string.surah_index_tab_juz, QuranStats.TOTAL_JUZ),
                                fontWeight = FontWeight.Bold
                            )
                        },
                        modifier = Modifier.testTag("tab_juz")
                    )
                }
            }
        },
        containerColor = MaterialTheme.colorScheme.background,
        contentWindowInsets = WindowInsets(0, 0, 0, 0)
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .testTag("surah_index_screen")
        ) {
            if (selectedTabIndex == 0) {
                SurahsTab(
                    uiState = uiState,
                    onEvent = onEvent,
                    onNavigateToSurahDetail = onNavigateToSurahDetail
                )
            } else {
                JuzTab(onNavigateToSurahDetail = onNavigateToSurahDetail)
            }
        }
    }
}

@OptIn(ExperimentalFoundationApi::class)
@Composable
private fun SurahsTab(
    uiState: SurahIndexUiState,
    onEvent: (SurahIndexEvent) -> Unit,
    onNavigateToSurahDetail: (Int) -> Unit
) {
    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        contentPadding = PaddingValues(horizontal = 16.dp, vertical = 12.dp),
        verticalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        // Collapsible Search + Filters — scrolls away
        item(key = "surah_search_header") {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .animateItem(),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                OutlinedTextField(
                    value = uiState.query,
                    onValueChange = { query -> onEvent(SurahIndexEvent.QueryChanged(query)) },
                    placeholder = { Text(stringResource(R.string.surah_index_search_hint)) },
                    leadingIcon = {
                        Icon(imageVector = Icons.Default.Search, contentDescription = null)
                    },
                    trailingIcon = {
                        if (uiState.query.isNotBlank()) {
                            IconButton(onClick = { onEvent(SurahIndexEvent.QueryChanged("")) }) {
                                Icon(imageVector = Icons.Default.Close, contentDescription = stringResource(R.string.cd_clear))
                            }
                        }
                    },
                    singleLine = true,
                    shape = RoundedCornerShape(12.dp),
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("surah_search_input")
                )

                Row(
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    FilterChip(
                        selected = uiState.filter == RevelationFilter.ALL,
                        onClick = { onEvent(SurahIndexEvent.FilterChanged(RevelationFilter.ALL)) },
                        label = { Text(stringResource(R.string.surah_index_filter_all, QuranStats.TOTAL_SURAHS)) },
                        shape = RoundedCornerShape(12.dp)
                    )
                    FilterChip(
                        selected = uiState.filter == RevelationFilter.MECCAN,
                        onClick = { onEvent(SurahIndexEvent.FilterChanged(RevelationFilter.MECCAN)) },
                        label = { Text(stringResource(R.string.surah_index_filter_meccan, QuranStats.MECCAN_SURAHS)) },
                        shape = RoundedCornerShape(12.dp)
                    )
                    FilterChip(
                        selected = uiState.filter == RevelationFilter.MEDINAN,
                        onClick = { onEvent(SurahIndexEvent.FilterChanged(RevelationFilter.MEDINAN)) },
                        label = { Text(stringResource(R.string.surah_index_filter_medinan, QuranStats.MEDINAN_SURAHS)) },
                        shape = RoundedCornerShape(12.dp)
                    )
                }
            }
        }

        when {
            uiState.isLoading -> item(key = "surah_loading") {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(vertical = 32.dp),
                    contentAlignment = Alignment.Center
                ) {
                    CircularProgressIndicator(color = MaterialTheme.colorScheme.primary)
                }
            }
            uiState.error != null -> item(key = "surah_error") {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(vertical = 32.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = uiState.error ?: "",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.error
                    )
                }
            }
            uiState.filteredSurahs.isEmpty() -> item(key = "surah_empty") {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(vertical = 32.dp)
                        .animateItem(),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = if (uiState.query.isBlank()) stringResource(R.string.surah_index_empty_filter)
                        else stringResource(R.string.surah_index_empty_query, uiState.query),
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
            else -> items(uiState.filteredSurahs, key = { it.id }) { surah ->
                val isBookmarked = uiState.bookmarkedSurahIds.contains(surah.id)
                SurahItemCard(
                    surah = surah,
                    onClick = { onNavigateToSurahDetail(surah.id) },
                    isBookmarked = isBookmarked,
                    onBookmarkClick = { onEvent(SurahIndexEvent.ToggleBookmark(surah.id)) },
                    modifier = Modifier.animateItem()
                )
            }
        }
        item(key = "surah_bottom_spacer") {
            Spacer(modifier = Modifier.height(16.dp))
        }
    }
}

@Composable
private fun JuzTab(onNavigateToSurahDetail: (Int) -> Unit) {
    // Juz Tab — no search, direct list
    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        contentPadding = PaddingValues(horizontal = 16.dp, vertical = 12.dp),
        verticalArrangement = Arrangement.spacedBy(10.dp)
    ) {
        items(SurahMetadata.JUZ_LIST, key = { it.id }) { juz ->
            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(16.dp))
                    .border(1.dp, MaterialTheme.colorScheme.outlineVariant, RoundedCornerShape(16.dp))
                    .clickable { onNavigateToSurahDetail(juz.startSurahId) }
                    .animateItem()
                    .testTag("juz_item_${juz.id}"),
                colors = CardDefaults.cardColors(
                    containerColor = MaterialTheme.colorScheme.surface
                ),
                elevation = CardDefaults.cardElevation(defaultElevation = 0.5.dp)
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(16.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(14.dp)
                    ) {
                        Box(
                            modifier = Modifier
                                .size(42.dp)
                                .clip(CircleShape)
                                .background(MaterialTheme.colorScheme.primaryContainer)
                                .border(1.dp, MaterialTheme.colorScheme.outlineVariant, CircleShape),
                            contentAlignment = Alignment.Center
                        ) {
                            Text(
                                text = juz.id.toString(),
                                style = MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.primary
                            )
                        }

                        Column {
                            Text(
                                text = juz.nameAr,
                                style = MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.onSurface
                            )
                            Text(
                                text = juz.description,
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }

                    Icon(
                        imageVector = Icons.AutoMirrored.Filled.MenuBook,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.primary
                    )
                }
            }
        }
        item {
            Spacer(modifier = Modifier.height(16.dp))
        }
    }
}