package com.example.ui.screens

import androidx.compose.animation.animateContentSize
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
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.data.util.QuranMetaConstants
import com.example.ui.components.RootItemCard
import com.example.ui.components.StatCard
import com.example.ui.theme.AppMotion
import com.example.ui.theme.Emerald800
import com.example.ui.theme.Emerald900
import com.example.ui.theme.QuranGold
import com.example.ui.theme.ShapeLarge
import com.example.ui.theme.ShapeMedium
import com.example.ui.theme.ShapeSmall
import com.example.ui.viewmodel.HomeViewModel
import com.example.ui.viewmodel.MainViewModel

@OptIn(ExperimentalFoundationApi::class)
@Composable
fun HomeScreen(
    mainViewModel: MainViewModel,
    onNavigateToSurahIndex: () -> Unit,
    onNavigateToSurahDetail: (Int, Int) -> Unit,
    onNavigateToRoots: () -> Unit,
    onNavigateToRootDetail: (Int) -> Unit,
    onNavigateToSearch: () -> Unit,
    onNavigateToGuide: () -> Unit,
    onNavigateToSetup: () -> Unit,
    onNavigateToBookmarks: () -> Unit = {},
    homeViewModel: HomeViewModel = viewModel()
) {
    val isDbReady by mainViewModel.isDbReady.collectAsState()
    val featuredRoots by homeViewModel.featuredRoots.collectAsState()
    val lastReadSurah by mainViewModel.lastReadSurah.collectAsState()
    val lastReadAyah by mainViewModel.lastReadAyah.collectAsState()
    val dynamicEnabled by mainViewModel.dynamicColorEnabled.collectAsState()
    val bookmarkedSurahs by mainViewModel.bookmarkedSurahs.collectAsState()
    val bookmarkedAyat by mainViewModel.bookmarkedAyat.collectAsState()

    val lastSurahMeta = QuranMetaConstants.SURAHS.firstOrNull { it.id == lastReadSurah } ?: QuranMetaConstants.SURAHS[0]

    Scaffold(
        modifier = Modifier.fillMaxSize(),
        containerColor = MaterialTheme.colorScheme.background,
        contentWindowInsets = WindowInsets(0, 0, 0, 0)
    ) { innerPadding ->
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .testTag("home_screen"),
            contentPadding = PaddingValues(horizontal = 16.dp, vertical = 16.dp),
            verticalArrangement = Arrangement.spacedBy(18.dp)
        ) {
            // Hero App Header - M3 Large 20dp + Telegram-like 250ms motion
            item {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .animateItem(
                            placementSpec = tween(
                                durationMillis = AppMotion.DurationMedium,
                                easing = AppMotion.EasingStandard
                            )
                        )
                        .clip(ShapeLarge)
                        .background(
                            Brush.verticalGradient(
                                colors = listOf(Emerald800, Emerald900)
                            )
                        )
                        .padding(20.dp)
                ) {
                    Column(
                        verticalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Column {
                                Text(
                                    text = "كلمات القرآن",
                                    style = MaterialTheme.typography.displayMedium,
                                    color = Color.White,
                                    fontWeight = FontWeight.Bold
                                )
                                Text(
                                    text = "المعجم والتحليل الصرفي الشامل لألفاظ التنزيل",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.onPrimaryContainer
                                )
                            }
                            Row(horizontalArrangement = Arrangement.spacedBy(8.dp), verticalAlignment = Alignment.CenterVertically) {
                                Box(
                                    modifier = Modifier
                                        .size(40.dp)
                                        .clip(CircleShape)
                                        .background(
                                            if (dynamicEnabled) MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.9f)
                                            else QuranGold.copy(alpha = 0.25f)
                                        )
                                        .border(
                                            1.dp,
                                            if (dynamicEnabled) MaterialTheme.colorScheme.primary else Color.White.copy(alpha = 0.25f),
                                            CircleShape
                                        )
                                        .clickable { mainViewModel.toggleDynamicColor() }
                                        .testTag("toggle_dynamic_color"),
                                    contentAlignment = Alignment.Center
                                ) {
                                    Text("🎨", fontSize = 16.sp)
                                }
                                Box(
                                    modifier = Modifier
                                        .size(44.dp)
                                        .clip(CircleShape)
                                        .background(QuranGold.copy(alpha = 0.25f))
                                        .clickable { mainViewModel.toggleDarkMode() }
                                        .testTag("toggle_dark_mode"),
                                    contentAlignment = Alignment.Center
                                ) {
                                    Text("🌓", fontSize = 20.sp)
                                }
                            }
                        }

                        // Search Trigger Bar - M3 Medium 16dp
                        Card(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(top = 8.dp)
                                .clip(ShapeMedium)
                                .border(1.dp, Color.White.copy(alpha = 0.25f), ShapeMedium)
                                .clickable { onNavigateToSearch() }
                                .animateContentSize(
                                    animationSpec = tween(
                                        durationMillis = AppMotion.DurationMedium,
                                        easing = AppMotion.EasingStandard
                                    )
                                )
                                .testTag("search_trigger_bar"),
                            colors = CardDefaults.cardColors(
                                containerColor = MaterialTheme.colorScheme.surface
                            )
                        ) {
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(horizontal = 14.dp, vertical = 12.dp),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(10.dp)
                            ) {
                                Text("🔍", fontSize = 18.sp)
                                Text(
                                    text = "ابحث عن جذر، كلمة، مصدر، أو نص آية...",
                                    style = MaterialTheme.typography.bodyMedium,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                        }
                    }
                }
            }

            // Database status banner (if not downloaded) - M3 Medium 16dp + 250ms animateItem
            if (!isDbReady) {
                item {
                    Card(
                        modifier = Modifier
                            .fillMaxWidth()
                            .animateItem(
                                placementSpec = tween(
                                    durationMillis = AppMotion.DurationMedium,
                                    easing = AppMotion.EasingStandard
                                )
                            )
                            .clip(ShapeMedium)
                            .border(1.dp, MaterialTheme.colorScheme.outlineVariant, ShapeMedium)
                            .animateContentSize(
                                animationSpec = tween(
                                    durationMillis = AppMotion.DurationMedium,
                                    easing = AppMotion.EasingStandard
                                )
                            )
                            .testTag("db_setup_banner"),
                        colors = CardDefaults.cardColors(
                            containerColor = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.45f)
                        )
                    ) {
                        Column(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(16.dp),
                            verticalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                                Text("💾", fontSize = 20.sp)
                                Text(
                                    text = "قاعدة البيانات الكاملة (118 ميجابايت)",
                                    style = MaterialTheme.typography.titleMedium,
                                    fontWeight = FontWeight.Bold,
                                    color = MaterialTheme.colorScheme.onSurface
                                )
                            }
                            Text(
                                text = "يمكنك تنزيل المعجم الكامل وقاعدة الصرف للعمل محلياً دون اتصال بالإنترنت، أو الاستمرار ببيانات المعاينة السريعة.",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                            Row(
                                horizontalArrangement = Arrangement.spacedBy(10.dp)
                            ) {
                                Button(
                                    onClick = onNavigateToSetup,
                                    colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.primary),
                                    shape = ShapeSmall,
                                    modifier = Modifier.testTag("download_db_btn")
                                ) {
                                    Text("تنزيل قاعدة البيانات")
                                }
                            }
                        }
                    }
                }
            }

            // Quick Access Nav Chips - M3 Medium + Telegram 250ms stagger
            item {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .animateItem(
                            placementSpec = tween(
                                durationMillis = AppMotion.DurationMedium,
                                easing = AppMotion.EasingStandard
                            )
                        )
                        .animateContentSize(
                            animationSpec = tween(
                                durationMillis = AppMotion.DurationMedium,
                                easing = AppMotion.EasingStandard
                            )
                        ),
                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    QuickNavCard(
                        title = "فهرس السور",
                        subtitle = "114 سورة",
                        icon = "📖",
                        onClick = onNavigateToSurahIndex,
                        modifier = Modifier.weight(1f)
                    )
                    QuickNavCard(
                        title = "معجم الجذور",
                        subtitle = "1642 جذر",
                        icon = "🌿",
                        onClick = onNavigateToRoots,
                        modifier = Modifier.weight(1f)
                    )
                    QuickNavCard(
                        title = "دليل الصرف",
                        subtitle = "أوزان وأبواب",
                        icon = "📐",
                        onClick = onNavigateToGuide,
                        modifier = Modifier.weight(1f)
                    )
                }
            }

            // Last read card - M3 Medium 16dp + 250ms motion
            item {
                Card(
                    modifier = Modifier
                        .fillMaxWidth()
                        .animateItem(
                            placementSpec = tween(
                                durationMillis = AppMotion.DurationMedium,
                                easing = AppMotion.EasingStandard
                            )
                        )
                        .clip(ShapeMedium)
                        .border(1.dp, MaterialTheme.colorScheme.outlineVariant, ShapeMedium)
                        .clickable { onNavigateToSurahDetail(lastReadSurah, lastReadAyah) }
                        .animateContentSize(
                            animationSpec = tween(
                                durationMillis = AppMotion.DurationMedium,
                                easing = AppMotion.EasingStandard
                            )
                        )
                        .testTag("continue_reading_card"),
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
                            horizontalArrangement = Arrangement.spacedBy(12.dp)
                        ) {
                            Box(
                                modifier = Modifier
                                    .size(46.dp)
                                    .clip(CircleShape)
                                    .background(MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.5f)),
                                contentAlignment = Alignment.Center
                            ) {
                                Text("🔖", fontSize = 22.sp)
                            }
                            Column {
                                Text(
                                    text = "متابعة التلاوة والتحليل",
                                    style = MaterialTheme.typography.titleMedium,
                                    fontWeight = FontWeight.Bold,
                                    color = MaterialTheme.colorScheme.onSurface
                                )
                                Text(
                                    text = "سورة ${lastSurahMeta.nameAr} • الآية $lastReadAyah",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                        }

                        Button(
                            onClick = { onNavigateToSurahDetail(lastReadSurah, lastReadAyah) },
                            colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.primary),
                            shape = ShapeSmall
                        ) {
                            Text("فتح")
                        }
                    }
                }
            }

            // Bookmarks quick card - always visible for easy access
            item {
                val hasBookmarks = bookmarkedSurahs.isNotEmpty() || bookmarkedAyat.isNotEmpty()
                Card(
                    modifier = Modifier
                        .fillMaxWidth()
                        .animateItem(
                            placementSpec = tween(
                                durationMillis = AppMotion.DurationMedium,
                                easing = AppMotion.EasingStandard
                            )
                        )
                        .clip(ShapeMedium)
                        .border(1.dp, MaterialTheme.colorScheme.outlineVariant, ShapeMedium)
                        .clickable { onNavigateToBookmarks() }
                        .testTag("bookmarks_quick_card"),
                    colors = CardDefaults.cardColors(
                        containerColor = if (hasBookmarks) MaterialTheme.colorScheme.tertiaryContainer.copy(alpha = 0.35f)
                        else MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f)
                    )
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
                            horizontalArrangement = Arrangement.spacedBy(12.dp)
                        ) {
                            Box(
                                modifier = Modifier
                                    .size(46.dp)
                                    .clip(CircleShape)
                                    .background(
                                        if (hasBookmarks) MaterialTheme.colorScheme.tertiaryContainer
                                        else MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.5f)
                                    ),
                                contentAlignment = Alignment.Center
                            ) {
                                Text(if (hasBookmarks) "⭐" else "🔖", fontSize = 22.sp)
                            }
                            Column {
                                Text(
                                    text = if (hasBookmarks) "إشاراتي المرجعية" else "إشاراتي المرجعية",
                                    style = MaterialTheme.typography.titleMedium,
                                    fontWeight = FontWeight.Bold,
                                    color = MaterialTheme.colorScheme.onSurface
                                )
                                Text(
                                    text = if (hasBookmarks) "${bookmarkedSurahs.size} سور • ${bookmarkedAyat.size} آيات محفوظة"
                                    else "احفظ السور والآيات للرجوع السريع",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                        }
                        Button(
                            onClick = onNavigateToBookmarks,
                            colors = ButtonDefaults.buttonColors(
                                containerColor = if (hasBookmarks) MaterialTheme.colorScheme.tertiary else MaterialTheme.colorScheme.primary
                            ),
                            shape = ShapeSmall,
                            modifier = Modifier.testTag("open_bookmarks_btn")
                        ) {
                            Text(if (hasBookmarks) "عرض" else "فتح")
                        }
                    }
                }
            }

            // Corpus Statistics Section Header - animated 250ms
            item {
                Column(
                    modifier = Modifier.animateItem(
                        placementSpec = tween(
                            durationMillis = AppMotion.DurationMedium,
                            easing = AppMotion.EasingStandard
                        )
                    ),
                    verticalArrangement = Arrangement.spacedBy(4.dp)
                ) {
                    Text(
                        text = "إحصاءات ومعطيات المدونة القرآنية",
                        style = MaterialTheme.typography.titleLarge,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onBackground
                    )
                    Text(
                        text = "مستخرجة من قاعدة بيانات مشروع كلمات القرآن المحققة",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }

            // 6 Stats Cards (2 per row) - M3 + 250ms animateItem
            item {
                Column(
                    modifier = Modifier
                        .animateItem(
                            placementSpec = tween(
                                durationMillis = AppMotion.DurationMedium,
                                easing = AppMotion.EasingStandard
                            )
                        )
                        .animateContentSize(
                            animationSpec = tween(
                                durationMillis = AppMotion.DurationMedium,
                                easing = AppMotion.EasingStandard
                            )
                        ),
                    verticalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        StatCard(
                            title = "كلمات فريدة",
                            value = QuranMetaConstants.STATS_UNIQUE_WORDS,
                            icon = "📝",
                            modifier = Modifier.weight(1f)
                        )
                        StatCard(
                            title = "جذور محققة",
                            value = QuranMetaConstants.STATS_VERIFIED_ROOTS,
                            icon = "🌿",
                            modifier = Modifier.weight(1f)
                        )
                    }
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        StatCard(
                            title = "مصادر لغوية",
                            value = QuranMetaConstants.STATS_MASADIR,
                            icon = "📚",
                            modifier = Modifier.weight(1f)
                        )
                        StatCard(
                            title = "مشتقات وأوزان",
                            value = QuranMetaConstants.STATS_DERIVATIVES,
                            icon = "✨",
                            modifier = Modifier.weight(1f)
                        )
                    }
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        StatCard(
                            title = "مواضع الكلمات",
                            value = QuranMetaConstants.STATS_WORD_POSITIONS,
                            icon = "📍",
                            modifier = Modifier.weight(1f)
                        )
                        StatCard(
                            title = "الآيات الكريمة",
                            value = QuranMetaConstants.STATS_AYAT,
                            icon = "۝",
                            modifier = Modifier.weight(1f)
                        )
                    }
                }
            }

            // Featured Roots Header - animated 250ms
            item {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .animateItem(
                            placementSpec = tween(
                                durationMillis = AppMotion.DurationMedium,
                                easing = AppMotion.EasingStandard
                            )
                        ),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "نماذج من الجذور القرآنية",
                        style = MaterialTheme.typography.titleLarge,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onBackground
                    )
                    OutlinedButton(
                        onClick = onNavigateToRoots,
                        shape = ShapeSmall
                    ) {
                        Text("عرض الكل")
                    }
                }
            }

            // Featured Roots List - Telegram-like staggered 250ms animateItem
            items(
                count = featuredRoots.size,
                key = { index -> featuredRoots[index].id }
            ) { index ->
                val rootItem = featuredRoots[index]
                RootItemCard(
                    rootItem = rootItem,
                    onClick = { onNavigateToRootDetail(rootItem.id) },
                    modifier = Modifier.animateItem(
                        placementSpec = tween(
                            durationMillis = AppMotion.DurationMedium,
                            delayMillis = (index * AppMotion.StaggerDelayStep.toInt()).coerceAtMost(120),
                            easing = AppMotion.EasingStandard
                        )
                    )
                )
            }

            item {
                Spacer(
                    modifier = Modifier
                        .height(16.dp)
                        .animateItem(
                            placementSpec = tween(
                                durationMillis = AppMotion.DurationMedium,
                                easing = AppMotion.EasingStandard
                            )
                        )
                )
            }
        }
    }
}

@OptIn(ExperimentalFoundationApi::class)
@Composable
fun QuickNavCard(
    title: String,
    subtitle: String,
    icon: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    Card(
        modifier = modifier
            .clip(ShapeMedium)
            .border(1.dp, MaterialTheme.colorScheme.outlineVariant, ShapeMedium)
            .clickable(onClick = onClick)
            .animateContentSize(
                animationSpec = tween(
                    durationMillis = AppMotion.DurationMedium,
                    easing = AppMotion.EasingStandard
                )
            ),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surface
        ),
        elevation = CardDefaults.cardElevation(defaultElevation = 0.5.dp)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(12.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(4.dp)
        ) {
            Text(icon, fontSize = 24.sp)
            Text(
                text = title,
                style = MaterialTheme.typography.titleSmall,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.onSurface
            )
            Text(
                text = subtitle,
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }
}
