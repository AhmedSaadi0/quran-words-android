package io.github.ahmedsaadi0.quranwords.ui.roots.word

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
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
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.snapshotFlow
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.pluralStringResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import io.github.ahmedsaadi0.quranwords.R
import io.github.ahmedsaadi0.quranwords.ui.roots.detail.components.CopyAllActionBar
import io.github.ahmedsaadi0.quranwords.ui.roots.detail.components.cards.AyahOccurrenceCard
import io.github.ahmedsaadi0.quranwords.ui.roots.detail.components.cards.EmptyTabNotice
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.flow.distinctUntilChanged

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun WordAyatScreen(
    uiState: WordAyatUiState,
    onEvent: (WordAyatEvent) -> Unit,
    onNavigateBack: () -> Unit,
    onNavigateToSurahDetail: (Int, Int) -> Unit,
    snackbarHostState: SnackbarHostState
) {
    val listState = rememberLazyListState()
    val loadError = uiState.error

    // Pagination trigger — snapshotFlow pattern (AyatTab precedent).
    LaunchedEffect(listState) {
        snapshotFlow { listState.layoutInfo.visibleItemsInfo.lastOrNull()?.index ?: -1 }
            .distinctUntilChanged()
            .collectLatest { lastIdx ->
                if (lastIdx == -1) return@collectLatest
                val adjusted = if (lastIdx > 0) lastIdx - 1 else lastIdx
                onEvent(WordAyatEvent.AyatNearingEnd(adjusted))
            }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Text(
                        // wordText is Arabic reference data (never translated).
                        text = if (uiState.wordText.isNotBlank()) {
                            stringResource(R.string.word_title_template, uiState.wordText)
                        } else {
                            stringResource(R.string.word_title_fallback)
                        },
                        fontWeight = FontWeight.Bold,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
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
                }
            )
        },
        snackbarHost = { SnackbarHost(snackbarHostState) }
    ) { innerPadding ->
        when {
            uiState.isLoading -> {
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(innerPadding),
                    contentAlignment = Alignment.Center
                ) {
                    CircularProgressIndicator(color = MaterialTheme.colorScheme.primary)
                }
            }
            uiState.occurrences.isEmpty() && loadError != null -> {
                WordAyatError(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(innerPadding),
                    message = loadError,
                    onRetry = { onEvent(WordAyatEvent.Retry) }
                )
            }
            uiState.occurrences.isEmpty() && !uiState.hasMore && uiState.totalCount == 0 -> {
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(innerPadding)
                        .testTag("word_ayat_empty"),
                    contentAlignment = Alignment.Center
                ) {
                    EmptyTabNotice(text = stringResource(R.string.word_no_ayat))
                }
            }
            else -> {
                WordAyatList(
                    uiState = uiState,
                    onEvent = onEvent,
                    onNavigateToSurahDetail = onNavigateToSurahDetail,
                    listState = listState,
                    contentPadding = innerPadding
                )
            }
        }
    }
}

@Composable
private fun WordAyatList(
    uiState: WordAyatUiState,
    onEvent: (WordAyatEvent) -> Unit,
    onNavigateToSurahDetail: (Int, Int) -> Unit,
    listState: LazyListState,
    contentPadding: PaddingValues
) {
    LazyColumn(
        state = listState,
        modifier = Modifier
            .fillMaxSize()
            .padding(contentPadding)
            .testTag("word_ayat_list"),
        contentPadding = PaddingValues(top = 12.dp, bottom = 24.dp),
        verticalArrangement = Arrangement.spacedBy(0.dp)
    ) {
        item {
            Box(
                modifier = Modifier
                    .padding(horizontal = 16.dp, vertical = 5.dp)
                    .animateItem()
            ) {
                CopyAllActionBar(
                    title = stringResource(R.string.root_ayat_title),
                    subtitle = if (uiState.totalCount > 0) {
                        stringResource(
                            R.string.occurrences_progress,
                            uiState.occurrences.size,
                            uiState.totalCount,
                            pluralStringResource(R.plurals.root_occurrences, uiState.totalCount, uiState.totalCount)
                        )
                    } else {
                        pluralStringResource(R.plurals.root_occurrences, uiState.occurrences.size, uiState.occurrences.size)
                    },
                    onCopyClick = { onEvent(WordAyatEvent.CopyAll) },
                    onShareClick = { onEvent(WordAyatEvent.ShareAll) },
                    copyButtonText = stringResource(R.string.copy_all),
                    copyTag = "copy_all_occurrences_btn",
                    shareTag = "share_all_occurrences_btn",
                    isCopying = uiState.isCopyingAll,
                    modifier = Modifier.testTag("copy_all_bar")
                )
            }
        }
        itemsIndexed(
            uiState.occurrences,
            key = { _, occ -> "${occ.surahId}-${occ.ayahNum}" }
        ) { _, occ ->
            Box(
                modifier = Modifier
                    .padding(horizontal = 16.dp, vertical = 5.dp)
                    .animateItem()
            ) {
                AyahOccurrenceCard(
                    occ = occ,
                    onClick = { onNavigateToSurahDetail(occ.surahId, occ.ayahNum) }
                )
            }
        }
        when {
            uiState.isLoadingMore -> item {
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
            uiState.error != null -> item {
                WordAyatError(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(16.dp),
                    message = uiState.error ?: "",
                    onRetry = { onEvent(WordAyatEvent.Retry) }
                )
            }
            !uiState.hasMore && uiState.occurrences.isNotEmpty() -> item {
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
                                if (uiState.totalCount > 0) uiState.totalCount else uiState.occurrences.size,
                                if (uiState.totalCount > 0) uiState.totalCount else uiState.occurrences.size
                            )
                        ),
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
        }
        item { Spacer(modifier = Modifier.height(80.dp)) }
    }
}

@Composable
private fun WordAyatError(
    modifier: Modifier,
    message: String,
    onRetry: () -> Unit
) {
    Column(
        modifier = modifier,
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
        Button(onClick = onRetry, modifier = Modifier.testTag("word_ayat_retry_btn")) {
            Text(stringResource(R.string.common_retry))
        }
    }
}