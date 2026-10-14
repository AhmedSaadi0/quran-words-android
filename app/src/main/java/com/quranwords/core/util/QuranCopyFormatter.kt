package com.quranwords.core.util

import com.quranwords.domain.model.AiExtendedSummary
import com.quranwords.domain.model.Ayah
import com.quranwords.domain.model.AyahOccurrenceModel
import com.quranwords.domain.model.RootMeaningModel
import com.quranwords.domain.model.Surah

object QuranCopyFormatter {

    /**
     * Single ayah:
     * {textUthmani} ﴿{ayahNum}﴾
     * [سورة {surahName}: {ayahNum}]
     */
    fun formatSingle(ayah: Ayah, surah: Surah?): String {
        val surahName = surah?.nameAr ?: "السورة"
        return buildString {
            append(ayah.textUthmani)
            append(" ﴿${ayah.ayah}﴾")
            append("\n")
            append("[سورة $surahName: ${ayah.ayah}]")
        }
    }

    /**
     * Multiple ayat:
     * Contiguous -> [سورة {name}: {start} - {end}]
     * Non-contiguous -> [سورة {name}: {n1}، {n2}، {n3}]
     */
    fun formatMultiple(ayahs: List<Ayah>, surah: Surah?): String {
        if (ayahs.isEmpty()) return ""
        if (ayahs.size == 1) return formatSingle(ayahs.first(), surah)
        val sorted = ayahs.sortedBy { it.ayah }
        val surahName = surah?.nameAr ?: "السورة"

        val body = sorted.joinToString("\n") { "${it.textUthmani} ﴿${it.ayah}﴾" }

        val suffix = if (isContiguous(sorted.map { it.ayah })) {
            "[سورة $surahName: ${sorted.first().ayah} - ${sorted.last().ayah}]"
        } else {
            val list = sorted.joinToString("، ") { it.ayah.toString() }
            "[سورة $surahName: $list]"
        }

        return "$body\n$suffix"
    }

    fun formatSelected(
        allAyat: List<Ayah>,
        selectedAyahNumbers: Set<Int>,
        surah: Surah?
    ): String {
        val selected = allAyat.filter { it.ayah in selectedAyahNumbers }
        return formatMultiple(selected, surah)
    }

    private fun isContiguous(numbers: List<Int>): Boolean {
        if (numbers.size < 2) return true
        val sorted = numbers.sorted()
        for (i in 1 until sorted.size) {
            if (sorted[i] != sorted[i - 1] + 1) return false
        }
        return true
    }

    /**
     * Occurrences (cross-surah) — Format B1:
     * Each ayah on its own block:
     * {textUthmani} ﴿{ayahNum}﴾
     * [سورة {surahNameAr}: {ayahNum}]
     * Blocks separated by double newlines.
     */
    fun formatOccurrences(occurrences: List<AyahOccurrenceModel>): String {
        if (occurrences.isEmpty()) return ""
        return occurrences.joinToString("\n\n") { occ ->
            buildString {
                append(occ.textUthmani)
                append(" ﴿${occ.ayahNum}﴾")
                append("\n")
                append("[سورة ${occ.surahNameAr}: ${occ.ayahNum}]")
            }
        }
    }

    /**
     * AI summary (single text):
     * [الملخص الذكي للجذر: {rootText}]
     * {summary}
     * Headers stay Arabic: the copied payload is reference data, not UI chrome.
     */
    fun formatAiSummary(rootText: String, summary: String): String {
        if (summary.isBlank()) return ""
        return "[الملخص الذكي للجذر: $rootText]\n\n${summary.trim()}"
    }

    fun formatAiSummary(
        rootText: String,
        summary: String,
        extended: AiExtendedSummary?,
        sectionTitles: List<String> = emptyList()
    ): String {
        val base = StringBuilder()
        val header = "[الملخص الذكي للجذر: $rootText]"
        base.append(header)
        if (summary.isNotBlank()) {
            // `summary_ar` carries inline `**bold**` on ~10 roots; strip markers so
            // the clipboard matches the rendered screen.
            val citedSummary = CitationMatcher.cite(
                MarkdownBlocks.renderPlain(summary.trim()), extended?.summaryRefs.orEmpty()
            )
            base.append("\n\n${CitationMatcher.renderPlain(citedSummary)}")
            appendNumberedRefs(base, citedSummary.orderedRefs)
        }
        if (extended != null) {
            val sections = listOf(
                extended.furuq to (sectionTitles.getOrNull(0) ?: "الفروق"),
                extended.shabaka to (sectionTitles.getOrNull(1) ?: "الشبكة القرآنية"),
                extended.hissi to (sectionTitles.getOrNull(2) ?: "الأصل الحسي"),
                extended.tatawwur to (sectionTitles.getOrNull(3) ?: "التطور الدلالي"),
                extended.tashrih to (sectionTitles.getOrNull(4) ?: "التشريح الصرفي")
            )
            sections.forEach { (section, title) ->
                val raw = section.text?.trim().orEmpty()
                if (raw.isBlank()) return@forEach
                // Strip Markdown markers so the clipboard never carries `- `,
                // `**` or backticks. Citation numbering still spans the whole
                // section, so the clipboard matches what the screen shows.
                val cited = CitationMatcher.cite(MarkdownBlocks.renderPlain(raw), section.refs)
                val body = CitationMatcher.renderPlain(cited)
                if (body.isNotBlank()) {
                    base.append("\n\n▪ $title\n$body")
                    appendNumberedRefs(base, cited.orderedRefs)
                }
            }
        }
        val result = base.toString()
        return if (result == header) "" else result
    }

    private fun appendNumberedRefs(base: StringBuilder, refs: List<String>) {
        if (refs.isEmpty()) return
        base.append("\nالمراجع:")
        refs.forEachIndexed { index, ref ->
            base.append("\n${CitationMatcher.toEasternArabicDigits(index + 1)}. $ref")
        }
    }

    /**
     * Lexicon meanings, each on its own block:
     * [معاني الجذر: {rootText}]
     * ▪ {bookName}
     * {definition}
     * Blocks separated by double newlines.
     */
    fun formatMeanings(rootText: String, meanings: List<RootMeaningModel>): String {
        if (meanings.isEmpty()) return ""
        return buildString {
            append("[معاني الجذر: $rootText]")
            meanings.forEach { meaning ->
                append("\n\n▪ ${meaning.bookName}\n${meaning.definition}")
            }
        }
    }
}
