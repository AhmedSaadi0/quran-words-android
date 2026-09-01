package io.github.ahmedsaadi0.quranwords.ui.screens

import android.content.Intent
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.slideInVertically
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.border
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
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Bookmark
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.outlined.BookmarkBorder
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
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.material3.rememberModalBottomSheetState
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
import androidx.compose.ui.draw.clip
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
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Constraints
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import io.github.ahmedsaadi0.quranwords.ui.components.AyahItemCard
import io.github.ahmedsaadi0.quranwords.ui.components.FontSizeControls
import io.github.ahmedsaadi0.quranwords.ui.components.JuzHizbSeparator
import io.github.ahmedsaadi0.quranwords.ui.components.MorphologyBottomSheet
import io.github.ahmedsaadi0.quranwords.ui.components.PageSeparator
import io.github.ahmedsaadi0.quranwords.ui.components.SurahPagesHeader
import io.github.ahmedsaadi0.quranwords.ui.theme.AppMotion
import io.github.ahmedsaadi0.quranwords.ui.theme.ShapeMedium
import io.github.ahmedsaadi0.quranwords.ui.viewmodel.MainViewModel
import io.github.ahmedsaadi0.quranwords.ui.viewmodel.SurahDetailViewModel
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.launch
import kotlin.math.roundToInt

@OptIn(ExperimentalMaterial3Api::class, ExperimentalFoundationApi::class)
@Composable
fun SurahDetailScreen(
    surahId: Int,
    targetAyah: Int = 1,
    mainViewModel: MainViewModel,
    surahDetailViewModel: SurahDetailViewModel,
    onNavigateBack: () -> Unit,
    onNavigateToRootDetail: (Int) -> Unit
) {
    val surah by surahDetailViewModel.surah.collectAsState()
    val ayat by surahDetailViewModel.ayat.collectAsState()
    val isLoading by surahDetailViewModel.isLoading.collectAsState()
    val isLoadingMore by surahDetailViewModel.isLoadingMore.collectAsState()
    val selectedWord by surahDetailViewModel.selectedWord.collectAsState()
    val selectedWordAyah by surahDetailViewModel.selectedWordAyah.collectAsState()
    val aiSummary by surahDetailViewModel.aiSummary.collectAsState()
    val aiModel by surahDetailViewModel.aiModel.collectAsState()
    val aiGeneratedAt by surahDetailViewModel.aiGeneratedAt.collectAsState()
    val isAiLoading by surahDetailViewModel.isAiLoading.collectAsState()
    val fontSize by mainViewModel.fontSize.collectAsState()
    val bookmarkedSurahs by mainViewModel.bookmarkedSurahs.collectAsState()
    val bookmarkedAyat by mainViewModel.bookmarkedAyat.collectAsState()
    val isDbReady by mainViewModel.isDbReady.collectAsState()

    val isSurahBookmarked = bookmarkedSurahs.contains(surahId.toString())
    val listState = rememberSaveable(saver = LazyListState.Saver) { LazyListState() }
    val hasBasmalah = surahId != 9 && surahId != 1
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)

    // Saveable scroll handling flag — prevents resetting scroll on back-nav & rotation
    var hasHandledInitialScroll by rememberSaveable(surahId, targetAyah) { mutableStateOf(false) }

    // Selection mode
    val isSelectionMode by surahDetailViewModel.isSelectionMode.collectAsState()
    val selectedAyahs by surahDetailViewModel.selectedAyahs.collectAsState()
    val snackbarHostState = remember { SnackbarHostState() }
    val scope = rememberCoroutineScope()
    val context = LocalContext.current
    val clipboardManager = LocalClipboardManager.current

    // Page header state
    val surahPages by surahDetailViewModel.surahPages.collectAsState()

    // Shared Header Collapse State (Quick-Return / Enter Always)
    var collapsibleHeightPx by rememberSaveable { mutableIntStateOf(0) }
    var headerOffsetPx by rememberSaveable { mutableFloatStateOf(0f) }

    val nestedScrollConnection = remember {
        object : NestedScrollConnection {
            override fun onPreScroll(available: Offset, source: NestedScrollSource): Offset {
                if (isSelectionMode) return Offset.Zero
                val delta = available.y
                if (collapsibleHeightPx > 0) {
                    // Quick-Return (Enter Always):
                    // ينطوي فوراً عند السحب لأسفل (delta < 0)
                    // ويظهر فوراً وبسلاسة بمجرد السحب لأعلى (delta > 0) في أي مكان بالصفحة!
                    if ((delta < 0f && headerOffsetPx > -collapsibleHeightPx) ||
                        (delta > 0f && headerOffsetPx < 0f)
                    ) {
                        val prevOffset = headerOffsetPx
                        headerOffsetPx = (headerOffsetPx + delta).coerceIn(-collapsibleHeightPx.toFloat(), 0f)
                        val consumed = headerOffsetPx - prevOffset
                        return Offset(0f, consumed)
                    }
                }
                return Offset.Zero
            }
        }
    }

    // Load surah only if not already loaded for this surahId
    LaunchedEffect(surahId) {
        if (surah?.id != surahId || ayat.isEmpty()) {
            hasHandledInitialScroll = false
            headerOffsetPx = 0f
            surahDetailViewModel.loadSurah(surahId)
        }
    }

    // Perform initial scroll ONLY once when first opening the surah
    LaunchedEffect(ayat, surah, hasHandledInitialScroll) {
        if (hasHandledInitialScroll) return@LaunchedEffect
        val currentSurah = surah
        if (ayat.isEmpty() || currentSurah == null) return@LaunchedEffect
        val idx = ayat.indexOfFirst { it.ayah == targetAyah }
        if (idx == -1 && ayat.size < currentSurah.ayahCount) {
            surahDetailViewModel.ensureAyahLoaded(targetAyah)
            return@LaunchedEffect
        }
        if (idx != -1) {
            val scrollIndex = idx + if (hasBasmalah) 1 else 0
            if (kotlin.math.abs(listState.firstVisibleItemIndex - scrollIndex) > 20) {
                listState.scrollToItem(scrollIndex)
            } else {
                listState.animateScrollToItem(scrollIndex)
            }
            hasHandledInitialScroll = true
        } else {
            hasHandledInitialScroll = true
        }
        if (targetAyah in 1..currentSurah.ayahCount) {
            surahDetailViewModel.updateLastRead(surahId, targetAyah)
            mainViewModel.updateLastRead(surahId, targetAyah)
        }
    }

    LaunchedEffect(listState, ayat) {
        snapshotFlow { listState.firstVisibleItemIndex }
            .distinctUntilChanged()
            .collectLatest { firstIdx ->
                if (ayat.isNotEmpty()) {
                    val ayatIdx = firstIdx - if (hasBasmalah) 1 else 0
                    if (ayatIdx in ayat.indices) {
                        val visibleAyah = ayat[ayatIdx].ayah
                        surahDetailViewModel.updateLastRead(surahId, visibleAyah)
                        mainViewModel.updateLastRead(surahId, visibleAyah)
                    }
                }
            }
    }

    LaunchedEffect(listState) {
        snapshotFlow { listState.layoutInfo.visibleItemsInfo.lastOrNull()?.index ?: 0 }
            .distinctUntilChanged()
            .collectLatest { lastIdx ->
                val ayatLastIdx = lastIdx - if (hasBasmalah) 1 else 0
                surahDetailViewModel.loadMoreIfNeeded(ayatLastIdx)
            }
    }

    val collapseProgress = if (collapsibleHeightPx > 0) {
        (1f + (headerOffsetPx / collapsibleHeightPx.toFloat())).coerceIn(0f, 1f)
    } else {
        1f
    }

    Scaffold(
        contentWindowInsets = WindowInsets(0, 0, 0, 0),
        containerColor = MaterialTheme.colorScheme.background,
        snackbarHost = { SnackbarHost(snackbarHostState) }
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .nestedScroll(nestedScrollConnection)
        ) {
            // Selection contextual bar OR Custom Dynamic Header
            if (isSelectionMode) {
                TopAppBar(
                    title = {
                        Text(
                            text = "تم تحديد ${selectedAyahs.size} آيات",
                            fontWeight = FontWeight.Bold
                        )
                    },
                    navigationIcon = {
                        IconButton(
                            onClick = { surahDetailViewModel.clearSelection() },
                            modifier = Modifier.testTag("dismiss_selection")
                        ) {
                            Icon(
                                imageVector = Icons.Filled.Close,
                                contentDescription = "إلغاء"
                            )
                        }
                    },
                    actions = {
                        IconButton(
                            onClick = { surahDetailViewModel.selectAllAyahs() },
                            modifier = Modifier.testTag("select_all_btn")
                        ) {
                            Text("✓", fontSize = 18.sp, fontWeight = FontWeight.Bold)
                        }
                        IconButton(
                            onClick = {
                                val formatted = surahDetailViewModel.getFormattedSelection()
                                if (formatted.isNotBlank()) {
                                    clipboardManager.setText(AnnotatedString(formatted))
                                    scope.launch {
                                        snackbarHostState.showSnackbar("تم نسخ ${selectedAyahs.size} آيات")
                                        surahDetailViewModel.clearSelection()
                                    }
                                }
                            },
                            modifier = Modifier.testTag("copy_selected_btn")
                        ) {
                            Text("📋", fontSize = 18.sp)
                        }
                        IconButton(
                            onClick = {
                                val formatted = surahDetailViewModel.getFormattedSelection()
                                if (formatted.isNotBlank()) {
                                    val intent = Intent(Intent.ACTION_SEND).apply {
                                        type = "text/plain"
                                        putExtra(Intent.EXTRA_TEXT, formatted)
                                    }
                                    context.startActivity(Intent.createChooser(intent, "مشاركة الآيات"))
                                }
                            },
                            modifier = Modifier.testTag("share_selected_btn")
                        ) {
                            Text("↗", fontSize = 18.sp)
                        }
                    },
                    colors = TopAppBarDefaults.topAppBarColors(
                        containerColor = MaterialTheme.colorScheme.primaryContainer,
                        titleContentColor = MaterialTheme.colorScheme.onPrimaryContainer
                    ),
                    modifier = Modifier
                        .fillMaxWidth()
                        .windowInsetsPadding(WindowInsets.statusBars)
                )
            } else {
                // Fixed & Collapsing Top Header Section
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .background(MaterialTheme.colorScheme.surface)
                        .windowInsetsPadding(WindowInsets.statusBars)
                ) {
                    // Pinned Top Bar Row (Dedicated 100% to Title + Back + Bookmark)
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(64.dp)
                            .padding(horizontal = 8.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(4.dp),
                            modifier = Modifier.weight(1f)
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
                            Column(verticalArrangement = Arrangement.Center) {
                                Text(
                                    text = surah?.let { "سورة ${it.nameAr}" } ?: "سورة",
                                    style = MaterialTheme.typography.titleLarge,
                                    fontWeight = FontWeight.Bold,
                                    color = MaterialTheme.colorScheme.onSurface,
                                    maxLines = 1,
                                    overflow = TextOverflow.Ellipsis
                                )
                                surah?.let { s ->
                                    Text(
                                        text = "${s.revelationType} • ${s.ayahCount} آيات",
                                        style = MaterialTheme.typography.labelSmall,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                                        maxLines = 1
                                    )
                                }
                            }
                        }

                        IconButton(
                            onClick = { mainViewModel.toggleSurahBookmark(surahId) },
                            modifier = Modifier.testTag("bookmark_button")
                        ) {
                            Icon(
                                imageVector = if (isSurahBookmarked) Icons.Filled.Bookmark else Icons.Outlined.BookmarkBorder,
                                contentDescription = "حفظ السورة",
                                tint = if (isSurahBookmarked) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }

                    // Collapsible Area (Font Size Controls + Surah Pages Header)
                    val visibleIdxForHeader = (listState.firstVisibleItemIndex - if (hasBasmalah) 1 else 0)
                        .coerceIn(0, (ayat.size - 1).coerceAtLeast(0))
                    val currentPageForHeader = ayat.getOrNull(visibleIdxForHeader)?.pageNumber

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
                                if (collapsibleHeightPx != naturalHeight && naturalHeight > 0) {
                                    collapsibleHeightPx = naturalHeight
                                }
                                val currentHeight = (naturalHeight + headerOffsetPx.roundToInt())
                                    .coerceIn(0, naturalHeight)
                                layout(placeable.width, currentHeight) {
                                    placeable.placeRelative(0, headerOffsetPx.roundToInt())
                                }
                            }
                    ) {
                        Column(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(horizontal = 12.dp, vertical = 6.dp),
                            verticalArrangement = Arrangement.spacedBy(6.dp)
                        ) {
                            // Row 1 of collapsible: Font Size Controls
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.Center,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                FontSizeControls(
                                    fontSize = fontSize,
                                    onFontSizeChange = { mainViewModel.setFontSize(it) }
                                )
                            }

                            // Row 2 of collapsible: Surah Pages Header
                            if (surahPages.isNotEmpty()) {
                                SurahPagesHeader(
                                    pages = surahPages,
                                    currentPage = currentPageForHeader,
                                    onPageClick = { page ->
                                        scope.launch {
                                            surahDetailViewModel.ensurePageLoaded(page)
                                            delay(100)
                                            val freshAyat = surahDetailViewModel.ayat.value
                                            val idx = freshAyat.indexOfFirst { it.pageNumber == page }
                                            if (idx != -1) {
                                                val scrollIdx = idx + if (hasBasmalah) 1 else 0
                                                listState.animateScrollToItem(
                                                    scrollIdx,
                                                    scrollOffset = 0
                                                )
                                            }
                                        }
                                    },
                                    modifier = Modifier.fillMaxWidth()
                                )
                            }
                        }
                    }
                }
            }

            // Main Content
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .weight(1f)
            ) {
                when {
                    isLoading && ayat.isEmpty() -> {
                        Box(
                            modifier = Modifier.fillMaxSize(),
                            contentAlignment = Alignment.Center
                        ) {
                            CircularProgressIndicator(color = MaterialTheme.colorScheme.primary)
                        }
                    }
                    ayat.isEmpty() -> {
                        Column(
                            modifier = Modifier
                                .fillMaxSize()
                                .padding(24.dp),
                            verticalArrangement = Arrangement.Center,
                            horizontalAlignment = Alignment.CenterHorizontally
                        ) {
                            Text("📥", fontSize = 48.sp)
                            Spacer(modifier = Modifier.height(16.dp))
                            Text(
                                text = "قاعدة البيانات غير متوفرة",
                                style = MaterialTheme.typography.titleLarge,
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.onBackground,
                                textAlign = TextAlign.Center
                            )
                            Spacer(modifier = Modifier.height(8.dp))
                            Text(
                                text = "يجب تنزيل قاعدة البيانات الكاملة (118 ميجابايت) لعرض آيات هذه السورة مع التحليل الصرفي.",
                                style = MaterialTheme.typography.bodyMedium,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                                textAlign = TextAlign.Center
                            )
                        }
                    }
                    else -> {
                        LazyColumn(
                            state = listState,
                            modifier = Modifier.fillMaxSize(),
                            contentPadding = PaddingValues(
                                start = 16.dp,
                                end = 16.dp,
                                top = 12.dp,
                                bottom = 24.dp
                            ),
                            verticalArrangement = Arrangement.spacedBy(14.dp)
                        ) {
                            if (hasBasmalah) {
                                item {
                                    Box(
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .clip(ShapeMedium)
                                            .background(MaterialTheme.colorScheme.surface)
                                            .border(
                                                1.dp,
                                                MaterialTheme.colorScheme.outlineVariant,
                                                ShapeMedium
                                            )
                                            .padding(vertical = 16.dp),
                                        contentAlignment = Alignment.Center
                                    ) {
                                        Text(
                                            text = "بِسْمِ اللَّهِ الرَّحْمَٰنِ الرَّحِيمِ",
                                            fontSize = 24.sp,
                                            lineHeight = 36.sp,
                                            fontWeight = FontWeight.Bold,
                                            color = MaterialTheme.colorScheme.primary,
                                            textAlign = TextAlign.Center
                                        )
                                    }
                                }
                            }

                            itemsIndexed(ayat, key = { _, ayah -> ayah.ayah }) { index, ayah ->
                                val isAyahBookmarked = bookmarkedAyat.contains("$surahId:${ayah.ayah}")
                                val prevAyah = ayat.getOrNull(index - 1)
                                val isJuzStart = prevAyah?.juz != ayah.juz
                                val isHizbStart = prevAyah?.hizb != ayah.hizb
                                val isRubStart = prevAyah?.rubElHizb != ayah.rubElHizb
                                val isPageStart = prevAyah?.pageNumber != ayah.pageNumber
                                val showJuzHizb = index == 0 || isJuzStart || isHizbStart || isRubStart
                                val showPage = isPageStart && ayah.pageNumber != null

                                Column(
                                    modifier = Modifier.animateItem(
                                        placementSpec = tween(
                                            durationMillis = AppMotion.DurationMedium,
                                            easing = AppMotion.EasingStandard
                                        )
                                    ),
                                    verticalArrangement = Arrangement.spacedBy(8.dp)
                                ) {
                                    if (showPage) {
                                        PageSeparator(
                                            pageNumber = ayah.pageNumber!!,
                                            modifier = Modifier
                                                .fillMaxWidth()
                                                .padding(horizontal = 4.dp)
                                        )
                                    }
                                    if (showJuzHizb && ayah.juz != null) {
                                        JuzHizbSeparator(
                                            juz = ayah.juz,
                                            hizb = ayah.hizb,
                                            rubElHizb = ayah.rubElHizb,
                                            isJuzStart = isJuzStart,
                                            isHizbStart = isHizbStart,
                                            modifier = Modifier
                                                .fillMaxWidth()
                                                .padding(horizontal = 4.dp)
                                        )
                                    }

                                    AnimatedVisibility(
                                        visible = true,
                                        enter = fadeIn(
                                            animationSpec = tween(
                                                durationMillis = 280,
                                                delayMillis = (index % 20) * 18,
                                                easing = AppMotion.EasingStandard
                                            )
                                        ) + slideInVertically(
                                            initialOffsetY = { it / 5 },
                                            animationSpec = tween(
                                                durationMillis = 280,
                                                delayMillis = (index % 20) * 15,
                                                easing = AppMotion.EasingEmphasized
                                            )
                                        )
                                    ) {
                                        AyahItemCard(
                                            ayah = ayah,
                                            fontSize = fontSize,
                                            isBookmarked = isAyahBookmarked,
                                            onBookmarkClick = { mainViewModel.toggleAyahBookmark(surahId, ayah.ayah) },
                                            onWordClick = { word ->
                                                if (!isSelectionMode) surahDetailViewModel.selectWord(word, ayah)
                                            },
                                            surah = surah,
                                            isSelected = selectedAyahs.contains(ayah.ayah),
                                            isSelectionMode = isSelectionMode,
                                            onToggleSelection = { surahDetailViewModel.toggleAyahSelection(ayah.ayah) },
                                            onEnterSelectionMode = { surahDetailViewModel.enterSelectionMode(ayah.ayah) }
                                        )
                                    }
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
                            }

                            item {
                                Spacer(modifier = Modifier.height(32.dp))
                            }
                        }
                    }
                }
            }
        }

        // Word Morphology Bottom Sheet
        selectedWord?.let { word ->
            MorphologyBottomSheet(
                word = word,
                ayah = selectedWordAyah,
                sheetState = sheetState,
                onDismiss = { surahDetailViewModel.clearSelectedWord() },
                onNavigateToRoot = { rootId, _ ->
                    if (rootId > 0) {
                        onNavigateToRootDetail(rootId)
                    }
                },
                aiSummary = aiSummary,
                aiModel = aiModel,
                aiGeneratedAt = aiGeneratedAt,
                isAiLoading = isAiLoading
            )
        }
    }
}