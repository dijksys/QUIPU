package io.github.dijksys.quipu.domain.usecase.settings

import io.github.dijksys.quipu.domain.model.AppSettings
import io.github.dijksys.quipu.domain.repository.SettingsRepository
import kotlinx.coroutines.flow.Flow
import javax.inject.Inject

class ObserveSettingsUseCase @Inject constructor(
    private val settingsRepository: SettingsRepository,
) {
    operator fun invoke(): Flow<AppSettings> = settingsRepository.observeSettings()
}
