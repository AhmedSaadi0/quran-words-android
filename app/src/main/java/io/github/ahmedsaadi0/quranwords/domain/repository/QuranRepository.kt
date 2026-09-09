package io.github.ahmedsaadi0.quranwords.domain.repository

import io.github.ahmedsaadi0.quranwords.domain.model.Ayah
import io.github.ahmedsaadi0.quranwords.domain.model.AyahOccurrenceModel
import io.github.ahmedsaadi0.quranwords.domain.model.DerivativeModel
import io.github.ahmedsaadi0.quranwords.domain.model.MasdarModel
import io.github.ahmedsaadi0.quranwords.domain.model.RootDetail
import io.github.ahmedsaadi0.quranwords.domain.model.RootItem
import io.github.ahmedsaadi0.quranwords.domain.model.RootWordModel
import io.github.ahmedsaadi0.quranwords.domain.model.SearchResult
import io.github.ahmedsaadi0.quranwords.domain.model.Surah
import kotlinx.coroutines.flow.Flow

interface QuranRepository {
    fun getSurahs(): Flow<List<Surah>>
    suspend fun getSurahById(id: Int): Surah?
    fun getAyatBySurah(surahId: Int): Flow<List<Ayah>>
    suspend fun getAyatBySurahPaged(surahId: Int, limit: Int, offset: Int): List<Ayah>
    suspend fun getAyahWithWords(surahId: Int, ayahNum: Int): Ayah?
    suspend fun getRootsPaged(limit: Int, offset: Int): List<RootItem>
    suspend fun getAllRoots(): List<RootItem>
    suspend fun getRootDetail(rootId: Int): RootDetail?
    suspend fun getRootOccurrencesPaged(rootId: Int, limit: Int, offset: Int): List<AyahOccurrenceModel>
    suspend fun getAllRootOccurrences(rootId: Int): List<AyahOccurrenceModel>
    suspend fun getRootOccurrencesCount(rootId: Int): Int
    suspend fun getRootWords(rootId: Int): List<RootWordModel>
    suspend fun getWordOccurrencesPaged(rootId: Int, wordIds: List<Int>, limit: Int, offset: Int): List<AyahOccurrenceModel>
    suspend fun getAllWordOccurrences(rootId: Int, wordIds: List<Int>): List<AyahOccurrenceModel>
    suspend fun getAllOccurrencesForWords(rootId: Int, wordIds: List<Int>): List<AyahOccurrenceModel>
    suspend fun getRootByText(rootText: String): RootDetail?
    suspend fun searchAll(query: String): SearchResult
    suspend fun searchRootsPaged(query: String, limit: Int, offset: Int): List<RootItem>
    suspend fun searchMasadirPaged(query: String, limit: Int, offset: Int): List<MasdarModel>
    suspend fun searchDerivativesPaged(query: String, limit: Int, offset: Int): List<DerivativeModel>
    suspend fun searchAyatPaged(query: String, limit: Int, offset: Int): List<Ayah>
    suspend fun getPagesForSurah(surahId: Int): List<Int>
    fun isDatabaseReady(): Boolean
    fun closeDb()
}
