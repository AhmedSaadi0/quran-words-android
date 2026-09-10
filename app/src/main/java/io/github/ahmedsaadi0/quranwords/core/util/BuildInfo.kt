package io.github.ahmedsaadi0.quranwords.core.util

/**
 * Platform facts snapshot injected into ViewModels (AGENTS §10.2) so they
 * read BuildConfig/Build/Locale without direct framework dependencies.
 * Provided by core.di.BuildInfoModule.
 */
data class BuildInfo(
    val appVersion: String,
    val androidRelease: String,
    val locale: String
)