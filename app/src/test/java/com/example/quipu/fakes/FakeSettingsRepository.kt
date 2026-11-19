package com.example.quipu.fakes

import com.example.quipu.domain.model.AppSettings
import com.example.quipu.domain.model.ThemeMode
import com.example.quipu.domain.repository.SettingsRepository
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.update

class FakeSettingsRepository(
    initial: AppSettings = AppSettings(),
) : SettingsRepository {

    private val state = MutableStateFlow(initial)

    val current: AppSettings get() = state.value

    override fun observeSettings(): Flow<AppSettings> = state

    override suspend fun setThemeMode(mode: ThemeMode) {
        state.update { it.copy(themeMode = mode) }
    }

    override suspend fun setNotificationsEnabled(enabled: Boolean) {
        state.update { it.copy(notificationsEnabled = enabled) }
    }
}
