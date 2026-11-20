package io.github.dijksys.quipu.fakes

import io.github.dijksys.quipu.domain.model.AppSettings
import io.github.dijksys.quipu.domain.model.ThemeMode
import io.github.dijksys.quipu.domain.repository.SettingsRepository
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.update

class FakeSettingsRepository(
    initial: AppSettings = AppSettings(),
) : SettingsRepository {

    private val state = MutableStateFlow(initial)

    var failOnboardingRead: Boolean = false
    var failOnboardingWrite: Boolean = false

    val current: AppSettings get() = state.value

    override fun observeSettings(): Flow<AppSettings> = state

    override fun observeOnboardingCompleted(): Flow<Boolean?> =
        if (failOnboardingRead) {
            flow { error("Fallo de lectura de onboarding simulado") }
        } else {
            state.map { it.onboardingCompleted }
        }

    override suspend fun getOnboardingCompleted(): Boolean? {
        if (failOnboardingRead) error("Fallo de lectura de onboarding simulado")
        return state.value.onboardingCompleted
    }

    override suspend fun initializeOnboardingCompleted(completed: Boolean): Boolean {
        if (failOnboardingWrite) error("Fallo de escritura de onboarding simulado")
        var result = completed
        state.update { current ->
            val existing = current.onboardingCompleted
            if (existing != null) {
                result = existing
                current
            } else {
                current.copy(onboardingCompleted = completed)
            }
        }
        return result
    }

    override suspend fun completeOnboarding() {
        if (failOnboardingWrite) error("Fallo de escritura de onboarding simulado")
        state.update { it.copy(onboardingCompleted = true) }
    }

    override suspend fun setThemeMode(mode: ThemeMode) {
        state.update { it.copy(themeMode = mode) }
    }

    override suspend fun setNotificationsEnabled(enabled: Boolean) {
        state.update { it.copy(notificationsEnabled = enabled) }
    }
}
