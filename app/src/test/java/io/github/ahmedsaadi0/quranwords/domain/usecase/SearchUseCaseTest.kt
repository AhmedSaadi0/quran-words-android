package io.github.ahmedsaadi0.quranwords.domain.usecase

import io.github.ahmedsaadi0.quranwords.core.util.Result
import io.github.ahmedsaadi0.quranwords.domain.model.SearchResult
import io.github.ahmedsaadi0.quranwords.domain.repository.QuranRepository
import io.mockk.coEvery
import io.mockk.mockk
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class SearchUseCaseTest {
    private val repo = mockk<QuranRepository>()
    private val useCase = SearchUseCase(repo)

    @Test
    fun `blank query returns empty SearchResult`() = runTest {
        val result = useCase("   ")
        assertTrue(result is Result.Success)
        assertEquals(SearchResult(), (result as Result.Success).data)
    }

    @Test
    fun `normalized blank returns empty`() = runTest {
        // query that normalizes to blank (e.g., only diacritics)
        val result = useCase("ًٌٍ")
        assertTrue(result is Result.Success)
        assertEquals(SearchResult(), (result as Result.Success).data)
    }

    @Test
    fun `valid query delegates to repository`() = runTest {
        val expected = SearchResult()
        coEvery { repo.searchAll(any()) } returns expected
        val result = useCase("كتب")
        assertTrue(result is Result.Success)
        assertEquals(expected, (result as Result.Success).data)
    }

    @Test
    fun `repository exception returns Error`() = runTest {
        coEvery { repo.searchAll(any()) } throws RuntimeException("DB error")
        val result = useCase("كتب")
        assertTrue(result is Result.Error)
    }
}
