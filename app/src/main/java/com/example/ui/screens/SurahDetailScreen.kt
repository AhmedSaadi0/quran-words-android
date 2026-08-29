package com.example.ui.screens

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
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.runtime.snapshotFlow
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.ui.components.AyahItemCard
import com.example.ui.components.MorphologyBottomSheet
import com.example.ui.theme.AppMotion
import com.example.ui.theme.ShapeMedium
import com.example.ui.viewmodel.MainViewModel
import com.example.ui.viewmodel.SurahDetailViewModel
import kotlinx.coroutines.flow.collectLatest

@OptIn(ExperimentalMaterial3Api::class, ExperimentalFoundationApi::class)
@Composable
fun SurahDetailScreen(
    surahId: Int,
    targetAyah: Int = 1,
    mainViewModel: MainViewModel,
    onNavigateBack: () -> Unit,
    onNavigateToRootDetail: (Int) -> Unit,
    viewModel: SurahDetailViewModel = viewModel()
) {
    val surah by viewModel.surah.collectAsState()
    val ayat by viewModel.ayat.collectAsState()
    val isLoading by viewModel.isLoading.collectAsState()
    val isLoadingMore by viewModel.isLoadingMore.collectAsState()
    val selectedWord by viewModel.selectedWord.collectAsState()
    val selectedWordAyah by viewModel.selectedWordAyah.collectAsState()
    val fontSize by mainViewModel.fontSize.collectAsState()
    val bookmarkedSurahs by mainViewModel.bookmarkedSurahs.collectAsState()
    val bookmarkedAyat by mainViewModel.bookmarkedAyat.collectAsState()
    val isDbReady by mainViewModel.isDbReady.collectAsState()

    val isSurahBookmarked = bookmarkedSurahs.contains(surahId.toString())
    val listState = rememberLazyListState()
    val hasBasmalah = surahId != 9 && surahId != 1
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
    var hasHandledInitialScroll by remember(surahId, targetAyah) { mutableStateOf(false) }

    LaunchedEffect(surahId) {
        hasHandledInitialScroll = false
        viewModel.loadSurah(surahId)
    }

    // Scroll to target ayah exactly once — pagination must NOT reset position
    LaunchedEffect(ayat, surah, hasHandledInitialScroll) {
        val currentSurah = surah
        if (hasHandledInitialScroll || ayat.isEmpty() || currentSurah == null) return@LaunchedEffect
        val idx = ayat.indexOfFirst { it.ayah == targetAyah }
        if (idx == -1 && ayat.size < currentSurah.ayahCount) {
            // Target not yet loaded — load pages sequentially then re-trigger
            viewModel.ensureAyahLoaded(targetAyah)
            return@LaunchedEffect
        }
        if (idx != -1) {
            val scrollIndex = idx + if (hasBasmalah) 1 else 0
            // Smooth if nearby, instant if far (avoids long animation for distant ayat)
            if (kotlin.math.abs(listState.firstVisibleItemIndex - scrollIndex) > 20) {
                listState.scrollToItem(scrollIndex)
            } else {
                listState.animateScrollToItem(scrollIndex)
            }
            hasHandledInitialScroll = true
        } else {
            // Target not found and no more pages
            hasHandledInitialScroll = true
        }
        if (targetAyah in 1..currentSurah.ayahCount) {
            viewModel.updateLastRead(surahId, targetAyah)
            mainViewModel.updateLastRead(surahId, targetAyah)
        }
    }

    // Track visible ayah for accurate continue-reading and trigger pagination
    LaunchedEffect(listState, ayat) {
        snapshotFlow { listState.firstVisibleItemIndex }
            .collectLatest { firstIdx ->
                if (ayat.isNotEmpty()) {
                    val ayatIdx = firstIdx - if (hasBasmalah) 1 else 0
                    if (ayatIdx in ayat.indices) {
                        val visibleAyah = ayat[ayatIdx].ayah
                        viewModel.updateLastRead(surahId, visibleAyah)
                        mainViewModel.updateLastRead(surahId, visibleAyah)
                    }
                }
            }
    }

    // Auto-pagination when near end (Telegram-like smooth loading)
    LaunchedEffect(listState) {
        snapshotFlow { listState.layoutInfo.visibleItemsInfo.lastOrNull()?.index ?: 0 }
            .collectLatest { lastIdx ->
                val ayatLastIdx = lastIdx - if (hasBasmalah) 1 else 0
                viewModel.loadMoreIfNeeded(ayatLastIdx)
            }
    }

    Scaffold(
        contentWindowInsets = WindowInsets(0, 0, 0, 0),
        topBar = {
            TopAppBar(
                title = {
                    Column {
                        Text(
                            text = surah?.let { "سُورَةُ ${it.nameAr}" } ?: "جاري التحميل...",
                            fontWeight = FontWeight.Bold,
                            style = MaterialTheme.typography.titleMedium
                        )
                        surah?.let {
                            Text(
                                text = "${it.revelationType} • ${it.ayahCount} آية",
                                style = MaterialTheme.typography.labelSmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
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
                    // Font Size Adjusters
                    Row(
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        IconButton(
                            onClick = {
                                if (fontSize > 18f) mainViewModel.setFontSize(fontSize - 2f)
                            },
                            modifier = Modifier.testTag("decrease_font_size")
                        ) {
                            Text("A-", fontWeight = FontWeight.Bold, fontSize = 14.sp)
                        }
                        IconButton(
                            onClick = {
                                if (fontSize < 36f) mainViewModel.setFontSize(fontSize + 2f)
                            },
                            modifier = Modifier.testTag("increase_font_size")
                        ) {
                            Text("A+", fontWeight = FontWeight.Bold, fontSize = 16.sp)
                        }
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.surface
                )
            )
        },
        containerColor = MaterialTheme.colorScheme.background
    ) { innerPadding ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .testTag("surah_detail_screen")
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

                        // Ayat List - paginated 20 per page, with Telegram-like staggered entrance (smooth, no stutter)
                        itemsIndexed(ayat, key = { _, ayah -> ayah.ayah }) { index, ayah ->
                            val isAyahBookmarked = bookmarkedAyat.contains("$surahId:${ayah.ayah}")
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
                                Box(
                                    modifier = Modifier.animateItem(
                                        placementSpec = tween(
                                            durationMillis = AppMotion.DurationMedium,
                                            easing = AppMotion.EasingStandard
                                        )
                                    )
                                ) {
                                    AyahItemCard(
                                        ayah = ayah,
                                        fontSize = fontSize,
                                        isBookmarked = isAyahBookmarked,
                                        onBookmarkClick = { mainViewModel.toggleAyahBookmark(surahId, ayah.ayah) },
                                        onWordClick = { word ->
                                            viewModel.selectWord(word, ayah)
                                        }
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

        // Word Morphology Bottom Sheet - outside Box to avoid recomposing LazyColumn on open (instant open)
        selectedWord?.let { word ->
            MorphologyBottomSheet(
                word = word,
                ayah = selectedWordAyah,
                sheetState = sheetState,
                onDismiss = { viewModel.clearSelectedWord() },
                onNavigateToRoot = { rootId, _ ->
                    if (rootId > 0) {
                        onNavigateToRootDetail(rootId)
                    }
                }
            )
        }
    }
}
