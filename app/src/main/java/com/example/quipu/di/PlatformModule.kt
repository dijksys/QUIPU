package com.example.quipu.di

import com.example.quipu.core.common.Clock
import com.example.quipu.core.common.SystemClock
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import javax.inject.Qualifier
import javax.inject.Singleton

@Qualifier
@Retention(AnnotationRetention.BINARY)
annotation class ApplicationScope

@Module
@InstallIn(SingletonComponent::class)
object PlatformModule {

    @Provides
    @Singleton
    fun provideClock(): Clock = SystemClock()

    /**
     * Ámbito de aplicación para trabajo que debe sobrevivir a la pantalla actual
     * (por ejemplo, la finalización de un BroadcastReceiver). Nunca se usa GlobalScope.
     */
    @Provides
    @Singleton
    @ApplicationScope
    fun provideApplicationScope(): CoroutineScope =
        CoroutineScope(SupervisorJob() + Dispatchers.Default)
}
