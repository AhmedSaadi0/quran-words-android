package io.github.ahmedsaadi0.quranwords.core.navigation

// TODO: Enable typed navigation with @Serializable once kotlinx-serialization plugin is added
// For now these are plain route types to document the intended NavGraph — see AGENTS.md §11
// To enable: add `alias(libs.plugins.kotlin.serialization)` + `implementation(libs.kotlinx.serialization.json)`

data object Home

data object SurahIndex

data class SurahDetail(val surahId: Int, val ayah: Int = 1)

data object Roots

data class RootDetail(val rootId: Int)

data object Search

data object Guide

data object DatabaseSetup

data object Bookmarks

// Legacy string routes kept for incremental migration — see ui/navigation/Screen.kt
// New code should use typed Routes above with NavHost { composable<Home> { ... } } once serialization is enabled
