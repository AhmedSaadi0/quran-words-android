package io.github.ahmedsaadi0.quranwords.ui.navigation

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.MenuBook
import androidx.compose.material.icons.filled.Architecture
import androidx.compose.material.icons.filled.Eco
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.NavigationBarItemDefaults
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.navigation.NavDestination
import androidx.navigation.NavDestination.Companion.hasRoute
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import androidx.navigation.toRoute
import io.github.ahmedsaadi0.quranwords.R
import io.github.ahmedsaadi0.quranwords.ui.bookmarks.BookmarksRoute
import io.github.ahmedsaadi0.quranwords.ui.roots.RootsListRoute
import io.github.ahmedsaadi0.quranwords.ui.roots.detail.RootDetailRoute
import io.github.ahmedsaadi0.quranwords.ui.roots.word.WordAyatRoute
import io.github.ahmedsaadi0.quranwords.ui.setup.DatabaseSetupRoute
import io.github.ahmedsaadi0.quranwords.ui.guide.MorphologyGuideScreen
import io.github.ahmedsaadi0.quranwords.ui.home.HomeRoute
import io.github.ahmedsaadi0.quranwords.ui.surah.detail.SurahDetailRoute
import io.github.ahmedsaadi0.quranwords.ui.search.SearchRoute
import io.github.ahmedsaadi0.quranwords.ui.surah.SurahIndexRoute
import io.github.ahmedsaadi0.quranwords.ui.theme.AppMotion

data class BottomNavItem(
    val route: Any,
    val labelRes: Int,
    val icon: ImageVector,
    val testTag: String
)

private val bottomNavItems = listOf(
    BottomNavItem(Home, R.string.nav_home, Icons.Filled.Home, "nav_home"),
    BottomNavItem(SurahIndex, R.string.nav_surahs, Icons.AutoMirrored.Filled.MenuBook, "nav_surahs"),
    BottomNavItem(Roots, R.string.nav_roots, Icons.Filled.Eco, "nav_roots"),
    BottomNavItem(Search, R.string.nav_search, Icons.Filled.Search, "nav_search"),
    BottomNavItem(Guide, R.string.nav_guide, Icons.Filled.Architecture, "nav_guide")
)

private fun NavDestination?.isTopLevelDestination(item: BottomNavItem): Boolean =
    this?.hasRoute(item.route::class) == true

@Composable
fun AppNavigation() {
    val navController = rememberNavController()
    val navBackStackEntry by navController.currentBackStackEntryAsState()
    val currentDestination = navBackStackEntry?.destination

    val showBottomBar = bottomNavItems.any { currentDestination.isTopLevelDestination(it) }

    Scaffold(
        bottomBar = {
            AnimatedVisibility(
                visible = showBottomBar,
                enter = slideInVertically(initialOffsetY = { it }),
                exit = slideOutVertically(targetOffsetY = { it })
            ) {
                NavigationBar(
                    containerColor = MaterialTheme.colorScheme.surface,
                    tonalElevation = 2.dp,
                    modifier = Modifier.testTag("main_bottom_nav")
                ) {
                    bottomNavItems.forEach { item ->
                        val selected = currentDestination.isTopLevelDestination(item)
                        NavigationBarItem(
                            selected = selected,
                            onClick = {
                                if (!selected) {
                                    navController.navigate(item.route) {
                                        popUpTo(Home::class) {
                                            saveState = true
                                        }
                                        launchSingleTop = true
                                        restoreState = true
                                    }
                                }
                            },
                            icon = {
                                Icon(
                                    imageVector = item.icon,
                                    contentDescription = stringResource(item.labelRes)
                                )
                            },
                            label = {
                                Text(
                                    text = stringResource(item.labelRes),
                                    fontWeight = if (selected) FontWeight.Bold else FontWeight.Normal,
                                    fontSize = 12.sp
                                )
                            },
                            colors = NavigationBarItemDefaults.colors(
                                selectedIconColor = MaterialTheme.colorScheme.primary,
                                selectedTextColor = MaterialTheme.colorScheme.primary,
                                indicatorColor = MaterialTheme.colorScheme.primaryContainer,
                                unselectedIconColor = MaterialTheme.colorScheme.onSurfaceVariant,
                                unselectedTextColor = MaterialTheme.colorScheme.onSurfaceVariant
                            ),
                            modifier = Modifier.testTag(item.testTag)
                        )
                    }
                }
            }
        },
        contentWindowInsets = WindowInsets(0, 0, 0, 0)
    ) { innerPadding ->
        NavHost(
            navController = navController,
            startDestination = Home,
            modifier = Modifier.padding(innerPadding),
            enterTransition = { AppMotion.navEnterTransition() },
            exitTransition = { AppMotion.navExitTransition() },
            popEnterTransition = { AppMotion.navPopEnterTransition() },
            popExitTransition = { AppMotion.navPopExitTransition() }
        ) {
            composable<Home> {
                HomeRoute(
                    onNavigateToSurahIndex = { navController.navigate(SurahIndex) },
                    onNavigateToSurahDetail = { surahId, ayah ->
                        navController.navigate(SurahDetail(surahId, ayah))
                    },
                    onNavigateToRoots = { navController.navigate(Roots) },
                    onNavigateToRootDetail = { rootId ->
                        navController.navigate(RootDetail(rootId))
                    },
                    onNavigateToSearch = { navController.navigate(Search) },
                    onNavigateToGuide = { navController.navigate(Guide) },
                    onNavigateToSetup = { navController.navigate(Setup) },
                    onNavigateToBookmarks = { navController.navigate(Bookmarks) }
                )
            }

            composable<SurahIndex> {
                SurahIndexRoute(
                    onNavigateBack = { navController.popBackStack() },
                    onNavigateToSurahDetail = { surahId ->
                        navController.navigate(SurahDetail(surahId))
                    }
                )
            }

            composable<SurahDetail> { backStackEntry ->
                val route: SurahDetail = backStackEntry.toRoute()
                SurahDetailRoute(
                    surahId = route.surahId,
                    targetAyah = route.ayah,
                    onNavigateBack = { navController.popBackStack() },
                    onNavigateToRootDetail = { rootId ->
                        navController.navigate(RootDetail(rootId))
                    }
                )
            }

            composable<Roots> {
                RootsListRoute(
                    onNavigateBack = { navController.popBackStack() },
                    onNavigateToRootDetail = { rootId ->
                        navController.navigate(RootDetail(rootId))
                    }
                )
            }

            composable<RootDetail> { backStackEntry ->
                val route: RootDetail = backStackEntry.toRoute()
                RootDetailRoute(
                    rootId = route.rootId,
                    onNavigateBack = { navController.popBackStack() },
                    onNavigateToSurahDetail = { surahId, ayahNum ->
                        navController.navigate(SurahDetail(surahId, ayahNum))
                    },
                    onNavigateToWordAyat = { rId, wId ->
                        navController.navigate(WordAyat(rId, wId))
                    }
                )
            }

            composable<WordAyat> { backStackEntry ->
                val route: WordAyat = backStackEntry.toRoute()
                WordAyatRoute(
                    rootId = route.rootId,
                    wordId = route.wordId,
                    onNavigateBack = { navController.popBackStack() },
                    onNavigateToSurahDetail = { surahId, ayahNum ->
                        navController.navigate(SurahDetail(surahId, ayahNum))
                    }
                )
            }

            composable<Search> {
                SearchRoute(
                    onNavigateBack = { navController.popBackStack() },
                    onNavigateToRootDetail = { rootId ->
                        navController.navigate(RootDetail(rootId))
                    },
                    onNavigateToSurahDetail = { surahId, ayahNum ->
                        navController.navigate(SurahDetail(surahId, ayahNum))
                    }
                )
            }

            composable<Guide> {
                MorphologyGuideScreen(
                    onNavigateBack = { navController.popBackStack() }
                )
            }

            composable<Setup> {
                DatabaseSetupRoute(
                    onNavigateBack = { navController.popBackStack() }
                )
            }

            composable<Bookmarks> {
                BookmarksRoute(
                    onNavigateBack = { navController.popBackStack() },
                    onNavigateToSurahDetail = { surahId, ayah ->
                        navController.navigate(SurahDetail(surahId, ayah))
                    }
                )
            }
        }
    }
}