package com.example.data.local

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import com.example.data.local.dao.AyahDao
import com.example.data.local.dao.DerivativeDao
import com.example.data.local.dao.MasdarDao
import com.example.data.local.dao.RootDao
import com.example.data.local.dao.SurahDao
import com.example.data.local.dao.WordDao
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
    version = 1,
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
