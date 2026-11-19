package com.quranwords.ui.search

import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.performClick
import com.quranwords.ui.theme.MyApplicationTheme
import org.junit.Assert.assertEquals
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [36])
class SearchPagingErrorTest {

    @get:Rule
    val composeTestRule = createComposeRule()

    @Test
    fun inline_retry_visible_with_results() {
        val events = mutableListOf<SearchEvent>()

        composeTestRule.setContent {
            MyApplicationTheme {
                SearchScreen(
                    uiState = SearchUiState(query = "كتب", pagingError = "page failed"),
                    onEvent = { events.add(it) },
                    onNavigateBack = {},
                    onNavigateToRootDetail = {},
                    onNavigateToSurahDetail = { _, _ -> }
                )
            }
        }

        composeTestRule.onNodeWithTag("search_paging_retry").assertIsDisplayed().performClick()
        assertEquals(listOf(SearchEvent.LoadMore(SearchTab.ROOTS)), events)
    }
}
