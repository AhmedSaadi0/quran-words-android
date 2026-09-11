package io.github.ahmedsaadi0.quranwords.ui.mushaf.components

import androidx.compose.foundation.gestures.awaitEachGesture
import androidx.compose.foundation.gestures.awaitFirstDown
import androidx.compose.foundation.gestures.waitForUpOrCancellation
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.Alignment
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalLayoutDirection
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.TextLayoutResult
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.withStyle
import androidx.compose.ui.unit.LayoutDirection
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import io.github.ahmedsaadi0.quranwords.core.util.toEasternArabicDigits
import io.github.ahmedsaadi0.quranwords.domain.model.Ayah
import io.github.ahmedsaadi0.quranwords.domain.model.WordToken
import io.github.ahmedsaadi0.quranwords.ui.mushaf.model.MushafSegment
import io.github.ahmedsaadi0.quranwords.ui.theme.AmiriQuran

private const val ANNOTATION_WORD = "word"
private const val ANNOTATION_MARKER = "marker"

/**
 * Single builder for page text, shared by the renderer and the text-fit
 * measurer so measured heights always match rendered layout.
 */
internal fun buildMushafAnnotated(
    ayat: List<Ayah>,
    markerStyle: SpanStyle
): AnnotatedString {
    return buildAnnotatedString {
        ayat.forEach { ayah ->
            if (ayah.words.isNotEmpty()) {
                ayah.words.forEach { word ->
                    pushStringAnnotation(ANNOTATION_WORD, "${word.wordAyahId}:${word.position}")
                    append(word.text)
                    pop()
                    append(" ")
                }
            } else {
                append(ayah.textUthmani)
                append(" ")
            }
            pushStringAnnotation(ANNOTATION_MARKER, "${ayah.surah}:${ayah.ayah}")
            withStyle(markerStyle) {
                append("﴿${ayah.ayah.toEasternArabicDigits()}﴾")
            }
            pop()
            append(" ")
        }
    }
}

/**
 * Continuous justified page body. Each [MushafSegment.TextRun] renders as one
 * AnnotatedString — the text engine owns line-breaking, so words flow like
 * print with no chip fragmentation. Taps resolve through [TextLayoutResult]
 * hit-testing: word ranges open morphology, ayah markers toggle bookmarks.
 *
 * If a segment overflows the available height (small screens, large fonts),
 * the body becomes scrollable as a fallback instead of clipping text.
 */
@Composable
fun MushafPageBody(
    segments: List<MushafSegment>,
    fontSize: Float,
    compactBanners: Boolean,
    onWordClick: (WordToken, Ayah) -> Unit,
    onMarkerClick: (surahId: Int, ayah: Int) -> Unit,
    onEmptyTap: () -> Unit,
    modifier: Modifier = Modifier
) {
    var overflow by remember(segments) { mutableStateOf(false) }
    Column(
        modifier = modifier
            .fillMaxWidth()
            .fillMaxHeight()
            .verticalScroll(rememberScrollState(), enabled = overflow)
            .testTag("mushaf_page_body"),
        // Sparse pages center vertically inside the frame; dense pages fill it
        // (centering is a no-op). Tight 4dp gaps reclaim space on transition
        // pages. Overflow keeps top-anchored scroll as fail-safe.
        verticalArrangement = Arrangement.spacedBy(
            4.dp,
            if (overflow) Alignment.Top else Alignment.CenterVertically
        )
    ) {
        segments.forEach { segment ->
            when (segment) {
                is MushafSegment.SurahStart -> MushafSurahBanner(banner = segment, compact = compactBanners)
                is MushafSegment.TextRun -> MushafTextRun(
                    ayat = segment.ayat,
                    fontSize = fontSize,
                    onWordClick = onWordClick,
                    onMarkerClick = onMarkerClick,
                    onEmptyTap = onEmptyTap,
                    onOverflow = { overflow = true }
                )
            }
        }
    }
}

@Composable
private fun MushafTextRun(
    ayat: List<Ayah>,
    fontSize: Float,
    onWordClick: (WordToken, Ayah) -> Unit,
    onMarkerClick: (surahId: Int, ayah: Int) -> Unit,
    onEmptyTap: () -> Unit,
    onOverflow: () -> Unit,
    modifier: Modifier = Modifier
) {
    val markerStyle = SpanStyle(color = MaterialTheme.colorScheme.tertiary, fontWeight = FontWeight.Bold)
    val annotated = remember(ayat, markerStyle) {
        buildMushafAnnotated(ayat, markerStyle)
    }
    val lookup = remember(ayat) {
        buildMap {
            ayat.forEach { ayah ->
                ayah.words.forEach { word ->
                    put("${word.wordAyahId}:${word.position}", word to ayah)
                }
            }
        }
    }
    var layoutResult by remember { mutableStateOf<TextLayoutResult?>(null) }

    CompositionLocalProvider(LocalLayoutDirection provides LayoutDirection.Rtl) {
        Text(
            text = annotated,
            fontFamily = AmiriQuran,
            fontSize = fontSize.sp,
            lineHeight = (fontSize * 1.75f).sp,
            textAlign = TextAlign.Justify,
            color = MaterialTheme.colorScheme.onSurface,
            onTextLayout = { result ->
                layoutResult = result
                if (result.didOverflowHeight) onOverflow()
            },
            modifier = modifier
                .fillMaxWidth()
                .pointerInput(layoutResult) {
                    // Custom tap detection (not detectTapGestures): a confirmed tap
                    // consumes down+up so the page-level immersive-toggle detector
                    // never double-fires. Pager drags cancel before up → unconsumed.
                    awaitEachGesture {
                        val down = awaitFirstDown()
                        val up = waitForUpOrCancellation() ?: return@awaitEachGesture
                        val result = layoutResult ?: return@awaitEachGesture
                        down.consume()
                        up.consume()
                        val offset = result.getOffsetForPosition(up.position)
                        val annotation = annotated.getStringAnnotations(offset, offset).firstOrNull()
                        if (annotation == null) {
                            onEmptyTap()
                            return@awaitEachGesture
                        }
                        when (annotation.tag) {
                            ANNOTATION_WORD -> lookup[annotation.item]?.let { (word, ayah) ->
                                onWordClick(word, ayah)
                            } ?: onEmptyTap()
                            ANNOTATION_MARKER -> annotation.item.split(":").let { parts ->
                                if (parts.size == 2) {
                                    onMarkerClick(parts[0].toIntOrNull() ?: 0, parts[1].toIntOrNull() ?: 0)
                                } else {
                                    onEmptyTap()
                                }
                            }
                        }
                    }
                }
        )
    }
}
