package io.github.ahmedsaadi0.quranwords.ui.roots.detail.tabs

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyListState
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.pluralStringResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import io.github.ahmedsaadi0.quranwords.R
import io.github.ahmedsaadi0.quranwords.domain.model.RootWordModel
import io.github.ahmedsaadi0.quranwords.ui.roots.detail.components.PaddedPagerItem
import io.github.ahmedsaadi0.quranwords.ui.roots.detail.components.SelectionActionBar
import io.github.ahmedsaadi0.quranwords.ui.screens.EmptyTabNotice
import io.github.ahmedsaadi0.quranwords.ui.screens.WordCard

/**
 * Words tab content (Phase 4).
 */
@Composable
fun WordsTab(
    words: List<RootWordModel>,
    isLoading: Boolean,
    selectedIds: Set<Int>,
    isSelectionMode: Boolean,
    isCopyingSelected: Boolean,
    onCopySelected: () -> Unit,
    onShareSelected: () -> Unit,
    onSelectAll: () -> Unit,
    onClearSelection: () -> Unit,
    onWordClick: (wordId: Int) -> Unit,
    onToggleSelection: (wordId: Int) -> Unit,
    onEnterSelection: (wordId: Int) -> Unit,
    modifier: Modifier = Modifier,
    listState: LazyListState? = null,
) {
    val state = listState ?: rememberLazyListState()
    LazyColumn(
        state = state,
        modifier = modifier
            .fillMaxSize()
            .testTag("root_detail_screen_3"),
        contentPadding = PaddingValues(top = 12.dp, bottom = 24.dp),
        verticalArrangement = Arrangement.spacedBy(0.dp)
    ) {
        when {
            isLoading && words.isEmpty() -> {
                item {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(32.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        CircularProgressIndicator(
                            color = MaterialTheme.colorScheme.primary
                        )
                    }
                }
            }
            words.isEmpty() -> {
                item {
                    EmptyTabNotice(text = stringResource(R.string.root_no_words))
                }
            }
            else -> {
                if (isSelectionMode) {
                    item {
                        PaddedPagerItem {
                            SelectionActionBar(
                                selectedCount = selectedIds.size,
                                countLabel = pluralStringResource(
                                    R.plurals.selected_words_count,
                                    selectedIds.size,
                                    selectedIds.size
                                ),
                                onCopyClick = onCopySelected,
                                onShareClick = onShareSelected,
                                onSelectAllClick = onSelectAll,
                                onClearClick = onClearSelection,
                                copyButtonText = stringResource(R.string.copy_ayat),
                                copyTag = "copy_selected_words_btn",
                                shareTag = "share_selected_words_btn",
                                selectAllTag = "select_all_words_btn",
                                clearTag = "clear_words_selection_btn",
                                isCopying = isCopyingSelected,
                                modifier = Modifier.testTag("selected_words_bar")
                            )
                        }
                    }
                }
                items(words, key = { it.wordId }) { word ->
                    PaddedPagerItem {
                        WordCard(
                            word = word,
                            isSelected = selectedIds.contains(word.wordId),
                            isSelectionMode = isSelectionMode,
                            onClick = {
                                if (isSelectionMode) {
                                    onToggleSelection(word.wordId)
                                } else {
                                    onWordClick(word.wordId)
                                }
                            },
                            onLongClick = {
                                if (isSelectionMode) {
                                    onToggleSelection(word.wordId)
                                } else {
                                    onEnterSelection(word.wordId)
                                }
                            }
                        )
                    }
                }
            }
        }
        item { Spacer(modifier = Modifier.height(80.dp)) }
    }
}
