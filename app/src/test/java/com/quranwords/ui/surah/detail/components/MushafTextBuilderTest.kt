package com.quranwords.ui.surah.detail.components

import com.quranwords.core.util.AyahRef
import com.quranwords.domain.model.Ayah
import com.quranwords.domain.model.WordToken
import org.junit.Assert.assertEquals
import org.junit.Test

class MushafTextBuilderTest {

    private fun word(id: Int, text: String) = WordToken(
        wordId = id, wordAyahId = id, position = 1, text = text, textClean = text, translation = ""
    )

    private fun ayah(num: Int, surah: Int, words: List<WordToken> = emptyList(), plain: String = "نص") = Ayah(
        id = surah * 1000 + num, surah = surah, ayah = num,
        textUthmani = plain, textUthmaniPlain = plain, textImlaei = plain,
        wordCount = words.size, words = words
    )

    @Test
    fun character_equivalence_between_render_and_fit_builders() {
        val ayat = listOf(
            ayah(1, 2, listOf(word(10, "كَتَبَ"), word(11, "اللَّهُ"))),
            ayah(2, 2, plain = "وَكَتَبَ"),
            ayah(1, 3, listOf(word(20, "قل")))
        )
        val options = MushafTextOptions(
            includeAnnotations = true,
            selectedAyahs = setOf(AyahRef(2, 1)),
            bookmarkedAyahs = setOf(AyahRef(3, 1)),
            focusedWordId = 11,
            endMarkerColor = androidx.compose.ui.graphics.Color.Red,
            bookmarkMarkerColor = androidx.compose.ui.graphics.Color.Blue,
            selectionBackground = androidx.compose.ui.graphics.Color.Green,
            bookmarkBackground = androidx.compose.ui.graphics.Color.Yellow,
            wordFocusHighlight = androidx.compose.ui.graphics.Color.Gray
        )

        val rendered = buildMushafAnnotatedString(ayat, 24f, options)
        val fitted = mushafBlockLayoutText(ayat, 24f)

        assertEquals(fitted.text, rendered.text)
        assertEquals(3, rendered.getStringAnnotations(FLOW_AYAH_TAG, 0, rendered.length).size)
    }

    @Test
    fun character_equivalence_across_page_shapes_and_paint_options() {
        val cases = listOf(
            listOf(ayah(1, 1, listOf(word(1, "بِسْمِ")))),
            listOf(ayah(4, 2, plain = "ذٰلِكَ")),
            listOf(ayah(1, 2), ayah(2, 2), ayah(3, 2)),
            listOf(ayah(1, 2), ayah(1, 3), ayah(2, 3)),
            listOf(ayah(286, 2, plain = "نُورٌ"))
        )
        cases.forEach { ayat ->
            val plain = buildMushafAnnotatedString(ayat, 24f)
            val painted = buildMushafAnnotatedString(
                ayat, 24f,
                MushafTextOptions(
                    includeAnnotations = true,
                    selectedAyahs = setOf(AyahRef(ayat.first().surah, ayat.first().ayah)),
                    bookmarkedAyahs = setOf(AyahRef(ayat.last().surah, ayat.last().ayah)),
                    focusedWordId = ayat.first().words.firstOrNull()?.wordAyahId,
                    endMarkerColor = androidx.compose.ui.graphics.Color.Red,
                    bookmarkMarkerColor = androidx.compose.ui.graphics.Color.Blue,
                    selectionBackground = androidx.compose.ui.graphics.Color.Green,
                    bookmarkBackground = androidx.compose.ui.graphics.Color.Yellow,
                    wordFocusHighlight = androidx.compose.ui.graphics.Color.Gray
                )
            )
            assertEquals(mushafBlockLayoutText(ayat, 24f).text, plain.text)
            assertEquals(plain.text, painted.text)
        }
    }
}
