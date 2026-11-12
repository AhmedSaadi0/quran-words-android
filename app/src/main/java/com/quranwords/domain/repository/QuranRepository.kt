package com.quranwords.domain.repository

import com.quranwords.domain.model.Ayah
import com.quranwords.domain.model.AyahOccurrenceModel
import com.quranwords.domain.model.DerivativeModel
import com.quranwords.domain.model.MasdarModel
import com.quranwords.domain.model.RootDetail
import com.quranwords.domain.model.RootItem
import com.quranwords.domain.model.RootWordModel
import com.quranwords.domain.model.SearchResult
import com.quranwords.domain.model.Surah
import com.quranwords.domain.model.WordToken
import kotlinx.coroutines.flow.Flow

interface QuranRepository {
    fun getSurahs(): Flow<List<Surah>>
    suspend fun getSurahById(id: Int): Surah?
    fun getAyatBySurah(surahId: Int): Flow<List<Ayah>>
    /**
     * The whole surah in one shot (text + words). A surah is finite static
     * data — Al-Baqarah, the longest, is 286 ayat — so paging it was a
     * premature optimization; `LazyColumn` already virtualizes rendering.
     */
    suspend fun getAyatBySurahFull(surahId: Int): List<Ayah>
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
    suspend fun searchAyatPaged(query: String, limit: Int, offset: Int, surahFilter: Int? = null): List<Ayah>
    suspend fun searchWordsPaged(query: String, limit: Int, offset: Int): List<WordToken>
    suspend fun getAyahDirect(surahId: Int, ayahNum: Int): Ayah?
    suspend fun getSurahForPage(pageNumber: Int): Pair<Int, Int>?
    suspend fun getTopRoots(limit: Int): List<RootItem>
    suspend fun getPagesForSurah(surahId: Int): List<Int>
    fun isDatabaseReady(): Boolean
    fun closeDb()
}
