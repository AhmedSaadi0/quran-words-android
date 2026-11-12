package com.quranwords.ui.search.components

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.quranwords.R
import com.quranwords.domain.model.WordToken
import com.quranwords.ui.components.RootItemCard
import com.quranwords.ui.search.LexiconSubScope
import com.quranwords.ui.search.SearchScope
import com.quranwords.ui.search.SearchUiState

@Composable
fun SearchLexiconBody(
    uiState: SearchUiState,
    listState: androidx.compose.foundation.lazy.LazyListState,
    onSubScope: (LexiconSubScope) -> Unit,
    onNavigateToRootDetail: (Int) -> Unit,
    onOpenRoot: (Int?) -> Unit,
    onRecordRecent: (String) -> Unit,
    modifier: Modifier = Modifier
) {
    Column(modifier = modifier.fillMaxSize()) {
        Row(modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            LexiconSubTab(stringResource(R.string.search_tab_roots, uiState.results.roots.size), uiState.lexiconSubScope == LexiconSubScope.ROOTS) { onSubScope(LexiconSubScope.ROOTS) }
            LexiconSubTab(stringResource(R.string.search_tab_masadir, uiState.results.masadir.size), uiState.lexiconSubScope == LexiconSubScope.MASADIR) { onSubScope(LexiconSubScope.MASADIR) }
            LexiconSubTab(stringResource(R.string.search_tab_derivatives, uiState.results.derivatives.size), uiState.lexiconSubScope == LexiconSubScope.DERIVATIVES) { onSubScope(LexiconSubScope.DERIVATIVES) }
        }
        when (uiState.lexiconSubScope) {
            LexiconSubScope.ROOTS -> {
                if (uiState.results.roots.isEmpty()) EmptySearchNotice(text = stringResource(R.string.search_no_roots))
                else LazyColumn(state = listState, modifier = Modifier.fillMaxSize().padding(horizontal = 16.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    items(uiState.results.roots, key = { it.id }) { RootItemCard(rootItem = it, onClick = { onRecordRecent(uiState.query); onNavigateToRootDetail(it.id) }, modifier = Modifier.testTag("root_item_${it.id}")) }
                    if (uiState.isLoadingMore) item(key = "lm") { SearchLoadingView() }
                    item(key = "spacer") { Spacer(modifier = Modifier.height(24.dp)) }
                }
            }
            LexiconSubScope.MASADIR -> {
                if (uiState.results.masadir.isEmpty()) EmptySearchNotice(text = stringResource(R.string.search_no_masadir))
                else LazyColumn(state = listState, modifier = Modifier.fillMaxSize().padding(horizontal = 16.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    items(uiState.results.masadir, key = { it.id }) { masdar ->
                        MasdarSearchCard(
                            masdar = masdar,
                            onClick = { onRecordRecent(uiState.query); onOpenRoot(masdar.rootId) }
                        )
                    }
                    if (uiState.isLoadingMore) item(key = "lm") { SearchLoadingView() }
                }
            }
            LexiconSubScope.DERIVATIVES -> {
                if (uiState.results.derivatives.isEmpty()) EmptySearchNotice(text = stringResource(R.string.search_no_derivatives))
                else LazyColumn(state = listState, modifier = Modifier.fillMaxSize().padding(horizontal = 16.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    items(uiState.results.derivatives, key = { it.id }) { derivative ->
                        DerivativeSearchCard(
                            derivative = derivative,
                            onClick = { onRecordRecent(uiState.query); onOpenRoot(derivative.rootId) }
                        )
                    }
                    if (uiState.isLoadingMore) item(key = "lm") { SearchLoadingView() }
                }
            }
        }
    }
}

@Composable
fun SearchWordsBody(
    uiState: SearchUiState,
    listState: androidx.compose.foundation.lazy.LazyListState,
    onSelectWord: (WordToken) -> Unit,
    modifier: Modifier = Modifier
) {
    if (uiState.results.words.isEmpty()) {
        EmptySearchNotice(text = stringResource(R.string.search_no_words))
        return
    }
    LazyColumn(state = listState, modifier = modifier.fillMaxSize().padding(horizontal = 16.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
        item(key = "count") { SectionHeader(title = stringResource(R.string.search_section_words, uiState.results.words.size)) }
        items(uiState.results.words, key = { it.wordId }) { WordSearchCard(word = it, onClick = { onSelectWord(it) }) }
        if (uiState.isLoadingMore) item(key = "lm") { SearchLoadingView() }
        item(key = "spacer") { Spacer(modifier = Modifier.height(24.dp)) }
    }
}

@Composable
private fun LexiconSubTab(label: String, selected: Boolean, onClick: () -> Unit) {
    FilterChip(
        selected = selected,
        onClick = onClick,
        label = { Text(text = label, fontWeight = if (selected) FontWeight.Bold else FontWeight.Normal) },
        shape = RoundedCornerShape(12.dp)
    )
}

/** Alias kept so SearchScreen can branch on scope without when-exhaustiveness boilerplate. */
fun isLexiconScope(scope: SearchScope): Boolean = scope == SearchScope.LEXICON
