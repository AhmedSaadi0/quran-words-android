package io.github.ahmedsaadi0.quranwords.data.local

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import io.github.ahmedsaadi0.quranwords.data.local.dao.AyahDao
import io.github.ahmedsaadi0.quranwords.data.local.dao.DerivativeDao
import io.github.ahmedsaadi0.quranwords.data.local.dao.MasdarDao
import io.github.ahmedsaadi0.quranwords.data.local.dao.RootDao
import io.github.ahmedsaadi0.quranwords.data.local.dao.SurahDao
import io.github.ahmedsaadi0.quranwords.data.local.dao.WordDao
import io.github.ahmedsaadi0.quranwords.data.local.entities.AyahEntity
import io.github.ahmedsaadi0.quranwords.data.local.entities.DerivativeEntity
import io.github.ahmedsaadi0.quranwords.data.local.entities.MasdarEntity
import io.github.ahmedsaadi0.quranwords.data.local.entities.MorphologyEntity
import io.github.ahmedsaadi0.quranwords.data.local.entities.RootAiSummaryEntity
import io.github.ahmedsaadi0.quranwords.data.local.entities.RootEntity
import io.github.ahmedsaadi0.quranwords.data.local.entities.RootGlossEntity
import io.github.ahmedsaadi0.quranwords.data.local.entities.RootMeaningEntity
import io.github.ahmedsaadi0.quranwords.data.local.entities.SurahEntity
import io.github.ahmedsaadi0.quranwords.data.local.entities.WordAyahEntity
import io.github.ahmedsaadi0.quranwords.data.local.entities.WordEntity

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
