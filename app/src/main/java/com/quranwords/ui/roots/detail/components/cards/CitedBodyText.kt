package com.quranwords.ui.roots.detail.components.cards

import androidx.compose.foundation.background
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.material3.ColorScheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.TextLayoutResult
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.quranwords.R
import com.quranwords.core.util.CitationMatcher
import com.quranwords.core.util.MdBlock
import com.quranwords.core.util.MarkdownBlocks
import com.quranwords.ui.theme.ArabicTextScale
import com.quranwords.ui.theme.ArabicTextScale.asExplanation
import com.quranwords.ui.theme.ShapeSmall
import kotlinx.coroutines.delay

/**
 * Cited body + numbered refs renderer shared by [AiSectionCard], the root
 * detail header subtitle and the morphology sheet summary.
 *
 * Inline `[...]` markers become tappable superscript clusters (`[١، ٢]`,
 * numbered by first appearance); tapping highlights the matching ref row.
 * Texts without markers (or with empty refs) render exactly as before.
 * Cite computation is memoized on `(body, refs)` for scrolling headers.
 *
 * **Markdown (opt-in via [MarkdownBlocks]).** Only ~8.5% of `root_ai_summary`
 * rows contain markup; the rest is legacy prose. Parsing therefore has to be a
 * no-op for plain text: a body with zero markers yields exactly one
 * [MdBlock.Paragraph], which takes the very same single-`Text` path the
 * pre-markdown renderer used. Bullets become a `Row` so long Arabic items get
 * a true hanging indent instead of wrapping under the marker.
 *
 * **Citation numbering stays global.** One [CitationMatcher.Session] is created
 * per body and threaded through every block in document order, so splitting
 * into blocks never restarts the `[١]`, `[٢]` sequence.
 *
 * @param collapsedBlockLimit when set and [isExpanded] is false, only the first
 *   N blocks render and a "read more" toggle appears. Used by the collapsing
 *   header to bound its own height; `null` (the default) disables clamping.
 */
@Composable
fun CitedBodyText(
    body: String,
    refs: List<String>,
    refsLabel: String,
    fontSize: Float,
    testTagPrefix: String,
    modifier: Modifier = Modifier,
    collapsedBlockLimit: Int? = null,
    isExpanded: Boolean = true,
    onToggleExpanded: (() -> Unit)? = null
) {
    val blocks = remember(body) { MarkdownBlocks.parse(body) }
    val clamped = collapsedBlockLimit != null && !isExpanded && blocks.size > collapsedBlockLimit
    val visibleBlocks = if (clamped) blocks.take(collapsedBlockLimit) else blocks
    // One session for the whole body, cited up-front in document order:
    // numbering follows first appearance across blocks, and `orderedRefs`
    // below is complete before any child reads it. Children stay pure —
    // they render pre-computed fragments and never mutate the session.
    val session = remember(body, mutableListOf<String>()) { CitationMatcher.Session(mutableListOf<String>()) }
    val fragmentsByBlock = remember(visibleBlocks, session) {
        visibleBlocks.map { block -> session.citeRanges(block.text) }
    }
    val orderedRefs = session.orderedRefs

    val textScale = ArabicTextScale.calculate(fontSize)
    val bodyStyle = MaterialTheme.typography.bodyMedium.asExplanation(textScale)
    val colors = MaterialTheme.colorScheme
    var highlightedRef by remember { mutableIntStateOf(0) }
    LaunchedEffect(highlightedRef) {
        if (highlightedRef != 0) {
            delay(1500)
            highlightedRef = 0
        }
    }

    Column(
        modifier = modifier,
        verticalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        visibleBlocks.forEachIndexed { index, block ->
            CitedBlockText(
                block = block,
                fragments = fragmentsByBlock[index],
                style = bodyStyle,
                colors = colors,
                onCiteTapped = { highlightedRef = it },
                modifier = Modifier.testTag("${testTagPrefix}_block_$index")
            )
        }
        if (clamped) {
            TextButton(
                onClick = { onToggleExpanded?.invoke() },
                modifier = Modifier.testTag("${testTagPrefix}_expand")
            ) {
                Text(
                    text = stringResource(R.string.read_more),
                    style = MaterialTheme.typography.labelLarge,
                    fontWeight = FontWeight.Bold,
                    color = colors.primary
                )
            }
        }
        // Refs were numbered while pre-citing above, so this list is complete.
        if (orderedRefs.isNotEmpty()) {
            Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                Text(
                    text = refsLabel,
                    style = MaterialTheme.typography.labelSmall,
                    fontWeight = FontWeight.Bold,
                    color = colors.onSurfaceVariant
                )
                orderedRefs.forEachIndexed { index, ref ->
                    val number = index + 1
                    Text(
                        text = "${CitationMatcher.toEasternArabicDigits(number)}. $ref",
                        style = MaterialTheme.typography.bodySmall.asExplanation(textScale),
                        color = colors.onSurfaceVariant,
                        modifier = Modifier
                            .fillMaxWidth()
                            .testTag("${testTagPrefix}_$index")
                            .background(
                                if (highlightedRef == number) colors.primaryContainer else colors.surface,
                                ShapeSmall
                            )
                            .padding(horizontal = 6.dp, vertical = 2.dp)
                    )
                }
            }
        }
    }
}

/**
 * Renders one [MdBlock]. Paragraphs use a bare `Text` (identical to the legacy
 * path); bullets use a `Row` with a fixed marker column so the body text gets
 * `Modifier.weight(1f)` — a real hanging indent, which matters because a single
 * Arabic bullet can run to several hundred characters.
 */
@Composable
private fun CitedBlockText(
    block: MdBlock,
    fragments: List<CitationMatcher.Fragment>,
    style: TextStyle,
    colors: ColorScheme,
    onCiteTapped: (Int) -> Unit,
    modifier: Modifier = Modifier
) {
    val annotated: AnnotatedString = remember(block, fragments, style, colors) {
        buildBlockString(block, fragments, style, colors)
    }
    var layout by remember { mutableStateOf<TextLayoutResult?>(null) }

    val tapModifier = Modifier.pointerInput(annotated) {
        detectTapGestures { offset ->
            val textLayout = layout ?: return@detectTapGestures
            val textOffset = textLayout.getOffsetForPosition(offset)
            annotated.getStringAnnotations("cite", textOffset, textOffset)
                .firstOrNull()
                ?.let { onCiteTapped(it.item.toIntOrNull() ?: 0) }
        }
    }

    when (block) {
        is MdBlock.Paragraph -> Text(
            text = annotated,
            style = style,
            color = colors.onSurface,
            onTextLayout = { layout = it },
            modifier = modifier.then(tapModifier)
        )
        is MdBlock.Bullet -> {
            val indent = if (block.depth > 0) 16.dp else 0.dp
            Row(
                modifier = modifier.then(tapModifier),
                verticalAlignment = Alignment.Top
            ) {
                Text(
                    text = MarkdownBlocks.glyphFor(block.depth),
                    style = style,
                    color = if (block.depth > 0) colors.onSurfaceVariant else colors.primary,
                    modifier = Modifier.width(14.dp + indent)
                )
                Text(
                    text = annotated,
                    style = style,
                    color = colors.onSurface,
                    onTextLayout = { layout = it },
                    modifier = Modifier.weight(1f)
                )
            }
        }
    }
}
