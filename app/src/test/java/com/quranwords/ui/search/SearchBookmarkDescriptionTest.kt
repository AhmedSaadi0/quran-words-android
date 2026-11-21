package com.quranwords.ui.search

import androidx.compose.ui.test.assertContentDescriptionEquals
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithTag
import com.quranwords.domain.model.Ayah
import com.quranwords.ui.search.components.AyahSearchCard
import com.quranwords.ui.theme.MyApplicationTheme
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [36], qualifiers = "en")
class SearchBookmarkDescriptionTest {
    @get:Rule val compose = createComposeRule()

    private val ayah = Ayah(
        id = 2, surah = 2, ayah = 3,
        textUthmani = "آية", textUthmaniPlain = "آية", textImlaei = "آية", wordCount = 1
    )

    @Test
    fun bookmark_description_reflects_state() {
        compose.setContent {
            MyApplicationTheme {
                AyahSearchCard(ayah, "", isBookmarked = false, onToggleBookmark = {}, onClick = {})
            }
        }
        compose.onNodeWithTag("bookmark_toggle_2_3")
            .assertContentDescriptionEquals("Bookmark ayah")

        compose.setContent {
            MyApplicationTheme {
                AyahSearchCard(ayah, "", isBookmarked = true, onToggleBookmark = {}, onClick = {})
            }
        }
        compose.onNodeWithTag("bookmark_toggle_2_3")
            .assertContentDescriptionEquals("Remove ayah bookmark")
    }
}
