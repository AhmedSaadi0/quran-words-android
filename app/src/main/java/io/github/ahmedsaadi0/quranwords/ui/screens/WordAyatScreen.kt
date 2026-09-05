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
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.snapshotFlow
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalClipboardManager
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
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
    val wordText by viewModel.wordText.collectAsState()
    val occurrences by viewModel.occurrences.collectAsState()
    val hasMore by viewModel.hasMore.collectAsState()
    val totalCount by viewModel.totalCount.collectAsState()
    val isLoading by viewModel.isLoading.collectAsState()
    val isLoadingMore by viewModel.isLoadingMore.collectAsState()
    val isCopyingAll by viewModel.isCopyingAll.collectAsState()

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
                        text = if (wordText.isNotBlank()) "الكلمة: $wordText" else "آيات الكلمة",
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
                            contentDescription = "رجوع"
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
                    EmptyTabNotice(text = "لا توجد آيات مسجلة لهذه الكلمة")
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
                            CopyAllOccurrencesBar(
                                totalCount = totalCount,
                                occurrencesSize = occurrences.size,
                                isCopying = isCopyingAll,
                                onCopyClick = {
                                    scope.launch {
                                        val formatted = viewModel.getAllFormatted()
                                        if (formatted.isNotBlank()) {
                                            clipboardManager.setText(AnnotatedString(formatted))
                                            snackbarHostState.showSnackbar("تم نسخ $totalCount آيات")
                                        } else {
                                            snackbarHostState.showSnackbar("لا توجد آيات للنسخ")
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
                                                    Intent.createChooser(sendIntent, "مشاركة الآيات")
                                                )
                                            } catch (_: ActivityNotFoundException) {
                                                snackbarHostState.showSnackbar("لا يوجد تطبيق للمشاركة")
                                            }
                                        } else {
                                            snackbarHostState.showSnackbar("لا توجد آيات للمشاركة")
                                        }
                                    }
                                }
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
                            AyahOccurrenceCard(
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
                                    text = "تم عرض جميع الآيات • ${if (totalCount > 0) totalCount else occurrences.size} موضع",
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
