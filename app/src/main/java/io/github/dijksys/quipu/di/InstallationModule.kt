package io.github.dijksys.quipu.di

import io.github.dijksys.quipu.core.installation.AndroidInstallationHistory
import io.github.dijksys.quipu.domain.installation.InstallationHistory
import dagger.Binds
import dagger.Module
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
abstract class InstallationModule {

    @Binds
    @Singleton
    abstract fun bindInstallationHistory(
        impl: AndroidInstallationHistory,
    ): InstallationHistory
}
