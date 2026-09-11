package io.github.ahmedsaadi0.quranwords.core

import io.github.ahmedsaadi0.quranwords.core.util.Error
import io.github.ahmedsaadi0.quranwords.core.util.Result
import io.github.ahmedsaadi0.quranwords.core.util.Success
import io.github.ahmedsaadi0.quranwords.core.util.runCatchingResult
import kotlinx.coroutines.CancellationException
import org.junit.Assert.assertEquals
import org.junit.Assert.assertSame
import org.junit.Assert.assertTrue
import org.junit.Test

class ResultTest {

  @Test
  fun `runCatchingResult returns Success with block value`() {
    val result = runCatchingResult { 42 }
    assertEquals(Success(42), result)
  }

  @Test
  fun `runCatchingResult maps exception to Error with message and cause`() {
    val cause = IllegalStateException("boom")
    val result = runCatchingResult<String> { throw cause }
    assertTrue(result is Error)
    assertEquals("boom", (result as Error).message)
    assertSame(cause, result.cause)
  }

  @Test
  fun `runCatchingResult uses fallback message when exception has null message`() {
    val result = runCatchingResult<String> { throw IllegalStateException() }
    assertTrue(result is Error)
    assertEquals("Unexpected error", (result as Error).message)
  }

  @Test
  fun `runCatchingResult rethrows CancellationException`() {
    val cancellation = CancellationException("cancelled")
    try {
      runCatchingResult<Unit> { throw cancellation }
      throw AssertionError("Expected CancellationException to propagate")
    } catch (expected: CancellationException) {
      assertSame(cancellation, expected)
    }
  }

  @Test
  fun `Error with null cause is representable`() {
    val error: Result<Int> = Error("db not ready")
    assertEquals("db not ready", (error as Error).message)
    assertEquals(null, error.cause)
  }
}
