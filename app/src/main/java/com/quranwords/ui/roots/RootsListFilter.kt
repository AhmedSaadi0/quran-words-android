package com.quranwords.ui.roots

import com.quranwords.core.util.ArabicNormalizer
import com.quranwords.domain.model.RootItem

/**
 * Pure filtering/sorting for the roots lexicon (AGENTS §9) — no Android
 * dependencies, so every rule below is unit-tested.
 *
 * Rules:
 * - a query matches the root letters, the Arabic gloss **or** the AI model
 *   (case-insensitive — model names are Latin, e.g. "muse-spark-1.3");
 * - the model chips narrow the list further (AND with the query);
 * - every sort order is stable: ties fall back to the root id.
 */
object RootsListFilter {

    /**
     * Distinct models in the corpus with their counts — blank/null models are
     * ignored (they get no chip). Ordered by count descending, then by the
     * model name so the chips never jump around between emissions.
     */
    fun modelOptions(roots: List<RootItem>): List<RootModelOption> =
        roots.asSequence()
            .mapNotNull { it.aiModel?.trim()?.takeIf { model -> model.isNotEmpty() } }
            .groupingBy { it }
            .eachCount()
            .map { (model, count) -> RootModelOption(model = model, count = count) }
            .sortedWith(
                compareByDescending<RootModelOption> { it.count }
                    .thenBy { it.model.lowercase() }
                    .thenBy { it.model }
            )

    /**
     * Applies the query, the model filter and the sort order.
     */
    fun filter(
        roots: List<RootItem>,
        query: String,
        selectedModels: Set<String>,
        sort: RootsSort
    ): List<RootItem> {
        val normalized = ArabicNormalizer.normalizeAr(query)
        val raw = query.trim()
        val hasQuery = normalized.isNotBlank() || raw.isNotBlank()
        val hasModelFilter = selectedModels.isNotEmpty()

        val filtered = roots.filter { root ->
            val matchesQuery = !hasQuery || matchesQuery(root, normalized, raw)
            val matchesModel = !hasModelFilter || rootModelKey(root) in selectedModels
            matchesQuery && matchesModel
        }
        return applySort(filtered, sort)
    }

    private fun matchesQuery(root: RootItem, normalized: String, raw: String): Boolean {
        if (normalized.isNotBlank()) {
            if (ArabicNormalizer.normalizeAr(root.root).contains(normalized)) return true
            val gloss = root.glossAr
            if (gloss != null && ArabicNormalizer.normalizeAr(gloss).contains(normalized)) return true
        }
        // Model names are Latin: compare the raw query, ignoring case.
        val model = root.aiModel
        return model != null && raw.isNotBlank() && model.contains(raw, ignoreCase = true)
    }

    private fun rootModelKey(root: RootItem): String? =
        root.aiModel?.trim()?.takeIf { it.isNotEmpty() }

    /** Stable sort — `id` is the tie-breaker of every order. */
    private fun applySort(roots: List<RootItem>, sort: RootsSort): List<RootItem> = when (sort) {
        RootsSort.Default -> roots.sortedBy { it.id }
        RootsSort.ByModel -> roots.sortedWith(
            compareBy<RootItem, String?>(nullsLast<String>()) { rootModelKey(it) }.thenBy { it.id }
        )
        RootsSort.ByOccurrences -> roots.sortedWith(
            compareByDescending<RootItem> { it.occurrencesCount }.thenBy { it.id }
        )
        RootsSort.ByMasadir -> roots.sortedWith(
            compareByDescending<RootItem> { it.masadirCount }.thenBy { it.id }
        )
        RootsSort.ByDerivatives -> roots.sortedWith(
            compareByDescending<RootItem> { it.derivativesCount }.thenBy { it.id }
        )
        RootsSort.Alphabetical -> roots.sortedWith(
            compareBy<RootItem> { alphabeticalKey(it.root) }.thenBy { it.id }
        )
    }

    /**
     * Alphabetical key: diacritics stripped but the letters are NOT folded
     * (أ/إ/آ stay distinct) so the order matches the Arabic alphabet.
     */
    private fun alphabeticalKey(root: String): String =
        ArabicNormalizer.stripDiacritics(root).trim()
}
