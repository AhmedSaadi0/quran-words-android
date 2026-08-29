package io.github.ahmedsaadi0.quranwords.core.di

import android.content.Context
import androidx.room.Room
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.components.SingletonComponent
import io.github.ahmedsaadi0.quranwords.data.local.QuranDatabase
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
object DatabaseModule {
    @Provides
    @Singleton
    fun provideDatabase(@ApplicationContext context: Context): QuranDatabase =
        Room.databaseBuilder(context, QuranDatabase::class.java, "quran_words.db")
            .createFromFile(context.getDatabasePath("quran_words.db"))
            .fallbackToDestructiveMigration(false)
            .build()

    @Provides
    fun provideSurahDao(db: QuranDatabase) = db.surahDao()

    @Provides
    fun provideAyahDao(db: QuranDatabase) = db.ayahDao()

    @Provides
    fun provideWordDao(db: QuranDatabase) = db.wordDao()

    @Provides
    fun provideRootDao(db: QuranDatabase) = db.rootDao()

    @Provides
    fun provideMasdarDao(db: QuranDatabase) = db.masdarDao()

    @Provides
    fun provideDerivativeDao(db: QuranDatabase) = db.derivativeDao()
}
