package io.github.ahmedsaadi0.quranwords.util

import java.net.URLEncoder

/**
 * حدود أحرف لبقاء رابط issues/new ضمن ~8KB بعد الترميز بالمائة
 * (العربية تتضخم ~3x عند الترميز، لذا الحدود متحفظة عمدًا).
 */
object MeaningReportLimits {
    const val MIN_DESCRIPTION = 10
    const val MAX_DESCRIPTION = 800
    const val MAX_SUGGESTION = 800
    const val MAX_QUOTE = 200
    const val MAX_AYAH_TEXT = 120
    const val MAX_SAMPLES = 2
}

enum class MeaningReportType(val ar: String) {
    INCORRECT("معنى خاطئ"),
    INCOMPLETE("معنى ناقص"),
    TYPO("خطأ إملائي"),
    WRONG_ATTRIBUTION("عزو خاطئ لكتاب")
}

data class ReportAyahSample(
    val surahNameAr: String,
    val ayahNum: Int,
    val text: String
)

data class MeaningReportContent(
    val rootText: String,
    val rootId: Int?,
    val reportType: MeaningReportType,
    /** اسم المصدر المقصود، مثل "لسان العرب" أو "الملخص الذكي". فارغ = كل المعاني. */
    val targetSource: String = "",
    /** مقتبس حرفي من المعنى المقصود (يُقلَّم تلقائيًا). */
    val targetQuote: String = "",
    val description: String,
    val suggestion: String = "",
    val samples: List<ReportAyahSample> = emptyList(),
    val appVersion: String = "",
    val dbVersionName: String = "",
    val dbVersionCode: Int = 0,
    val androidRelease: String = "",
    val locale: String = "",
    val pageUrl: String? = null
)

fun validateMeaningReport(description: String): Boolean =
    description.trim().length >= MeaningReportLimits.MIN_DESCRIPTION

private fun trimmed(text: String, max: Int): String {
    val t = text.trim()
    if (t.length <= max) return t
    return t.take(max).trimEnd() + "…"
}

private fun escapeMarkdown(text: String): String {
    val sb = StringBuilder(text.length)
    for (c in text) {
        if (c == '\\' || c == '`' || c == '*' || c == '_' || c == '[' ||
            c == ']' || c == '#' || c == '>' || c == '|' || c == '~'
        ) {
            sb.append('\\')
        }
        sb.append(c)
    }
    return sb.toString()
}

fun buildMeaningReportMarkdown(c: MeaningReportContent): String {
    val lines = mutableListOf<String>()
    lines.add("**الجذر / Root:** ${c.rootText}")
    lines.add("**نوع البلاغ / Type:** ${c.reportType.ar}")
    if (c.targetSource.isNotBlank()) {
        lines.add("**المعنى المقصود / Target:** ${c.targetSource}")
    }
    if (c.targetQuote.isNotBlank()) {
        lines.add("")
        lines.add("> ${escapeMarkdown(trimmed(c.targetQuote, MeaningReportLimits.MAX_QUOTE))}")
    }
    lines.add("")
    lines.add("**شرح المشكلة / Details:**")
    lines.add(trimmed(c.description, MeaningReportLimits.MAX_DESCRIPTION))
    if (c.suggestion.isNotBlank()) {
        lines.add("")
        lines.add("**التصحيح المقترح / Suggestion:**")
        lines.add(trimmed(c.suggestion, MeaningReportLimits.MAX_SUGGESTION))
    }
    val samples = c.samples.take(MeaningReportLimits.MAX_SAMPLES)
    if (samples.isNotEmpty()) {
        lines.add("")
        lines.add("**شواهد / Samples:**")
        samples.forEach { s ->
            lines.add("- [${s.surahNameAr}: ${s.ayahNum}] ${trimmed(s.text, MeaningReportLimits.MAX_AYAH_TEXT)}")
        }
    }
    lines.add("")
    lines.add("---")
    lines.add("**البيئة / Environment (آلية — لا تحذف):**")
    if (c.rootId != null) lines.add("- root_id: ${c.rootId}")
    lines.add("- root: ${c.rootText}")
    if (c.appVersion.isNotBlank()) lines.add("- app: ${c.appVersion}")
    if (c.dbVersionCode > 0 || c.dbVersionName.isNotBlank()) {
        lines.add("- db: ${c.dbVersionName} (code ${c.dbVersionCode})")
    }
    if (c.androidRelease.isNotBlank()) lines.add("- android: ${c.androidRelease}")
    if (c.locale.isNotBlank()) lines.add("- locale: ${c.locale}")
    return lines.joinToString("\n")
}

fun buildMeaningReportIssueUrl(content: MeaningReportContent): String {
    val title = "[تقرير معنى] الجذر: ${content.rootText} — ${content.reportType.ar}"
    val body = buildMeaningReportMarkdown(content)
    fun enc(v: String) = URLEncoder.encode(v, "UTF-8").replace("+", "%20")
    val params = listOf(
        "title=${enc(title)}",
        "body=${enc(body)}",
        "labels=${enc(GITHUB_ISSUE_LABEL)}"
    ).joinToString("&")
    return "$GITHUB_NEW_ISSUE_BASE?$params"
}
