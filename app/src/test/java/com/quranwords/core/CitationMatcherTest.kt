package com.quranwords.core

import com.quranwords.core.util.CitationMatcher
import com.quranwords.core.util.CitationMatcher.Segment
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class CitationMatcherTest {

    @Test
    fun `numbers follow appearance order not refs order`() {
        val refs = listOf("الصحاح", "مقاييس اللغة")
        val body = "قال [مَقَايِيسِ اللُّغَةِ] ثم [الصِّحَاحِ]"
        val cited = CitationMatcher.cite(body, refs)
        assertEquals(
            listOf(
                Segment.Text("قال "),
                Segment.Cluster(listOf(1)),
                Segment.Text(" ثم "),
                Segment.Cluster(listOf(2))
            ),
            cited.segments
        )
        assertEquals(listOf("مقاييس اللغة", "الصحاح"), cited.orderedRefs)
    }

    @Test
    fun `adjacent markers merge into one cluster`() {
        val refs = listOf("مقاييس اللغة", "الصحاح")
        val cited = CitationMatcher.cite("[مقاييس اللغة] [الصحاح] نص", refs)
        assertEquals(
            listOf(Segment.Cluster(listOf(1, 2)), Segment.Text(" نص")),
            cited.segments
        )
        assertEquals("[١، ٢]", CitationMatcher.formatCluster(listOf(1, 2)))
    }

    @Test
    fun `repeated marker reuses same number`() {
        val refs = listOf("الصحاح")
        val cited = CitationMatcher.cite("[الصحاح] وسط [الصِّحَاحِ]", refs)
        assertEquals(
            listOf(
                Segment.Cluster(listOf(1)),
                Segment.Text(" وسط "),
                Segment.Cluster(listOf(1))
            ),
            cited.segments
        )
        assertEquals(listOf("الصحاح"), cited.orderedRefs)
    }

    @Test
    fun `unmatched bracket stays literal`() {
        val refs = listOf("الصحاح")
        val cited = CitationMatcher.cite("نص [كتاب مجهول] ثم [الصحاح]", refs)
        assertEquals(
            listOf(
                Segment.Text("نص [كتاب مجهول] ثم "),
                Segment.Cluster(listOf(1))
            ),
            cited.segments
        )
    }

    @Test
    fun `empty bracket stays literal`() {
        val cited = CitationMatcher.cite("نص [] ذيل", listOf("الصحاح"))
        assertEquals(listOf(Segment.Text("نص [] ذيل")), cited.segments)
    }

    @Test
    fun `no brackets returns plain text with refs order kept`() {
        val refs = listOf("الصحاح", "لسان العرب")
        val cited = CitationMatcher.cite("نص قديم بلا أقواس", refs)
        assertEquals(listOf(Segment.Text("نص قديم بلا أقواس")), cited.segments)
        assertEquals(refs, cited.orderedRefs)
    }

    @Test
    fun `empty refs returns body untouched`() {
        val cited = CitationMatcher.cite("نص [الصحاح]", emptyList())
        assertEquals(listOf(Segment.Text("نص [الصحاح]")), cited.segments)
    }

    @Test
    fun `uncited refs appended after cited`() {
        val refs = listOf("لسان العرب", "الصحاح", "المفردات")
        val cited = CitationMatcher.cite("ورد في [الصحاح]", refs)
        assertEquals(listOf("الصحاح", "لسان العرب", "المفردات"), cited.orderedRefs)
    }

    @Test
    fun `eastern digits convert correctly`() {
        assertEquals("١", CitationMatcher.toEasternArabicDigits(1))
        assertEquals("١٢", CitationMatcher.toEasternArabicDigits(12))
        assertEquals("٢٥", CitationMatcher.toEasternArabicDigits(25))
    }

    @Test
    fun `renderPlain inlines clusters and keeps literals`() {
        val refs = listOf("مقاييس اللغة", "الصحاح")
        val cited = CitationMatcher.cite("[مقاييس اللغة] [الصحاح] و[غريب]", refs)
        assertEquals("[١، ٢] و[غريب]", CitationMatcher.renderPlain(cited))
        assertTrue(cited.orderedRefs.size == 2)
    }

    @Test
    fun `reported sample marker strictly matches plain ref`() {
        assertEquals(
            "مقاييس اللغه",
            CitationMatcher.cleanToken("[مَقَايِيسِ اللُّغَةِ]".removeSurrounding("[", "]"))
        )
        assertEquals("مقاييس اللغه", CitationMatcher.cleanToken("مقاييس اللغة"))
        val cited = CitationMatcher.cite(
            "أصله عند [مَقَايِيسِ اللُّغَةِ] باب",
            listOf("مقاييس اللغة")
        )
        assertEquals(
            listOf(
                Segment.Text("أصله عند "),
                Segment.Cluster(listOf(1)),
                Segment.Text(" باب")
            ),
            cited.segments
        )
    }

    @Test
    fun `nbsp inside marker matches plain ref`() {
        val bodyWithNbsp = "قال [مقاييس" + '\u00A0' + "اللغة] نص"
        val cited = CitationMatcher.cite(bodyWithNbsp, listOf("مقاييس اللغة"))
        assertEquals(1, cited.segments.filterIsInstance<Segment.Cluster>().size)
        assertEquals(listOf("مقاييس اللغة"), cited.orderedRefs)
        assertEquals("", CitationMatcher.cleanToken('\u00A0'.toString()))
    }

    @Test
    fun `zero-width chars inside marker are ignored`() {
        val cited = CitationMatcher.cite(
            "قال [مقاييس\u200B اللغة] نص",
            listOf("مقاييس اللغة")
        )
        assertEquals(1, cited.segments.filterIsInstance<Segment.Cluster>().size)
    }

    @Test
    fun `trailing case endings match plain ref`() {
        val cited = CitationMatcher.cite(
            "باب [الشِّعْرٌ] و[العِلْمَ] و[الفِطْنَةِ]",
            listOf("الشعر", "العلم", "الفطنة")
        )
        assertEquals(3, cited.segments.filterIsInstance<Segment.Cluster>().size)
        assertEquals(listOf("الشعر", "العلم", "الفطنة"), cited.orderedRefs)
    }

    @Test
    fun `edge punctuation around marker is trimmed`() {
        assertEquals("الصحاح", CitationMatcher.cleanToken("، الصحاح؛"))
        assertEquals("", CitationMatcher.cleanToken(" "))
        val cited = CitationMatcher.cite(
            "نص [، الصحاح؛] ذيل",
            listOf("الصحاح")
        )
        assertEquals(
            listOf(
                Segment.Text("نص "),
                Segment.Cluster(listOf(1)),
                Segment.Text(" ذيل")
            ),
            cited.segments
        )
    }

    // --- Session: numbering must stay global across Markdown blocks -----
    //
    // The renderer splits a body into blocks (paragraphs/bullets) and cites
    // each one separately. Numbering must still follow first appearance across
    // the whole body, otherwise the screen and the clipboard disagree.

    @Test
    fun `session keeps numbering continuous across blocks`() {
        val session = CitationMatcher.Session(listOf("الصحاح", "المقاييس", "التاج"))

        val first = session.citeFragment("قال [الصحاح] ثم")
        val second = session.citeFragment("ورد [المقاييس] و [التاج] و [الصحاح]")

        assertEquals(
            listOf(Segment.Text("قال "), Segment.Cluster(listOf(1)), Segment.Text(" ثم")),
            first
        )
        assertEquals(
            listOf(
                Segment.Text("ورد "),
                Segment.Cluster(listOf(2)),
                Segment.Text(" و "),
                Segment.Cluster(listOf(3)),
                Segment.Text(" و "),
                Segment.Cluster(listOf(1))
            ),
            second
        )
        // Ordered by appearance across BOTH blocks, never per block.
        assertEquals(listOf("الصحاح", "المقاييس", "التاج"), session.orderedRefs)
    }

    @Test
    fun `session never cited refs are appended last`() {
        val session = CitationMatcher.Session(listOf("التاج", "الصحاح"))
        session.citeFragment("نص [الصحاح]")
        assertEquals(listOf("الصحاح", "التاج"), session.orderedRefs)
    }

    @Test
    fun `citeRanges offsets address the scanned string`() {
        val source = "نص [الصحاح] ذيل"
        val fragments = CitationMatcher.Session(listOf("الصحاح")).citeRanges(source)

        assertEquals(3, fragments.size)
        val head = fragments[0] as CitationMatcher.Fragment.Plain
        val cite = fragments[1] as CitationMatcher.Fragment.Cite
        val tail = fragments[2] as CitationMatcher.Fragment.Plain
        assertEquals("نص ", source.substring(head.start, head.end))
        assertEquals("[الصحاح]", source.substring(cite.start, cite.end))
        assertEquals(listOf(1), cite.numbers)
        assertEquals(" ذيل", source.substring(tail.start, tail.end))
    }

    @Test
    fun `citeRanges merges adjacent markers and swallows the gap`() {
        val source = "[مقاييس اللغة] [الصحاح] نص"
        val fragments = CitationMatcher.Session(listOf("مقاييس اللغة", "الصحاح"))
            .citeRanges(source)

        val cite = fragments.first() as CitationMatcher.Fragment.Cite
        assertEquals(listOf(1, 2), cite.numbers)
        assertEquals("[مقاييس اللغة] [الصحاح]", source.substring(cite.start, cite.end))
        assertEquals(" نص", source.substring(cite.end, source.length))
    }

    @Test
    fun `citeRanges returns one plain range when there are no refs`() {
        val fragments = CitationMatcher.Session(emptyList()).citeRanges("نص كامل")
        assertEquals(
            listOf(CitationMatcher.Fragment.Plain(0, "نص كامل".length)),
            fragments
        )
    }

    @Test
    fun `citeRanges leaves unresolved brackets literal`() {
        val source = "نص [مجهول] ثم"
        val fragments = CitationMatcher.Session(listOf("الصحاح")).citeRanges(source)

        assertTrue(fragments.all { it is CitationMatcher.Fragment.Plain })
        assertEquals(source, fragments.joinToString("") { source.substring(it.start, it.end) })
    }
}
