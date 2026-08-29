package io.github.ahmedsaadi0.quranwords.data.local.datasource

import io.github.ahmedsaadi0.quranwords.data.local.dao.AyahDao
import io.github.ahmedsaadi0.quranwords.data.local.dao.WordDao
import io.github.ahmedsaadi0.quranwords.data.local.entities.AyahEntity
import io.github.ahmedsaadi0.quranwords.data.local.entities.MorphologyEntity
import io.github.ahmedsaadi0.quranwords.data.local.entities.WordAyahEntity
import io.github.ahmedsaadi0.quranwords.data.local.entities.WordEntity
import javax.inject.Inject
import kotlinx.coroutines.flow.Flow

interface AyahLocalDataSource {
    fun getAyatBySurah(surahId: Int): Flow<List<AyahEntity>>
    suspend fun getAyah(surahId: Int, ayahNum: Int): AyahEntity?
    suspend fun getWordAyahRecords(ayahId: Int): List<WordAyahEntity>
    suspend fun getWordById(wordId: Int): WordEntity?
    suspend fun getMorphology(wordAyahId: Int): MorphologyEntity?
    suspend fun getCount(): Int
}

class AyahLocalDataSourceImpl @Inject constructor(
    private val ayahDao: AyahDao,
    private val wordDao: WordDao
) : AyahLocalDataSource {
    override fun getAyatBySurah(surahId: Int): Flow<List<AyahEntity>> = ayahDao.getAyatBySurah(surahId)
    override suspend fun getAyah(surahId: Int, ayahNum: Int): AyahEntity? = ayahDao.getAyah(surahId, ayahNum)
    override suspend fun getWordAyahRecords(ayahId: Int): List<WordAyahEntity> = wordDao.getWordAyahRecords(ayahId)
    override suspend fun getWordById(wordId: Int): WordEntity? = wordDao.getWordById(wordId)
    override suspend fun getMorphology(wordAyahId: Int): MorphologyEntity? = wordDao.getMorphologyForWordAyah(wordAyahId)
    override suspend fun getCount(): Int = ayahDao.getCount()
}
