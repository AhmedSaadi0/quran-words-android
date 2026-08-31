package io.github.ahmedsaadi0.quranwords.ui.screens

import android.content.Intent
import android.net.Uri
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
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.HelpOutline
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Tab
import androidx.compose.material3.TabRow
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.runtime.snapshotFlow
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.clipToBounds
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.input.nestedscroll.NestedScrollConnection
import androidx.compose.ui.input.nestedscroll.NestedScrollSource
import androidx.compose.ui.input.nestedscroll.nestedScroll
import androidx.compose.ui.layout.layout
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Constraints
import androidx.compose.ui.unit.dp
import io.github.ahmedsaadi0.quranwords.ui.components.ReportIssueCard
import io.github.ahmedsaadi0.quranwords.ui.theme.AppMotion
import io.github.ahmedsaadi0.quranwords.ui.theme.ShapeLarge
import io.github.ahmedsaadi0.quranwords.ui.viewmodel.RootViewModel
import io.github.ahmedsaadi0.quranwords.util.BuildIssueUrlOptions
import io.github.ahmedsaadi0.quranwords.util.buildGithubIssueUrl
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.launch
import kotlin.math.roundToInt

@OptIn(ExperimentalMaterial3Api::class, ExperimentalFoundationApi::class)
@Composable
fun RootDetailScreen(
    rootId: Int,
    onNavigateBack: () -> Unit,
    onNavigateToSurahDetail: (Int, Int) -> Unit,
    rootViewModel: RootViewModel
) {
    val rootDetail by rootViewModel.rootDetail.collectAsState()
    val isLoading by rootViewModel.isLoading.collectAsState()
    val occurrences by rootViewModel.occurrences.collectAsState()
    val occurrencesHasMore by rootViewModel.occurrencesHasMore.collectAsState()
    val isOccurrencesLoadingMore by rootViewModel.isOccurrencesLoadingMore.collectAsState()

    var showReportDialog by remember { mutableStateOf(false) }

    val meaningsState = rememberLazyListState()
    val masadirState = rememberLazyListState()
    val derivativesState = rememberLazyListState()
    val ayatState = rememberLazyListState()

    val pagerState = rememberPagerState(initialPage = 0, pageCount = { 4 })
    val scope = rememberCoroutineScope()

    // Shared Header Collapse State (Native CoordinatorLayout / AppBarLayout behavior)
    var subtitleHeightPx by remember { mutableIntStateOf(0) }
    var headerOffsetPx by remember { mutableFloatStateOf(0f) }

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
            if (page != 3) return@collectLatest
            if (lastIdx == -1) return@collectLatest
            rootViewModel.loadMoreOccurrencesIfNeeded(lastIdx)
        }
    }

    val collapseProgress = if (subtitleHeightPx > 0) {
        (1f + (headerOffsetPx / subtitleHeightPx.toFloat())).coerceIn(0f, 1f)
    } else {
        1f
    }
    val isCollapsed = collapseProgress < 0.1f

    Scaffold(
        containerColor = MaterialTheme.colorScheme.background,
        contentWindowInsets = WindowInsets(0, 0, 0, 0)
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
                                    contentDescription = "رجوع"
                                )
                            }
                            Text(
                                text = "الجذر: [ ${item.root} ]",
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
                                imageVector = Icons.Default.HelpOutline, // أو Icons.Outlined.Flag / Icons.Outlined.Feedback
                                contentDescription = "الإبلاغ عن معنى",
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
                                    // Measure child without restricting height to 0
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
                                        if (cleaned.length >= 16) cleaned.substring(
                                            0,
                                            16
                                        ) else cleaned
                                    } catch (_: Exception) {
                                        aiGeneratedAt ?: ""
                                    }
                                    val metaLine = buildString {
                                        if (!aiModel.isNullOrBlank()) append("🤖 $aiModel")
                                        if (!aiModel.isNullOrBlank() && formattedDate.isNotBlank()) append(
                                            "  •  "
                                        )
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
                            }
                        }
                    }

                    // Pinned TabRow
                    TabRow(
                        selectedTabIndex = pagerState.currentPage,
                        containerColor = MaterialTheme.colorScheme.surface,
                        divider = {}
                    ) {
                        Tab(
                            selected = pagerState.currentPage == 0,
                            onClick = {
                                scope.launch {
                                    pagerState.animateScrollToPage(
                                        0,
                                        animationSpec = tween(
                                            durationMillis = AppMotion.DurationMedium,
                                            easing = AppMotion.EasingStandard
                                        )
                                    )
                                }
                            },
                            text = {
                                Text(
                                    "المعاجم (${detail.meanings.size})",
                                    fontWeight = FontWeight.Bold
                                )
                            }
                        )
                        Tab(
                            selected = pagerState.currentPage == 1,
                            onClick = {
                                scope.launch {
                                    pagerState.animateScrollToPage(
                                        1,
                                        animationSpec = tween(
                                            durationMillis = AppMotion.DurationMedium,
                                            easing = AppMotion.EasingStandard
                                        )
                                    )
                                }
                            },
                            text = {
                                Text(
                                    "المصادر (${detail.masadir.size})",
                                    fontWeight = FontWeight.Bold
                                )
                            }
                        )
                        Tab(
                            selected = pagerState.currentPage == 2,
                            onClick = {
                                scope.launch {
                                    pagerState.animateScrollToPage(
                                        2,
                                        animationSpec = tween(
                                            durationMillis = AppMotion.DurationMedium,
                                            easing = AppMotion.EasingStandard
                                        )
                                    )
                                }
                            },
                            text = {
                                Text(
                                    "المشتقات (${detail.derivatives.size})",
                                    fontWeight = FontWeight.Bold
                                )
                            }
                        )
                        Tab(
                            selected = pagerState.currentPage == 3,
                            onClick = {
                                scope.launch {
                                    pagerState.animateScrollToPage(
                                        3,
                                        animationSpec = tween(
                                            durationMillis = AppMotion.DurationMedium,
                                            easing = AppMotion.EasingStandard
                                        )
                                    )
                                }
                            },
                            text = {
                                Text(
                                    "الآيات (${detail.item.occurrencesCount})",
                                    fontWeight = FontWeight.Bold
                                )
                            }
                        )
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
                                        EmptyTabNotice(text = "لا توجد معاني مدخلة لهذا الجذر حالياً")
                                    }
                                } else {
                                    items(detail.meanings, key = { it.id }) { meaning ->
                                        Box(
                                            modifier = Modifier
                                                .padding(horizontal = 16.dp, vertical = 5.dp)
                                                .animateItem()
                                        ) {
                                            MeaningCard(meaning)
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
                                        EmptyTabNotice(text = "لا توجد مصادر مسجلة لهذا الجذر")
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
                                        EmptyTabNotice(text = "لا توجد مشتقات مسجلة لهذا الجذر")
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
                                state = ayatState,
                                modifier = Modifier
                                    .fillMaxSize()
                                    .testTag("root_detail_screen_3"),
                                contentPadding = PaddingValues(top = 12.dp, bottom = 24.dp),
                                verticalArrangement = Arrangement.spacedBy(0.dp)
                            ) {
                                if (occurrences.isEmpty() && !occurrencesHasMore && !isOccurrencesLoadingMore) {
                                    item {
                                        EmptyTabNotice(text = "لا توجد مواضع مسجلة لهذا الجذر في هذه النسخة")
                                    }
                                } else {
                                    itemsIndexed(
                                        occurrences,
                                        key = { index, occ -> "${occ.surahId}-${occ.ayahNum}-${occ.matchedWordText}-${occ.textUthmani.hashCode()}-$index" }
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
                                                    text = "اسحب للأسفل لتحميل المزيد • ${occurrences.size} / ${detail.item.occurrencesCount}",
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
                                                    text = "تم عرض جميع الآيات • ${occurrences.size} موضع",
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
                val context = LocalContext.current
                AlertDialog(
                    onDismissRequest = { showReportDialog = false },
                    title = { Text("الإبلاغ عن معنى", fontWeight = FontWeight.Bold) },
                    text = {
                        Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                            Text(
                                text = "هل وجدت معنى غير صحيح أو ناقص للجذر [${item.root}]؟",
                                style = MaterialTheme.typography.bodyMedium,
                                color = MaterialTheme.colorScheme.onSurface
                            )
                            ReportIssueCard(rootText = item.root, rootId = item.id)
                        }
                    },
                    confirmButton = {
                        TextButton(
                            onClick = {
                                val url = buildGithubIssueUrl(
                                    BuildIssueUrlOptions(
                                        item.root,
                                        item.id,
                                        null
                                    )
                                )
                                context.startActivity(Intent(Intent.ACTION_VIEW, Uri.parse(url)))
                                showReportDialog = false
                            }
                        ) { Text("فتح GitHub") }
                    },
                    dismissButton = {
                        TextButton(onClick = { showReportDialog = false }) { Text("إلغاء") }
                    },
                    shape = ShapeLarge
                )
            }
        }
    }
}