package io.github.ahmedsaadi0.quranwords.domain.repository

import io.github.ahmedsaadi0.quranwords.domain.model.Ayah
import io.github.ahmedsaadi0.quranwords.domain.model.AyahOccurrenceModel
import io.github.ahmedsaadi0.quranwords.domain.model.RootDetail
import io.github.ahmedsaadi0.quranwords.domain.model.RootItem
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
    suspend fun getRootDetail(rootId: Int): RootDetail?
    suspend fun getRootOccurrencesPaged(rootId: Int, limit: Int, offset: Int): List<AyahOccurrenceModel>
    suspend fun getAllRootOccurrences(rootId: Int): List<AyahOccurrenceModel>
    suspend fun getRootOccurrencesCount(rootId: Int): Int
    suspend fun getRootByText(rootText: String): RootDetail?
    suspend fun searchAll(query: String): SearchResult
    fun isDatabaseReady(): Boolean
}
