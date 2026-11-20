package io.github.dijksys.quipu.data.repository

import io.github.dijksys.quipu.core.datastore.AppSettingsDataStore
import io.github.dijksys.quipu.domain.model.AppSettings
import io.github.dijksys.quipu.domain.model.ThemeMode
import io.github.dijksys.quipu.domain.repository.SettingsRepository
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class SettingsRepositoryImpl @Inject constructor(
    private val dataStore: AppSettingsDataStore,
) : SettingsRepository {

    override fun observeSettings(): Flow<AppSettings> = dataStore.settings

    override fun observeOnboardingCompleted(): Flow<Boolean?> =
        dataStore.settings.map { it.onboardingCompleted }

    override suspend fun getOnboardingCompleted(): Boolean? =
        dataStore.settings.first().onboardingCompleted

    override suspend fun initializeOnboardingCompleted(completed: Boolean): Boolean =
        dataStore.initializeOnboardingCompleted(completed)

    override suspend fun completeOnboarding() {
        dataStore.setOnboardingCompleted(true)
    }

    override suspend fun setThemeMode(mode: ThemeMode) {
        dataStore.setThemeMode(mode)
    }

    override suspend fun setNotificationsEnabled(enabled: Boolean) {
        dataStore.setNotificationsEnabled(enabled)
    }
}
