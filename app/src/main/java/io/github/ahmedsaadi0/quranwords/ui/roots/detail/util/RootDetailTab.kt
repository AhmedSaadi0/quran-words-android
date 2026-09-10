package io.github.ahmedsaadi0.quranwords.ui.roots.detail.util

import androidx.annotation.StringRes
import io.github.ahmedsaadi0.quranwords.R

/**
 * Canonical tab order for RootDetail (visible pager order).
 *
 * Replaces the previous `tabContentIds = listOf(0, 4, 3, 1, 2)` indirection in
 * RootDetailScreen. Pager index == [RootDetailTab] ordinal, so `when(tab)` is
 * exhaustive and no `-1` index hacks are needed.
 *
 * [contentId] preserves the legacy content identifiers used by testTags
 * (`root_detail_screen_<contentId>`) to avoid breaking existing UI tests:
 * 0 meanings, 1 masadir, 2 derivatives, 3 words, 4 ayat.
 */
enum class RootDetailTab(
    @StringRes val titleRes: Int,
    val contentId: Int,
) {
    MEANINGS(R.string.tab_meanings, 0),
    AYAT(R.string.tab_ayat, 4),
    WORDS(R.string.tab_words, 3),
    MASADIR(R.string.tab_masadir, 1),
    DERIVATIVES(R.string.tab_derivatives, 2),
}
