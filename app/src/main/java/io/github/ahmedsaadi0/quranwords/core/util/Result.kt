package io.github.ahmedsaadi0.quranwords.core.util

import kotlinx.coroutines.CancellationException

/**
 * Domain result contract (AGENTS §13). Repositories and UseCases return this
 * instead of throwing or silently returning empty data; `Loading` belongs in
 * UiState, never as a terminal Result.
 *
 * Shadows `kotlin.Result` inside this module by design — call-sites importing
 * both must fully-qualify the Kotlin stdlib one.
 */
sealed interface Result<out T> {
    data class Success<out T>(val data: T) : Result<T>
    data class Error(val message: String, val cause: Throwable? = null) : Result<Nothing>
}

/**
 * Wraps [block] into a [Result], mapping any thrown exception to
 * [Result.Error]. [CancellationException] is rethrown as required for
 * structured concurrency — cancelling a coroutine is never a business error.
 */
inline fun <T> runCatchingResult(block: () -> T): Result<T> = try {
    Result.Success(block())
} catch (cancellation: CancellationException) {
    throw cancellation
} catch (throwable: Throwable) {
    Result.Error(throwable.message ?: "Unexpected error", throwable)
}
