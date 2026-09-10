package io.github.ahmedsaadi0.quranwords.ui.screens

import androidx.compose.animation.animateContentSize
import androidx.compose.animation.core.tween
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.IntrinsicSize
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBars
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import android.os.Build
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.RadioButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.pluralStringResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import io.github.ahmedsaadi0.quranwords.R
import io.github.ahmedsaadi0.quranwords.core.util.AppLanguage
import io.github.ahmedsaadi0.quranwords.data.util.QuranMetaConstants
import io.github.ahmedsaadi0.quranwords.domain.model.DbUpdateState
import io.github.ahmedsaadi0.quranwords.ui.components.DbUpdateBanner
import io.github.ahmedsaadi0.quranwords.ui.components.RootItemCard
import io.github.ahmedsaadi0.quranwords.ui.components.StatCard
import io.github.ahmedsaadi0.quranwords.ui.theme.AppMotion
import io.github.ahmedsaadi0.quranwords.ui.theme.ShapeLarge
import io.github.ahmedsaadi0.quranwords.ui.theme.ShapeMedium
import io.github.ahmedsaadi0.quranwords.ui.theme.ShapeSmall
import io.github.ahmedsaadi0.quranwords.ui.viewmodel.DbUpdateViewModel
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
    homeViewModel: HomeViewModel,
    dbUpdateViewModel: DbUpdateViewModel = hiltViewModel()
) {
    val isDbReady by mainViewModel.isDbReady.collectAsStateWithLifecycle()
    val featuredRoots by homeViewModel.featuredRoots.collectAsStateWithLifecycle()
    val lastReadSurah by mainViewModel.lastReadSurah.collectAsStateWithLifecycle()
    val lastReadAyah by mainViewModel.lastReadAyah.collectAsStateWithLifecycle()
    val dynamicEnabled by mainViewModel.dynamicColorEnabled.collectAsStateWithLifecycle()
    val bookmarkedSurahs by mainViewModel.bookmarkedSurahs.collectAsStateWithLifecycle()
    val bookmarkedAyat by mainViewModel.bookmarkedAyat.collectAsStateWithLifecycle()
    val dbUpdateState by dbUpdateViewModel.state.collectAsStateWithLifecycle()

    LaunchedEffect(isDbReady) {
        if (isDbReady) dbUpdateViewModel.checkOnce()
    }

    val lastSurahMeta = QuranMetaConstants.SURAHS.firstOrNull { it.id == lastReadSurah } ?: QuranMetaConstants.SURAHS[0]
    val darkModeSetting by mainViewModel.darkModeSetting.collectAsStateWithLifecycle()
    val language by mainViewModel.language.collectAsStateWithLifecycle()
    var showThemeDialog by remember { mutableStateOf(false) }

    Scaffold(
        modifier = Modifier.fillMaxSize(),
        containerColor = MaterialTheme.colorScheme.background,
        contentWindowInsets = WindowInsets.statusBars
    ) { innerPadding ->
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .testTag("home_screen"),
            contentPadding = PaddingValues(horizontal = 16.dp, vertical = 16.dp),
            verticalArrangement = Arrangement.spacedBy(18.dp)
        ) {
            // Hero App Header - الآن يتبع الثيم (فاتح/غامق/نظام) وألوان النظام/الزيتوني - M3 Large 20dp
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
                        .background(MaterialTheme.colorScheme.primaryContainer)
                        .border(1.dp, MaterialTheme.colorScheme.outlineVariant, ShapeLarge)
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
                            Column(modifier = Modifier.weight(1f)) {
                                Text(
                                    text = stringResource(R.string.home_title),
                                    style = MaterialTheme.typography.displayMedium,
                                    color = MaterialTheme.colorScheme.onPrimaryContainer,
                                    fontWeight = FontWeight.Bold
                                )
                                Text(
                                    text = stringResource(R.string.home_subtitle),
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.onPrimaryContainer.copy(alpha = 0.8f)
                                )
                            }
                            Row(horizontalArrangement = Arrangement.spacedBy(8.dp), verticalAlignment = Alignment.CenterVertically) {
                                // زر الإعدادات الجديد - يفتح اختيار الثيم والألوان
                                Box(
                                    modifier = Modifier
                                        .size(44.dp)
                                        .clip(CircleShape)
                                        .background(MaterialTheme.colorScheme.surface.copy(alpha = 0.9f))
                                        .border(1.dp, MaterialTheme.colorScheme.outlineVariant, CircleShape)
                                        .clickable { showThemeDialog = true }
                                        .testTag("open_theme_dialog"),
                                    contentAlignment = Alignment.Center
                                ) {
                                    Text("⚙️", fontSize = 20.sp)
                                }
                                Box(
                                    modifier = Modifier
                                        .size(40.dp)
                                        .clip(CircleShape)
                                        .background(MaterialTheme.colorScheme.surface.copy(alpha = 0.9f))
                                        .border(1.dp, MaterialTheme.colorScheme.outlineVariant, CircleShape)
                                        .clickable { mainViewModel.toggleDynamicColor() }
                                        .testTag("toggle_dynamic_color"),
                                    contentAlignment = Alignment.Center
                                ) {
                                    Text("🎨", fontSize = 16.sp)
                                }
                                Box(
                                    modifier = Modifier
                                        .size(40.dp)
                                        .clip(CircleShape)
                                        .background(MaterialTheme.colorScheme.surface.copy(alpha = 0.9f))
                                        .border(1.dp, MaterialTheme.colorScheme.outlineVariant, CircleShape)
                                        .clickable { mainViewModel.toggleDarkMode() }
                                        .testTag("toggle_dark_mode"),
                                    contentAlignment = Alignment.Center
                                ) {
                                    Text("🌓", fontSize = 16.sp)
                                }
                            }
                        }

                        // Search Trigger Bar - M3 Medium 16dp - الآن يتبع الثيم
                        Card(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(top = 8.dp)
                                .clip(ShapeMedium)
                                .border(1.dp, MaterialTheme.colorScheme.outlineVariant, ShapeMedium)
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
                                    text = stringResource(R.string.home_search_hint),
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
                                    text = stringResource(R.string.home_db_title),
                                    style = MaterialTheme.typography.titleMedium,
                                    fontWeight = FontWeight.Bold,
                                    color = MaterialTheme.colorScheme.onSurface
                                )
                            }
                            Text(
                                text = stringResource(R.string.home_db_body),
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
                                    Text(stringResource(R.string.home_db_action))
                                }
                            }
                        }
                    }
                }
            }

            if (isDbReady && dbUpdateState is DbUpdateState.UpdateAvailable) {
                val info = (dbUpdateState as DbUpdateState.UpdateAvailable).info
                item {
                    DbUpdateBanner(
                        info = info,
                        installedName = "",
                        onUpdateClick = onNavigateToSetup,
                        onDismissClick = { dbUpdateViewModel.dismiss(info) },
                        modifier = Modifier.animateItem(
                            placementSpec = tween(
                                durationMillis = AppMotion.DurationMedium,
                                easing = AppMotion.EasingStandard
                            )
                        )
                    )
                }
            }

            // Quick Access Nav Chips - equal-size cards: the Row takes the tallest
            // child's height and every card stretches to it, so longer English
            // labels cannot break the symmetry. M3 Medium + Telegram 250ms stagger.
            item {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(IntrinsicSize.Max)
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
                        title = stringResource(R.string.home_nav_surahs_title),
                        subtitle = stringResource(R.string.home_nav_surahs_subtitle, 114),
                        icon = "📖",
                        onClick = onNavigateToSurahIndex,
                        modifier = Modifier
                            .weight(1f)
                            .fillMaxHeight()
                    )
                    QuickNavCard(
                        title = stringResource(R.string.home_nav_roots_title),
                        subtitle = stringResource(R.string.home_nav_roots_subtitle, 1642),
                        icon = "🌿",
                        onClick = onNavigateToRoots,
                        modifier = Modifier
                            .weight(1f)
                            .fillMaxHeight()
                    )
                    QuickNavCard(
                        title = stringResource(R.string.home_nav_guide_title),
                        subtitle = stringResource(R.string.home_nav_guide_subtitle),
                        icon = "📐",
                        onClick = onNavigateToGuide,
                        modifier = Modifier
                            .weight(1f)
                            .fillMaxHeight()
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
                    // Twin layout with the bookmarks card: icon + weighted text column,
                    // no trailing button (the whole card is clickable). Long labels
                    // ellipsize instead of squeezing the layout.
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(16.dp),
                        horizontalArrangement = Arrangement.spacedBy(12.dp),
                        verticalAlignment = Alignment.CenterVertically
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
                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                text = stringResource(R.string.home_continue_title),
                                style = MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.onSurface,
                                maxLines = 2,
                                overflow = TextOverflow.Ellipsis
                            )
                            // Surah name is Arabic reference data (never translated);
                            // only the surrounding chrome template is localized.
                            Text(
                                text = stringResource(
                                    R.string.home_continue_template,
                                    lastSurahMeta.nameAr,
                                    lastReadAyah
                                ),
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis
                            )
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
                    // Twin of the continue-reading card above; tag retained on the
                    // content row so the navigation entry point stays testable.
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(16.dp)
                            .testTag("open_bookmarks_btn"),
                        horizontalArrangement = Arrangement.spacedBy(12.dp),
                        verticalAlignment = Alignment.CenterVertically
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
                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                text = stringResource(R.string.home_bookmarks_title),
                                style = MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.onSurface,
                                maxLines = 2,
                                overflow = TextOverflow.Ellipsis
                            )
                            Text(
                                text = if (hasBookmarks) {
                                    val surahsCount = bookmarkedSurahs.size
                                    val ayatCount = bookmarkedAyat.size
                                    val surahsText = pluralStringResource(
                                        R.plurals.home_bookmarks_surahs,
                                        surahsCount,
                                        surahsCount
                                    )
                                    val ayatText = pluralStringResource(
                                        R.plurals.home_bookmarks_ayahs,
                                        ayatCount,
                                        ayatCount
                                    )
                                    "$surahsText • $ayatText ${stringResource(R.string.home_bookmarks_saved_suffix)}"
                                } else {
                                    stringResource(R.string.home_bookmarks_empty)
                                },
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis
                            )
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
                        text = stringResource(R.string.home_stats_title),
                        style = MaterialTheme.typography.titleLarge,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onBackground
                    )
                    Text(
                        text = stringResource(R.string.home_stats_subtitle),
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
                            title = stringResource(R.string.home_stat_unique),
                            tag = "unique",
                            value = QuranMetaConstants.STATS_UNIQUE_WORDS,
                            icon = "📝",
                            modifier = Modifier.weight(1f)
                        )
                        StatCard(
                            title = stringResource(R.string.home_stat_verified),
                            tag = "verified",
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
                            title = stringResource(R.string.home_stat_masadir),
                            tag = "masadir",
                            value = QuranMetaConstants.STATS_MASADIR,
                            icon = "📚",
                            modifier = Modifier.weight(1f)
                        )
                        StatCard(
                            title = stringResource(R.string.home_stat_derivatives),
                            tag = "derivatives",
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
                            title = stringResource(R.string.home_stat_positions),
                            tag = "positions",
                            value = QuranMetaConstants.STATS_WORD_POSITIONS,
                            icon = "📍",
                            modifier = Modifier.weight(1f)
                        )
                        StatCard(
                            title = stringResource(R.string.home_stat_ayat),
                            tag = "ayat",
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
                        text = stringResource(R.string.home_featured_title),
                        style = MaterialTheme.typography.titleLarge,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onBackground
                    )
                    OutlinedButton(
                        onClick = onNavigateToRoots,
                        shape = ShapeSmall
                    ) {
                        Text(stringResource(R.string.common_view_all))
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
        if (showThemeDialog) {
            ThemeChooserDialog(
                darkModeSetting = darkModeSetting,
                dynamicEnabled = dynamicEnabled,
                language = language,
                onDarkModeChange = { mainViewModel.setDarkModeSetting(it) },
                onDynamicChange = { mainViewModel.setDynamicColorEnabled(it) },
                onLanguageChange = { mainViewModel.setLanguage(it) },
                onDismiss = { showThemeDialog = false }
            )
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
            // Fixed icon slot so the icon never shifts the text block.
            Box(
                modifier = Modifier.size(40.dp),
                contentAlignment = Alignment.Center
            ) {
                Text(icon, fontSize = 24.sp)
            }
            // Fixed title slot (2 lines): font stays identical, overflow ellipsizes
            // instead of growing the card — cards remain equal by construction.
            Text(
                text = title,
                style = MaterialTheme.typography.titleSmall,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.onSurface,
                maxLines = 2,
                overflow = TextOverflow.Ellipsis,
                textAlign = TextAlign.Center,
                modifier = Modifier
                    .fillMaxWidth()
                    .heightIn(min = 40.dp)
            )
            Text(
                text = subtitle,
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
                textAlign = TextAlign.Center,
                modifier = Modifier.fillMaxWidth()
            )
        }
    }
}

@Composable
private fun ThemeChooserDialog(
    darkModeSetting: Int,
    dynamicEnabled: Boolean,
    language: String,
    onDarkModeChange: (Int) -> Unit,
    onDynamicChange: (Boolean) -> Unit,
    onLanguageChange: (String) -> Unit,
    onDismiss: () -> Unit
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(stringResource(R.string.theme_title), fontWeight = FontWeight.Bold) },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(16.dp)) {
                // Theme section
                Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                    Text(stringResource(R.string.theme_section), style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.primary)
                    ThemeOptionRow(label = stringResource(R.string.theme_system), selected = darkModeSetting == 0, onClick = { onDarkModeChange(0) })
                    ThemeOptionRow(label = stringResource(R.string.theme_light), selected = darkModeSetting == 1, onClick = { onDarkModeChange(1) })
                    ThemeOptionRow(label = stringResource(R.string.theme_dark), selected = darkModeSetting == 2, onClick = { onDarkModeChange(2) })
                }
                // Colors section
                Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                    Text(stringResource(R.string.theme_colors), style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.primary)
                    ThemeOptionRow(
                        label = stringResource(R.string.theme_colors_app),
                        subLabel = stringResource(R.string.theme_colors_app_sub),
                        selected = !dynamicEnabled,
                        onClick = { onDynamicChange(false) }
                    )
                    ThemeOptionRow(
                        label = stringResource(R.string.theme_colors_system),
                        subLabel = if (Build.VERSION.SDK_INT >= 31) stringResource(R.string.theme_colors_system_sub) else stringResource(R.string.theme_colors_unsupported),
                        selected = dynamicEnabled,
                        enabled = Build.VERSION.SDK_INT >= 31,
                        onClick = { if (Build.VERSION.SDK_INT >= 31) onDynamicChange(true) }
                    )
                }
                // Language section: system default clears the per-app override
                // (LanguageManager applies an empty locale list), ar/en pin it.
                Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                    Text(stringResource(R.string.lang_title), style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.primary)
                    ThemeOptionRow(
                        label = stringResource(R.string.lang_system),
                        selected = language == AppLanguage.SYSTEM,
                        onClick = { onLanguageChange(AppLanguage.SYSTEM) }
                    )
                    ThemeOptionRow(
                        label = stringResource(R.string.lang_arabic),
                        selected = language == AppLanguage.ARABIC,
                        onClick = { onLanguageChange(AppLanguage.ARABIC) }
                    )
                    ThemeOptionRow(
                        label = stringResource(R.string.lang_english),
                        selected = language == AppLanguage.ENGLISH,
                        onClick = { onLanguageChange(AppLanguage.ENGLISH) }
                    )
                }
            }
        },
        confirmButton = {
            TextButton(onClick = onDismiss, modifier = androidx.compose.ui.Modifier.testTag("close_theme_dialog")) {
                Text(stringResource(R.string.common_close))
            }
        },
        shape = ShapeMedium
    )
}

@Composable
private fun ThemeOptionRow(
    label: String,
    selected: Boolean,
    onClick: () -> Unit,
    subLabel: String? = null,
    enabled: Boolean = true
) {
    Row(
        modifier = androidx.compose.ui.Modifier
            .fillMaxWidth()
            .clip(ShapeSmall)
            .clickable(enabled = enabled, onClick = onClick)
            .padding(horizontal = 8.dp, vertical = 6.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        RadioButton(selected = selected, onClick = onClick, enabled = enabled)
        Column(modifier = androidx.compose.ui.Modifier.weight(1f)) {
            Text(
                text = label,
                style = MaterialTheme.typography.bodyMedium,
                fontWeight = if (selected) FontWeight.Bold else FontWeight.Normal,
                color = if (enabled) MaterialTheme.colorScheme.onSurface else MaterialTheme.colorScheme.onSurface.copy(alpha = 0.5f)
            )
            subLabel?.let {
                Text(
                    text = it,
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = if (enabled) 1f else 0.5f)
                )
            }
        }
    }
}
