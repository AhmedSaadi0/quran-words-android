package io.github.ahmedsaadi0.quranwords.ui.navigation

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.compose.foundation.layout.padding
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
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import androidx.navigation.navArgument
import androidx.hilt.navigation.compose.hiltViewModel
import io.github.ahmedsaadi0.quranwords.ui.theme.AppMotion
import io.github.ahmedsaadi0.quranwords.ui.screens.BookmarksScreen
import io.github.ahmedsaadi0.quranwords.ui.screens.DatabaseSetupScreen
import io.github.ahmedsaadi0.quranwords.ui.screens.HomeScreen
import io.github.ahmedsaadi0.quranwords.ui.screens.MorphologyGuideScreen
import io.github.ahmedsaadi0.quranwords.ui.screens.RootDetailScreen
import io.github.ahmedsaadi0.quranwords.ui.screens.RootsListScreen
import io.github.ahmedsaadi0.quranwords.ui.screens.SearchScreen
import io.github.ahmedsaadi0.quranwords.ui.screens.SurahDetailScreen
import io.github.ahmedsaadi0.quranwords.ui.screens.SurahIndexScreen
import io.github.ahmedsaadi0.quranwords.ui.viewmodel.DatabaseSetupViewModel
import io.github.ahmedsaadi0.quranwords.ui.viewmodel.HomeViewModel
import io.github.ahmedsaadi0.quranwords.ui.viewmodel.MainViewModel
import io.github.ahmedsaadi0.quranwords.ui.viewmodel.RootViewModel
import io.github.ahmedsaadi0.quranwords.ui.viewmodel.SearchViewModel
import io.github.ahmedsaadi0.quranwords.ui.viewmodel.SurahDetailViewModel
import io.github.ahmedsaadi0.quranwords.ui.viewmodel.SurahViewModel

data class BottomNavItem(
    val route: String,
    val title: String,
    val iconEmoji: String
)

@Composable
fun AppNavigation(
    mainViewModel: MainViewModel
) {
    val navController = rememberNavController()
    val navBackStackEntry by navController.currentBackStackEntryAsState()
    val currentRoute = navBackStackEntry?.destination?.route

    val bottomNavItems = listOf(
        BottomNavItem(Screen.Home.route, "الرئيسية", "🏠"),
        BottomNavItem(Screen.SurahIndex.route, "السور", "📖"),
        BottomNavItem(Screen.Roots.route, "الجذور", "🌿"),
        BottomNavItem(Screen.Search.route, "البحث", "🔍"),
        BottomNavItem(Screen.Guide.route, "الدليل", "📐")
    )

    val showBottomBar = bottomNavItems.any { it.route == currentRoute }

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
                        val selected = currentRoute == item.route
                        NavigationBarItem(
                            selected = selected,
                            onClick = {
                                if (currentRoute != item.route) {
                                    navController.navigate(item.route) {
                                        popUpTo(Screen.Home.route) {
                                            saveState = true
                                        }
                                        launchSingleTop = true
                                        restoreState = true
                                    }
                                }
                            },
                            icon = {
                                Text(item.iconEmoji, fontSize = 20.sp)
                            },
                            label = {
                                Text(
                                    text = item.title,
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
                            modifier = Modifier.testTag("nav_${item.route}")
                        )
                    }
                }
            }
        },
        contentWindowInsets = androidx.compose.foundation.layout.WindowInsets(0, 0, 0, 0)
    ) { innerPadding ->
        NavHost(
            navController = navController,
            startDestination = Screen.Home.route,
            modifier = Modifier.padding(innerPadding),
            enterTransition = { AppMotion.navEnterTransition() },
            exitTransition = { AppMotion.navExitTransition() },
            popEnterTransition = { AppMotion.navPopEnterTransition() },
            popExitTransition = { AppMotion.navPopExitTransition() }
        ) {
            composable(
                Screen.Home.route,
                enterTransition = { AppMotion.navEnterTransition() },
                exitTransition = { AppMotion.navExitTransition() },
                popEnterTransition = { AppMotion.navPopEnterTransition() },
                popExitTransition = { AppMotion.navPopExitTransition() }
            ) {
                HomeScreen(
                    mainViewModel = mainViewModel,
                    homeViewModel = hiltViewModel<HomeViewModel>(),
                    onNavigateToSurahIndex = { navController.navigate(Screen.SurahIndex.route) },
                    onNavigateToSurahDetail = { surahId, ayah ->
                        navController.navigate(Screen.SurahDetail.createRoute(surahId, ayah))
                    },
                    onNavigateToRoots = { navController.navigate(Screen.Roots.route) },
                    onNavigateToRootDetail = { rootId ->
                        navController.navigate(Screen.RootDetail.createRoute(rootId))
                    },
                    onNavigateToSearch = { navController.navigate(Screen.Search.route) },
                    onNavigateToGuide = { navController.navigate(Screen.Guide.route) },
                    onNavigateToSetup = { navController.navigate(Screen.DatabaseSetup.route) },
                    onNavigateToBookmarks = { navController.navigate(Screen.Bookmarks.route) }
                )
            }

            composable(
                Screen.SurahIndex.route,
                enterTransition = { AppMotion.navEnterTransition() },
                exitTransition = { AppMotion.navExitTransition() },
                popEnterTransition = { AppMotion.navPopEnterTransition() },
                popExitTransition = { AppMotion.navPopExitTransition() }
            ) {
                SurahIndexScreen(
                    onNavigateBack = { navController.popBackStack() },
                    onNavigateToSurahDetail = { surahId ->
                        navController.navigate(Screen.SurahDetail.createRoute(surahId))
                    },
                    mainViewModel = mainViewModel,
                    surahViewModel = hiltViewModel<SurahViewModel>()
                )
            }

            composable(
                route = Screen.SurahDetail.route,
                arguments = listOf(
                    navArgument("surahId") { type = NavType.IntType },
                    navArgument("ayah") { type = NavType.IntType; defaultValue = 1 }
                ),
                enterTransition = { AppMotion.navEnterTransition() },
                exitTransition = { AppMotion.navExitTransition() },
                popEnterTransition = { AppMotion.navPopEnterTransition() },
                popExitTransition = { AppMotion.navPopExitTransition() }
            ) { backStackEntry ->
                val surahId = backStackEntry.arguments?.getInt("surahId") ?: 1
                val targetAyah = backStackEntry.arguments?.getInt("ayah") ?: 1
                SurahDetailScreen(
                    surahId = surahId,
                    targetAyah = targetAyah,
                    mainViewModel = mainViewModel,
                    surahDetailViewModel = hiltViewModel<SurahDetailViewModel>(),
                    onNavigateBack = { navController.popBackStack() },
                    onNavigateToRootDetail = { rootId ->
                        navController.navigate(Screen.RootDetail.createRoute(rootId))
                    }
                )
            }

            composable(
                Screen.Roots.route,
                enterTransition = { AppMotion.navEnterTransition() },
                exitTransition = { AppMotion.navExitTransition() },
                popEnterTransition = { AppMotion.navPopEnterTransition() },
                popExitTransition = { AppMotion.navPopExitTransition() }
            ) {
                RootsListScreen(
                    onNavigateBack = { navController.popBackStack() },
                    onNavigateToRootDetail = { rootId ->
                        navController.navigate(Screen.RootDetail.createRoute(rootId))
                    },
                    rootViewModel = hiltViewModel<RootViewModel>()
                )
            }

            composable(
                route = Screen.RootDetail.route,
                arguments = listOf(navArgument("rootId") { type = NavType.IntType }),
                enterTransition = { AppMotion.navEnterTransition() },
                exitTransition = { AppMotion.navExitTransition() },
                popEnterTransition = { AppMotion.navPopEnterTransition() },
                popExitTransition = { AppMotion.navPopExitTransition() }
            ) { backStackEntry ->
                val rootId = backStackEntry.arguments?.getInt("rootId") ?: 1
                RootDetailScreen(
                    rootId = rootId,
                    onNavigateBack = { navController.popBackStack() },
                    onNavigateToSurahDetail = { surahId, ayahNum ->
                        navController.navigate(Screen.SurahDetail.createRoute(surahId, ayahNum))
                    },
                    rootViewModel = hiltViewModel<RootViewModel>()
                )
            }

            composable(
                Screen.Search.route,
                enterTransition = { AppMotion.navEnterTransition() },
                exitTransition = { AppMotion.navExitTransition() },
                popEnterTransition = { AppMotion.navPopEnterTransition() },
                popExitTransition = { AppMotion.navPopExitTransition() }
            ) {
                SearchScreen(
                    onNavigateBack = { navController.popBackStack() },
                    onNavigateToRootDetail = { rootId ->
                        navController.navigate(Screen.RootDetail.createRoute(rootId))
                    },
                    onNavigateToSurahDetail = { surahId, ayahNum ->
                        navController.navigate(Screen.SurahDetail.createRoute(surahId, ayahNum))
                    },
                    searchViewModel = hiltViewModel<SearchViewModel>()
                )
            }

            composable(
                Screen.Guide.route,
                enterTransition = { AppMotion.navEnterTransition() },
                exitTransition = { AppMotion.navExitTransition() },
                popEnterTransition = { AppMotion.navPopEnterTransition() },
                popExitTransition = { AppMotion.navPopExitTransition() }
            ) {
                MorphologyGuideScreen(
                    onNavigateBack = { navController.popBackStack() }
                )
            }

            composable(
                Screen.DatabaseSetup.route,
                enterTransition = { AppMotion.navEnterTransition() },
                exitTransition = { AppMotion.navExitTransition() },
                popEnterTransition = { AppMotion.navPopEnterTransition() },
                popExitTransition = { AppMotion.navPopExitTransition() }
            ) {
                DatabaseSetupScreen(
                    mainViewModel = mainViewModel,
                    onNavigateBack = { navController.popBackStack() },
                    setupViewModel = hiltViewModel<DatabaseSetupViewModel>()
                )
            }

            composable(
                Screen.Bookmarks.route,
                enterTransition = { AppMotion.navEnterTransition() },
                exitTransition = { AppMotion.navExitTransition() },
                popEnterTransition = { AppMotion.navPopEnterTransition() },
                popExitTransition = { AppMotion.navPopExitTransition() }
            ) {
                BookmarksScreen(
                    mainViewModel = mainViewModel,
                    onNavigateBack = { navController.popBackStack() },
                    onNavigateToSurahDetail = { surahId, ayah ->
                        navController.navigate(Screen.SurahDetail.createRoute(surahId, ayah))
                    }
                )
            }
        }
    }
}
