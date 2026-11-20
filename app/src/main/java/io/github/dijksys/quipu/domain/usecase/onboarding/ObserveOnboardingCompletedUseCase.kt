package io.github.dijksys.quipu.domain.usecase.onboarding

import io.github.dijksys.quipu.domain.repository.SettingsRepository
import kotlinx.coroutines.flow.Flow
import javax.inject.Inject

class ObserveOnboardingCompletedUseCase @Inject constructor(
    private val settingsRepository: SettingsRepository,
) {
    operator fun invoke(): Flow<Boolean?> = settingsRepository.observeOnboardingCompleted()
}
