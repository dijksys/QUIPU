package com.example.quipu.domain.usecase.settings

import com.example.quipu.domain.model.ThemeMode
import com.example.quipu.domain.repository.SettingsRepository
import javax.inject.Inject

class SetThemeModeUseCase @Inject constructor(
    private val settingsRepository: SettingsRepository,
) {
    suspend operator fun invoke(mode: ThemeMode) {
        settingsRepository.setThemeMode(mode)
    }
}
