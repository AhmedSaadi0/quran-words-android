package com.example.ui.screens

import androidx.compose.animation.Crossfade
import androidx.compose.animation.core.tween
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
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
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.data.util.ArabicNormalizer
import com.example.data.util.QuranMetaConstants
import com.example.ui.components.SurahItemCard
import com.example.ui.theme.AppMotion
import com.example.ui.viewmodel.MainViewModel
import com.example.ui.viewmodel.SurahViewModel

@OptIn(ExperimentalMaterial3Api::class, ExperimentalFoundationApi::class)
@Composable
fun SurahIndexScreen(
    onNavigateBack: () -> Unit,
    onNavigateToSurahDetail: (Int) -> Unit,
    surahViewModel: SurahViewModel = viewModel(),
    mainViewModel: MainViewModel = viewModel()
) {
    val surahs by surahViewModel.surahs.collectAsState()
    val filterType by surahViewModel.filterType.collectAsState()
    val searchQuery by surahViewModel.searchQuery.collectAsState()
    val bookmarkedSurahs by mainViewModel.bookmarkedSurahs.collectAsState()

    var selectedTabIndex by remember { mutableIntStateOf(0) }

    val filteredSurahs = remember(surahs, filterType, searchQuery) {
        val queryNorm = ArabicNormalizer.normalizeAr(searchQuery)
        surahs.filter { surah ->
            val matchesFilter = when (filterType) {
                "meccan" -> surah.revelationType.contains("مكية")
                "medinan" -> surah.revelationType.contains("مدنية")
                else -> true
            }
            val matchesQuery = if (queryNorm.isBlank()) true else {
                ArabicNormalizer.normalizeAr(surah.nameAr).contains(queryNorm) ||
                surah.nameEn.contains(searchQuery, ignoreCase = true) ||
                surah.id.toString() == searchQuery.trim()
            }
            matchesFilter && matchesQuery
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Text(
                        text = "فهرس القرآن الكريم",
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
                            contentDescription = "رجوع"
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
                .testTag("surah_index_screen")
        ) {
            // Tabs: السور / الأجزاء
            TabRow(
                selectedTabIndex = selectedTabIndex,
                containerColor = MaterialTheme.colorScheme.surface
            ) {
                Tab(
                    selected = selectedTabIndex == 0,
                    onClick = { selectedTabIndex = 0 },
                    text = {
                        Text(
                            text = "السور (${QuranMetaConstants.STATS_SURAHS})",
                            fontWeight = FontWeight.Bold
                        )
                    },
                    modifier = Modifier.testTag("tab_surahs")
                )
                Tab(
                    selected = selectedTabIndex == 1,
                    onClick = { selectedTabIndex = 1 },
                    text = {
                        Text(
                            text = "الأجزاء (30)",
                            fontWeight = FontWeight.Bold
                        )
                    },
                    modifier = Modifier.testTag("tab_juz")
                )
            }

            if (selectedTabIndex == 0) {
                // Search and Filters
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp, vertical = 10.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    OutlinedTextField(
                        value = searchQuery,
                        onValueChange = { surahViewModel.setSearchQuery(it) },
                        placeholder = { Text("ابحث باسم السورة أو رقمها...") },
                        leadingIcon = {
                            Icon(imageVector = Icons.Default.Search, contentDescription = null)
                        },
                        trailingIcon = {
                            if (searchQuery.isNotBlank()) {
                                IconButton(onClick = { surahViewModel.setSearchQuery("") }) {
                                    Icon(imageVector = Icons.Default.Close, contentDescription = "مسح")
                                }
                            }
                        },
                        singleLine = true,
                        shape = RoundedCornerShape(12.dp),
                        modifier = Modifier
                            .fillMaxWidth()
                            .testTag("surah_search_input")
                    )

                    // Filter chips: الكل، مكية، مدنية - M3 small 12dp
                    Row(
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        FilterChip(
                            selected = filterType == "all",
                            onClick = { surahViewModel.setFilter("all") },
                            label = { Text("الكل (114)") },
                            shape = RoundedCornerShape(12.dp)
                        )
                        FilterChip(
                            selected = filterType == "meccan",
                            onClick = { surahViewModel.setFilter("meccan") },
                            label = { Text("مكية (86)") },
                            shape = RoundedCornerShape(12.dp)
                        )
                        FilterChip(
                            selected = filterType == "medinan",
                            onClick = { surahViewModel.setFilter("medinan") },
                            label = { Text("مدنية (28)") },
                            shape = RoundedCornerShape(12.dp)
                        )
                    }
                }

                // Surah list with filtering animation
                Crossfade(
                    targetState = filteredSurahs,
                    animationSpec = tween(durationMillis = AppMotion.DurationMedium),
                    label = "surahFilterCrossfade"
                ) { animatedList ->
                    LazyColumn(
                        modifier = Modifier.fillMaxSize(),
                        contentPadding = PaddingValues(horizontal = 16.dp, vertical = 8.dp),
                        verticalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        items(animatedList, key = { it.id }) { surah ->
                            val isBookmarked = bookmarkedSurahs.contains(surah.id.toString())
                            SurahItemCard(
                                surah = surah,
                                onClick = { onNavigateToSurahDetail(surah.id) },
                                isBookmarked = isBookmarked,
                                onBookmarkClick = { mainViewModel.toggleSurahBookmark(surah.id) },
                                modifier = Modifier.animateItem()
                            )
                        }
                        item {
                            Spacer(modifier = Modifier.height(16.dp))
                        }
                    }
                }
            } else {
                // Juz Tab with animateItem
                LazyColumn(
                    modifier = Modifier.fillMaxSize(),
                    contentPadding = PaddingValues(horizontal = 16.dp, vertical = 12.dp),
                    verticalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    items(QuranMetaConstants.JUZ_LIST, key = { it.id }) { juz ->
                        Card(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clip(RoundedCornerShape(16.dp))
                                .border(1.dp, MaterialTheme.colorScheme.outlineVariant, RoundedCornerShape(16.dp))
                                .clickable { onNavigateToSurahDetail(juz.startSurahId) }
                                .animateItem()
                                .testTag("juz_item_${juz.id}"),
                            colors = CardDefaults.cardColors(
                                containerColor = MaterialTheme.colorScheme.surface
                            ),
                            elevation = CardDefaults.cardElevation(defaultElevation = 0.5.dp)
                        ) {
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(16.dp),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Row(
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.spacedBy(14.dp)
                                ) {
                                    Box(
                                        modifier = Modifier
                                            .size(42.dp)
                                            .clip(CircleShape)
                                            .background(MaterialTheme.colorScheme.primaryContainer)
                                            .border(1.dp, MaterialTheme.colorScheme.outlineVariant, CircleShape),
                                        contentAlignment = Alignment.Center
                                    ) {
                                        Text(
                                            text = juz.id.toString(),
                                            style = MaterialTheme.typography.titleMedium,
                                            fontWeight = FontWeight.Bold,
                                            color = MaterialTheme.colorScheme.primary
                                        )
                                    }

                                    Column {
                                        Text(
                                            text = juz.nameAr,
                                            style = MaterialTheme.typography.titleMedium,
                                            fontWeight = FontWeight.Bold,
                                            color = MaterialTheme.colorScheme.onSurface
                                        )
                                        Text(
                                            text = juz.description,
                                            style = MaterialTheme.typography.bodySmall,
                                            color = MaterialTheme.colorScheme.onSurfaceVariant
                                        )
                                    }
                                }

                                Text("📖", fontSize = 20.sp)
                            }
                        }
                    }
                    item {
                        Spacer(modifier = Modifier.height(16.dp))
                    }
                }
            }
        }
    }
}
