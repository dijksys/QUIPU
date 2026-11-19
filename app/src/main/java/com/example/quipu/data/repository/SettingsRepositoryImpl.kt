package com.example.quipu.data.repository

import com.example.quipu.core.datastore.AppSettingsDataStore
import com.example.quipu.domain.model.AppSettings
import com.example.quipu.domain.model.ThemeMode
import com.example.quipu.domain.repository.SettingsRepository
import kotlinx.coroutines.flow.Flow
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class SettingsRepositoryImpl @Inject constructor(
    private val dataStore: AppSettingsDataStore,
) : SettingsRepository {

    override fun observeSettings(): Flow<AppSettings> = dataStore.settings

    override suspend fun setThemeMode(mode: ThemeMode) {
        dataStore.setThemeMode(mode)
    }

    override suspend fun setNotificationsEnabled(enabled: Boolean) {
        dataStore.setNotificationsEnabled(enabled)
    }
}
