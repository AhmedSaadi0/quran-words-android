package io.github.ahmedsaadi0.quranwords.ui.screens

import android.content.ActivityNotFoundException
import android.content.Intent
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
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
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.snapshotFlow
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalClipboardManager
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.pluralStringResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import io.github.ahmedsaadi0.quranwords.R
import io.github.ahmedsaadi0.quranwords.ui.roots.detail.components.CopyAllActionBar
import io.github.ahmedsaadi0.quranwords.ui.roots.detail.components.cards.AyahOccurrenceCard
import io.github.ahmedsaadi0.quranwords.ui.roots.detail.components.cards.EmptyTabNotice
import io.github.ahmedsaadi0.quranwords.ui.viewmodel.WordAyatViewModel
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.launch

@OptIn(ExperimentalMaterial3Api::class, ExperimentalFoundationApi::class)
@Composable
fun WordAyatScreen(
    rootId: Int,
    wordId: Int,
    onNavigateBack: () -> Unit,
    onNavigateToSurahDetail: (Int, Int) -> Unit,
    viewModel: WordAyatViewModel
) {
    val wordText by viewModel.wordText.collectAsStateWithLifecycle()
    val occurrences by viewModel.occurrences.collectAsStateWithLifecycle()
    val hasMore by viewModel.hasMore.collectAsStateWithLifecycle()
    val totalCount by viewModel.totalCount.collectAsStateWithLifecycle()
    val isLoading by viewModel.isLoading.collectAsStateWithLifecycle()
    val isLoadingMore by viewModel.isLoadingMore.collectAsStateWithLifecycle()
    val isCopyingAll by viewModel.isCopyingAll.collectAsStateWithLifecycle()

    val listState: LazyListState = rememberLazyListState()
    val scope = rememberCoroutineScope()
    val snackbarHostState = remember { SnackbarHostState() }
    val clipboardManager = LocalClipboardManager.current
    val context = LocalContext.current

    LaunchedEffect(rootId, wordId) {
        viewModel.loadWord(rootId, wordId)
    }

    LaunchedEffect(listState) {
        snapshotFlow { listState.layoutInfo.visibleItemsInfo.lastOrNull()?.index ?: -1 }
            .distinctUntilChanged()
            .collectLatest { lastIdx ->
                if (lastIdx == -1) return@collectLatest
                val adjusted = if (lastIdx > 0) lastIdx - 1 else lastIdx
                viewModel.loadMoreIfNeeded(adjusted)
            }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Text(
                        // wordText is Arabic reference data (never translated).
                        text = if (wordText.isNotBlank()) stringResource(R.string.word_title_template, wordText)
                        else stringResource(R.string.word_title_fallback),
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
            isLoading -> {
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(innerPadding),
                    contentAlignment = Alignment.Center
                ) {
                    CircularProgressIndicator(color = MaterialTheme.colorScheme.primary)
                }
            }
            occurrences.isEmpty() && !hasMore && !isLoading && totalCount == 0 -> {
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
                LazyColumn(
                    state = listState,
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(innerPadding)
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
                                subtitle = if (totalCount > 0) stringResource(
                                    R.string.occurrences_progress,
                                    occurrences.size,
                                    totalCount,
                                    pluralStringResource(R.plurals.root_occurrences, totalCount, totalCount)
                                ) else pluralStringResource(R.plurals.root_occurrences, occurrences.size, occurrences.size),
                                onCopyClick = {
                                    scope.launch {
                                        val formatted = viewModel.getAllFormatted()
                                        if (formatted.isNotBlank()) {
                                            clipboardManager.setText(AnnotatedString(formatted))
                                            snackbarHostState.showSnackbar(
                                                context.resources.getQuantityString(
                                                    R.plurals.copied_ayat_count,
                                                    totalCount,
                                                    totalCount
                                                )
                                            )
                                        } else {
                                            snackbarHostState.showSnackbar(context.getString(R.string.no_ayat_copy))
                                        }
                                    }
                                },
                                onShareClick = {
                                    scope.launch {
                                        val formatted = viewModel.getAllFormatted()
                                        if (formatted.isNotBlank()) {
                                            try {
                                                val sendIntent = Intent(Intent.ACTION_SEND).apply {
                                                    type = "text/plain"
                                                    putExtra(Intent.EXTRA_TEXT, formatted)
                                                }
                                                context.startActivity(
                                                    Intent.createChooser(sendIntent, context.getString(R.string.share_ayat))
                                                )
                                            } catch (_: ActivityNotFoundException) {
                                                snackbarHostState.showSnackbar(context.getString(R.string.no_share_app))
                                            }
                                        } else {
                                            snackbarHostState.showSnackbar(context.getString(R.string.no_ayat_share))
                                        }
                                    }
                                },
                                copyButtonText = stringResource(R.string.copy_all),
                                copyTag = "copy_all_occurrences_btn",
                                shareTag = "share_all_occurrences_btn",
                                isCopying = isCopyingAll,
                                modifier = Modifier.testTag("copy_all_bar")
                            )
                        }
                    }
                    itemsIndexed(
                        occurrences,
                        key = { _, occ -> "${occ.surahId}-${occ.ayahNum}" }
                    ) { _, occ ->
                        Box(
                            modifier = Modifier
                                .padding(horizontal = 16.dp, vertical = 5.dp)
                                .animateItem()
                        ) {
                            AyahOccurrenceCard (
                                occ = occ,
                                onClick = { onNavigateToSurahDetail(occ.surahId, occ.ayahNum) }
                            )
                        }
                    }
                    if (isLoadingMore) {
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
                    } else if (!hasMore && occurrences.isNotEmpty()) {
                        item {
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
                                            if (totalCount > 0) totalCount else occurrences.size,
                                            if (totalCount > 0) totalCount else occurrences.size
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
        }
    }
}
