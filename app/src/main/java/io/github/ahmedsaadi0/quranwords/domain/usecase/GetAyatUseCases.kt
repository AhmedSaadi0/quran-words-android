package io.github.ahmedsaadi0.quranwords.domain.usecase

import io.github.ahmedsaadi0.quranwords.core.util.Result
import io.github.ahmedsaadi0.quranwords.domain.model.Ayah
import io.github.ahmedsaadi0.quranwords.domain.model.Surah
import io.github.ahmedsaadi0.quranwords.domain.repository.QuranRepository
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.map
import javax.inject.Inject

class GetSurahsUseCase @Inject constructor(
    private val repository: QuranRepository
) {
    operator fun invoke(): Flow<Result<List<Surah>>> =
        repository.getSurahs()
            .map<List<Surah>, Result<List<Surah>>> { Result.Success(it) }
            .catch { emit(Result.Error(it.message ?: "Failed to load surahs", it)) }
}

class GetSurahByIdUseCase @Inject constructor(
    private val repository: QuranRepository
) {
    suspend operator fun invoke(id: Int): Result<Surah?> {
        if (id !in 1..114) return Result.Error("Invalid surah id: $id")
        return try {
            Result.Success(repository.getSurahById(id))
        } catch (e: Exception) {
            Result.Error(e.message ?: "Failed to load surah", e)
        }
    }
}

class GetAyatPagedUseCase @Inject constructor(
    private val repository: QuranRepository
) {
    suspend operator fun invoke(surahId: Int, limit: Int, offset: Int): Result<List<Ayah>> {
        if (surahId !in 1..114) return Result.Error("Invalid surah id")
        if (limit <= 0 || offset < 0) return Result.Error("Invalid paging params")
        return try {
            Result.Success(repository.getAyatBySurahPaged(surahId, limit, offset))
        } catch (e: Exception) {
            Result.Error(e.message ?: "Failed to load ayat", e)
        }
    }
}

class GetAyahWithWordsUseCase @Inject constructor(
    private val repository: QuranRepository
) {
    suspend operator fun invoke(surahId: Int, ayahNum: Int): Result<Ayah?> {
        if (surahId !in 1..114 || ayahNum < 1) return Result.Error("Invalid ayah")
        return try {
            Result.Success(repository.getAyahWithWords(surahId, ayahNum))
        } catch (e: Exception) {
            Result.Error(e.message ?: "Failed to load ayah", e)
        }
    }
}
