package com.example.quipu.domain.usecase.settings

import com.example.quipu.domain.model.AppSettings
import com.example.quipu.domain.repository.SettingsRepository
import kotlinx.coroutines.flow.Flow
import javax.inject.Inject

class ObserveSettingsUseCase @Inject constructor(
    private val settingsRepository: SettingsRepository,
) {
    operator fun invoke(): Flow<AppSettings> = settingsRepository.observeSettings()
}
