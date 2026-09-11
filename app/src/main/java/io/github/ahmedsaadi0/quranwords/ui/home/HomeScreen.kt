package io.github.ahmedsaadi0.quranwords.ui.home

import androidx.compose.animation.core.tween
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.statusBars
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.dp
import io.github.ahmedsaadi0.quranwords.domain.model.DbUpdateState
import io.github.ahmedsaadi0.quranwords.ui.home.components.BookmarksQuickCard
import io.github.ahmedsaadi0.quranwords.ui.home.components.ContinueReadingCard
import io.github.ahmedsaadi0.quranwords.ui.home.components.DbSetupBanner
import io.github.ahmedsaadi0.quranwords.ui.home.components.FeaturedRootsSection
import io.github.ahmedsaadi0.quranwords.ui.home.components.HomeHeader
import io.github.ahmedsaadi0.quranwords.ui.home.components.HomeUpdateBanner
import io.github.ahmedsaadi0.quranwords.ui.home.components.QuickNavRow
import io.github.ahmedsaadi0.quranwords.ui.home.components.StatsGrid
import io.github.ahmedsaadi0.quranwords.ui.theme.AppMotion

/**
 * Stateless home screen: composes the feature sections; all state comes from
 * [HomeUiState] + the banner state, all actions are callbacks.
 */
@Composable
fun HomeScreen(
    uiState: HomeUiState,
    updateState: DbUpdateState,
    onOpenSettings: () -> Unit,
    onToggleDynamicColor: () -> Unit,
    onToggleDarkMode: () -> Unit,
    onNavigateToSurahIndex: () -> Unit,
    onNavigateToSurahDetail: (Int, Int) -> Unit,
    onNavigateToRoots: () -> Unit,
    onNavigateToRootDetail: (Int) -> Unit,
    onNavigateToSearch: () -> Unit,
    onNavigateToGuide: () -> Unit,
    onNavigateToSetup: () -> Unit,
    onNavigateToBookmarks: () -> Unit,
    onDismissUpdate: (DbUpdateState.UpdateAvailable) -> Unit
) {
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
            item(key = "home_header") {
                HomeHeader(
                    onOpenSettings = onOpenSettings,
                    onToggleDynamicColor = onToggleDynamicColor,
                    onToggleDarkMode = onToggleDarkMode,
                    onNavigateToSearch = onNavigateToSearch
                )
            }

            if (!uiState.isDbReady) {
                item(key = "home_db_banner") {
                    DbSetupBanner(
                        onNavigateToSetup = onNavigateToSetup,
                        modifier = Modifier.animateItem(placementSpec = placementSpec())
                    )
                }
            }

            if (uiState.isDbReady && updateState is DbUpdateState.UpdateAvailable) {
                item(key = "home_update_banner") {
                    HomeUpdateBanner(
                        info = updateState.info,
                        onUpdateClick = onNavigateToSetup,
                        onDismissClick = { onDismissUpdate(updateState) },
                        modifier = Modifier.animateItem(placementSpec = placementSpec())
                    )
                }
            }

            item(key = "home_quick_nav") {
                QuickNavRow(
                    onNavigateToSurahIndex = onNavigateToSurahIndex,
                    onNavigateToRoots = onNavigateToRoots,
                    onNavigateToGuide = onNavigateToGuide,
                    modifier = Modifier.animateItem(placementSpec = placementSpec())
                )
            }

            item(key = "home_continue") {
                ContinueReadingCard(
                    lastReadSurah = uiState.lastReadSurah,
                    lastReadAyah = uiState.lastReadAyah,
                    onNavigateToSurahDetail = onNavigateToSurahDetail,
                    modifier = Modifier.animateItem(placementSpec = placementSpec())
                )
            }

            item(key = "home_bookmarks") {
                BookmarksQuickCard(
                    bookmarkedSurahs = uiState.bookmarkedSurahs,
                    bookmarkedAyat = uiState.bookmarkedAyat,
                    onNavigateToBookmarks = onNavigateToBookmarks,
                    modifier = Modifier.animateItem(placementSpec = placementSpec())
                )
            }

            item(key = "home_stats_header") {
                StatsGrid(modifier = Modifier.animateItem(placementSpec = placementSpec()))
            }

            FeaturedRootsSection(
                featuredRoots = uiState.featuredRoots,
                onNavigateToRoots = onNavigateToRoots,
                onNavigateToRootDetail = onNavigateToRootDetail
            )
        }
    }
}

private fun placementSpec() = tween<IntOffset>(
    durationMillis = AppMotion.DurationMedium,
    easing = AppMotion.EasingStandard
)