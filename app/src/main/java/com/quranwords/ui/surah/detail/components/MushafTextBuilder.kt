package com.quranwords.ui.surah.detail.components

import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.withStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.sp
import com.quranwords.core.util.AyahRef
import com.quranwords.core.util.sanitizeUthmanicText
import com.quranwords.core.util.toEasternArabicDigits
import com.quranwords.domain.model.Ayah

internal const val FLOW_WORD_TAG = "word_id"
internal const val FLOW_MARKER_TAG = "ayah_marker"
internal const val FLOW_AYAH_TAG = "ayah_num"

/**
 * Paint-only options. They never change characters or line breaks, so the
 * renderer and the page-fit measurement can use the same character builder
 * with different options (P3-1).
 */
internal data class MushafTextOptions(
    val includeAnnotations: Boolean = false,
    val selectedAyahs: Set<AyahRef> = emptySet(),
    val bookmarkedAyahs: Set<AyahRef> = emptySet(),
    val focusedWordId: Int? = null,
    val endMarkerColor: Color? = null,
    val bookmarkMarkerColor: Color? = null,
    val selectionBackground: Color? = null,
    val bookmarkBackground: Color? = null,
    val wordFocusHighlight: Color? = null
)

/**
 * Single source of truth for mushaf characters. Selection/bookmark/focus
 * styles and hit-test annotations are opt-in; the character stream and the
 * layout-affecting marker style are always identical.
 */
internal fun buildMushafAnnotatedString(
    ayat: List<Ayah>,
    fontSize: Float,
    options: MushafTextOptions = MushafTextOptions()
): AnnotatedString = buildAnnotatedString {
    ayat.forEachIndexed { ayahIndex, ayah ->
        // Inter-ayah space belongs to neither range: highlights stay isolated.
        if (ayahIndex > 0) append(" ")
        val ayahStart = length
        val ref = AyahRef(ayah.surah, ayah.ayah)
        val isSelected = ref in options.selectedAyahs
        val isBookmarked = ref in options.bookmarkedAyahs

        if (ayah.words.isNotEmpty()) {
            ayah.words.forEachIndexed { wordIndex, word ->
                if (wordIndex > 0) append(" ")
                val wordStart = length
                append(sanitizeUthmanicText(word.text))
                if (options.includeAnnotations) {
                    addStringAnnotation(FLOW_WORD_TAG, word.wordAyahId.toString(), wordStart, length)
                }
                if (options.focusedWordId == word.wordAyahId && options.wordFocusHighlight != null) {
                    addStyle(SpanStyle(background = options.wordFocusHighlight), wordStart, length)
                }
            }
        } else {
            append(sanitizeUthmanicText(ayah.textUthmani))
        }

        withStyle(
            SpanStyle(
                color = (if (isBookmarked) options.bookmarkMarkerColor else options.endMarkerColor)
                    ?: Color.Unspecified,
                fontSize = (fontSize * 0.9f).sp,
                fontWeight = FontWeight.Bold
            )
        ) {
            // NBSP keeps the marker attached to the preceding word on the line.
            append("\u00A0")
            val markerStart = length
            append("﴿${ayah.ayah.toEasternArabicDigits()}﴾")
            if (options.includeAnnotations) {
                addStringAnnotation(FLOW_MARKER_TAG, ayah.ayah.toString(), markerStart, length)
            }
        }
        if (options.includeAnnotations) {
            addStringAnnotation(FLOW_AYAH_TAG, ayah.ayah.toString(), ayahStart, length)
        }
        when {
            isSelected && options.selectionBackground != null ->
                addStyle(SpanStyle(background = options.selectionBackground), ayahStart, length)
            isBookmarked && options.bookmarkBackground != null ->
                addStyle(SpanStyle(background = options.bookmarkBackground), ayahStart, length)
        }
    }
}
