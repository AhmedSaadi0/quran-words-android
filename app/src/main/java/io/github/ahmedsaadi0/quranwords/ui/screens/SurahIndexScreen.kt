package io.github.ahmedsaadi0.quranwords.ui.screens

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
import androidx.compose.material3.LargeTopAppBar
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Tab
import androidx.compose.material3.TabRow
import androidx.compose.material3.Text
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
import androidx.compose.ui.input.nestedscroll.nestedScroll
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.viewmodel.compose.viewModel
import io.github.ahmedsaadi0.quranwords.data.util.ArabicNormalizer
import io.github.ahmedsaadi0.quranwords.data.util.QuranMetaConstants
import io.github.ahmedsaadi0.quranwords.ui.components.SurahItemCard
import io.github.ahmedsaadi0.quranwords.ui.theme.AppMotion
import io.github.ahmedsaadi0.quranwords.ui.viewmodel.MainViewModel
import io.github.ahmedsaadi0.quranwords.ui.viewmodel.SurahViewModel

@OptIn(ExperimentalMaterial3Api::class, ExperimentalFoundationApi::class)
@Composable
fun SurahIndexScreen(
    onNavigateBack: () -> Unit,
    onNavigateToSurahDetail: (Int) -> Unit,
    mainViewModel: MainViewModel,
    surahViewModel: SurahViewModel
) {
    val surahs by surahViewModel.surahs.collectAsState()
    val filterType by surahViewModel.filterType.collectAsState()
    val searchQuery by surahViewModel.searchQuery.collectAsState()
    val bookmarkedSurahs by mainViewModel.bookmarkedSurahs.collectAsState()

    var selectedTabIndex by remember { mutableIntStateOf(0) }

    val scrollBehavior = TopAppBarDefaults.exitUntilCollapsedScrollBehavior()

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
        modifier = Modifier.nestedScroll(scrollBehavior.nestedScrollConnection),
        topBar = {
            Column(modifier = Modifier.background(MaterialTheme.colorScheme.surface)) {
                LargeTopAppBar(
                    title = {
                        Column(verticalArrangement = Arrangement.spacedBy(2.dp)) {
                            Text(
                                text = "فهرس القرآن الكريم",
                                fontWeight = FontWeight.Bold
                            )
                            // Collapsible detail — visible only when expanded (earns the collapse)
                            if (scrollBehavior.state.collapsedFraction < 0.5f) {
                                Text(
                                    text = "114 سور • 86 مكية • 28 مدنية • 30 جزء",
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
                    scrollBehavior = scrollBehavior,
                    colors = TopAppBarDefaults.largeTopAppBarColors(
                        containerColor = MaterialTheme.colorScheme.background,
                        scrolledContainerColor = MaterialTheme.colorScheme.surface,
                    )
                )

                // Pinned Tabs: remain visible when search collapses
                TabRow(
                    selectedTabIndex = selectedTabIndex,
                    containerColor = MaterialTheme.colorScheme.surface,
                    divider = {}
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
            }
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
            if (selectedTabIndex == 0) {
                // Search collapses on scroll — inside LazyColumn as first item
                LazyColumn(
                    modifier = Modifier.fillMaxSize(),
                    contentPadding = PaddingValues(horizontal = 16.dp, vertical = 12.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    // Collapsible Search + Filters — scrolls away
                    item(key = "surah_search_header") {
                        Column(
                            modifier = Modifier
                                .fillMaxWidth()
                                .animateItem(),
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
                    }

                    if (filteredSurahs.isEmpty()) {
                        item(key = "surah_empty") {
                            Box(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(vertical = 32.dp)
                                    .animateItem(),
                                contentAlignment = Alignment.Center
                            ) {
                                Text(
                                    text = if (searchQuery.isBlank()) "لا توجد سور مطابقة للفلتر"
                                    else "لا توجد نتائج لـ \"$searchQuery\"",
                                    style = MaterialTheme.typography.bodyMedium,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                        }
                    } else {
                        items(filteredSurahs, key = { it.id }) { surah ->
                            val isBookmarked = bookmarkedSurahs.contains(surah.id.toString())
                            SurahItemCard(
                                surah = surah,
                                onClick = { onNavigateToSurahDetail(surah.id) },
                                isBookmarked = isBookmarked,
                                onBookmarkClick = { mainViewModel.toggleSurahBookmark(surah.id) },
                                modifier = Modifier.animateItem()
                            )
                        }
                    }
                    item(key = "surah_bottom_spacer") {
                        Spacer(modifier = Modifier.height(16.dp))
                    }
                }
            } else {
                // Juz Tab — no search, direct list
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
