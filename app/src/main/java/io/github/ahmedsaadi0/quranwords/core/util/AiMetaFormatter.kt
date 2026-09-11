package io.github.ahmedsaadi0.quranwords.core.util

/**
 * Pure helpers for the AI meta line (extracted from RootDetail UI/VM, Phase 1
 * of UI_REFACTOR_PLAN). No Android deps — unit-testable.
 */

fun cleanAiDate(raw: String?): String {
    if (raw.isNullOrBlank()) return ""
    return try {
        val cleaned = raw.replace("T", " ")
        if (cleaned.length >= 16) cleaned.substring(0, 16) else cleaned
    } catch (_: Exception) {
        raw
    }
}

fun formatAiMetaLine(aiModel: String?, aiGeneratedAt: String?): String? {
    val date = cleanAiDate(aiGeneratedAt)
    val line = buildString {
        if (!aiModel.isNullOrBlank()) append(aiModel)
        if (!aiModel.isNullOrBlank() && date.isNotBlank()) append("  •  ")
        if (date.isNotBlank()) append(date)
    }
    return line.takeIf { it.isNotBlank() }
}
