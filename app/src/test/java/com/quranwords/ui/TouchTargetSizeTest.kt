package com.quranwords.ui

import androidx.compose.ui.test.assertHeightIsAtLeast
import androidx.compose.ui.test.assertWidthIsAtLeast
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.unit.dp
import com.quranwords.domain.model.AiExtendedSection
import com.quranwords.domain.model.AiExtendedSummary
import com.quranwords.ui.components.FontSizeControls
import com.quranwords.ui.roots.detail.tabs.AiSummaryTab
import com.quranwords.ui.search.components.AyahSurahFilterBar
import com.quranwords.ui.theme.MyApplicationTheme
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [36])
class TouchTargetSizeTest {
    @get:Rule val compose = createComposeRule()

    @Test
    fun interactive_nodes_have_48dp_bounds() {
        compose.setContent {
            MyApplicationTheme {
                FontSizeControls(fontSize = 24f, onFontSizeChange = {})
                AyahSurahFilterBar(selectedSurahId = null, onSelect = {})
                AiSummaryTab(
                    extended = AiExtendedSummary(hissi = AiExtendedSection("text")),
                    aiMetaLine = null,
                    hasAiMeta = false,
                    fontSize = 24f,
                    onCopySection = {}
                )
            }
        }

        listOf("decrease_font_size", "increase_font_size", "ayah_filter_all", "ayah_filter_surah", "copy_ai_section_hissi")
            .forEach { tag ->
                compose.onNodeWithTag(tag)
                    .assertWidthIsAtLeast(48.dp)
                    .assertHeightIsAtLeast(48.dp)
            }
    }
}
