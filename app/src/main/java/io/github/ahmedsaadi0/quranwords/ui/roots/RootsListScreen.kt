package io.github.ahmedsaadi0.quranwords.ui.roots

import androidx.compose.animation.Crossfade
import androidx.compose.animation.core.tween
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.pluralStringResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import io.github.ahmedsaadi0.quranwords.R
import io.github.ahmedsaadi0.quranwords.ui.components.RootItemCard
import io.github.ahmedsaadi0.quranwords.ui.roots.detail.components.cards.EmptyTabNotice
import io.github.ahmedsaadi0.quranwords.ui.theme.AppMotion

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun RootsListScreen(
    uiState: RootsListUiState,
    onEvent: (RootsListEvent) -> Unit,
    onNavigateBack: () -> Unit,
    onNavigateToRootDetail: (Int) -> Unit
) {
    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Text(
                        text = "${stringResource(R.string.roots_title)} (${pluralStringResource(R.plurals.roots_count, uiState.totalCount, uiState.totalCount)})",
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
                .testTag("roots_list_screen")
        ) {
            // Search box - M3 small shape 12dp for input chips
            OutlinedTextField(
                value = uiState.query,
                onValueChange = { query -> onEvent(RootsListEvent.QueryChanged(query)) },
                placeholder = { Text(stringResource(R.string.roots_search_hint)) },
                leadingIcon = {
                    Icon(imageVector = Icons.Default.Search, contentDescription = null)
                },
                trailingIcon = {
                    if (uiState.query.isNotBlank()) {
                        IconButton(onClick = { onEvent(RootsListEvent.QueryChanged("")) }) {
                            Icon(imageVector = Icons.Default.Close, contentDescription = stringResource(R.string.cd_clear))
                        }
                    }
                },
                singleLine = true,
                shape = RoundedCornerShape(12.dp),
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 10.dp)
                    .testTag("root_search_input")
            )

            val loadError = uiState.error
            when {
                uiState.isLoading && uiState.totalCount == 0 -> RootsListLoading()
                loadError != null -> RootsListError(
                    message = loadError,
                    onRetry = { onEvent(RootsListEvent.Retry) }
                )
                !uiState.isLoading && uiState.totalCount == 0 -> RootsListError(
                    message = stringResource(R.string.roots_error_load),
                    onRetry = { onEvent(RootsListEvent.Retry) }
                )
                uiState.filteredRoots.isEmpty() -> EmptyTabNotice(
                    text = stringResource(R.string.roots_empty_results)
                )
                else -> RootsListContent(
                    uiState = uiState,
                    onNavigateToRootDetail = onNavigateToRootDetail
                )
            }
        }
    }
}

@Composable
private fun RootsListLoading() {
    Column(
        modifier = Modifier.fillMaxSize(),
        verticalArrangement = Arrangement.Center,
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        CircularProgressIndicator(color = MaterialTheme.colorScheme.primary)
    }
}

@Composable
private fun RootsListError(message: String, onRetry: () -> Unit) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(24.dp),
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
        Button(onClick = onRetry, modifier = Modifier.testTag("roots_retry_btn")) {
            Text(stringResource(R.string.common_retry))
        }
    }
}

@Composable
private fun RootsListContent(
    uiState: RootsListUiState,
    onNavigateToRootDetail: (Int) -> Unit
) {
    // Search filtering animation - Crossfade for query changes
    Crossfade(
        targetState = uiState.query,
        animationSpec = tween(durationMillis = AppMotion.DurationMedium),
        label = "rootsSearchCrossfade"
    ) { _ ->
        LazyColumn(
            modifier = Modifier.fillMaxSize(),
            contentPadding = PaddingValues(horizontal = 16.dp, vertical = 8.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            items(uiState.filteredRoots, key = { it.id }) { rootItem ->
                RootItemCard(
                    rootItem = rootItem,
                    onClick = { onNavigateToRootDetail(rootItem.id) },
                    modifier = Modifier.animateItem()
                )
            }
            item {
                Spacer(modifier = Modifier.height(24.dp))
            }
        }
    }
}