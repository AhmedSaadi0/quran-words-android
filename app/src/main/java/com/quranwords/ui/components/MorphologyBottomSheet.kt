package com.quranwords.ui.components

import androidx.compose.animation.animateContentSize
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.SheetState
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.quranwords.R
import com.quranwords.core.util.cleanAiDate
import com.quranwords.domain.model.AiExtendedSummary
import com.quranwords.domain.model.Ayah
import com.quranwords.domain.model.WordToken
import com.quranwords.ui.roots.detail.components.cards.AiSectionCard
import com.quranwords.ui.roots.detail.components.cards.CitedBodyText
import com.quranwords.ui.theme.AppMotion
import com.quranwords.ui.theme.ShapeMedium
import com.quranwords.ui.theme.ShapeSmall

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MorphologyBottomSheet(
    word: WordToken,
    ayah: Ayah?,
    sheetState: SheetState,
    onDismiss: () -> Unit,
    onNavigateToRoot: (rootId: Int, rootText: String) -> Unit,
    modifier: Modifier = Modifier,
    aiSummary: String? = null,
    aiExtended: AiExtendedSummary? = null,
    aiModel: String? = null,
    aiGeneratedAt: String? = null,
    isAiLoading: Boolean = false,
    fontSize: Float = 24f
) {
    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = sheetState,
        containerColor = MaterialTheme.colorScheme.surface,
        modifier = modifier.testTag("morphology_bottom_sheet")
    ) {
        // Content appears instantly with sheet slide (no double animation) - fixes lag feeling
        // Scrollable so long summaries show in full instead of clipping
        // at the sheet height (sheet opens fully expanded; content scrolls).
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 20.dp, vertical = 12.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            // Prominent Word Header
            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(4.dp)
            ) {
                Text(
                    text = word.text,
                    fontSize = 36.sp,
                    lineHeight = 52.sp,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.primary,
                    textAlign = TextAlign.Center
                )
                if (word.translation.isNotBlank()) {
                    Text(
                        text = word.translation,
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        textAlign = TextAlign.Center
                    )
                }
                if (ayah != null) {
                    Text(
                        text = stringResource(R.string.morpho_ayah, ayah.ayah),
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.primary
                    )
                }
            }

            HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant)

            // Root Card & Action — M3 ShapeMedium 16dp + balanced 250ms
            if (!word.rootText.isNullOrBlank()) {
                Card(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(ShapeMedium)
                        .border(1.dp, MaterialTheme.colorScheme.outlineVariant, ShapeMedium)
                        .clickable {
                            onNavigateToRoot(word.rootId ?: 0, word.rootText)
                            onDismiss()
                        }
                        .animateContentSize(
                            animationSpec = tween(
                                durationMillis = AppMotion.DurationMedium,
                                easing = AppMotion.EasingStandard
                            )
                        )
                        .testTag("open_root_${word.rootText}"),
                    shape = ShapeMedium,
                    colors = CardDefaults.cardColors(
                        containerColor = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.5f)
                    )
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(14.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column {
                            Text(
                                text = stringResource(R.string.morpho_root),
                                style = MaterialTheme.typography.labelSmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                            Text(
                                text = "[ ${word.rootText.toCharArray().joinToString(" ")} ]",
                                style = MaterialTheme.typography.titleLarge,
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.primary
                            )
                        }

                        Button(
                            onClick = {
                                onNavigateToRoot(word.rootId ?: 0, word.rootText)
                                onDismiss()
                            },
                            colors = ButtonDefaults.buttonColors(
                                containerColor = MaterialTheme.colorScheme.primary
                            ),
                            shape = ShapeSmall
                        ) {
                            Text(
                                text = stringResource(R.string.morpho_details),
                                style = MaterialTheme.typography.labelMedium
                            )
                        }
                    }
                }
            }

            // AI Summary Card - stateless, data supplied by ViewModel (Clean Arch §10.3)
            AiSummarySection(
                word = word,
                aiSummary = aiSummary,
                summaryRefs = aiExtended?.summaryRefs.orEmpty(),
                aiModel = aiModel,
                aiDate = aiGeneratedAt,
                isLoading = isAiLoading,
                fontSize = fontSize
            )

            // Extended-section foldable cards below the summary — same order
            // as the root detail tab (فروق، شبكة، حسي، تطور، تشريح).
            // Single expanded-tags list (resets per word) instead of one
            // remember per card, so blank-filtering can't shift saved state.
            if (!isAiLoading && aiExtended != null) {
                var expandedSections by rememberSaveable(word.rootId) {
                    mutableStateOf(emptyList<String>())
                }
                val sections = listOf(
                    Triple(aiExtended.furuq, R.string.ai_section_furuq, "furuq"),
                    Triple(aiExtended.shabaka, R.string.ai_section_shabaka, "shabaka"),
                    Triple(aiExtended.hissi, R.string.ai_section_hissi, "hissi"),
                    Triple(aiExtended.tatawwur, R.string.ai_section_tatawwur, "tatawwur"),
                    Triple(aiExtended.tashrih, R.string.ai_section_tashrih, "tashrih")
                )
                sections.filter { it.first.isNotBlank() }.forEach { (section, titleRes, tag) ->
                    val expanded = tag in expandedSections
                    AiSectionCard(
                        title = stringResource(titleRes),
                        body = section.text?.trim().orEmpty(),
                        refs = section.refs,
                        refsLabel = stringResource(R.string.ai_refs_label),
                        fontSize = fontSize,
                        expanded = expanded,
                        onToggle = {
                            expandedSections = if (expanded) expandedSections - tag
                            else expandedSections + tag
                        },
                        testTagSuffix = "morpho_$tag"
                    )
                }
            }

            Spacer(modifier = Modifier.height(16.dp))
        }
    }
}

@Composable
private fun AiSummarySection(
    word: WordToken,
    aiSummary: String?,
    summaryRefs: List<String>,
    aiModel: String?,
    aiDate: String?,
    isLoading: Boolean,
    fontSize: Float
) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .heightIn(min = 90.dp)
            .clip(ShapeMedium)
            .background(MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f))
            .border(1.dp, MaterialTheme.colorScheme.outlineVariant, ShapeMedium)
            .padding(14.dp),
        verticalArrangement = Arrangement.spacedBy(10.dp)
    ) {
        Text(
            text = stringResource(R.string.morpho_ai_title),
            style = MaterialTheme.typography.titleSmall,
            fontWeight = FontWeight.Bold,
            color = MaterialTheme.colorScheme.onSurface
        )

        when {
            isLoading -> {
                Box(
                    modifier = Modifier.fillMaxWidth().padding(vertical = 12.dp),
                    contentAlignment = Alignment.Center
                ) {
                    CircularProgressIndicator(
                        modifier = Modifier.size(24.dp),
                        color = MaterialTheme.colorScheme.primary,
                        strokeWidth = 2.dp
                    )
                }
            }
            !aiSummary.isNullOrBlank() -> {
                CitedBodyText(
                    body = aiSummary.trim(),
                    refs = summaryRefs,
                    refsLabel = stringResource(R.string.ai_refs_label),
                    fontSize = fontSize,
                    testTagPrefix = "ai_ref_morpho_summary"
                )
                // Model + date chips - stacked vertically to prevent wrapping, with time included
                if (!aiModel.isNullOrBlank() || !aiDate.isNullOrBlank()) {
                    Column(
                        modifier = Modifier.fillMaxWidth(),
                        verticalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        aiModel?.let { model ->
                            Box(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .clip(RoundedCornerShape(6.dp))
                                    .background(MaterialTheme.colorScheme.surface)
                                    .border(1.dp, MaterialTheme.colorScheme.outlineVariant, RoundedCornerShape(6.dp))
                                    .padding(horizontal = 8.dp, vertical = 6.dp)
                            ) {
                                Text(
                                    text = model,
                                    style = MaterialTheme.typography.labelSmall,
                                    fontWeight = FontWeight.Medium,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                        }
                        aiDate?.let { rawDate ->
                            val dateTimeFormatted = cleanAiDate(rawDate)
                            Box(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .clip(RoundedCornerShape(6.dp))
                                    .background(MaterialTheme.colorScheme.surface)
                                    .border(1.dp, MaterialTheme.colorScheme.outlineVariant, RoundedCornerShape(6.dp))
                                    .padding(horizontal = 8.dp, vertical = 6.dp)
                            ) {
                                Text(
                                    text = dateTimeFormatted,
                                    style = MaterialTheme.typography.labelSmall,
                                    fontWeight = FontWeight.Medium,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                        }
                    }
                }
            }
            else -> {
                Text(
                    text = if (word.rootId == null) stringResource(R.string.morpho_no_root)
                    else stringResource(R.string.morpho_no_summary),
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }
    }
}
