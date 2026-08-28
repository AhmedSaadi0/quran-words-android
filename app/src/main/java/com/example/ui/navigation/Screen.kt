package com.example.ui.navigation

sealed class Screen(val route: String) {
    object Home : Screen("home")
    object SurahIndex : Screen("surahs")
    object SurahDetail : Screen("surah_detail/{surahId}?ayah={ayah}") {
        fun createRoute(surahId: Int, ayah: Int = 1) = "surah_detail/$surahId?ayah=$ayah"
    }
    object Roots : Screen("roots")
    object RootDetail : Screen("root_detail/{rootId}") {
        fun createRoute(rootId: Int) = "root_detail/$rootId"
    }
    object Search : Screen("search")
    object Guide : Screen("guide")
    object DatabaseSetup : Screen("setup")
    object Bookmarks : Screen("bookmarks")
}
