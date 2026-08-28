package com.example.util

import java.net.URLEncoder

/**
 * Mirrors frontend/src/lib/github.ts buildGithubIssueUrl exactly
 */
private const val GITHUB_REPO = "AhmedSaadi0/quran-words"
private const val GITHUB_ISSUE_LABEL = "تقرير-معنى"
private const val GITHUB_NEW_ISSUE_BASE = "https://github.com/$GITHUB_REPO/issues/new"

data class BuildIssueUrlOptions(
    val rootText: String,
    val rootId: Int? = null,
    val pageUrl: String? = null
)

fun buildGithubIssueUrl(opts: BuildIssueUrlOptions): String {
    val rootText = opts.rootText
    val rootId = opts.rootId
    val pageUrl = opts.pageUrl

    val title = "[تقرير معنى] الجذر: $rootText / [Meaning Report] Root: $rootText"
    val safePageUrl = pageUrl?.trim()?.takeIf { it.isNotEmpty() } ?: "/roots/${encodeURIComponent(rootText)}"

    val bodyLines = mutableListOf<String>()
    bodyLines.add("**الجذر / Root:** $rootText")
    bodyLines.add("**الرابط / URL:** $safePageUrl")
    bodyLines.add("")
    bodyLines.add("**نوع البلاغ / Report type:** معنى غير صحيح أو ناقص / Incorrect or incomplete meaning")
    bodyLines.add("")
    bodyLines.add("**التفاصيل / Details:**")
    bodyLines.add("[اكتب هنا وصف المشكلة والاقتراح / Describe the issue and your suggestion here]")
    bodyLines.add("")
    bodyLines.add("---")
    bodyLines.add("**بيانات تقنية / Technical info (لا تحذف — do not delete):**")
    if (rootId != null) bodyLines.add("- root_id: $rootId")
    bodyLines.add("- root: $rootText")
    bodyLines.add("- page: $safePageUrl")

    val body = bodyLines.joinToString("\n")

    fun enc(v: String) = URLEncoder.encode(v, "UTF-8").replace("+", "%20")

    val params = listOf(
        "title=${enc(title)}",
        "body=${enc(body)}",
        "labels=${enc(GITHUB_ISSUE_LABEL)}"
    ).joinToString("&")

    return "$GITHUB_NEW_ISSUE_BASE?$params"
}

private fun encodeURIComponent(str: String): String {
    return URLEncoder.encode(str, "UTF-8")
        .replace("+", "%20")
        .replace("%21", "!")
        .replace("%27", "'")
        .replace("%28", "(")
        .replace("%29", ")")
        .replace("%7E", "~")
}

const val GITHUB_ISSUES_URL = "https://github.com/$GITHUB_REPO/issues"
const val GITHUB_REPO_URL = "https://github.com/$GITHUB_REPO"
