package io.github.ahmedsaadi0.quranwords.di

import android.content.Context
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.components.SingletonComponent
import io.github.ahmedsaadi0.quranwords.core.di.IoDispatcher
import io.github.ahmedsaadi0.quranwords.data.remote.DatabaseDownloadManager
import io.github.ahmedsaadi0.quranwords.data.remote.ManifestRemoteDataSource
import io.github.ahmedsaadi0.quranwords.data.repository.DbUpdateRepositoryImpl
import io.github.ahmedsaadi0.quranwords.data.repository.QuranRepositoryImpl
import io.github.ahmedsaadi0.quranwords.domain.repository.DbUpdateRepository
import io.github.ahmedsaadi0.quranwords.domain.repository.QuranRepository
import io.github.ahmedsaadi0.quranwords.domain.repository.UserPreferencesRepository
import io.github.ahmedsaadi0.quranwords.domain.usecase.CheckDbUpdateUseCase
import kotlinx.coroutines.CoroutineDispatcher
import okhttp3.OkHttpClient
import java.util.concurrent.TimeUnit
import javax.inject.Singleton

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
}