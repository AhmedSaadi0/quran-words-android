package io.github.ahmedsaadi0.quranwords.ui.screens

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
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.RoundedCornerShape
import android.content.Intent
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Close
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LargeTopAppBar
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.runtime.snapshotFlow
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.input.nestedscroll.nestedScroll
import androidx.compose.ui.platform.LocalClipboardManager
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.viewmodel.compose.viewModel
import io.github.ahmedsaadi0.quranwords.ui.components.AyahItemCard
import io.github.ahmedsaadi0.quranwords.ui.components.FontSizeControls
import io.github.ahmedsaadi0.quranwords.ui.components.JuzHizbSeparator
import io.github.ahmedsaadi0.quranwords.ui.components.MorphologyBottomSheet
import io.github.ahmedsaadi0.quranwords.ui.theme.AppMotion
import io.github.ahmedsaadi0.quranwords.ui.theme.ShapeMedium
import io.github.ahmedsaadi0.quranwords.ui.viewmodel.MainViewModel
import io.github.ahmedsaadi0.quranwords.ui.viewmodel.SurahDetailViewModel
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.launch

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
    val listState = rememberLazyListState()
    val hasBasmalah = surahId != 9 && surahId != 1
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
    var hasHandledInitialScroll by remember(surahId, targetAyah) { mutableStateOf(false) }

    // Selection mode (Option A)
    val isSelectionMode by surahDetailViewModel.isSelectionMode.collectAsState()
    val selectedAyahs by surahDetailViewModel.selectedAyahs.collectAsState()
    val snackbarHostState = remember { SnackbarHostState() }
    val scope = rememberCoroutineScope()
    val context = LocalContext.current
    val clipboardManager = LocalClipboardManager.current

    // إضافة سلوك التمرير للهيدر (Collapsing Effect)
    val scrollBehavior = TopAppBarDefaults.exitUntilCollapsedScrollBehavior()

    LaunchedEffect(surahId) {
        hasHandledInitialScroll = false
        surahDetailViewModel.loadSurah(surahId)
    }

    // ... (نفس منطق التمرير التلقائي والـ Pagination بدون تغيير)
    LaunchedEffect(ayat, surah, hasHandledInitialScroll) {
        val currentSurah = surah
        if (hasHandledInitialScroll || ayat.isEmpty() || currentSurah == null) return@LaunchedEffect
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
            .collectLatest { lastIdx ->
                val ayatLastIdx = lastIdx - if (hasBasmalah) 1 else 0
                surahDetailViewModel.loadMoreIfNeeded(ayatLastIdx)
            }
    }

    Scaffold(
        modifier = Modifier.nestedScroll(scrollBehavior.nestedScrollConnection),
        contentWindowInsets = WindowInsets(0, 0, 0, 0),
        topBar = {
            if (isSelectionMode) {
                // Contextual Selection Bar (Option A)
                LargeTopAppBar(
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
                    colors = TopAppBarDefaults.largeTopAppBarColors(
                        containerColor = MaterialTheme.colorScheme.primaryContainer,
                        titleContentColor = MaterialTheme.colorScheme.onPrimaryContainer
                    )
                )
            } else {
                LargeTopAppBar(
                    title = {
                        Column {
                            Text(
                                text = surah?.let { "سُورَةُ ${it.nameAr}" } ?: "جاري التحميل...",
                                fontWeight = FontWeight.ExtraBold,
                            )
                            if (scrollBehavior.state.collapsedFraction < 0.5f) {
                                surah?.let {
                                    Text(
                                        text = "${it.revelationType} • ${it.ayahCount} آية",
                                        style = MaterialTheme.typography.labelSmall,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant
                                    )
                                }
                            }
                        }
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
                    },
                    actions = {
                        // Surah bookmark toggle
                        IconButton(
                            onClick = { mainViewModel.toggleSurahBookmark(surahId) },
                            modifier = Modifier.testTag("bookmark_surah_$surahId")
                        ) {
                            Text(
                                text = if (isSurahBookmarked) "🔖" else "☆",
                                fontSize = 20.sp,
                                color = if (isSurahBookmarked) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                        FontSizeControls(
                            fontSize = fontSize,
                            onFontSizeChange = { mainViewModel.setFontSize(it) },
                            modifier = Modifier
                                .padding(end = 8.dp)
                                .size(width = 180.dp, height = 38.dp)
                        )
                    },
                    scrollBehavior = scrollBehavior,
                    colors = TopAppBarDefaults.largeTopAppBarColors(
                        containerColor = MaterialTheme.colorScheme.background,
                        scrolledContainerColor = MaterialTheme.colorScheme.surface,
                        titleContentColor = MaterialTheme.colorScheme.onBackground,
                    )
                )
            }
        },
        containerColor = MaterialTheme.colorScheme.background,
        snackbarHost = { SnackbarHost(snackbarHostState) }
    ) { innerPadding ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
        ) {
            when {
                isLoading && ayat.isEmpty() -> {
                    Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                        CircularProgressIndicator(color = MaterialTheme.colorScheme.primary)
                    }
                }
                ayat.isEmpty() -> {
                    // No preview data - show download required (prevents stutter from seed->real switch)
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
                        Spacer(modifier = Modifier.height(20.dp))
                        androidx.compose.material3.Button(
                            onClick = { /* handled via navigation - user can go back and open setup */ },
                            enabled = false,
                            shape = ShapeMedium
                        ) {
                            Text("الرجوع وتنزيل قاعدة البيانات من الصفحة الرئيسية")
                        }
                        if (!isDbReady) {
                            Spacer(modifier = Modifier.height(12.dp))
                            Text(
                                text = "اذهب إلى الصفحة الرئيسية → تنزيل قاعدة البيانات",
                                style = MaterialTheme.typography.labelSmall,
                                color = MaterialTheme.colorScheme.primary,
                                textAlign = TextAlign.Center
                            )
                        }
                    }
                }
                else -> {
                    LazyColumn(
                        state = listState,
                        modifier = Modifier.fillMaxSize(),
                        contentPadding = PaddingValues(horizontal = 16.dp, vertical = 12.dp),
                        verticalArrangement = Arrangement.spacedBy(14.dp)
                    ) {
                        // Basmalah Banner (except Surah 9 and Surah 1) — M3 ShapeMedium 16dp
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

                        // Ayat List - paginated 20 per page, with inline Juz/Hizb/Rub separators (DB-backed, 28dp)
                        itemsIndexed(ayat, key = { _, ayah -> ayah.ayah }) { index, ayah ->
                            val isAyahBookmarked = bookmarkedAyat.contains("$surahId:${ayah.ayah}")

                            // Determine if this ayah starts a new Juz/Hizb/Rub boundary vs previous ayah
                            // First ayah always shows its position context; subsequent ayahs compare to prev
                            val prevAyah = ayat.getOrNull(index - 1)
                            val isJuzStart = prevAyah?.juz != ayah.juz
                            val isHizbStart = prevAyah?.hizb != ayah.hizb
                            val isRubStart = prevAyah?.rubElHizb != ayah.rubElHizb
                            val showSeparator = index == 0 || isJuzStart || isHizbStart || isRubStart

                            androidx.compose.foundation.layout.Column(
                                modifier = Modifier.animateItem(
                                    placementSpec = tween(
                                        durationMillis = AppMotion.DurationMedium,
                                        easing = AppMotion.EasingStandard
                                    )
                                ),
                                verticalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                                if (showSeparator && ayah.juz != null) {
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

                                // Staggered entrance: each ayah fades + slides up with small delay (index % 20 to keep per-page stagger)
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

        // Word Morphology Bottom Sheet - stateless, VM supplies AI data (UDF)
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
