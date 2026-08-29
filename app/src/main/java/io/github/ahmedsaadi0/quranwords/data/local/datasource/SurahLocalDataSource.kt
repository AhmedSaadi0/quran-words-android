package io.github.ahmedsaadi0.quranwords.data.local.datasource

import io.github.ahmedsaadi0.quranwords.data.local.dao.SurahDao
import io.github.ahmedsaadi0.quranwords.data.local.entities.SurahEntity
import kotlinx.coroutines.flow.Flow
import javax.inject.Inject

interface SurahLocalDataSource {
    fun getAllSurahs(): Flow<List<SurahEntity>>
    suspend fun getSurahById(id: Int): SurahEntity?
}

class SurahLocalDataSourceImpl @Inject constructor(
    private val dao: SurahDao
) : SurahLocalDataSource {
    override fun getAllSurahs(): Flow<List<SurahEntity>> = dao.getAllSurahs()
    override suspend fun getSurahById(id: Int): SurahEntity? = dao.getSurahById(id)
}
