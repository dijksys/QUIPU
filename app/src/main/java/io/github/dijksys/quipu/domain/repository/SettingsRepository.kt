package io.github.dijksys.quipu.domain.repository

import io.github.dijksys.quipu.domain.model.AppSettings
import io.github.dijksys.quipu.domain.model.ThemeMode
import kotlinx.coroutines.flow.Flow

interface SettingsRepository {
    fun observeSettings(): Flow<AppSettings>
    fun observeOnboardingCompleted(): Flow<Boolean?>
    suspend fun getOnboardingCompleted(): Boolean?
    suspend fun initializeOnboardingCompleted(completed: Boolean): Boolean
    suspend fun completeOnboarding()
    suspend fun setThemeMode(mode: ThemeMode)
    suspend fun setNotificationsEnabled(enabled: Boolean)
}
