package io.github.ahmedsaadi0.quranwords.ui.roots.detail.tabs

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyListState
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.snapshotFlow
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.pluralStringResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.filter
import io.github.ahmedsaadi0.quranwords.R
import io.github.ahmedsaadi0.quranwords.domain.model.AyahOccurrenceModel
import io.github.ahmedsaadi0.quranwords.ui.roots.detail.components.CopyAllActionBar
import io.github.ahmedsaadi0.quranwords.ui.roots.detail.components.PaddedPagerItem
import io.github.ahmedsaadi0.quranwords.ui.roots.detail.components.cards.AyahOccurrenceCard
import io.github.ahmedsaadi0.quranwords.ui.roots.detail.components.cards.EmptyTabNotice


/**
 * Ayat occurrences tab content (Phase 4).
 *
 * Pagination trigger stays with the caller for this phase (snapshotFlow in
 * RootDetailScreen); it moves inside this tab in Phase 5.
 */
@Composable
fun AyatTab(
    occurrences: List<AyahOccurrenceModel>,
    totalCount: Int,
    hasMore: Boolean,
    isLoadingMore: Boolean,
    isCopyingAll: Boolean,
    onCopyAll: () -> Unit,
    onShareAll: () -> Unit,
    onOccurrenceClick: (surahId: Int, ayahNum: Int) -> Unit,
    onNearEnd: (lastVisibleOccurrenceIndex: Int) -> Unit = {},
    modifier: Modifier = Modifier,
    listState: LazyListState? = null,
) {
    val state = listState ?: rememberLazyListState()

    // Phase 5b: pagination trigger lives with the list (not in the parent).
    // Item 0 is the CopyAll bar when data exists, so occurrence index = lastIndex - 1.
    // distinctUntilChanged prevents redundant VM calls on every recomposition.
    LaunchedEffect(state, hasMore) {
        snapshotFlow {
            state.layoutInfo.visibleItemsInfo.lastOrNull()?.index ?: -1
        }
            .distinctUntilChanged()
            .filter { it != -1 }
            .collectLatest { lastIdx ->
                val adjusted = if (lastIdx > 0) lastIdx - 1 else lastIdx
                onNearEnd(adjusted)
            }
    }
    LazyColumn(
        state = state,
        modifier = modifier
            .fillMaxSize()
            .testTag("root_detail_screen_4"),
        contentPadding = PaddingValues(top = 12.dp, bottom = 24.dp),
        verticalArrangement = Arrangement.spacedBy(0.dp)
    ) {
        if (occurrences.isEmpty() && !hasMore && !isLoadingMore) {
            item {
                EmptyTabNotice(text = stringResource(R.string.root_no_occurrences))
            }
        } else {
            item {
                PaddedPagerItem {
                    val subtitle = if (totalCount > 0) {
                        stringResource(
                            R.string.occurrences_progress,
                            occurrences.size,
                            totalCount,
                            pluralStringResource(R.plurals.root_occurrences, totalCount, totalCount)
                        )
                    } else {
                        pluralStringResource(R.plurals.root_occurrences, occurrences.size, occurrences.size)
                    }
                    CopyAllActionBar(
                        title = stringResource(R.string.root_ayat_title),
                        subtitle = subtitle,
                        onCopyClick = onCopyAll,
                        onShareClick = onShareAll,
                        copyButtonText = stringResource(R.string.copy_all),
                        copyTag = "copy_all_occurrences_btn",
                        shareTag = "share_all_occurrences_btn",
                        isCopying = isCopyingAll,
                        modifier = Modifier.testTag("copy_all_bar")
                    )
                }
            }
            itemsIndexed(
                occurrences,
                key = { _, occ -> "${occ.surahId}-${occ.ayahNum}" }
            ) { _, occ ->
                PaddedPagerItem {
                    AyahOccurrenceCard (
                        occ = occ,
                        onClick = { onOccurrenceClick(occ.surahId, occ.ayahNum) }
                    )
                }
            }
            if (isLoadingMore) {
                item {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(16.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        CircularProgressIndicator(
                            modifier = Modifier.size(24.dp),
                            color = MaterialTheme.colorScheme.primary,
                            strokeWidth = 2.dp
                        )
                    }
                }
            } else if (hasMore && occurrences.isNotEmpty()) {
                item {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 16.dp, vertical = 8.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            text = stringResource(R.string.root_load_more, occurrences.size, totalCount),
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
            } else if (!hasMore && occurrences.isNotEmpty()) {
                item {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(16.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            text = stringResource(
                                R.string.word_all_shown,
                                pluralStringResource(
                                    R.plurals.root_occurrences,
                                    occurrences.size,
                                    occurrences.size
                                )
                            ),
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
            }
        }
        item { Spacer(modifier = Modifier.height(80.dp)) }
    }
}
