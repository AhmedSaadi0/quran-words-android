package io.github.ahmedsaadi0.quranwords.ui.screens

import android.content.ActivityNotFoundException
import android.content.Intent
import androidx.compose.animation.core.tween
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
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
import androidx.compose.foundation.layout.statusBars
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyListState
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.PagerState
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.outlined.ReportProblem
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Tab
import androidx.compose.material3.ScrollableTabRow
import androidx.compose.material3.Badge
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.runtime.snapshotFlow
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clipToBounds
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.input.nestedscroll.NestedScrollConnection
import androidx.compose.ui.input.nestedscroll.NestedScrollSource
import androidx.compose.ui.input.nestedscroll.nestedScroll
import androidx.compose.ui.layout.layout
import androidx.compose.ui.platform.LocalClipboardManager
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.pluralStringResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Constraints
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import io.github.ahmedsaadi0.quranwords.R
import io.github.ahmedsaadi0.quranwords.ui.components.ReportMeaningDialog
import io.github.ahmedsaadi0.quranwords.ui.theme.AppMotion
import io.github.ahmedsaadi0.quranwords.ui.viewmodel.RootViewModel
import io.github.ahmedsaadi0.quranwords.util.ReportAyahSample
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.launch
import kotlin.math.roundToInt

@OptIn(ExperimentalMaterial3Api::class, ExperimentalFoundationApi::class)
@Composable
fun RootDetailScreen(
    rootId: Int,
    onNavigateBack: () -> Unit,
    onNavigateToSurahDetail: (Int, Int) -> Unit,
    onNavigateToWordAyat: (Int, Int) -> Unit = { _, _ -> },
    rootViewModel: RootViewModel
) {
    val rootDetail by rootViewModel.rootDetail.collectAsState()
    val isLoading by rootViewModel.isLoading.collectAsState()
    val occurrences by rootViewModel.occurrences.collectAsState()
    val occurrencesHasMore by rootViewModel.occurrencesHasMore.collectAsState()
    val isOccurrencesLoadingMore by rootViewModel.isOccurrencesLoadingMore.collectAsState()
    val isCopyingAll by rootViewModel.isCopyingAll.collectAsState()
    val rootWords by rootViewModel.rootWords.collectAsState()
    val isWordsLoading by rootViewModel.isWordsLoading.collectAsState()
    val selectedWordIds by rootViewModel.selectedWordIds.collectAsState()
    val isWordSelectionMode by rootViewModel.isWordSelectionMode.collectAsState()
    val selectedMeaningIds by rootViewModel.selectedMeaningIds.collectAsState()
    val isMeaningSelectionMode by rootViewModel.isMeaningSelectionMode.collectAsState()

    var showReportDialog by remember { mutableStateOf(false) }

    val meaningsState = rememberSaveable(saver = LazyListState.Saver) { LazyListState() }
    val masadirState = rememberSaveable(saver = LazyListState.Saver) { LazyListState() }
    val derivativesState = rememberSaveable(saver = LazyListState.Saver) { LazyListState() }
    val ayatState = rememberSaveable(saver = LazyListState.Saver) { LazyListState() }
    val wordsState = rememberSaveable(saver = LazyListState.Saver) { LazyListState() }

    val pagerState = rememberPagerState(initialPage = 0, pageCount = { 5 })
    val scope = rememberCoroutineScope()
    val snackbarHostState = remember { SnackbarHostState() }
    val clipboardManager = LocalClipboardManager.current
    val context = LocalContext.current

    // Shared Header Collapse State (Native CoordinatorLayout / AppBarLayout behavior) — saveable across process death / back-nav
    var subtitleHeightPx by rememberSaveable { mutableIntStateOf(0) }
    var headerOffsetPx by rememberSaveable { mutableFloatStateOf(0f) }

    val nestedScrollConnection = remember {
        object : NestedScrollConnection {
            override fun onPreScroll(available: Offset, source: NestedScrollSource): Offset {
                val delta = available.y
                // Finger dragging up -> collapse header first
                if (delta < 0f && subtitleHeightPx > 0) {
                    val prevOffset = headerOffsetPx
                    headerOffsetPx =
                        (headerOffsetPx + delta).coerceIn(-subtitleHeightPx.toFloat(), 0f)
                    val consumed = headerOffsetPx - prevOffset
                    return Offset(0f, consumed)
                }
                return Offset.Zero
            }

            override fun onPostScroll(
                consumed: Offset,
                available: Offset,
                source: NestedScrollSource
            ): Offset {
                val delta = available.y
                // Finger dragging down & list reached top -> expand header
                if (delta > 0f && subtitleHeightPx > 0) {
                    val prevOffset = headerOffsetPx
                    headerOffsetPx =
                        (headerOffsetPx + delta).coerceIn(-subtitleHeightPx.toFloat(), 0f)
                    val consumed = headerOffsetPx - prevOffset
                    return Offset(0f, consumed)
                }
                return Offset.Zero
            }
        }
    }

    LaunchedEffect(rootId) {
        rootViewModel.loadRootDetail(rootId)
    }

    LaunchedEffect(pagerState, ayatState) {
        snapshotFlow {
            val page = pagerState.currentPage
            val lastIdx = ayatState.layoutInfo.visibleItemsInfo.lastOrNull()?.index ?: -1
            page to lastIdx
        }.collectLatest { (page, lastIdx) ->
            if (page != 4) return@collectLatest
            if (lastIdx == -1) return@collectLatest
            // LazyColumn has CopyAllOccurrencesBar as item 0 when data exists, so subtract 1
            val adjustedIdx = if (lastIdx > 0) lastIdx - 1 else lastIdx
            rootViewModel.loadMoreOccurrencesIfNeeded(adjustedIdx)
        }
    }

    val collapseProgress = if (subtitleHeightPx > 0) {
        (1f + (headerOffsetPx / subtitleHeightPx.toFloat())).coerceIn(0f, 1f)
    } else {
        1f
    }

    Scaffold(
        containerColor = MaterialTheme.colorScheme.background,
        contentWindowInsets = WindowInsets(0, 0, 0, 0),
        snackbarHost = { SnackbarHost(snackbarHostState) }
    ) { innerPadding ->
        if (isLoading || rootDetail == null) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(innerPadding),
                contentAlignment = Alignment.Center
            ) {
                CircularProgressIndicator(color = MaterialTheme.colorScheme.primary)
            }
        } else {
            val detail = rootDetail!!
            val item = detail.item

            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(innerPadding)
                    .nestedScroll(nestedScrollConnection)
            ) {
                // Fixed & Collapsing Top Section
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .background(MaterialTheme.colorScheme.surface)
                        .windowInsetsPadding(WindowInsets.statusBars)
                ) {
                    // Pinned Top Bar Row (Always 64dp)
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(64.dp)
                            .padding(horizontal = 16.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(4.dp)
                        ) {
                            IconButton(
                                onClick = onNavigateBack,
                                modifier = Modifier.testTag("back_button")
                            ) {
                                Icon(
                                    imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                                    contentDescription = stringResource(R.string.cd_back)
                                )
                            }
                            Text(
                                // root text is Arabic reference data (never translated).
                                text = stringResource(R.string.root_title_template, item.root),
                                style = MaterialTheme.typography.titleLarge,
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.onSurface,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis
                            )
                        }
                        IconButton(
                            onClick = { showReportDialog = true },
                            modifier = Modifier.testTag("report_help_button")
                        ) {
                            Icon(
                                imageVector = Icons.Outlined.ReportProblem,
                                contentDescription = stringResource(R.string.cd_report),
                                tint = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }

                    // Collapsible Subtitle Container (Measured naturally, collapses pixel-by-pixel)
                    val subtitleText = detail.aiSummary?.takeIf { it.isNotBlank() }
                        ?: detail.item.glossAr?.takeIf { it.isNotBlank() }
                    val hasSubtitle = !subtitleText.isNullOrBlank()
                    val aiModel = detail.aiModel
                    val aiGeneratedAt = detail.aiGeneratedAt
                    val hasAiMeta = !aiModel.isNullOrBlank() || !aiGeneratedAt.isNullOrBlank()

                    if (hasSubtitle || hasAiMeta) {
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clipToBounds()
                                .graphicsLayer {
                                    alpha = collapseProgress
                                }
                                .layout { measurable, constraints ->
                                    val placeable = measurable.measure(
                                        constraints.copy(
                                            minHeight = 0,
                                            maxHeight = Constraints.Infinity
                                        )
                                    )
                                    val naturalHeight = placeable.height
                                    if (subtitleHeightPx != naturalHeight && naturalHeight > 0) {
                                        subtitleHeightPx = naturalHeight
                                    }
                                    val currentHeight =
                                        (naturalHeight + headerOffsetPx.roundToInt())
                                            .coerceIn(0, naturalHeight)
                                    layout(placeable.width, currentHeight) {
                                        placeable.placeRelative(0, headerOffsetPx.roundToInt())
                                    }
                                }
                        ) {
                            Column(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(horizontal = 16.dp, vertical = 8.dp),
                                verticalArrangement = Arrangement.spacedBy(6.dp)
                            ) {
                                if (hasSubtitle) {
                                    Text(
                                        text = subtitleText ?: "",
                                        style = MaterialTheme.typography.bodyMedium,
                                        color = MaterialTheme.colorScheme.onSurface
                                    )
                                }
                                if (hasAiMeta) {
                                    val formattedDate = try {
                                        val raw = aiGeneratedAt ?: ""
                                        val cleaned = raw.replace("T", " ")
                                        if (cleaned.length >= 16) cleaned.substring(0, 16) else cleaned
                                    } catch (_: Exception) {
                                        aiGeneratedAt ?: ""
                                    }
                                    val metaLine = buildString {
                                        if (!aiModel.isNullOrBlank()) append("🤖 $aiModel")
                                        if (!aiModel.isNullOrBlank() && formattedDate.isNotBlank()) append("  •  ")
                                        if (formattedDate.isNotBlank()) append("🕒 $formattedDate")
                                    }
                                    if (metaLine.isNotBlank()) {
                                        Text(
                                            text = metaLine,
                                            style = MaterialTheme.typography.labelSmall,
                                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                                            maxLines = 1,
                                            overflow = TextOverflow.Ellipsis
                                        )
                                    }
                                }
                                // Compact AI-summary actions (copy/share like Ayat):
                                // inside the measured container so header collapse keeps working.
                                if (hasSubtitle) {
                                    Row(
                                        horizontalArrangement = Arrangement.spacedBy(0.dp),
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        IconButton(
                                            onClick = {
                                                val formatted = io.github.ahmedsaadi0.quranwords.core.util.QuranCopyFormatter.formatAiSummary(
                                                    item.root,
                                                    detail.aiSummary ?: ""
                                                )
                                                if (formatted.isNotBlank()) {
                                                    clipboardManager.setText(AnnotatedString(formatted))
                                                    scope.launch {
                                                        snackbarHostState.showSnackbar(context.getString(R.string.ai_summary_copied))
                                                    }
                                                }
                                            },
                                            modifier = Modifier
                                                .size(28.dp)
                                                .testTag("copy_ai_summary_btn")
                                        ) {
                                            Text("📋", fontSize = 13.sp)
                                        }
                                        IconButton(
                                            onClick = {
                                                val formatted = io.github.ahmedsaadi0.quranwords.core.util.QuranCopyFormatter.formatAiSummary(
                                                    item.root,
                                                    detail.aiSummary ?: ""
                                                )
                                                if (formatted.isNotBlank()) {
                                                    try {
                                                        val sendIntent = Intent(Intent.ACTION_SEND).apply {
                                                            type = "text/plain"
                                                            putExtra(Intent.EXTRA_TEXT, formatted)
                                                        }
                                                        context.startActivity(
                                                            Intent.createChooser(sendIntent, context.getString(R.string.common_share))
                                                        )
                                                    } catch (_: ActivityNotFoundException) {
                                                        scope.launch {
                                                            snackbarHostState.showSnackbar(context.getString(R.string.no_share_app))
                                                        }
                                                    }
                                                }
                                            },
                                            modifier = Modifier
                                                .size(28.dp)
                                                .testTag("share_ai_summary_btn")
                                        ) {
                                            Text("↗", fontSize = 15.sp)
                                        }
                                    }
                                }
                            }
                        }
                    }

                    // Pinned tabs: scrollable row + count badges (5 tabs don't fit a fixed row)
                    ScrollableTabRow(
                        selectedTabIndex = pagerState.currentPage,
                        containerColor = MaterialTheme.colorScheme.surface,
                        edgePadding = 16.dp,
                        divider = {}
                    ) {
                        val tabs = listOf(
                            stringResource(R.string.tab_meanings) to detail.meanings.size,
                            stringResource(R.string.tab_masadir) to detail.masadir.size,
                            stringResource(R.string.tab_derivatives) to detail.derivatives.size,
                            stringResource(R.string.tab_words) to rootWords.size,
                            stringResource(R.string.tab_ayat) to detail.item.occurrencesCount
                        )
                        tabs.forEachIndexed { index, (title, count) ->
                            val selected = pagerState.currentPage == index
                            Tab(
                                selected = selected,
                                onClick = {
                                    scope.launch {
                                        pagerState.animateScrollToPage(
                                            index,
                                            animationSpec = tween(
                                                durationMillis = AppMotion.DurationMedium,
                                                easing = AppMotion.EasingStandard
                                            )
                                        )
                                    }
                                },
                                modifier = Modifier.testTag("root_tab_$index"),
                                text = {
                                    Row(
                                        verticalAlignment = Alignment.CenterVertically,
                                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                                    ) {
                                        Text(
                                            title,
                                            fontWeight = FontWeight.Bold,
                                            maxLines = 1
                                        )
                                        Badge(
                                            containerColor = if (selected) MaterialTheme.colorScheme.primary
                                            else MaterialTheme.colorScheme.surfaceVariant,
                                            contentColor = if (selected) MaterialTheme.colorScheme.onPrimary
                                            else MaterialTheme.colorScheme.onSurfaceVariant
                                        ) {
                                            Text(
                                                text = count.toString(),
                                                style = MaterialTheme.typography.labelSmall,
                                                fontWeight = FontWeight.Bold
                                            )
                                        }
                                    }
                                }
                            )
                        }
                    }
                }

                // Pure content lists inside HorizontalPager
                HorizontalPager(
                    state = pagerState,
                    modifier = Modifier
                        .weight(1f)
                        .fillMaxWidth(),
                    beyondViewportPageCount = 1
                ) { page ->
                    when (page) {
                        0 -> {
                            LazyColumn(
                                state = meaningsState,
                                modifier = Modifier
                                    .fillMaxSize()
                                    .testTag("root_detail_screen_0"),
                                contentPadding = PaddingValues(top = 12.dp, bottom = 24.dp),
                                verticalArrangement = Arrangement.spacedBy(0.dp)
                            ) {
                                if (detail.meanings.isEmpty()) {
                                    item {
                                        EmptyTabNotice(text = stringResource(R.string.root_no_meanings))
                                    }
                                } else {
                                    item {
                                        Box(
                                            modifier = Modifier
                                                .padding(horizontal = 16.dp, vertical = 5.dp)
                                                .animateItem()
                                        ) {
                                            CopyAllMeaningsBar(
                                                meaningsCount = detail.meanings.size,
                                                isCopying = isCopyingAll,
                                                onCopyClick = {
                                                    scope.launch {
                                                        val formatted = rootViewModel.getAllMeaningsFormatted()
                                                        if (formatted.isNotBlank()) {
                                                            clipboardManager.setText(AnnotatedString(formatted))
                                                            snackbarHostState.showSnackbar(
                                                                context.resources.getQuantityString(
                                                                    R.plurals.copied_meanings_count,
                                                                    detail.meanings.size,
                                                                    detail.meanings.size
                                                                )
                                                            )
                                                        } else {
                                                            snackbarHostState.showSnackbar(context.getString(R.string.no_meanings_copy))
                                                        }
                                                    }
                                                },
                                                onShareClick = {
                                                    scope.launch {
                                                        val formatted = rootViewModel.getAllMeaningsFormatted()
                                                        if (formatted.isNotBlank()) {
                                                            try {
                                                                val sendIntent = Intent(Intent.ACTION_SEND).apply {
                                                                    type = "text/plain"
                                                                    putExtra(Intent.EXTRA_TEXT, formatted)
                                                                }
                                                                context.startActivity(
                                                                    Intent.createChooser(sendIntent, context.getString(R.string.share_meanings))
                                                                )
                                                            } catch (_: ActivityNotFoundException) {
                                                                snackbarHostState.showSnackbar(context.getString(R.string.no_share_app))
                                                            }
                                                        } else {
                                                            snackbarHostState.showSnackbar(context.getString(R.string.no_meanings_share))
                                                        }
                                                    }
                                                }
                                            )
                                        }
                                    }
                                    if (isMeaningSelectionMode) {
                                        item {
                                            Box(
                                                modifier = Modifier
                                                    .padding(horizontal = 16.dp, vertical = 5.dp)
                                                    .animateItem()
                                            ) {
                                                SelectedMeaningsBar(
                                                    selectedCount = selectedMeaningIds.size,
                                                    isCopying = isCopyingAll,
                                                    onCopyClick = {
                                                        scope.launch {
                                                            val formatted = rootViewModel.getSelectedMeaningsFormatted()
                                                            if (formatted.isNotBlank()) {
                                                                clipboardManager.setText(AnnotatedString(formatted))
                                                                snackbarHostState.showSnackbar(
                                                                    context.resources.getQuantityString(
                                                                        R.plurals.copied_meanings_count,
                                                                        selectedMeaningIds.size,
                                                                        selectedMeaningIds.size
                                                                    )
                                                                )
                                                            } else {
                                                                snackbarHostState.showSnackbar(context.getString(R.string.no_meanings_copy))
                                                            }
                                                        }
                                                    },
                                                    onShareClick = {
                                                        scope.launch {
                                                            val formatted = rootViewModel.getSelectedMeaningsFormatted()
                                                            if (formatted.isNotBlank()) {
                                                                try {
                                                                    val sendIntent = Intent(Intent.ACTION_SEND).apply {
                                                                        type = "text/plain"
                                                                        putExtra(Intent.EXTRA_TEXT, formatted)
                                                                    }
                                                                    context.startActivity(
                                                                        Intent.createChooser(sendIntent, context.getString(R.string.share_meanings))
                                                                    )
                                                                } catch (_: ActivityNotFoundException) {
                                                                    snackbarHostState.showSnackbar(context.getString(R.string.no_share_app))
                                                                }
                                                            } else {
                                                                snackbarHostState.showSnackbar(context.getString(R.string.no_meanings_share))
                                                            }
                                                        }
                                                    },
                                                    onSelectAllClick = { rootViewModel.selectAllMeanings() },
                                                    onClearClick = { rootViewModel.clearMeaningSelection() }
                                                )
                                            }
                                        }
                                    }
                                    items(detail.meanings, key = { it.id }) { meaning ->
                                        Box(
                                            modifier = Modifier
                                                .padding(horizontal = 16.dp, vertical = 5.dp)
                                                .animateItem()
                                        ) {
                                            MeaningCard(
                                                meaning = meaning,
                                                isSelected = selectedMeaningIds.contains(meaning.id),
                                                isSelectionMode = isMeaningSelectionMode,
                                                onClick = {
                                                    if (isMeaningSelectionMode) {
                                                        rootViewModel.toggleMeaningSelection(meaning.id)
                                                    }
                                                },
                                                onLongClick = {
                                                    if (isMeaningSelectionMode) {
                                                        rootViewModel.toggleMeaningSelection(meaning.id)
                                                    } else {
                                                        rootViewModel.enterMeaningSelectionMode(meaning.id)
                                                    }
                                                }
                                            )
                                        }
                                    }
                                }
                                item { Spacer(modifier = Modifier.height(80.dp)) }
                            }
                        }

                        1 -> {
                            LazyColumn(
                                state = masadirState,
                                modifier = Modifier
                                    .fillMaxSize()
                                    .testTag("root_detail_screen_1"),
                                contentPadding = PaddingValues(top = 12.dp, bottom = 24.dp),
                                verticalArrangement = Arrangement.spacedBy(0.dp)
                            ) {
                                if (detail.masadir.isEmpty()) {
                                    item {
                                        EmptyTabNotice(text = stringResource(R.string.root_no_masadir))
                                    }
                                } else {
                                    items(detail.masadir, key = { it.id }) { masdar ->
                                        Box(
                                            modifier = Modifier
                                                .padding(horizontal = 16.dp, vertical = 5.dp)
                                                .animateItem()
                                        ) {
                                            MasdarCard(masdar)
                                        }
                                    }
                                }
                                item { Spacer(modifier = Modifier.height(80.dp)) }
                            }
                        }

                        2 -> {
                            LazyColumn(
                                state = derivativesState,
                                modifier = Modifier
                                    .fillMaxSize()
                                    .testTag("root_detail_screen_2"),
                                contentPadding = PaddingValues(top = 12.dp, bottom = 24.dp),
                                verticalArrangement = Arrangement.spacedBy(0.dp)
                            ) {
                                if (detail.derivatives.isEmpty()) {
                                    item {
                                        EmptyTabNotice(text = stringResource(R.string.root_no_derivatives))
                                    }
                                } else {
                                    items(detail.derivatives, key = { it.id }) { derivative ->
                                        Box(
                                            modifier = Modifier
                                                .padding(horizontal = 16.dp, vertical = 5.dp)
                                                .animateItem()
                                        ) {
                                            DerivativeCard(derivative)
                                        }
                                    }
                                }
                                item { Spacer(modifier = Modifier.height(80.dp)) }
                            }
                        }

                        3 -> {
                            LazyColumn(
                                state = wordsState,
                                modifier = Modifier
                                    .fillMaxSize()
                                    .testTag("root_detail_screen_3"),
                                contentPadding = PaddingValues(top = 12.dp, bottom = 24.dp),
                                verticalArrangement = Arrangement.spacedBy(0.dp)
                            ) {
                                when {
                                    isWordsLoading && rootWords.isEmpty() -> {
                                        item {
                                            Box(
                                                modifier = Modifier
                                                    .fillMaxWidth()
                                                    .padding(32.dp),
                                                contentAlignment = Alignment.Center
                                            ) {
                                                CircularProgressIndicator(
                                                    color = MaterialTheme.colorScheme.primary
                                                )
                                            }
                                        }
                                    }
                                    rootWords.isEmpty() -> {
                                        item {
                                            EmptyTabNotice(text = stringResource(R.string.root_no_words))
                                        }
                                    }
                                    else -> {
                                        if (isWordSelectionMode) {
                                            item {
                                                Box(
                                                    modifier = Modifier
                                                        .padding(horizontal = 16.dp, vertical = 5.dp)
                                                        .animateItem()
                                                ) {
                                                    SelectedWordsBar(
                                                        selectedCount = selectedWordIds.size,
                                                        isCopying = isCopyingAll,
                                                        onCopyClick = {
                                                            scope.launch {
                                                                val formatted =
                                                                    rootViewModel.getSelectedWordsOccurrencesFormatted()
                                                                if (formatted.isNotBlank()) {
                                                                    clipboardManager.setText(
                                                                        AnnotatedString(formatted)
                                                                    )
                                                                    snackbarHostState.showSnackbar(
                                                                        context.getString(R.string.root_copied_selected)
                                                                    )
                                                                } else {
                                                                    snackbarHostState.showSnackbar(context.getString(R.string.no_ayat_copy))
                                                                }
                                                            }
                                                        },
                                                        onShareClick = {
                                                            scope.launch {
                                                                val formatted =
                                                                    rootViewModel.getSelectedWordsOccurrencesFormatted()
                                                                if (formatted.isNotBlank()) {
                                                                    try {
                                                                        val sendIntent =
                                                                            Intent(Intent.ACTION_SEND).apply {
                                                                                type = "text/plain"
                                                                                putExtra(
                                                                                    Intent.EXTRA_TEXT,
                                                                                    formatted
                                                                                )
                                                                            }
                                                                        context.startActivity(
                                                                            Intent.createChooser(
                                                                                sendIntent,
                                                                                context.getString(R.string.share_ayat)
                                                                            )
                                                                        )
                                                                    } catch (_: ActivityNotFoundException) {
                                                                        snackbarHostState.showSnackbar(context.getString(R.string.no_share_app))
                                                                    }
                                                                } else {
                                                                    snackbarHostState.showSnackbar(context.getString(R.string.no_ayat_share))
                                                                }
                                                            }
                                                        },
                                                        onSelectAllClick = {
                                                            rootViewModel.selectAllWords()
                                                        },
                                                        onClearClick = {
                                                            rootViewModel.clearWordSelection()
                                                        }
                                                    )
                                                }
                                            }
                                        }
                                        items(rootWords, key = { it.wordId }) { word ->
                                            Box(
                                                modifier = Modifier
                                                    .padding(horizontal = 16.dp, vertical = 5.dp)
                                                    .animateItem()
                                            ) {
                                                WordCard(
                                                    word = word,
                                                    isSelected = selectedWordIds.contains(word.wordId),
                                                    isSelectionMode = isWordSelectionMode,
                                                    onClick = {
                                                        if (isWordSelectionMode) {
                                                            rootViewModel.toggleWordSelection(word.wordId)
                                                        } else {
                                                            onNavigateToWordAyat(rootId, word.wordId)
                                                        }
                                                    },
                                                    onLongClick = {
                                                        if (isWordSelectionMode) {
                                                            rootViewModel.toggleWordSelection(word.wordId)
                                                        } else {
                                                            rootViewModel.enterWordSelectionMode(word.wordId)
                                                        }
                                                    }
                                                )
                                            }
                                        }
                                    }
                                }
                                item { Spacer(modifier = Modifier.height(80.dp)) }
                            }
                        }

                        4 -> {
                            LazyColumn(
                                state = ayatState,
                                modifier = Modifier
                                    .fillMaxSize()
                                    .testTag("root_detail_screen_4"),
                                contentPadding = PaddingValues(top = 12.dp, bottom = 24.dp),
                                verticalArrangement = Arrangement.spacedBy(0.dp)
                            ) {
                                if (occurrences.isEmpty() && !occurrencesHasMore && !isOccurrencesLoadingMore) {
                                    item {
                                        EmptyTabNotice(text = stringResource(R.string.root_no_occurrences))
                                    }
                                } else {
                                    // Copy all bar — first item, fetches ALL via single query (bypasses pagination)
                                    item {
                                        Box(
                                            modifier = Modifier
                                                .padding(horizontal = 16.dp, vertical = 5.dp)
                                                .animateItem()
                                        ) {
                                            CopyAllOccurrencesBar(
                                                totalCount = detail.item.occurrencesCount,
                                                occurrencesSize = occurrences.size,
                                                isCopying = isCopyingAll,
                                                onCopyClick = {
                                                    scope.launch {
                                                        val formatted = rootViewModel.getAllOccurrencesFormatted()
                                                        if (formatted.isNotBlank()) {
                                                            clipboardManager.setText(AnnotatedString(formatted))
                                                            snackbarHostState.showSnackbar(
                                                                context.resources.getQuantityString(
                                                                    R.plurals.copied_ayat_count,
                                                                    detail.item.occurrencesCount,
                                                                    detail.item.occurrencesCount
                                                                )
                                                            )
                                                        } else {
                                                            snackbarHostState.showSnackbar(context.getString(R.string.no_ayat_copy))
                                                        }
                                                    }
                                                },
                                                onShareClick = {
                                                    scope.launch {
                                                        val formatted = rootViewModel.getAllOccurrencesFormatted()
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
                                                onClick = {
                                                    onNavigateToSurahDetail(
                                                        occ.surahId,
                                                        occ.ayahNum
                                                    )
                                                }
                                            )
                                        }
                                    }
                                    if (isOccurrencesLoadingMore) {
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
                                    } else if (occurrencesHasMore && occurrences.isNotEmpty()) {
                                        item {
                                            Box(
                                                modifier = Modifier
                                                    .fillMaxWidth()
                                                    .padding(horizontal = 16.dp, vertical = 8.dp),
                                                contentAlignment = Alignment.Center
                                            ) {
                                                Text(
                                                    text = stringResource(R.string.root_load_more, occurrences.size, detail.item.occurrencesCount),
                                                    style = MaterialTheme.typography.labelSmall,
                                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                                )
                                            }
                                        }
                                    } else if (!occurrencesHasMore && occurrences.isNotEmpty()) {
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
                                                            occurrences.size,
                                                            occurrences.size
                                                        )
                                                    ),
                                                    style = MaterialTheme.typography.labelSmall,
                                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                                )
                                            }
                                        }
                                    }
                                }
                                item { Spacer(modifier = Modifier.height(80.dp)) }
                            }
                        }
                    }
                }
            }

            if (showReportDialog) {
                val reportSamples = remember(occurrences) {
                    occurrences.take(2).map {
                        ReportAyahSample(it.surahNameAr, it.ayahNum, it.textUthmani)
                    }
                }
                ReportMeaningDialog(
                    rootText = item.root,
                    rootId = item.id,
                    aiSummary = detail.aiSummary ?: "",
                    samples = reportSamples,
                    onDismissRequest = { showReportDialog = false }
                )
            }
        }
    }
}