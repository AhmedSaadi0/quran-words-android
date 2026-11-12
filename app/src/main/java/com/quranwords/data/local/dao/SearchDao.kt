package com.quranwords.data.local.dao

import androidx.room.Dao
import androidx.room.RawQuery
import androidx.sqlite.db.SimpleSQLiteQuery
import com.quranwords.data.local.entities.AyahEntity
import com.quranwords.data.local.entities.RootEntity
import com.quranwords.data.local.entities.WordEntity

/**
 * Dynamic search DAO (Full Plan v2, Fix #2 + Fix #4).
 *
 * Every multi-word / word / top-roots query is built by
 * [com.quranwords.data.local.search.SearchQueryBuilder] as a [SimpleSQLiteQuery]
 * with `bindArgs` — user input never touches the SQL text.
 * `observedEntities` keeps Room's invalidation correct.
 */
@Dao
interface SearchDao {
    @RawQuery(observedEntities = [AyahEntity::class])
    suspend fun searchAyat(query: SimpleSQLiteQuery): List<AyahEntity>

    @RawQuery(observedEntities = [WordEntity::class])
    suspend fun searchWords(query: SimpleSQLiteQuery): List<WordEntity>

    @RawQuery(observedEntities = [RootEntity::class])
    suspend fun topRoots(query: SimpleSQLiteQuery): List<RootEntity>

    @androidx.room.Query("SELECT * FROM ayat WHERE surah = :surahId AND ayah = :ayahNum LIMIT 1")
    suspend fun directAyah(surahId: Int, ayahNum: Int): AyahEntity?
}
