package com.quranwords.core

import com.quranwords.core.util.MdBlock
import com.quranwords.core.util.MdSpan
import com.quranwords.core.util.MarkdownBlocks
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * Parser contract tests for the `root_ai_summary` Markdown dialect.
 *
 * The first test is the regression guard that matters most: ~91.5% of the 1,642
 * roots are legacy plain prose and must parse to exactly one [MdBlock.Paragraph]
 * whose flattened text equals the source, so the renderer keeps taking the same
 * single-`Text` path it took before Markdown support existed.
 */
class MarkdownBlocksTest {

    // --- Graceful degradation -------------------------------------------

    @Test
    fun `legacy plain prose parses to exactly one identical paragraph`() {
        // Real `tatawwur` row (root_id = 1), verbatim from the shipped DB.
        val raw = "يَرْجِعُ البابُ إِلَى العُلُوِّ الحِسِّيِّ المَحْسُوسِ سَقْفًا " +
            "يُظِلُّ وَمَطَرًا يَنْزِلُ مِن فَوْقُ، ثُمَّ انْتَقَلَ فِي عُرْفِ اللِّسانِ " +
            "إِلَى الرِّفْعَةِ المَعْنَوِيَّةِ فَصارَ الِاسْمُ تَنْوِيهًا بِالمُسَمَّى " +
            "وَرَفْعًا لِذِكْرِهِ [التحرير والتنوير]."

        val blocks = MarkdownBlocks.parse(raw)

        assertEquals(1, blocks.size)
        val paragraph = blocks.first()
        assertTrue(paragraph is MdBlock.Paragraph)
        assertEquals(raw, paragraph.text)
        // No markup means a single plain span — nothing for the renderer to style.
        assertEquals(listOf(MdSpan.Text(raw)), paragraph.inlines)
        assertFalse(MarkdownBlocks.hasMarkdown(raw))
    }

    @Test
    fun `soft line breaks without markers stay one paragraph`() {
        // Real `hissi` shape (root_id = 145): one bold label per line, no bullets.
        val raw = "**أصل:** قياس [المقاييس]\n**خبر:** نص [المحكم]"

        val blocks = MarkdownBlocks.parse(raw)

        assertEquals(1, blocks.size)
        val paragraph = blocks.first() as MdBlock.Paragraph
        assertEquals("أصل: قياس [المقاييس]\nخبر: نص [المحكم]", paragraph.text)
        assertEquals(
            listOf(
                MdSpan.Bold("أصل:"),
                MdSpan.Text(" قياس [المقاييس]\n"),
                MdSpan.Bold("خبر:"),
                MdSpan.Text(" نص [المحكم]")
            ),
            paragraph.inlines
        )
    }

    @Test
    fun `guard fires for a bullet that is not on the first line`() {
        // A body whose first line is prose must still be detected as Markdown.
        val raw = "مقدمة قصيرة.\n- عنصر أول\n- عنصر ثانٍ"
        assertTrue(MarkdownBlocks.hasMarkdown(raw))
        assertEquals(3, MarkdownBlocks.parse(raw).size)
    }

    @Test
    fun `renderPlain of legacy text is the source verbatim`() {
        val raw = "نص عادي [المرجع] لا يحتوي تنسيقا."
        assertEquals(raw, MarkdownBlocks.renderPlain(raw))
    }

    // --- Bullets ---------------------------------------------------------

    @Test
    fun `top level bullets parse with depth zero`() {
        val raw = "- **الأصل:** جذر [المقاييس]\n- **الشبكة:** نص [التاج]"

        val blocks = MarkdownBlocks.parse(raw)

        assertEquals(2, blocks.size)
        assertEquals(listOf(0, 0), blocks.map { (it as MdBlock.Bullet).depth })
        assertEquals("الأصل: جذر [المقاييس]", blocks[0].text)
        assertEquals(
            listOf(MdSpan.Bold("الأصل:"), MdSpan.Text(" جذر [المقاييس]")),
            blocks[0].inlines
        )
    }

    @Test
    fun `nested bullets use depth one for two space indent`() {
        val raw = "- **السماء والأرض**\n  - **المائز:** كُلُّ سَماءٍ [التاج]\n" +
            "  - **الثمرة:** آثَرَ التنزيل [البصائر]"

        val blocks = MarkdownBlocks.parse(raw)

        assertEquals(3, blocks.size)
        assertEquals(0, (blocks[0] as MdBlock.Bullet).depth)
        assertEquals(1, (blocks[1] as MdBlock.Bullet).depth)
        assertEquals(1, (blocks[2] as MdBlock.Bullet).depth)
        assertEquals("المائز: كُلُّ سَماءٍ [التاج]", blocks[1].text)
    }

    @Test
    fun `indent beyond one level is clamped`() {
        val raw = "- عمق\n    - أعمق\n        - أعمق"
        val depths = MarkdownBlocks.parse(raw).map { (it as MdBlock.Bullet).depth }
        assertEquals(listOf(0, 1, 1), depths)
    }

    @Test
    fun `a non bullet line after a list closes the list`() {
        // Real `shabaka` shape (root_id = 1): witness bullets then a prose tail.
        val raw = "- {بِسْمِ ٱللَّهِ} [الفاتحة: 1]\n- {ثُمَّ ٱسْتَوَىٰ} [البقرة: 29]\n" +
            "وَتَتَعَدَّدُ وُجُوهُ السَّماءِ فِي التَّنْزِيلِ [النزهة]."

        val blocks = MarkdownBlocks.parse(raw)

        assertEquals(3, blocks.size)
        assertTrue(blocks[0] is MdBlock.Bullet)
        assertTrue(blocks[1] is MdBlock.Bullet)
        assertTrue(blocks[2] is MdBlock.Paragraph)
        assertEquals("وَتَتَعَدَّدُ وُجُوهُ السَّماءِ فِي التَّنْزِيلِ [النزهة].", blocks[2].text)
    }

    @Test
    fun `plain prose with blank lines still parses as one paragraph`() {
        // Degradation contract: blank lines alone are NOT Markdown, so a legacy
        // body keeps the single-`Text` path even when it is multi-paragraph.
        // Real shape — 65 `summary_ar` rows carry `\n\n` with zero markers.
        val raw = "الفقرة الأولى.\n\nالفقرة الثانية."

        val blocks = MarkdownBlocks.parse(raw)

        assertEquals(1, blocks.size)
        assertEquals(raw, blocks.first().text)
        assertFalse(MarkdownBlocks.hasMarkdown(raw))
    }

    @Test
    fun `blank line separates two paragraphs once markup is present`() {
        val raw = "**الأصل:** نص [المقاييس]\n\n**التطور:** نص [التاج]"
        val blocks = MarkdownBlocks.parse(raw)

        assertEquals(2, blocks.size)
        assertTrue(blocks.all { it is MdBlock.Paragraph })
        assertEquals(listOf("الأصل: نص [المقاييس]", "التطور: نص [التاج]"), blocks.map { it.text })
    }

    // --- Inline markup ---------------------------------------------------

    @Test
    fun `bold spans are extracted and the markers removed`() {
        val spans = MarkdownBlocks.parse("- أصل **قول** النطق [المقاييس]").first().inlines

        assertEquals(
            listOf(
                MdSpan.Text("أصل "),
                MdSpan.Bold("قول"),
                MdSpan.Text(" النطق [المقاييس]")
            ),
            spans
        )
    }

    @Test
    fun `code spans keep the inner text without backticks`() {
        val spans = MarkdownBlocks.parse("لا مصادر له في جدول `masadir` هنا").first().inlines

        assertEquals(
            listOf(
                MdSpan.Text("لا مصادر له في جدول "),
                MdSpan.Code("masadir"),
                MdSpan.Text(" هنا")
            ),
            spans
        )
        assertTrue(MarkdownBlocks.hasMarkdown("جدول `masadir`"))
    }

    @Test
    fun `bold and code interleave in source order`() {
        val raw = "`code` ثم **bold** ثم `code2`"
        val spans = MarkdownBlocks.parse("- $raw").first().inlines

        assertEquals(
            listOf(
                MdSpan.Code("code"),
                MdSpan.Text(" ثم "),
                MdSpan.Bold("bold"),
                MdSpan.Text(" ثم "),
                MdSpan.Code("code2")
            ),
            spans
        )
        assertEquals(raw, MarkdownBlocks.parse("- $raw").first().text)
    }

    @Test
    fun `multiple bold spans on one line are all extracted`() {
        val raw = "و**الله** أصله **إلاه** [الصحاح]"
        assertEquals(
            listOf(
                MdSpan.Text("و"),
                MdSpan.Bold("الله"),
                MdSpan.Text(" أصله "),
                MdSpan.Bold("إلاه"),
                MdSpan.Text(" [الصحاح]")
            ),
            MarkdownBlocks.parse(raw).first().inlines
        )
    }

    // --- The single asterisk rule ---------------------------------------

    @Test
    fun `single asterisks stay literal and are never italics`() {
        // Real Quranic poetics (root_id = 3): `*` is a caesura, not emphasis.
        val raw = "قال تعالى: ﴿فَوَيْلٌ لِلْمُصَلِّينَ * الَّذِينَ هُمْ عَن صَلَاتِهِمْ " +
            "سَاهُونَ﴾ [المؤمنون: 4]."

        val blocks = MarkdownBlocks.parse(raw)

        assertEquals(1, blocks.size)
        assertEquals(raw, blocks.first().text)
        assertEquals(listOf(MdSpan.Text(raw)), blocks.first().inlines)
        // A lone `*` must not flip the markdown guard on.
        assertFalse(MarkdownBlocks.hasMarkdown(raw))
    }

    @Test
    fun `whole string asterisk wrapper is left literal too`() {
        // Real placeholder rows (root_id = 49/284/350): `*لا تتوفر...*`.
        val raw = "*لا تتوفر مادة مستقلة لهذا القسم في المصادر المعتمدة.*"
        assertEquals(raw, MarkdownBlocks.parse(raw).first().text)
    }

    @Test
    fun `unbalanced bold marker degrades to literal text`() {
        val raw = "نص **غير مغلق"
        val blocks = MarkdownBlocks.parse(raw)

        assertEquals(raw, blocks.first().text)
        assertEquals(listOf(MdSpan.Text(raw)), blocks.first().inlines)
    }

    // --- Quranic braces stay verbatim ------------------------------------

    @Test
    fun `quranic braces are never rewritten`() {
        val raw = "- {وَعَلَّمَ ءَادَمَ ٱلْأَسْمَآءَ كُلَّهَا} [البقرة: 31]"
        val blocks = MarkdownBlocks.parse(raw)

        assertEquals(1, blocks.size)
        assertEquals("{وَعَلَّمَ ءَادَمَ ٱلْأَسْمَآءَ كُلَّهَا} [البقرة: 31]", blocks[0].text)
    }

    // --- renderPlain -----------------------------------------------------

    @Test
    fun `renderPlain strips markers and keeps bullet hierarchy`() {
        val raw = "- **السماء:** كُلُّ ما عَلَاكَ [التاج]\n  - **الأصل:** السِّينُ [المقاييس]\n" +
            "وتَتَعَدَّدُ وُجُوهُهَا."

        assertEquals(
            "▪ السماء: كُلُّ ما عَلَاكَ [التاج]\n" +
                "  ▪ الأصل: السِّينُ [المقاييس]\n" +
                "وتَتَعَدَّدُ وُجُوهُهَا.",
            MarkdownBlocks.renderPlain(raw)
        )
    }

    @Test
    fun `renderPlain keeps every visible character`() {
        val raw = "- **أ**ب `c` د\nبارٍ * هـ"
        val plain = MarkdownBlocks.renderPlain(raw)
        assertTrue(plain.contains("أب"))
        assertTrue(plain.contains("c"))
        assertFalse(plain.contains("**"))
        assertFalse(plain.contains("`"))
        // The caesura asterisk survives untouched.
        assertTrue(plain.contains("بارٍ *"))
    }

    @Test
    fun `empty and blank bodies parse to no blocks`() {
        assertEquals(emptyList<MdBlock>(), MarkdownBlocks.parse(""))
        assertEquals(emptyList<MdBlock>(), MarkdownBlocks.parse("   \n  \n"))
        assertEquals("", MarkdownBlocks.renderPlain(""))
    }

    @Test
    fun `blank lines around a list do not emit empty paragraphs`() {
        val raw = "\n- أول\n- ثاني\n\n"
        val blocks = MarkdownBlocks.parse(raw)
        assertEquals(2, blocks.size)
        assertTrue(blocks.all { it is MdBlock.Bullet })
    }
}