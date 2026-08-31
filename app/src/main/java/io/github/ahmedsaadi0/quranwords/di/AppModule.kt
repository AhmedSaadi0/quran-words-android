package io.github.ahmedsaadi0.quranwords.di

import android.content.Context
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.components.SingletonComponent
import io.github.ahmedsaadi0.quranwords.data.remote.DatabaseDownloadManager
import io.github.ahmedsaadi0.quranwords.data.repository.QuranRepositoryImpl
import io.github.ahmedsaadi0.quranwords.data.repository.UserPreferencesRepository
import io.github.ahmedsaadi0.quranwords.domain.repository.QuranRepository
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.Dispatchers
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
    fun provideDatabaseDownloadManager(@ApplicationContext context: Context): DatabaseDownloadManager {
        return DatabaseDownloadManager(context)
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
