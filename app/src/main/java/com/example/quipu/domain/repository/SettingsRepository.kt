package com.example.quipu.domain.repository

import com.example.quipu.domain.model.AppSettings
import com.example.quipu.domain.model.ThemeMode
import kotlinx.coroutines.flow.Flow

interface SettingsRepository {
    fun observeSettings(): Flow<AppSettings>
    suspend fun setThemeMode(mode: ThemeMode)
    suspend fun setNotificationsEnabled(enabled: Boolean)
}
