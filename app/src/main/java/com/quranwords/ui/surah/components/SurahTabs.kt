package com.quranwords.ui.surah.components

import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import com.quranwords.R
import com.quranwords.core.util.QuranStats
import com.quranwords.core.util.RevelationFilter
import com.quranwords.core.util.SurahMetadata
import com.quranwords.ui.surah.SurahIndexEvent
import com.quranwords.ui.surah.SurahIndexUiState

/**
 * Surahs tab of the index — the quran.com-style flat list: full-bleed rows
 * with octagram medallions and hairline dividers, interleaved juz bands when
 * no query/filter is active. No per-row cards (the old card-in-card look).
 */
@OptIn(ExperimentalFoundationApi::class)
@Composable
fun SurahsTab(
    uiState: SurahIndexUiState,
    onEvent: (SurahIndexEvent) -> Unit,
    onNavigateToSurahDetail: (Int, Int) -> Unit
) {
    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        contentPadding = PaddingValues(bottom = 16.dp)
    ) {
        item(key = "surah_search_header") {
            Column(
                modifier = Modifier.fillMaxWidth().animateItem().padding(horizontal = 16.dp, vertical = 12.dp),
                verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                OutlinedTextField(
                    value = uiState.query,
                    onValueChange = { query -> onEvent(SurahIndexEvent.QueryChanged(query)) },
                    placeholder = { Text(stringResource(R.string.surah_index_search_hint)) },
                    leadingIcon = { Icon(Icons.Filled.Search, contentDescription = null) },
                    trailingIcon = {
                        if (uiState.query.isNotBlank()) {
                            IconButton(onClick = { onEvent(SurahIndexEvent.QueryChanged("")) }) {
                                Icon(Icons.Filled.Close, contentDescription = stringResource(R.string.cd_clear))
                            }
                        }
                    },
                    singleLine = true,
                    shape = RoundedCornerShape(16.dp),
                    modifier = Modifier.fillMaxWidth().testTag("surah_search_input")
                )
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
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
                Box(modifier = Modifier.fillMaxWidth().padding(vertical = 32.dp), contentAlignment = Alignment.Center) {
                    CircularProgressIndicator(color = MaterialTheme.colorScheme.primary)
                }
            }
            uiState.error != null -> item(key = "surah_error") {
                Box(modifier = Modifier.fillMaxWidth().padding(vertical = 32.dp), contentAlignment = Alignment.Center) {
                    Text(text = uiState.error ?: "", style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.error)
                }
            }
            uiState.filteredSurahs.isEmpty() -> item(key = "surah_empty") {
                Box(modifier = Modifier.fillMaxWidth().padding(vertical = 32.dp).animateItem(), contentAlignment = Alignment.Center) {
                    Text(
                        text = if (uiState.query.isBlank()) stringResource(R.string.surah_index_empty_filter) else stringResource(R.string.surah_index_empty_query, uiState.query),
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
            else -> {
                val showInterleaved = uiState.query.isBlank() && uiState.filter == RevelationFilter.ALL
                if (showInterleaved) {
                    val grouped = uiState.filteredSurahs.groupBy { it.juzStart }
                    val orderedJuzIds = uiState.filteredSurahs.map { it.juzStart }.distinct()
                    items(grouped.entries.filter { it.key in orderedJuzIds }, key = { "juz_group_${it.key}" }) { entry ->
                        val juz = SurahMetadata.JUZ_LIST.firstOrNull { it.id == entry.key } ?: return@items
                        Column(modifier = Modifier.animateItem()) {
                            JuzHeaderRow(
                                juzId = entry.key,
                                onClick = { onNavigateToSurahDetail(juz.startSurahId, juz.startAyah) }
                            )
                            entry.value.forEach { surah ->
                                SurahRow(
                                    surah = surah,
                                    isBookmarked = uiState.bookmarkedSurahIds.contains(surah.id),
                                    onClick = { onNavigateToSurahDetail(surah.id, 1) },
                                    onBookmarkClick = { onEvent(SurahIndexEvent.ToggleBookmark(surah.id)) }
                                )
                            }
                        }
                    }
                } else {
                    items(uiState.filteredSurahs, key = { it.id }) { surah ->
                        SurahRow(
                            surah = surah,
                            isBookmarked = uiState.bookmarkedSurahIds.contains(surah.id),
                            onClick = { onNavigateToSurahDetail(surah.id, 1) },
                            onBookmarkClick = { onEvent(SurahIndexEvent.ToggleBookmark(surah.id)) },
                            modifier = Modifier.animateItem()
                        )
                    }
                }
            }
        }
    }
}

/**
 * Juz tab — the same flat quran.com rows: octagram juz number, the juz label
 * over its surah range, hairline dividers, no cards.
 */
@OptIn(ExperimentalFoundationApi::class)
@Composable
fun JuzTab(onNavigateToSurahDetail: (Int, Int) -> Unit) {
    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        contentPadding = PaddingValues(bottom = 16.dp)
    ) {
        items(SurahMetadata.JUZ_LIST, key = { it.id }) { juz ->
            JuzRow(
                juzId = juz.id,
                onClick = { onNavigateToSurahDetail(juz.startSurahId, juz.startAyah) },
                modifier = Modifier.animateItem()
            )
        }
    }
}
