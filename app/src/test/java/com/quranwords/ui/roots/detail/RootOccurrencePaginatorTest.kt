package com.quranwords.ui.roots.detail

import androidx.lifecycle.SavedStateHandle
import com.quranwords.domain.model.AyahOccurrenceModel
import com.quranwords.domain.model.RootDetail
import com.quranwords.domain.model.RootItem
import com.quranwords.fake.FakeQuranRepository
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Test

class RootOccurrencePaginatorTest {

    private fun occurrence(n: Int) = AyahOccurrenceModel(2, "البقرة", n, "نص", "كلمة")

    private fun detail(count: Int, seeded: Int) = RootDetail(
        item = RootItem(1, "كتب", occurrencesCount = count),
        ayatOccurrences = (1..seeded).map(::occurrence)
    )

    private class PagingRepo(private val page: List<AyahOccurrenceModel>) : FakeQuranRepository() {
        val calls = mutableListOf<Pair<Int, Int>>()
        override suspend fun getRootOccurrencesPaged(rootId: Int, limit: Int, offset: Int): List<AyahOccurrenceModel> {
            calls += limit to offset
            return page
        }
    }

    @Test
    fun restore_loads_until_saved_offset() = runTest {
        val handle = SavedStateHandle(
            mapOf("currentRootIdForOcc" to 1, "occOffset" to 3, "occTotalCount" to 4)
        )
        val repo = PagingRepo(listOf(occurrence(2), occurrence(3)))
        val errors = mutableListOf<String>()
        val paginator = RootOccurrencePaginator(repo, handle) { errors += it }

        val restoreTarget = paginator.resetFor(1)
        paginator.seed(detail(count = 4, seeded = 1), restoreTarget)

        assertEquals(3, restoreTarget)
        assertEquals(3, paginator.occurrences.value.size)
        assertEquals(3, handle.get<Int>("occOffset"))
        assertEquals(listOf(30 to 1), repo.calls)
        assertEquals(emptyList<String>(), errors)
    }

    @Test
    fun empty_page_stops_paging_but_keeps_existing_rows() = runTest {
        val paginator = RootOccurrencePaginator(PagingRepo(emptyList()), SavedStateHandle()) {}
        paginator.resetFor(1)
        paginator.seed(detail(count = 2, seeded = 1), 0)
        assertFalse(paginator.hasMore.value)

        paginator.loadMore(this)
        assertFalse(paginator.hasMore.value)
        assertEquals(1, paginator.occurrences.value.size)
    }
}
