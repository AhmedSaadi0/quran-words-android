package com.quranwords.core.di

import android.os.Build
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import com.quranwords.BuildConfig
import com.quranwords.core.util.BuildInfo
import java.util.Locale
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
object BuildInfoModule {

    @Singleton
    @Provides
    fun provideBuildInfo(): BuildInfo = BuildInfo(
        appVersion = "${BuildConfig.VERSION_NAME} (${BuildConfig.VERSION_CODE})",
        androidRelease = Build.VERSION.RELEASE ?: "",
        locale = runCatching { Locale.getDefault().toLanguageTag() }.getOrDefault("")
    )
}