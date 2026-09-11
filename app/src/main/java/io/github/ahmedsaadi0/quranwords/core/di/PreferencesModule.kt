package io.github.ahmedsaadi0.quranwords.core.di

import dagger.Binds
import dagger.Module
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import io.github.ahmedsaadi0.quranwords.core.datastore.UserPreferencesDataStore
import io.github.ahmedsaadi0.quranwords.domain.repository.UserPreferencesRepository
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
abstract class PreferencesModule {

    @Binds
    @Singleton
    abstract fun bindUserPreferencesRepository(
        impl: UserPreferencesDataStore
    ): UserPreferencesRepository
}