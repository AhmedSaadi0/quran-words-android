package com.quranwords.ui.roots.detail.tabs

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyListState
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.ContentCopy
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
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
import com.quranwords.ui.roots.detail.components.cards.CitedBodyText
import com.quranwords.ui.roots.detail.components.cards.EmptyTabNotice

/**
 * AI summary tab as pure foldable cards (Decisions #33/#34): المعنى الأصلي
 * (the only card opened by default), then التطور الدلالي and الفروق collapsed.
 * Every card ends with the model/date line + a copy button. The header no
 * longer hosts the summary.
 */
@Composable
fun AiSummaryTab(
    extended: AiExtendedSummary,
    aiMetaLine: String?,
    hasAiMeta: Boolean,
    fontSize: Float,
    onCopySection: (String) -> Unit,
    modifier: Modifier = Modifier,
    listState: LazyListState? = null
) {
    val state = listState ?: rememberLazyListState()
    val refsLabel = stringResource(R.string.ai_refs_label)
    val sections = listOf(
        Triple(extended.hissi, R.string.ai_section_hissi, "hissi"),
        Triple(extended.tatawwur, R.string.ai_section_tatawwur, "tatawwur"),
        Triple(extended.furuq, R.string.ai_section_furuq, "furuq")
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
                // المعنى الأصلي is the only card opened by default; the rest closed.
                val defaultExpanded = tag == "hissi"
                item(key = "ai_$tag") {
                    var expanded by rememberSaveable { mutableStateOf(defaultExpanded) }
                    PaddedPagerItem {
                        AiSectionCard(
                            title = stringResource(titleRes),
                            body = "",
                            refs = emptyList(),
                            refsLabel = refsLabel,
                            fontSize = fontSize,
                            expanded = expanded,
                            onToggle = { expanded = !expanded },
                            testTagSuffix = tag,
                            bodyContent = {
                                Column(
                                    modifier = Modifier.padding(start = 14.dp, end = 14.dp, bottom = 14.dp),
                                    verticalArrangement = Arrangement.spacedBy(8.dp)
                                ) {
                                    CitedBodyText(
                                        body = body,
                                        refs = section.refs,
                                        refsLabel = refsLabel,
                                        fontSize = fontSize,
                                        testTagPrefix = "ai_ref_$tag"
                                    )
                                    AiCardFooter(aiMetaLine = aiMetaLine, hasAiMeta = hasAiMeta) {
                                        IconButton(
                                            onClick = { onCopySection(tag) },
                                            modifier = Modifier
                                                .size(28.dp)
                                                .testTag("copy_ai_section_$tag")
                                        ) {
                                            Icon(
                                                imageVector = Icons.Outlined.ContentCopy,
                                                contentDescription = stringResource(R.string.cd_copy_ai_summary),
                                                tint = MaterialTheme.colorScheme.onSurfaceVariant,
                                                modifier = Modifier.size(16.dp)
                                            )
                                        }
                                    }
                                }
                            }
                        )
                    }
                }
            }
        }
        item { Spacer(modifier = Modifier.height(80.dp)) }
    }
}

/** Model/date line + trailing card actions shared by every AI card. */
@Composable
private fun AiCardFooter(
    aiMetaLine: String?,
    hasAiMeta: Boolean,
    content: @Composable () -> Unit
) {
    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
        if (hasAiMeta && !aiMetaLine.isNullOrBlank()) {
            Text(
                text = aiMetaLine,
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
        content()
    }
}
