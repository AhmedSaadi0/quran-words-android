package io.github.ahmedsaadi0.quranwords.ui.roots.detail.tabs

import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyListState
import androidx.compose.foundation.lazy.items
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import io.github.ahmedsaadi0.quranwords.R
import io.github.ahmedsaadi0.quranwords.domain.model.MasdarModel
import io.github.ahmedsaadi0.quranwords.ui.roots.detail.components.PaddedPagerItem

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.fillMaxSize
import io.github.ahmedsaadi0.quranwords.ui.roots.detail.components.cards.EmptyTabNotice
import io.github.ahmedsaadi0.quranwords.ui.roots.detail.components.cards.MasdarCard

/**
 * Masadir tab content (Phase 4).
 * Pure list + empty state; owns its scroll state when not provided.
 */
@Composable
fun MasadirTab(
    masadir: List<MasdarModel>,
    modifier: Modifier = Modifier,
    listState: LazyListState? = null,
) {
    // NOTE: parent currently owns states via rememberSaveable; this internal
    // fallback preserves previewability. Caller should pass its saved state.
    val state = listState ?: androidx.compose.foundation.lazy.rememberLazyListState()
    LazyColumn(
        state = state,
        modifier = modifier
            .fillMaxSize()
            .testTag("root_detail_screen_1"),
        contentPadding = PaddingValues(top = 12.dp, bottom = 24.dp),
        verticalArrangement = Arrangement.spacedBy(0.dp)
    ) {
        if (masadir.isEmpty()) {
            item {
                EmptyTabNotice(text = stringResource(R.string.root_no_masadir))
            }
        } else {
            items(masadir, key = { it.id }) { masdar ->
                PaddedPagerItem {
                    MasdarCard(masdar)
                }
            }
        }
        item { Spacer(modifier = Modifier.height(80.dp)) }
    }
}
