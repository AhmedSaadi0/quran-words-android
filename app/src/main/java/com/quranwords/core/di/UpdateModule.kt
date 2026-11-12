package com.quranwords.core.di

import android.content.Context
import com.google.android.play.core.appupdate.AppUpdateManager
import com.google.android.play.core.appupdate.AppUpdateManagerFactory
import com.quranwords.data.remote.PlayAppUpdateDataSource
import com.quranwords.domain.repository.AppUpdateRepository
import com.quranwords.ui.update.AppUpdateFlowLauncher
import dagger.Binds
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import dagger.hilt.android.qualifiers.ApplicationContext
import javax.inject.Singleton

/**
 * Play In-App Updates wiring (Decision #29). The AppUpdateManager only needs an
 * application Context; the ActivityResultLauncher always stays with the composition.
 */
@Module
@InstallIn(SingletonComponent::class)
object UpdateProvidesModule {

    @Provides
    @Singleton
    fun provideAppUpdateManager(@ApplicationContext context: Context): AppUpdateManager =
        AppUpdateManagerFactory.create(context)
}

@Module
@InstallIn(SingletonComponent::class)
abstract class UpdateBindingsModule {

    @Binds
    @Singleton
    abstract fun bindAppUpdateRepository(impl: PlayAppUpdateDataSource): AppUpdateRepository

    @Binds
    @Singleton
    abstract fun bindAppUpdateFlowLauncher(impl: PlayAppUpdateDataSource): AppUpdateFlowLauncher
}
