package io.github.dijksys.quipu.domain.repository

import io.github.dijksys.quipu.domain.model.AppSettings
import io.github.dijksys.quipu.domain.model.ThemeMode
import kotlinx.coroutines.flow.Flow

interface SettingsRepository {
    fun observeSettings(): Flow<AppSettings>
    suspend fun setThemeMode(mode: ThemeMode)
    suspend fun setNotificationsEnabled(enabled: Boolean)
}
