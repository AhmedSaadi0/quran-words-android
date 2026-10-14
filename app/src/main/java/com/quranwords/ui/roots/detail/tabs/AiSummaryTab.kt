package com.quranwords.ui.roots.detail.tabs

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyListState
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import com.quranwords.R
import com.quranwords.domain.model.AiExtendedSummary
import com.quranwords.ui.roots.detail.components.PaddedPagerItem
import com.quranwords.ui.roots.detail.components.cards.AiSectionCard
import com.quranwords.ui.roots.detail.components.cards.EmptyTabNotice

@Composable
fun AiSummaryTab(
    extended: AiExtendedSummary,
    fontSize: Float,
    modifier: Modifier = Modifier,
    listState: LazyListState? = null
) {
    val state = listState ?: rememberLazyListState()
    val refsLabel = stringResource(R.string.ai_refs_label)
    val sections = listOf(
        Triple(extended.furuq, R.string.ai_section_furuq, "furuq"),
        Triple(extended.shabaka, R.string.ai_section_shabaka, "shabaka"),
        Triple(extended.hissi, R.string.ai_section_hissi, "hissi"),
        Triple(extended.tatawwur, R.string.ai_section_tatawwur, "tatawwur"),
        Triple(extended.tashrih, R.string.ai_section_tashrih, "tashrih")
    )
    val visible = sections.filter { it.first.isNotBlank() }

    LazyColumn(
        state = state,
        modifier = modifier
            .fillMaxSize()
            .testTag("root_detail_screen_5"),
        contentPadding = PaddingValues(top = 12.dp, bottom = 24.dp),
        verticalArrangement = Arrangement.spacedBy(0.dp)
    ) {
        if (visible.isEmpty()) {
            item {
                EmptyTabNotice(text = stringResource(R.string.root_no_ai_summary))
            }
        } else {
            visible.forEach { (section, titleRes, tag) ->
                val body = section.text?.trim().orEmpty()
                item(key = "ai_$tag") {
                    var expanded by rememberSaveable { mutableStateOf(true) }
                    PaddedPagerItem {
                        AiSectionCard(
                            title = stringResource(titleRes),
                            body = body,
                            refs = section.refs,
                            refsLabel = refsLabel,
                            fontSize = fontSize,
                            expanded = expanded,
                            onToggle = { expanded = !expanded },
                            testTagSuffix = tag
                        )
                    }
                }
            }
        }
        item { Spacer(modifier = Modifier.height(80.dp)) }
    }
}
