package io.github.ahmedsaadi0.quranwords.ui.search

import androidx.compose.animation.Crossfade
import androidx.compose.animation.core.tween
import androidx.compose.foundation.ExperimentalFoundationApi
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
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Tab
import androidx.compose.material3.TabRow
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.runtime.snapshotFlow
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import io.github.ahmedsaadi0.quranwords.R
import io.github.ahmedsaadi0.quranwords.ui.components.RootItemCard
import io.github.ahmedsaadi0.quranwords.ui.theme.AppMotion
import kotlinx.coroutines.flow.distinctUntilChanged

@OptIn(ExperimentalMaterial3Api::class, ExperimentalFoundationApi::class)
@Composable
fun SearchScreen(
    uiState: SearchUiState,
    onEvent: (SearchEvent) -> Unit,
    onNavigateBack: () -> Unit,
    onNavigateToRootDetail: (Int) -> Unit,
    onNavigateToSurahDetail: (Int, Int) -> Unit
) {
    var selectedTabIndex by rememberSaveable { mutableIntStateOf(0) }
    val listState = rememberLazyListState()
    val searchError = uiState.error

    LaunchedEffect(listState, selectedTabIndex) {
        snapshotFlow { listState.layoutInfo.visibleItemsInfo.lastOrNull()?.index ?: -1 }
            .distinctUntilChanged()
            .collect { lastIdx ->
                if (lastIdx == -1) return@collect
                val tab = SearchTab.entries.getOrNull(selectedTabIndex) ?: return@collect
                onEvent(SearchEvent.NearingEnd(tab, lastIdx))
            }
    }

    LaunchedEffect(selectedTabIndex, uiState.query) {
        listState.scrollToItem(0)
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Text(
                        text = stringResource(R.string.search_title),
                        fontWeight = FontWeight.Bold
                    )
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
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.surface
                )
            )
        },
        containerColor = MaterialTheme.colorScheme.background,
        contentWindowInsets = WindowInsets(0, 0, 0, 0)
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .testTag("search_screen")
        ) {
            // Search Input Field - M3 small shape 12dp for input, unified
            OutlinedTextField(
                value = uiState.query,
                onValueChange = { query -> onEvent(SearchEvent.QueryChanged(query)) },
                placeholder = { Text(stringResource(R.string.search_hint)) },
                leadingIcon = {
                    Icon(imageVector = Icons.Default.Search, contentDescription = null)
                },
                trailingIcon = {
                    if (uiState.query.isNotBlank()) {
                        IconButton(onClick = { onEvent(SearchEvent.QueryChanged("")) }) {
                            Icon(imageVector = Icons.Default.Close, contentDescription = stringResource(R.string.cd_clear))
                        }
                    }
                },
                singleLine = true,
                shape = RoundedCornerShape(12.dp),
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 10.dp)
                    .testTag("unified_search_input")
            )

            // Result category tabs
            TabRow(
                selectedTabIndex = selectedTabIndex,
                containerColor = MaterialTheme.colorScheme.surface
            ) {
                Tab(
                    selected = selectedTabIndex == 0,
                    onClick = { selectedTabIndex = 0 },
                    text = { Text(stringResource(R.string.search_tab_roots, uiState.results.roots.size), fontWeight = FontWeight.Bold) }
                )
                Tab(
                    selected = selectedTabIndex == 1,
                    onClick = { selectedTabIndex = 1 },
                    text = { Text(stringResource(R.string.search_tab_masadir, uiState.results.masadir.size), fontWeight = FontWeight.Bold) }
                )
                Tab(
                    selected = selectedTabIndex == 2,
                    onClick = { selectedTabIndex = 2 },
                    text = { Text(stringResource(R.string.search_tab_derivatives, uiState.results.derivatives.size), fontWeight = FontWeight.Bold) }
                )
                Tab(
                    selected = selectedTabIndex == 3,
                    onClick = { selectedTabIndex = 3 },
                    text = { Text(stringResource(R.string.search_tab_ayat, uiState.results.ayat.size), fontWeight = FontWeight.Bold) }
                )
            }

            when {
                uiState.isSearching -> {
                    Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                        CircularProgressIndicator(color = MaterialTheme.colorScheme.primary)
                    }
                }
                searchError != null -> {
                    SearchError(
                        modifier = Modifier.fillMaxSize(),
                        message = searchError,
                        onRetry = { onEvent(SearchEvent.Retry) }
                    )
                }
                uiState.query.isBlank() -> {
                    Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                        Column(
                            horizontalAlignment = Alignment.CenterHorizontally,
                            verticalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Filled.Search,
                                contentDescription = null,
                                tint = MaterialTheme.colorScheme.onSurfaceVariant,
                                modifier = Modifier.size(48.dp)
                            )
                            Text(
                                text = stringResource(R.string.search_empty_hint),
                                style = MaterialTheme.typography.bodyMedium,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }
                }
                else -> {
                    // Crossfade for query changes - balanced animation
                    Crossfade(
                        targetState = uiState.query,
                        animationSpec = tween(durationMillis = AppMotion.DurationMedium),
                        label = "searchQueryCrossfade"
                    ) { _ ->
                        LazyColumn(
                            state = listState,
                            modifier = Modifier
                                .fillMaxSize()
                                .testTag("search_results_list"),
                            contentPadding = PaddingValues(horizontal = 16.dp, vertical = 10.dp),
                            verticalArrangement = Arrangement.spacedBy(10.dp)
                        ) {
                            when (SearchTab.entries[selectedTabIndex]) {
                                SearchTab.ROOTS -> {
                                    if (uiState.results.roots.isEmpty()) {
                                        item { EmptySearchNotice(stringResource(R.string.search_no_roots)) }
                                    } else {
                                        items(uiState.results.roots, key = { it.id }) { rootItem ->
                                            RootItemCard(
                                                rootItem = rootItem,
                                                onClick = { onNavigateToRootDetail(rootItem.id) },
                                                modifier = Modifier.animateItem()
                                            )
                                        }
                                    }
                                }
                                SearchTab.MASADIR -> {
                                    if (uiState.results.masadir.isEmpty()) {
                                        item { EmptySearchNotice(stringResource(R.string.search_no_masadir)) }
                                    } else {
                                        items(uiState.results.masadir, key = { it.id }) { masdar ->
                                            Card(
                                                modifier = Modifier
                                                    .fillMaxWidth()
                                                    .animateItem(),
                                                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                                                shape = RoundedCornerShape(16.dp),
                                                border = CardDefaults.outlinedCardBorder()
                                            ) {
                                                Row(
                                                    modifier = Modifier
                                                        .fillMaxWidth()
                                                        .padding(14.dp),
                                                    horizontalArrangement = Arrangement.SpaceBetween,
                                                    verticalAlignment = Alignment.CenterVertically
                                                ) {
                                                    Column {
                                                        Text(
                                                            text = masdar.masdarAr,
                                                            style = MaterialTheme.typography.titleMedium,
                                                            fontWeight = FontWeight.Bold
                                                        )
                                                        Text(
                                                            // form/pattern are Arabic reference data; labels are chrome.
                                                            text = stringResource(R.string.search_masdar_line, masdar.form ?: "", masdar.pattern ?: ""),
                                                            style = MaterialTheme.typography.bodySmall,
                                                            color = MaterialTheme.colorScheme.onSurfaceVariant
                                                        )
                                                    }
                                                }
                                            }
                                        }
                                    }
                                }
                                SearchTab.DERIVATIVES -> {
                                    if (uiState.results.derivatives.isEmpty()) {
                                        item { EmptySearchNotice(stringResource(R.string.search_no_derivatives)) }
                                    } else {
                                        items(uiState.results.derivatives, key = { it.id }) { derivative ->
                                            Card(
                                                modifier = Modifier
                                                    .fillMaxWidth()
                                                    .animateItem(),
                                                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                                                shape = RoundedCornerShape(16.dp),
                                                border = CardDefaults.outlinedCardBorder()
                                            ) {
                                                Row(
                                                    modifier = Modifier
                                                        .fillMaxWidth()
                                                        .padding(14.dp),
                                                    horizontalArrangement = Arrangement.SpaceBetween,
                                                    verticalAlignment = Alignment.CenterVertically
                                                ) {
                                                    Column {
                                                        Text(
                                                            text = derivative.formAr,
                                                            style = MaterialTheme.typography.titleMedium,
                                                            fontWeight = FontWeight.Bold
                                                        )
                                                        Text(
                                                            // derivativeType/pattern are Arabic data; label is chrome.
                                                            text = stringResource(R.string.search_derivative_line, derivative.derivativeType, derivative.pattern),
                                                            style = MaterialTheme.typography.bodySmall,
                                                            color = MaterialTheme.colorScheme.onSurfaceVariant
                                                        )
                                                    }
                                                }
                                            }
                                        }
                                    }
                                }
                                SearchTab.AYAT -> {
                                    if (uiState.results.ayat.isEmpty()) {
                                        item { EmptySearchNotice(stringResource(R.string.search_no_ayat)) }
                                    } else {
                                        // Stable composite key — replaces fragile hashCode().
                                        items(
                                            uiState.results.ayat,
                                            key = { "${it.surah}_${it.ayah}_${it.textUthmani}" }
                                        ) { ayah ->
                                            Card(
                                                modifier = Modifier
                                                    .fillMaxWidth()
                                                    .clip(RoundedCornerShape(16.dp))
                                                    .clickable { onNavigateToSurahDetail(ayah.surah, ayah.ayah) }
                                                    .animateItem(),
                                                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                                                border = CardDefaults.outlinedCardBorder()
                                            ) {
                                                Column(
                                                    modifier = Modifier.padding(14.dp),
                                                    verticalArrangement = Arrangement.spacedBy(6.dp)
                                                ) {
                                                    Text(
                                                        text = stringResource(R.string.search_ayah_ref, ayah.surah, ayah.ayah),
                                                        style = MaterialTheme.typography.labelMedium,
                                                        fontWeight = FontWeight.Bold,
                                                        color = MaterialTheme.colorScheme.primary
                                                    )
                                                    Text(
                                                        text = "${ayah.textUthmani} ﴿${ayah.ayah}﴾",
                                                        style = MaterialTheme.typography.bodyLarge,
                                                        lineHeight = 26.sp,
                                                        color = MaterialTheme.colorScheme.onSurface
                                                    )
                                                }
                                            }
                                        }
                                    }
                                }
                            }

                            if (uiState.isLoadingMore) {
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
                            }

                            item {
                                Spacer(modifier = Modifier.height(24.dp))
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun SearchError(
    modifier: Modifier,
    message: String,
    onRetry: () -> Unit
) {
    Column(
        modifier = modifier.padding(24.dp),
        verticalArrangement = Arrangement.Center,
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Text(
            text = message,
            style = MaterialTheme.typography.bodyLarge,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            textAlign = TextAlign.Center
        )
        Spacer(modifier = Modifier.height(12.dp))
        Button(onClick = onRetry, modifier = Modifier.testTag("search_retry_btn")) {
            Text(stringResource(R.string.common_retry))
        }
    }
}

@Composable
fun EmptySearchNotice(text: String) {
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .padding(32.dp),
        contentAlignment = Alignment.Center
    ) {
        Text(
            text = text,
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
    }
}