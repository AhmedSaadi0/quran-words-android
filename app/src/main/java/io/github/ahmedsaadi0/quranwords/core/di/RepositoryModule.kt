package io.github.ahmedsaadi0.quranwords.core.di

import android.content.Context
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.components.SingletonComponent
import io.github.ahmedsaadi0.quranwords.data.local.QuranDatabase
import io.github.ahmedsaadi0.quranwords.data.remote.DatabaseDownloadManager
import io.github.ahmedsaadi0.quranwords.data.remote.datasource.DatabaseDownloadDataSource
import io.github.ahmedsaadi0.quranwords.data.remote.datasource.DatabaseDownloadDataSourceImpl
import io.github.ahmedsaadi0.quranwords.data.repository.QuranRepositoryImpl
import io.github.ahmedsaadi0.quranwords.data.repository.UserPreferencesRepository
import io.github.ahmedsaadi0.quranwords.domain.repository.QuranRepository
import okhttp3.OkHttpClient
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
object RepositoryModule {

    @Provides
    @Singleton
    fun provideDatabaseDownloadManager(
        @ApplicationContext context: Context,
        client: OkHttpClient
    ): DatabaseDownloadManager = DatabaseDownloadManager(context, client)

    @Provides
    @Singleton
    fun provideDatabaseDownloadDataSource(
        @ApplicationContext context: Context,
        client: OkHttpClient
    ): DatabaseDownloadDataSource = DatabaseDownloadDataSourceImpl(context, client)

    @Provides
    @Singleton
    fun provideQuranRepository(
        @ApplicationContext context: Context,
        downloadManager: DatabaseDownloadManager
    ): QuranRepository = QuranRepositoryImpl(context, downloadManager)

    @Provides
    @Singleton
    fun provideUserPreferencesRepository(
        @ApplicationContext context: Context
    ): UserPreferencesRepository = UserPreferencesRepository(context)
}
