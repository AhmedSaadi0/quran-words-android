package io.github.ahmedsaadi0.quranwords.ui.screens

import androidx.compose.animation.core.tween
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import io.github.ahmedsaadi0.quranwords.data.util.QuranMetaConstants
import io.github.ahmedsaadi0.quranwords.ui.components.RootItemCard
import io.github.ahmedsaadi0.quranwords.ui.home.components.BookmarksQuickCard
import io.github.ahmedsaadi0.quranwords.ui.home.components.DbSetupBanner
import io.github.ahmedsaadi0.quranwords.ui.home.components.HomeHeader
import io.github.ahmedsaadi0.quranwords.ui.home.components.LastReadCard
import io.github.ahmedsaadi0.quranwords.ui.home.components.QuickNavRow
import io.github.ahmedsaadi0.quranwords.ui.home.components.StatsGrid
import io.github.ahmedsaadi0.quranwords.ui.theme.AppMotion
import io.github.ahmedsaadi0.quranwords.ui.theme.ShapeSmall
import io.github.ahmedsaadi0.quranwords.ui.viewmodel.HomeViewModel
import io.github.ahmedsaadi0.quranwords.ui.viewmodel.MainViewModel

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
    homeViewModel: HomeViewModel = hiltViewModel()
) {
    val isDbReady by mainViewModel.isDbReady.collectAsState()
    val featuredRoots by homeViewModel.featuredRoots.collectAsState()
    val lastReadSurah by mainViewModel.lastReadSurah.collectAsState()
    val lastReadAyah by mainViewModel.lastReadAyah.collectAsState()
    val bookmarkedSurahs by mainViewModel.bookmarkedSurahs.collectAsState()
    val bookmarkedAyat by mainViewModel.bookmarkedAyat.collectAsState()
    val darkModeSetting by mainViewModel.darkModeSetting.collectAsState()
    val dynamicEnabled by mainViewModel.dynamicColorEnabled.collectAsState()
    val lastSurahMeta = QuranMetaConstants.SURAHS.firstOrNull { it.id == lastReadSurah } ?: QuranMetaConstants.SURAHS[0]
    var showThemeDialog by remember { mutableStateOf(false) }

    Scaffold(
        modifier = Modifier.fillMaxSize(),
        containerColor = MaterialTheme.colorScheme.background,
        contentWindowInsets = WindowInsets(0, 0, 0, 0)
    ) { innerPadding ->
        LazyColumn(
            modifier = Modifier.fillMaxSize().padding(innerPadding).testTag("home_screen"),
            contentPadding = PaddingValues(horizontal = 16.dp, vertical = 16.dp),
            verticalArrangement = Arrangement.spacedBy(18.dp)
        ) {
            item {
                HomeHeader(
                    dynamicEnabled = dynamicEnabled,
                    onOpenThemeDialog = { showThemeDialog = true },
                    onToggleDynamicColor = { mainViewModel.toggleDynamicColor() },
                    onToggleDarkMode = { mainViewModel.toggleDarkMode() },
                    onNavigateToSearch = onNavigateToSearch,
                    modifier = Modifier.animateItem(tween(AppMotion.DurationMedium, easing = AppMotion.EasingStandard))
                )
            }
            if (!isDbReady) {
                item {
                    DbSetupBanner(
                        onDownloadClick = onNavigateToSetup,
                        modifier = Modifier.animateItem(tween(AppMotion.DurationMedium, easing = AppMotion.EasingStandard))
                    )
                }
            }
            item {
                QuickNavRow(
                    onSurahIndex = onNavigateToSurahIndex,
                    onRoots = onNavigateToRoots,
                    onGuide = onNavigateToGuide,
                    modifier = Modifier.animateItem(tween(AppMotion.DurationMedium, easing = AppMotion.EasingStandard))
                )
            }
            item {
                LastReadCard(
                    surahNameAr = lastSurahMeta.nameAr,
                    ayahNum = lastReadAyah,
                    onClick = { onNavigateToSurahDetail(lastReadSurah, lastReadAyah) },
                    modifier = Modifier.animateItem(tween(AppMotion.DurationMedium, easing = AppMotion.EasingStandard))
                )
            }
            item {
                BookmarksQuickCard(
                    surahsCount = bookmarkedSurahs.size,
                    ayatCount = bookmarkedAyat.size,
                    onClick = onNavigateToBookmarks,
                    modifier = Modifier.animateItem(tween(AppMotion.DurationMedium, easing = AppMotion.EasingStandard))
                )
            }
            item {
                androidx.compose.foundation.layout.Column(
                    modifier = Modifier.animateItem(tween(AppMotion.DurationMedium, easing = AppMotion.EasingStandard)),
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
            item {
                StatsGrid(modifier = Modifier.animateItem(tween(AppMotion.DurationMedium, easing = AppMotion.EasingStandard)))
            }
            item {
                androidx.compose.foundation.layout.Row(
                    modifier = Modifier.fillMaxWidth().animateItem(tween(AppMotion.DurationMedium, easing = AppMotion.EasingStandard)),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = androidx.compose.ui.Alignment.CenterVertically
                ) {
                    Text(
                        text = "نماذج من الجذور القرآنية",
                        style = MaterialTheme.typography.titleLarge,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onBackground
                    )
                    OutlinedButton(onClick = onNavigateToRoots, shape = ShapeSmall) { Text("عرض الكل") }
                }
            }
            items(count = featuredRoots.size, key = { featuredRoots[it].id }) { index ->
                RootItemCard(
                    rootItem = featuredRoots[index],
                    onClick = { onNavigateToRootDetail(featuredRoots[index].id) },
                    modifier = Modifier.animateItem(
                        tween(AppMotion.DurationMedium, delayMillis = (index * AppMotion.StaggerDelayStep.toInt()).coerceAtMost(120), easing = AppMotion.EasingStandard)
                    )
                )
            }
            item { Spacer(modifier = Modifier.height(16.dp).animateItem(tween(AppMotion.DurationMedium, easing = AppMotion.EasingStandard))) }
        }
        if (showThemeDialog) {
            ThemeChooserDialog(
                darkModeSetting = darkModeSetting,
                dynamicEnabled = dynamicEnabled,
                onDarkModeChange = { mainViewModel.setDarkModeSetting(it) },
                onDynamicChange = { mainViewModel.setDynamicColorEnabled(it) },
                onDismiss = { showThemeDialog = false }
            )
        }
    }
}

@Composable
private fun ThemeChooserDialog(
    darkModeSetting: Int,
    dynamicEnabled: Boolean,
    onDarkModeChange: (Int) -> Unit,
    onDynamicChange: (Boolean) -> Unit,
    onDismiss: () -> Unit
) {
    androidx.compose.material3.AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("المظهر والألوان", fontWeight = FontWeight.Bold) },
        text = {
            androidx.compose.foundation.layout.Column(verticalArrangement = Arrangement.spacedBy(16.dp)) {
                androidx.compose.foundation.layout.Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                    Text("الثيم", style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.primary)
                    ThemeOptionRow(label = "تلقائي حسب النظام", selected = darkModeSetting == 0, onClick = { onDarkModeChange(0) })
                    ThemeOptionRow(label = "فاتح", selected = darkModeSetting == 1, onClick = { onDarkModeChange(1) })
                    ThemeOptionRow(label = "غامق", selected = darkModeSetting == 2, onClick = { onDarkModeChange(2) })
                }
                androidx.compose.foundation.layout.Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                    Text("الألوان", style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.primary)
                    ThemeOptionRow(label = "ألوان التطبيق الزيتوني", subLabel = "Natural Tones", selected = !dynamicEnabled, onClick = { onDynamicChange(false) })
                    ThemeOptionRow(
                        label = "ألوان النظام",
                        subLabel = if (android.os.Build.VERSION.SDK_INT >= 31) "Material You (Android 12+)" else "غير مدعوم على هذا الجهاز",
                        selected = dynamicEnabled,
                        enabled = android.os.Build.VERSION.SDK_INT >= 31,
                        onClick = { if (android.os.Build.VERSION.SDK_INT >= 31) onDynamicChange(true) }
                    )
                }
            }
        },
        confirmButton = {
            androidx.compose.material3.TextButton(onClick = onDismiss, modifier = Modifier.testTag("close_theme_dialog")) { Text("إغلاق") }
        },
        shape = io.github.ahmedsaadi0.quranwords.ui.theme.ShapeMedium
    )
}

@Composable
private fun ThemeOptionRow(label: String, selected: Boolean, onClick: () -> Unit, subLabel: String? = null, enabled: Boolean = true) {
    androidx.compose.foundation.layout.Row(
        modifier = Modifier.fillMaxWidth().clip(ShapeSmall).clickable(enabled = enabled, onClick = onClick).padding(horizontal = 8.dp, vertical = 6.dp),
        verticalAlignment = androidx.compose.ui.Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        androidx.compose.material3.RadioButton(selected = selected, onClick = onClick, enabled = enabled)
        androidx.compose.foundation.layout.Column(modifier = Modifier.weight(1f)) {
            Text(
                text = label,
                style = MaterialTheme.typography.bodyMedium,
                fontWeight = if (selected) FontWeight.Bold else FontWeight.Normal,
                color = if (enabled) MaterialTheme.colorScheme.onSurface else MaterialTheme.colorScheme.onSurface.copy(alpha = 0.5f)
            )
            subLabel?.let {
                Text(text = it, style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = if (enabled) 1f else 0.5f))
            }
        }
    }
}
