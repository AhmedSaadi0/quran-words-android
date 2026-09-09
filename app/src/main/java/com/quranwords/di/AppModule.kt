package com.quranwords.di

import android.content.Context
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.components.SingletonComponent
import com.quranwords.data.remote.DatabaseDownloadManager
import com.quranwords.data.remote.ManifestRemoteDataSource
import com.quranwords.data.repository.DbUpdateRepositoryImpl
import com.quranwords.data.repository.QuranRepositoryImpl
import com.quranwords.data.repository.UserPreferencesRepository
import com.quranwords.domain.repository.DbUpdateRepository
import com.quranwords.domain.repository.QuranRepository
import com.quranwords.domain.usecase.CheckDbUpdateUseCase
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.Dispatchers
import okhttp3.OkHttpClient
import java.util.concurrent.TimeUnit
import javax.inject.Qualifier
import javax.inject.Singleton

@Retention(AnnotationRetention.BINARY)
@Qualifier
annotation class IoDispatcher

@Retention(AnnotationRetention.BINARY)
@Qualifier
annotation class MainDispatcher

@Retention(AnnotationRetention.BINARY)
@Qualifier
annotation class DefaultDispatcher

@Module
@InstallIn(SingletonComponent::class)
object AppModule {

    @Provides
    @Singleton
    fun provideOkHttpClient(): OkHttpClient {
        return OkHttpClient.Builder()
            .followRedirects(true)
            .followSslRedirects(true)
            .connectTimeout(30, TimeUnit.SECONDS)
            .readTimeout(120, TimeUnit.SECONDS)
            .build()
    }

    @Provides
    @Singleton
    fun provideDatabaseDownloadManager(
        @ApplicationContext context: Context,
        client: OkHttpClient
    ): DatabaseDownloadManager {
        return DatabaseDownloadManager(context, client)
    }

    @Provides
    @Singleton
    fun provideManifestRemoteDataSource(
        client: OkHttpClient,
        @IoDispatcher ioDispatcher: CoroutineDispatcher
    ): ManifestRemoteDataSource {
        return ManifestRemoteDataSource(client, ioDispatcher)
    }

    @Provides
    @Singleton
    fun provideDbUpdateRepository(
        manifestSource: ManifestRemoteDataSource,
        preferences: UserPreferencesRepository,
        downloadManager: DatabaseDownloadManager
    ): DbUpdateRepository {
        return DbUpdateRepositoryImpl(manifestSource, preferences, downloadManager)
    }

    @Provides
    @Singleton
    fun provideCheckDbUpdateUseCase(
        repository: DbUpdateRepository
    ): CheckDbUpdateUseCase {
        return CheckDbUpdateUseCase(repository)
    }

    @Provides
    @Singleton
    fun provideQuranRepository(
        @ApplicationContext context: Context,
        downloadManager: DatabaseDownloadManager,
        @IoDispatcher ioDispatcher: CoroutineDispatcher
    ): QuranRepository {
        return QuranRepositoryImpl(context, downloadManager, ioDispatcher)
    }

    @Provides
    @Singleton
    fun provideUserPreferencesRepository(@ApplicationContext context: Context): UserPreferencesRepository {
        return UserPreferencesRepository(context)
    }

    @IoDispatcher
    @Provides
    fun provideIoDispatcher(): CoroutineDispatcher = Dispatchers.IO

    @MainDispatcher
    @Provides
    fun provideMainDispatcher(): CoroutineDispatcher = Dispatchers.Main

    @DefaultDispatcher
    @Provides
    fun provideDefaultDispatcher(): CoroutineDispatcher = Dispatchers.Default
}
