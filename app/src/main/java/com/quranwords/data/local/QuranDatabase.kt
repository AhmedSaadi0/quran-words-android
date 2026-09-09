package com.quranwords.data.local

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import com.quranwords.data.local.dao.AyahDao
import com.quranwords.data.local.dao.DerivativeDao
import com.quranwords.data.local.dao.MasdarDao
import com.quranwords.data.local.dao.RootDao
import com.quranwords.data.local.dao.SurahDao
import com.quranwords.data.local.dao.WordDao
import com.quranwords.data.local.entities.AyahEntity
import com.quranwords.data.local.entities.DerivativeEntity
import com.quranwords.data.local.entities.MasdarEntity
import com.quranwords.data.local.entities.MorphologyEntity
import com.quranwords.data.local.entities.RootAiSummaryEntity
import com.quranwords.data.local.entities.RootEntity
import com.quranwords.data.local.entities.RootGlossEntity
import com.quranwords.data.local.entities.RootMeaningEntity
import com.quranwords.data.local.entities.SurahEntity
import com.quranwords.data.local.entities.WordAyahEntity
import com.quranwords.data.local.entities.WordEntity

@Database(
    entities = [
        SurahEntity::class,
        AyahEntity::class,
        WordEntity::class,
        WordAyahEntity::class,
        RootEntity::class,
        MorphologyEntity::class,
        MasdarEntity::class,
        DerivativeEntity::class,
        RootMeaningEntity::class,
        RootGlossEntity::class,
        RootAiSummaryEntity::class
    ],
    version = 2,
    exportSchema = false
)
abstract class QuranDatabase : RoomDatabase() {
    abstract fun surahDao(): SurahDao
    abstract fun ayahDao(): AyahDao
    abstract fun wordDao(): WordDao
    abstract fun rootDao(): RootDao
    abstract fun masdarDao(): MasdarDao
    abstract fun derivativeDao(): DerivativeDao

    companion object {
        @Volatile
        private var INSTANCE: QuranDatabase? = null

        fun getInstance(context: Context): QuranDatabase {
            return INSTANCE ?: synchronized(this) {
                val instance = Room.databaseBuilder(
                    context.applicationContext,
                    QuranDatabase::class.java,
                    "quran_words.db"
                )
                .fallbackToDestructiveMigration(false)
                .build()
                INSTANCE = instance
                instance
            }
        }

        fun closeIfNeeded() {
            synchronized(this) {
                try {
                    INSTANCE?.close()
                } catch (_: Exception) {
                }
                INSTANCE = null
            }
        }
    }
}
