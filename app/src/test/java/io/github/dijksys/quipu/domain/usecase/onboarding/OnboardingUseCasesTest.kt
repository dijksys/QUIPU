package io.github.dijksys.quipu.domain.usecase.onboarding

import io.github.dijksys.quipu.domain.model.AppSettings
import io.github.dijksys.quipu.domain.model.ThemeMode
import io.github.dijksys.quipu.fakes.FakeInstallationHistory
import io.github.dijksys.quipu.fakes.FakeSettingsRepository
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class OnboardingUseCasesTest {

    @Test
    fun `fresh install resolves onboarding as incomplete and persists it`() = runTest {
        val settings = FakeSettingsRepository()
        val history = FakeInstallationHistory(priorInstallation = false)
        val useCase = ResolveOnboardingCompletedUseCase(settings, history)

        val completed = useCase()

        assertFalse(completed)
        assertEquals(false, settings.current.onboardingCompleted)
    }

    @Test
    fun `updated installation resolves onboarding as completed and persists it`() = runTest {
        val settings = FakeSettingsRepository()
        val history = FakeInstallationHistory(priorInstallation = true)
        val useCase = ResolveOnboardingCompletedUseCase(settings, history)

        val completed = useCase()

        assertTrue(completed)
        assertEquals(true, settings.current.onboardingCompleted)
    }

    @Test
    fun `stored incomplete decision wins over installation history`() = runTest {
        val settings = FakeSettingsRepository(
            AppSettings(onboardingCompleted = false),
        )
        val history = FakeInstallationHistory(priorInstallation = true)
        val useCase = ResolveOnboardingCompletedUseCase(settings, history)

        assertFalse(useCase())
        assertEquals(false, settings.current.onboardingCompleted)
    }

    @Test
    fun `stored completed decision wins over installation history`() = runTest {
        val settings = FakeSettingsRepository(
            AppSettings(onboardingCompleted = true),
        )
        val history = FakeInstallationHistory(priorInstallation = false)
        val useCase = ResolveOnboardingCompletedUseCase(settings, history)

        assertTrue(useCase())
        assertEquals(true, settings.current.onboardingCompleted)
    }

    @Test
    fun `resolving onboarding preserves existing theme and notifications`() = runTest {
        val settings = FakeSettingsRepository(
            AppSettings(
                themeMode = ThemeMode.DARK,
                notificationsEnabled = false,
            ),
        )
        val useCase = ResolveOnboardingCompletedUseCase(
            settings,
            FakeInstallationHistory(priorInstallation = true),
        )

        useCase()

        assertEquals(ThemeMode.DARK, settings.current.themeMode)
        assertFalse(settings.current.notificationsEnabled)
        assertEquals(true, settings.current.onboardingCompleted)
    }

    @Test
    fun `completing onboarding preserves existing theme and notifications`() = runTest {
        val settings = FakeSettingsRepository(
            AppSettings(
                themeMode = ThemeMode.LIGHT,
                notificationsEnabled = true,
                onboardingCompleted = false,
            ),
        )
        val useCase = CompleteOnboardingUseCase(settings)

        useCase()

        assertEquals(ThemeMode.LIGHT, settings.current.themeMode)
        assertTrue(settings.current.notificationsEnabled)
        assertEquals(true, settings.current.onboardingCompleted)
    }
}
