package io.github.dijksys.quipu.domain.usecase.onboarding

import io.github.dijksys.quipu.domain.installation.InstallationHistory
import io.github.dijksys.quipu.domain.repository.SettingsRepository
import javax.inject.Inject

/**
 * Resuelve una única vez si la bienvenida debe mostrarse. Si la preferencia
 * todavía no existe, usa la evidencia de instalación para decidir y la
 * persiste; las instalaciones actualizadas desde versiones sin onboarding se
 * consideran ya completadas.
 */
class ResolveOnboardingCompletedUseCase @Inject constructor(
    private val settingsRepository: SettingsRepository,
    private val installationHistory: InstallationHistory,
) {
    suspend operator fun invoke(): Boolean {
        settingsRepository.getOnboardingCompleted()?.let { return it }

        val completed = installationHistory.hadPriorInstallation()
        return settingsRepository.initializeOnboardingCompleted(completed)
    }
}
