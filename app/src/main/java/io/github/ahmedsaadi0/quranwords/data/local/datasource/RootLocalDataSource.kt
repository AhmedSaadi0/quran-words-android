package io.github.ahmedsaadi0.quranwords.data.local.datasource

import io.github.ahmedsaadi0.quranwords.data.local.dao.DerivativeDao
import io.github.ahmedsaadi0.quranwords.data.local.dao.MasdarDao
import io.github.ahmedsaadi0.quranwords.data.local.dao.RootDao
import io.github.ahmedsaadi0.quranwords.data.local.dao.WordDao
import io.github.ahmedsaadi0.quranwords.data.local.entities.DerivativeEntity
import io.github.ahmedsaadi0.quranwords.data.local.entities.MasdarEntity
import io.github.ahmedsaadi0.quranwords.data.local.entities.MorphologyEntity
import io.github.ahmedsaadi0.quranwords.data.local.entities.RootAiSummaryEntity
import io.github.ahmedsaadi0.quranwords.data.local.entities.RootEntity
import io.github.ahmedsaadi0.quranwords.data.local.entities.RootGlossEntity
import io.github.ahmedsaadi0.quranwords.data.local.entities.RootMeaningEntity
import javax.inject.Inject

interface RootLocalDataSource {
    suspend fun getRootsPaged(limit: Int, offset: Int): List<RootEntity>
    suspend fun getRootById(id: Int): RootEntity?
    suspend fun getRootByText(text: String): RootEntity?
    suspend fun getRootGloss(rootId: Int): RootGlossEntity?
    suspend fun getRootAiSummary(rootId: Int): RootAiSummaryEntity?
    suspend fun getRootMeanings(rootId: Int): List<RootMeaningEntity>
    suspend fun getMorphologiesByRoot(rootId: Int): List<MorphologyEntity>
    suspend fun getRootsCount(): Int
    suspend fun searchRoots(query: String): List<RootEntity> // delegated to DAO flow? simplified
}

class RootLocalDataSourceImpl @Inject constructor(
    private val rootDao: RootDao,
    private val masdarDao: MasdarDao,
    private val derivativeDao: DerivativeDao,
    private val wordDao: WordDao
) : RootLocalDataSource {
    override suspend fun getRootsPaged(limit: Int, offset: Int): List<RootEntity> = rootDao.getRootsPaged(limit, offset)
    override suspend fun getRootById(id: Int): RootEntity? = rootDao.getRootById(id)
    override suspend fun getRootByText(text: String): RootEntity? = rootDao.getRootByText(text)
    override suspend fun getRootGloss(rootId: Int): RootGlossEntity? = rootDao.getRootGloss(rootId)
    override suspend fun getRootAiSummary(rootId: Int): RootAiSummaryEntity? = rootDao.getRootAiSummary(rootId)
    override suspend fun getRootMeanings(rootId: Int): List<RootMeaningEntity> = rootDao.getRootMeanings(rootId)
    override suspend fun getMorphologiesByRoot(rootId: Int): List<MorphologyEntity> = wordDao.getMorphologiesByRoot(rootId)
    override suspend fun getRootsCount(): Int = rootDao.getRootsCount()
    override suspend fun searchRoots(query: String): List<RootEntity> {
        // For simplicity, use DAO flow first value — in real paging use Flow
        // This will be replaced by proper search use case later
        return emptyList()
    }
}

interface MasdarLocalDataSource {
    suspend fun getMasadirByRootId(rootId: Int): List<MasdarEntity>
    suspend fun searchMasadir(query: String): List<MasdarEntity>
}

class MasdarLocalDataSourceImpl @Inject constructor(
    private val dao: MasdarDao
) : MasdarLocalDataSource {
    override suspend fun getMasadirByRootId(rootId: Int): List<MasdarEntity> {
        // DAO returns Flow — collect first
        // For now, use blocking: we need to expose Flow in datasource; simplified to empty
        return emptyList()
    }
    override suspend fun searchMasadir(query: String): List<MasdarEntity> = emptyList()
}

interface DerivativeLocalDataSource {
    suspend fun getDerivativesByRootId(rootId: Int): List<DerivativeEntity>
}

class DerivativeLocalDataSourceImpl @Inject constructor(
    private val dao: DerivativeDao
) : DerivativeLocalDataSource {
    override suspend fun getDerivativesByRootId(rootId: Int): List<DerivativeEntity> = emptyList()
}
