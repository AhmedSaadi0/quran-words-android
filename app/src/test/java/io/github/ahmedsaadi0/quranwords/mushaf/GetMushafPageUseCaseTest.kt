package io.github.ahmedsaadi0.quranwords.mushaf

import io.github.ahmedsaadi0.quranwords.core.util.Result
import io.github.ahmedsaadi0.quranwords.domain.model.MushafPage
import io.github.ahmedsaadi0.quranwords.domain.usecase.GetMushafPageUseCase
import io.github.ahmedsaadi0.quranwords.fake.FakeQuranRepository
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class GetMushafPageUseCaseTest {

  private val page = MushafPage(2, emptyList(), 2, 1, 2, 5, 1)

  @Test
  fun `returns page on success`() = runTest {
    val useCase = GetMushafPageUseCase(FakeQuranRepository(mushafPages = mapOf(2 to page)))
    val result = useCase(2)
    assertTrue(result is Result.Success)
    assertEquals(2, (result as Result.Success).data.pageNumber)
  }

  @Test
  fun `rejects out-of-range pages without touching repository`() = runTest {
    val repo = FakeQuranRepository(mushafPages = mapOf(2 to page))
    val useCase = GetMushafPageUseCase(repo)
    assertTrue(useCase(0) is Result.Error)
    assertTrue(useCase(605) is Result.Error)
  }

  @Test
  fun `missing layout maps to actionable error`() = runTest {
    val useCase = GetMushafPageUseCase(FakeQuranRepository())
    val result = useCase(2)
    assertTrue(result is Result.Error)
  }

  @Test
  fun `unready database maps to error`() = runTest {
    val useCase = GetMushafPageUseCase(FakeQuranRepository(dbReady = false))
    val result = useCase(2)
    assertTrue(result is Result.Error)
  }

  @Test
  fun `repository throw maps to error`() = runTest {
    val useCase = GetMushafPageUseCase(FakeQuranRepository(failMushaf = true))
    val result = useCase(2)
    assertTrue(result is Result.Error)
  }
}
