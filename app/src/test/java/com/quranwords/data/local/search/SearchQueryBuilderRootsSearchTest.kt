package com.quranwords.data.local.search

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * The roots search query is the only path the independent search screen
 * uses for the ROOTS scope, so its model term must be covered (AGENTS §15).
 */
class SearchQueryBuilderRootsSearchTest {

    @Test
    fun `roots search selects the shared projection including the model`() {
        val built = SearchQueryBuilder.rootsSearchQuery("muse", "muse", 20, 0)

        assertTrue(built.sql.contains("ra.model"))
        assertTrue(built.sql.contains("r.occurrences_count"))
        assertTrue(built.sql.contains("FROM roots r"))
    }

    @Test
    fun `roots search matches the model column case insensitively`() {
        val built = SearchQueryBuilder.rootsSearchQuery("muse", "muse", 20, 0)

        val modelTerms = built.sql.split("OR").count { it.trimStart().startsWith("ra.model") }
        assertEquals(2, modelTerms)
        assertTrue(built.args.take(4).any { it.toString().contains("muse") })
    }

    @Test
    fun `user input never reaches the sql string`() {
        val built = SearchQueryBuilder.rootsSearchQuery("كتب' OR '1'='1", "كتب", 20, 0)

        assertFalse(built.sql.contains("كتب"))
        assertFalse(built.sql.contains("'1'='1"))
        // Every bind arg is passed through rawQuery, not concatenated.
        assertTrue(built.args.size == 6)
    }

    @Test
    fun `like wildcards in the query are escaped`() {
        val built = SearchQueryBuilder.rootsSearchQuery("%", "%", 20, 0)

        val firstArg = built.args.first().toString()
        assertTrue(firstArg.contains("\\%"))
        assertFalse(firstArg.contains("%%"))
    }

    @Test
    fun `top roots keeps the shared projection`() {
        val built = SearchQueryBuilder.topRootsQuery(50)

        assertTrue(built.sql.contains("ra.model"))
        assertTrue(built.sql.contains("ORDER BY r.occurrences_count DESC"))
        assertEquals(1, built.args.size)
    }
}
