package com.quranwords.ui

import com.quranwords.domain.model.RootItem
import com.quranwords.ui.roots.RootsListFilter
import com.quranwords.ui.roots.RootsSort
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * Pure rules of the roots lexicon — model options, filtering and the stable
 * sort orders (AGENTS §15).
 */
class RootsListFilterTest {

    private fun root(
        id: Int,
        text: String,
        model: String? = null,
        occurrences: Int = 0,
        masadir: Int = 0,
        derivatives: Int = 0
    ) = RootItem(
        id = id,
        root = text,
        aiModel = model,
        occurrencesCount = occurrences,
        masadirCount = masadir,
        derivativesCount = derivatives
    )

    @Test
    fun `model options ignore null and blank models`() {
        val roots = listOf(
            root(1, "كتب", model = "muse-spark-1.3"),
            root(2, "علم", model = null),
            root(3, "رحم", model = "   "),
            root(4, "حمد", model = "muse-spark-1.3")
        )

        val options = RootsListFilter.modelOptions(roots)

        assertEquals(1, options.size)
        assertEquals("muse-spark-1.3", options.first().model)
        assertEquals(2, options.first().count)
    }

    @Test
    fun `model options are ordered by count descending then by name`() {
        val roots = listOf(
            root(1, "كتب", model = "zeta"),
            root(2, "علم", model = "alpha"),
            root(3, "رحم", model = "zeta"),
            root(4, "حمد", model = "beta"),
            root(5, "قرا", model = "beta")
        )

        val options = RootsListFilter.modelOptions(roots)

        assertEquals(listOf("beta", "zeta", "alpha"), options.map { it.model })
        assertEquals(listOf(2, 2, 1), options.map { it.count })
    }

    @Test
    fun `query matches the model name regardless of case`() {
        val roots = listOf(
            root(1, "كتب", model = "muse-spark-1.3"),
            root(2, "علم", model = "Muse Spark 1.3 / methodology 2.1"),
            root(3, "رحم", model = "stealth/space-bunny-alpha")
        )

        val result = RootsListFilter.filter(roots, "muse", emptySet(), RootsSort.Default)

        assertEquals(listOf(1, 2), result.map { it.id })
    }

    @Test
    fun `model chips narrow the list and combine with the query`() {
        val roots = listOf(
            root(1, "كتب", model = "muse-spark-1.3"),
            root(2, "علم", model = "muse-spark-1.3"),
            root(3, "رحم", model = "stealth/space-bunny-alpha")
        )

        val byModel = RootsListFilter.filter(roots, "", setOf("muse-spark-1.3"), RootsSort.Default)
        assertEquals(listOf(1, 2), byModel.map { it.id })

        val byModelAndQuery = RootsListFilter.filter(roots, "كتب", setOf("muse-spark-1.3"), RootsSort.Default)
        assertEquals(listOf(1), byModelAndQuery.map { it.id })

        val noMatch = RootsListFilter.filter(roots, "كتب", setOf("stealth/space-bunny-alpha"), RootsSort.Default)
        assertTrue(noMatch.isEmpty())
    }

    @Test
    fun `by model pushes missing models last and keeps the id tie breaker`() {
        val roots = listOf(
            root(3, "رحم", model = null),
            root(1, "كتب", model = "muse-spark-1.3"),
            root(2, "علم", model = ""),
            root(4, "حمد", model = "muse-spark-1.3")
        )

        val sorted = RootsListFilter.filter(roots, "", emptySet(), RootsSort.ByModel)

        // Same model → id ascending; missing models last, also id ascending.
        assertEquals(listOf(1, 4, 2, 3), sorted.map { it.id })
    }

    @Test
    fun `numeric sorts are descending with a stable id tie breaker`() {
        val roots = listOf(
            root(5, "رحم", occurrences = 10),
            root(3, "كتب", occurrences = 10),
            root(1, "علم", occurrences = 50)
        )

        val sorted = RootsListFilter.filter(roots, "", emptySet(), RootsSort.ByOccurrences)

        assertEquals(listOf(1, 3, 5), sorted.map { it.id })
    }

    @Test
    fun `alphabetical sort uses the root letters`() {
        val roots = listOf(
            root(1, "كتب"),
            root(2, "ألم"),
            root(3, "رحم")
        )

        val sorted = RootsListFilter.filter(roots, "", emptySet(), RootsSort.Alphabetical)

        // Arabic code points: أ (0623) < ر (0631) < ك (0643)
        assertEquals(listOf(2, 3, 1), sorted.map { it.id })
    }

    @Test
    fun `blank query keeps every root`() {
        val roots = listOf(root(1, "كتب", model = "muse-spark-1.3"), root(2, "علم"))

        val result = RootsListFilter.filter(roots, "   ", emptySet(), RootsSort.Default)

        assertEquals(listOf(1, 2), result.map { it.id })
    }
}
