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
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import io.github.ahmedsaadi0.quranwords.R
import io.github.ahmedsaadi0.quranwords.domain.model.DerivativeModel
import io.github.ahmedsaadi0.quranwords.ui.roots.detail.components.PaddedPagerItem
import io.github.ahmedsaadi0.quranwords.ui.screens.DerivativeCard
import io.github.ahmedsaadi0.quranwords.ui.screens.EmptyTabNotice

/**
 * Derivatives tab content (Phase 4).
 */
@Composable
fun DerivativesTab(
    derivatives: List<DerivativeModel>,
    modifier: Modifier = Modifier,
    listState: LazyListState? = null,
) {
    val state = listState ?: rememberLazyListState()
    LazyColumn(
        state = state,
        modifier = modifier
            .fillMaxSize()
            .testTag("root_detail_screen_2"),
        contentPadding = PaddingValues(top = 12.dp, bottom = 24.dp),
        verticalArrangement = Arrangement.spacedBy(0.dp)
    ) {
        if (derivatives.isEmpty()) {
            item {
                EmptyTabNotice(text = stringResource(R.string.root_no_derivatives))
            }
        } else {
            items(derivatives, key = { it.id }) { derivative ->
                PaddedPagerItem {
                    DerivativeCard(derivative)
                }
            }
        }
        item { Spacer(modifier = Modifier.height(80.dp)) }
    }
}
