package com.example.domain.repository

import com.example.domain.model.Ayah
import com.example.domain.model.AyahOccurrenceModel
import com.example.domain.model.RootDetail
import com.example.domain.model.RootItem
import com.example.domain.model.SearchResult
import com.example.domain.model.Surah
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
    suspend fun getRootOccurrencesCount(rootId: Int): Int
    suspend fun getRootByText(rootText: String): RootDetail?
    suspend fun searchAll(query: String): SearchResult
    fun isDatabaseReady(): Boolean
}
