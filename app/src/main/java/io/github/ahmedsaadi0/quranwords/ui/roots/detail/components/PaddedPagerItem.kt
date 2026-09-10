package io.github.ahmedsaadi0.quranwords.ui.roots.detail.components

import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.lazy.LazyItemScope
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.compose.foundation.layout.padding

/**
 * DRY wrapper for pager list items (Phase 1b).
 *
 * Replaces ~10 repetitions of:
 * `Box(Modifier.padding(horizontal = 16.dp, vertical = 5.dp).animateItem())`.
 *
 * Must be called from within a [androidx.compose.foundation.lazy.LazyListScope]
 * `item {}` / `items {}` block so [LazyItemScope.animateItem] is available.
 * No visual or testTag change — inner content keeps its own tags.
 */
@OptIn(ExperimentalFoundationApi::class)
@Composable
fun LazyItemScope.PaddedPagerItem(
    modifier: Modifier = Modifier,
    content: @Composable () -> Unit,
) {
    Box(
        modifier = modifier
            .padding(horizontal = 16.dp, vertical = 5.dp)
            .animateItem()
    ) {
        content()
    }
}
