package io.github.ahmedsaadi0.quranwords.ui.navigation

import kotlinx.serialization.Serializable

/**
 * Type-safe navigation routes (AGENTS §11, Decision 11). Params are non-null
 * by construction — no string parsing, no silent `?: 1` fallbacks.
 */
@Serializable
data object Home

@Serializable
data object SurahIndex

@Serializable
data class SurahDetail(val surahId: Int, val ayah: Int = 1)

@Serializable
data object Roots

@Serializable
data class RootDetail(val rootId: Int)

@Serializable
data class WordAyat(val rootId: Int, val wordId: Int)

@Serializable
data object Search

@Serializable
data object Guide

@Serializable
data object Setup

@Serializable
data object Bookmarks