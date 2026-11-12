package com.quranwords.ui.search

import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.snapshotFlow
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.quranwords.R
import com.quranwords.domain.model.WordToken
import com.quranwords.ui.search.components.AyahSurahFilterBar
import com.quranwords.ui.search.components.AyahSearchCard
import com.quranwords.ui.search.components.EmptySearchNotice
import com.quranwords.ui.search.components.SearchAllSectionsView
import com.quranwords.ui.search.components.SearchErrorView
import com.quranwords.ui.search.components.SearchExplorerEmptyView
import com.quranwords.ui.search.components.SearchScopeChips
import com.quranwords.ui.search.components.SearchLexiconBody
import com.quranwords.ui.search.components.SearchLoadingView
import com.quranwords.ui.search.components.SearchWordsBody
import com.quranwords.ui.search.components.SectionHeader
import com.quranwords.ui.search.components.SurahSearchCard
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.launch

@OptIn(ExperimentalMaterial3Api::class, ExperimentalFoundationApi::class)
@Composable
fun SearchScreen(
    uiState: SearchUiState,
    onEvent: (SearchEvent) -> Unit,
    onNavigateBack: () -> Unit,
    onNavigateToRootDetail: (Int) -> Unit,
    onNavigateToSurahDetail: (Int, Int) -> Unit,
    onRecordRecent: (String) -> Unit = {},
    bookmarkedAyat: Set<String> = emptySet(),
    onToggleAyahBookmark: (Int, Int) -> Unit = { _, _ -> }
) {
    val listState = rememberLazyListState()
    val searchError = uiState.error
    // Every result opens something: surahs/ayat → the ayah, roots/masadir/
    // derivatives/words → the root detail. Items with no root recorded still
    // get an explicit response (never a silent no-op click).
    val snackbarHostState = remember { SnackbarHostState() }
    val scope = rememberCoroutineScope()
    val noRootMessage = stringResource(R.string.search_item_no_root)
    val onRootOpened: (Int?) -> Unit = { rootId ->
        if (rootId != null) {
            onNavigateToRootDetail(rootId)
        } else {
            scope.launch { snackbarHostState.showSnackbar(noRootMessage) }
        }
    }
    val onWordSelected: (WordToken) -> Unit = { word ->
        onRecordRecent(uiState.query)
        onRootOpened(word.rootId)
    }

    LaunchedEffect(listState, uiState.scope) {
        snapshotFlow { listState.layoutInfo.visibleItemsInfo.lastOrNull()?.index ?: -1 }
            .distinctUntilChanged()
            .collect { lastIdx ->
                if (lastIdx == -1) return@collect
                val tab = when (uiState.scope) {
                    SearchScope.AYAHS -> SearchTab.AYAT
                    SearchScope.WORDS -> SearchTab.WORDS
                    SearchScope.LEXICON -> when (uiState.lexiconSubScope) {
                        LexiconSubScope.ROOTS -> SearchTab.ROOTS
                        LexiconSubScope.MASADIR -> SearchTab.MASADIR
                        LexiconSubScope.DERIVATIVES -> SearchTab.DERIVATIVES
                    }
                    else -> SearchTab.ROOTS
                }
                onEvent(SearchEvent.NearingEnd(tab, lastIdx))
            }
    }
    LaunchedEffect(uiState.scope, uiState.query) { listState.scrollToItem(0) }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(text = stringResource(R.string.search_title), fontWeight = FontWeight.Bold) },
                navigationIcon = {
                    IconButton(onClick = onNavigateBack, modifier = Modifier.testTag("back_button")) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = stringResource(R.string.cd_back))
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = MaterialTheme.colorScheme.surface)
            )
        },
        snackbarHost = { SnackbarHost(snackbarHostState) },
        containerColor = MaterialTheme.colorScheme.background,
        contentWindowInsets = WindowInsets(0, 0, 0, 0)
    ) { innerPadding ->
        Column(modifier = Modifier.fillMaxSize().padding(innerPadding).testTag("search_screen")) {
            OutlinedTextField(
                value = uiState.query,
                onValueChange = { onEvent(SearchEvent.QueryChanged(it)) },
                placeholder = { Text(stringResource(R.string.search_hint_unified)) },
                leadingIcon = { Icon(Icons.Filled.Search, contentDescription = null) },
                trailingIcon = {
                    if (uiState.query.isNotBlank()) {
                        IconButton(onClick = { onEvent(SearchEvent.QueryChanged("")) }) {
                            Icon(Icons.Filled.Close, contentDescription = stringResource(R.string.cd_clear))
                        }
                    }
                },
                singleLine = true,
                shape = RoundedCornerShape(12.dp),
                modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 10.dp).testTag("unified_search_input")
            )
            SearchScopeChips(
                scope = uiState.scope,
                ayahCount = uiState.results.ayat.size,
                wordCount = uiState.results.words.size,
                surahCount = uiState.results.surahs.size,
                rootCount = uiState.results.roots.size,
                onScopeChanged = { onEvent(SearchEvent.ScopeChanged(it)) },
                modifier = Modifier.padding(horizontal = 16.dp, vertical = 4.dp)
            )
            when {
                uiState.isSearching -> SearchLoadingView()
                searchError != null -> SearchErrorView(message = searchError, onRetry = { onEvent(SearchEvent.Retry) })
                uiState.query.isBlank() -> SearchExplorerEmptyView(
                    recentSearches = uiState.recentSearches,
                    topRootsCount = uiState.topRoots.size,
                    onRecentSelected = { onEvent(SearchEvent.RecentSearchSelected(it)) },
                    onRecentRemoved = { onEvent(SearchEvent.RecentSearchRemoved(it)) },
                    onClearRecents = { onEvent(SearchEvent.ClearRecents) },
                    onTopRoots = { onEvent(SearchEvent.ScopeChanged(SearchScope.LEXICON)) }
                )
                else -> SearchBody(
                    uiState = uiState,
                    listState = listState,
                    bookmarkedAyat = bookmarkedAyat,
                    onEvent = onEvent,
                    onNavigateToRootDetail = onNavigateToRootDetail,
                    onNavigateToSurahDetail = onNavigateToSurahDetail,
                    onToggleAyahBookmark = onToggleAyahBookmark,
                    onRecordRecent = onRecordRecent,
                    onSelectWord = onWordSelected,
                    onOpenRoot = onRootOpened
                )
            }
        }
    }
}

@Composable
private fun SearchBody(
    uiState: SearchUiState,
    listState: androidx.compose.foundation.lazy.LazyListState,
    bookmarkedAyat: Set<String>,
    onEvent: (SearchEvent) -> Unit,
    onNavigateToRootDetail: (Int) -> Unit,
    onNavigateToSurahDetail: (Int, Int) -> Unit,
    onToggleAyahBookmark: (Int, Int) -> Unit,
    onRecordRecent: (String) -> Unit,
    onSelectWord: (WordToken) -> Unit,
    onOpenRoot: (Int?) -> Unit
) {
    when (uiState.scope) {
        SearchScope.ALL -> SearchAllSectionsView(
            query = uiState.query,
            results = uiState.results,
            listState = listState,
            bookmarkedAyat = bookmarkedAyat,
            onSelectSurah = { id -> onRecordRecent(uiState.query); onNavigateToSurahDetail(id, 1) },
            onSelectAyah = { s, a -> onRecordRecent(uiState.query); onNavigateToSurahDetail(s, a) },
            onToggleAyahBookmark = onToggleAyahBookmark,
            onSelectRoot = { id -> onRecordRecent(uiState.query); onNavigateToRootDetail(id) },
            onSelectWord = onSelectWord,
            onSeeAllAyahs = { onEvent(SearchEvent.ScopeChanged(SearchScope.AYAHS)) },
            onSeeAllWords = { onEvent(SearchEvent.ScopeChanged(SearchScope.WORDS)) },
            modifier = Modifier.fillMaxSize().padding(horizontal = 16.dp, vertical = 10.dp).testTag("search_results_list")
        )
        SearchScope.AYAHS -> Column(modifier = Modifier.fillMaxSize()) {
            AyahSurahFilterBar(selectedSurahId = uiState.surahFilter, onSelect = { onEvent(SearchEvent.SurahFilterChanged(it)) })
            if (uiState.results.ayat.isEmpty()) {
                EmptySearchNotice(text = stringResource(R.string.search_no_ayat))
            } else {
                LazyColumn(state = listState, modifier = Modifier.fillMaxSize().padding(horizontal = 16.dp), verticalArrangement = androidx.compose.foundation.layout.Arrangement.spacedBy(10.dp)) {
                    item(key = "count") {
                        SectionHeader(title = stringResource(R.string.search_section_ayat, uiState.results.ayat.size))
                    }
                    items(uiState.results.ayat, key = { "${it.surah}_${it.ayah}_${it.textUthmani}" }) { ayah ->
                        val key = "${ayah.surah}:${ayah.ayah}"
                        AyahSearchCard(
                            ayah = ayah,
                            query = uiState.query,
                            isBookmarked = bookmarkedAyat.contains(key),
                            onToggleBookmark = { onToggleAyahBookmark(ayah.surah, ayah.ayah) },
                            onClick = { onRecordRecent(uiState.query); onNavigateToSurahDetail(ayah.surah, ayah.ayah) }
                        )
                    }
                    if (uiState.isLoadingMore) item(key = "loading_more") { SearchLoadingView() }
                    item(key = "spacer") { Spacer(modifier = Modifier.height(24.dp)) }
                }
            }
        }
        SearchScope.WORDS -> SearchWordsBody(uiState = uiState, listState = listState, onSelectWord = onSelectWord)
        SearchScope.SURAHS -> {
            if (uiState.results.surahs.isEmpty()) {
                EmptySearchNotice(text = stringResource(R.string.search_no_surahs))
            } else {
                LazyColumn(state = listState, modifier = Modifier.fillMaxSize().padding(horizontal = 16.dp), verticalArrangement = androidx.compose.foundation.layout.Arrangement.spacedBy(10.dp)) {
                    items(uiState.results.surahs, key = { it.id }) { SurahSearchCard(surah = it, onClick = { onRecordRecent(uiState.query); onNavigateToSurahDetail(it.id, 1) }) }
                    item(key = "spacer") { Spacer(modifier = Modifier.height(24.dp)) }
                }
            }
        }
        SearchScope.LEXICON -> SearchLexiconBody(
            uiState = uiState,
            listState = listState,
            onSubScope = { onEvent(SearchEvent.LexiconSubScopeChanged(it)) },
            onNavigateToRootDetail = onNavigateToRootDetail,
            onOpenRoot = onOpenRoot,
            onRecordRecent = onRecordRecent
        )
    }
}
