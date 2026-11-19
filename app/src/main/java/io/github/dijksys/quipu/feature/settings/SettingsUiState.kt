package io.github.dijksys.quipu.feature.settings

import io.github.dijksys.quipu.domain.model.ThemeMode

data class SettingsUiState(
    val isLoading: Boolean = true,
    val themeMode: ThemeMode = ThemeMode.SYSTEM,
    val notificationsEnabled: Boolean = true,
)

sealed interface SettingsEvent {
    data object Failure : SettingsEvent
}
