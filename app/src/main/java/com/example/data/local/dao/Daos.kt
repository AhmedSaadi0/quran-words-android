package com.example.data.local.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import com.example.data.local.entities.AyahEntity
import com.example.data.local.entities.DerivativeEntity
import com.example.data.local.entities.MasdarEntity
import com.example.data.local.entities.MorphologyEntity
import com.example.data.local.entities.RootAiSummaryEntity
import com.example.data.local.entities.RootEntity
import com.example.data.local.entities.RootGlossEntity
import com.example.data.local.entities.RootMeaningEntity
import com.example.data.local.entities.SurahEntity
import com.example.data.local.entities.WordAyahEntity
import com.example.data.local.entities.WordEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface SurahDao {
    @Query("SELECT * FROM surahs ORDER BY id ASC")
    fun getAllSurahs(): Flow<List<SurahEntity>>

    @Query("SELECT * FROM surahs WHERE id = :id LIMIT 1")
    suspend fun getSurahById(id: Int): SurahEntity?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertAll(surahs: List<SurahEntity>)
}

@Dao
interface AyahDao {
    @Query("SELECT * FROM ayat WHERE surah = :surahId ORDER BY ayah ASC")
    fun getAyatBySurah(surahId: Int): Flow<List<AyahEntity>>

    @Query("SELECT * FROM ayat WHERE surah = :surahId AND ayah = :ayahNum LIMIT 1")
    suspend fun getAyah(surahId: Int, ayahNum: Int): AyahEntity?

    @Query("SELECT * FROM ayat WHERE text_uthmani_plain LIKE '%' || :query || '%' OR text_imlaei LIKE '%' || :query || '%' LIMIT 100")
    fun searchAyat(query: String): Flow<List<AyahEntity>>

    @Query("SELECT COUNT(*) FROM ayat")
    suspend fun getCount(): Int

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertAll(ayat: List<AyahEntity>)
}

@Dao
interface WordDao {
    @Query("""
        SELECT w.* FROM words w
        JOIN word_ayah wa ON wa.word_id = w.id
        WHERE wa.ayah_id = :ayahId
        ORDER BY wa.position ASC
    """)
    fun getWordsForAyah(ayahId: Int): Flow<List<WordEntity>>

    @Query("""
        SELECT wa.* FROM word_ayah wa
        WHERE wa.ayah_id = :ayahId
        ORDER BY wa.position ASC
    """)
    suspend fun getWordAyahRecords(ayahId: Int): List<WordAyahEntity>

    @Query("SELECT * FROM words WHERE id = :wordId LIMIT 1")
    suspend fun getWordById(wordId: Int): WordEntity?

    @Query("SELECT * FROM words WHERE text_plain LIKE '%' || :query || '%' OR text_clean LIKE '%' || :query || '%' LIMIT 50")
    fun searchWords(query: String): Flow<List<WordEntity>>

    @Query("SELECT * FROM word_morphology WHERE word_ayah_id = :wordAyahId LIMIT 1")
    suspend fun getMorphologyForWordAyah(wordAyahId: Int): MorphologyEntity?

    @Query("SELECT * FROM word_morphology WHERE root_id = :rootId")
    suspend fun getMorphologiesByRoot(rootId: Int): List<MorphologyEntity>
}

@Dao
interface RootDao {
    @Query("SELECT * FROM roots ORDER BY id ASC")
    fun getAllRoots(): Flow<List<RootEntity>>

    @Query("SELECT * FROM roots ORDER BY id ASC LIMIT :limit OFFSET :offset")
    suspend fun getRootsPaged(limit: Int, offset: Int): List<RootEntity>

    @Query("SELECT * FROM roots WHERE id = :id LIMIT 1")
    suspend fun getRootById(id: Int): RootEntity?

    @Query("SELECT * FROM roots WHERE root = :rootText LIMIT 1")
    suspend fun getRootByText(rootText: String): RootEntity?

    @Query("SELECT * FROM roots WHERE root LIKE '%' || :query || '%' LIMIT 50")
    fun searchRoots(query: String): Flow<List<RootEntity>>

    @Query("SELECT * FROM root_glosses WHERE root_id = :rootId LIMIT 1")
    suspend fun getRootGloss(rootId: Int): RootGlossEntity?

    @Query("SELECT * FROM root_ai_summary WHERE root_id = :rootId LIMIT 1")
    suspend fun getRootAiSummary(rootId: Int): RootAiSummaryEntity?

    @Query("SELECT * FROM root_meanings WHERE root_id = :rootId")
    suspend fun getRootMeanings(rootId: Int): List<RootMeaningEntity>

    @Query("SELECT COUNT(*) FROM roots")
    suspend fun getRootsCount(): Int
}

@Dao
interface MasdarDao {
    @Query("SELECT * FROM masadir WHERE root_id = :rootId ORDER BY id ASC")
    fun getMasadirByRootId(rootId: Int): Flow<List<MasdarEntity>>

    @Query("SELECT * FROM masadir WHERE masdar_plain LIKE '%' || :query || '%' OR masdar_ar LIKE '%' || :query || '%' LIMIT 50")
    fun searchMasadir(query: String): Flow<List<MasdarEntity>>

    @Query("SELECT COUNT(*) FROM masadir")
    suspend fun getCount(): Int
}

@Dao
interface DerivativeDao {
    @Query("SELECT * FROM derivatives WHERE root_id = :rootId ORDER BY id ASC")
    fun getDerivativesByRootId(rootId: Int): Flow<List<DerivativeEntity>>

    @Query("SELECT * FROM derivatives WHERE form_plain LIKE '%' || :query || '%' OR form_ar LIKE '%' || :query || '%' LIMIT 50")
    fun searchDerivatives(query: String): Flow<List<DerivativeEntity>>

    @Query("SELECT COUNT(*) FROM derivatives")
    suspend fun getCount(): Int
}
