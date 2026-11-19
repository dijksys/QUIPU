package io.github.dijksys.quipu.domain.usecase.settings

import io.github.dijksys.quipu.domain.model.ThemeMode
import io.github.dijksys.quipu.domain.repository.SettingsRepository
import javax.inject.Inject

class SetThemeModeUseCase @Inject constructor(
    private val settingsRepository: SettingsRepository,
) {
    suspend operator fun invoke(mode: ThemeMode) {
        settingsRepository.setThemeMode(mode)
    }
}
