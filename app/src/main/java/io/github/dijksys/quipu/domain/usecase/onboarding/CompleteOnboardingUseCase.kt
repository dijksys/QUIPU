package io.github.dijksys.quipu.domain.usecase.onboarding

import io.github.dijksys.quipu.domain.repository.SettingsRepository
import javax.inject.Inject

class CompleteOnboardingUseCase @Inject constructor(
    private val settingsRepository: SettingsRepository,
) {
    suspend operator fun invoke() {
        settingsRepository.completeOnboarding()
    }
}
