package io.github.ahmedsaadi0.quranwords.ui.roots.detail.tabs

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyListState
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.pluralStringResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import io.github.ahmedsaadi0.quranwords.R
import io.github.ahmedsaadi0.quranwords.domain.model.RootMeaningModel
import io.github.ahmedsaadi0.quranwords.ui.roots.detail.components.CopyAllActionBar
import io.github.ahmedsaadi0.quranwords.ui.roots.detail.components.PaddedPagerItem
import io.github.ahmedsaadi0.quranwords.ui.roots.detail.components.SelectionActionBar
import io.github.ahmedsaadi0.quranwords.ui.screens.EmptyTabNotice
import io.github.ahmedsaadi0.quranwords.ui.screens.MeaningCard

/**
 * Meanings tab content (Phase 4).
 *
 * Stateless: all copy/share/selection callbacks are provided by the caller
 * (currently RootDetailScreen, later RootDetailRoute via [RootDetailEvent]).
 * Preserves testTags and empty/selection states exactly.
 */
@Composable
fun MeaningsTab(
    meanings: List<RootMeaningModel>,
    selectedIds: Set<Int>,
    isSelectionMode: Boolean,
    isCopyingAll: Boolean,
    isCopyingSelected: Boolean,
    onCopyAll: () -> Unit,
    onShareAll: () -> Unit,
    onCopySelected: () -> Unit,
    onShareSelected: () -> Unit,
    onSelectAll: () -> Unit,
    onClearSelection: () -> Unit,
    onToggleSelection: (Int) -> Unit,
    onEnterSelection: (Int) -> Unit,
    modifier: Modifier = Modifier,
    listState: LazyListState? = null,
) {
    val state = listState ?: rememberLazyListState()
    LazyColumn(
        state = state,
        modifier = modifier
            .fillMaxSize()
            .testTag("root_detail_screen_0"),
        contentPadding = PaddingValues(top = 12.dp, bottom = 24.dp),
        verticalArrangement = Arrangement.spacedBy(0.dp)
    ) {
        if (meanings.isEmpty()) {
            item {
                EmptyTabNotice(text = stringResource(R.string.root_no_meanings))
            }
        } else {
            item {
                PaddedPagerItem {
                    CopyAllActionBar(
                        title = stringResource(R.string.meanings_bar_title),
                        subtitle = pluralStringResource(R.plurals.meanings_count, meanings.size, meanings.size),
                        onCopyClick = onCopyAll,
                        onShareClick = onShareAll,
                        copyButtonText = stringResource(R.string.copy_meanings),
                        copyTag = "copy_all_meanings_btn",
                        shareTag = "share_all_meanings_btn",
                        isCopying = isCopyingAll,
                        modifier = Modifier.testTag("copy_all_meanings_bar")
                    )
                }
            }
            if (isSelectionMode) {
                item {
                    PaddedPagerItem {
                        SelectionActionBar(
                            selectedCount = selectedIds.size,
                            countLabel = pluralStringResource(
                                R.plurals.selected_meanings_count,
                                selectedIds.size,
                                selectedIds.size
                            ),
                            onCopyClick = onCopySelected,
                            onShareClick = onShareSelected,
                            onSelectAllClick = onSelectAll,
                            onClearClick = onClearSelection,
                            copyButtonText = stringResource(R.string.copy_meanings),
                            copyTag = "copy_selected_meanings_btn",
                            shareTag = "share_selected_meanings_btn",
                            selectAllTag = "select_all_meanings_btn",
                            clearTag = "clear_meanings_selection_btn",
                            isCopying = isCopyingSelected,
                            modifier = Modifier.testTag("selected_meanings_bar")
                        )
                    }
                }
            }
            items(meanings, key = { it.id }) { meaning ->
                PaddedPagerItem {
                    MeaningCard(
                        meaning = meaning,
                        isSelected = selectedIds.contains(meaning.id),
                        isSelectionMode = isSelectionMode,
                        onClick = {
                            if (isSelectionMode) {
                                onToggleSelection(meaning.id)
                            }
                        },
                        onLongClick = {
                            if (isSelectionMode) {
                                onToggleSelection(meaning.id)
                            } else {
                                onEnterSelection(meaning.id)
                            }
                        }
                    )
                }
            }
        }
        item { Spacer(modifier = Modifier.height(80.dp)) }
    }
}
