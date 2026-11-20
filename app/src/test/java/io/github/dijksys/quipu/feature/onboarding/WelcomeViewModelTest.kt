package io.github.dijksys.quipu.feature.onboarding

import io.github.dijksys.quipu.domain.usecase.onboarding.CompleteOnboardingUseCase
import io.github.dijksys.quipu.domain.usecase.onboarding.ObserveOnboardingCompletedUseCase
import io.github.dijksys.quipu.domain.usecase.onboarding.ResolveOnboardingCompletedUseCase
import io.github.dijksys.quipu.fakes.FakeInstallationHistory
import io.github.dijksys.quipu.fakes.FakeSettingsRepository
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotEquals
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test

@OptIn(ExperimentalCoroutinesApi::class)
class WelcomeViewModelTest {

    private val dispatcher = StandardTestDispatcher()
    private lateinit var settings: FakeSettingsRepository
    private lateinit var history: FakeInstallationHistory

    @Before
    fun setUp() {
        Dispatchers.setMain(dispatcher)
        settings = FakeSettingsRepository()
        history = FakeInstallationHistory()
    }

    @After
    fun tearDown() {
        Dispatchers.resetMain()
    }

    @Test
    fun `first install shows welcome`() = runTest(dispatcher) {
        val viewModel = createViewModel()
        advanceUntilIdle()

        assertEquals(WelcomeStatus.READY, viewModel.uiState.value.status)
        assertEquals(false, settings.current.onboardingCompleted)
    }

    @Test
    fun `existing updated installation skips welcome`() = runTest(dispatcher) {
        history.priorInstallation = true
        val viewModel = createViewModel()
        advanceUntilIdle()

        assertEquals(WelcomeStatus.COMPLETED, viewModel.uiState.value.status)
        assertEquals(true, settings.current.onboardingCompleted)
    }

    @Test
    fun `initial decision is preserved after restart`() = runTest(dispatcher) {
        val first = createViewModel()
        advanceUntilIdle()
        assertEquals(WelcomeStatus.READY, first.uiState.value.status)

        history.priorInstallation = true
        val restarted = createViewModel()
        advanceUntilIdle()

        assertEquals(WelcomeStatus.READY, restarted.uiState.value.status)
        assertEquals(false, settings.current.onboardingCompleted)
    }

    @Test
    fun `completing onboarding persists true and opens the main flow`() = runTest(dispatcher) {
        val viewModel = createViewModel()
        advanceUntilIdle()

        viewModel.onContinue()
        advanceUntilIdle()

        assertEquals(WelcomeStatus.COMPLETED, viewModel.uiState.value.status)
        assertEquals(true, settings.current.onboardingCompleted)
    }

    @Test
    fun `second opening does not show welcome again`() = runTest(dispatcher) {
        val first = createViewModel()
        advanceUntilIdle()
        first.onContinue()
        advanceUntilIdle()

        val second = createViewModel()
        advanceUntilIdle()

        assertEquals(WelcomeStatus.COMPLETED, second.uiState.value.status)
    }

    @Test
    fun `closing before completing keeps welcome visible`() = runTest(dispatcher) {
        val first = createViewModel()
        advanceUntilIdle()
        assertEquals(WelcomeStatus.READY, first.uiState.value.status)

        val reopened = createViewModel()
        advanceUntilIdle()

        assertEquals(WelcomeStatus.READY, reopened.uiState.value.status)
        assertEquals(false, settings.current.onboardingCompleted)
    }

    @Test
    fun `write failure keeps welcome and does not complete`() = runTest(dispatcher) {
        val viewModel = createViewModel()
        advanceUntilIdle()
        settings.failOnboardingWrite = true

        viewModel.onContinue()
        advanceUntilIdle()

        assertEquals(WelcomeStatus.ERROR, viewModel.uiState.value.status)
        assertEquals(WelcomeError.COMPLETION, viewModel.uiState.value.error)
        assertNotEquals(WelcomeStatus.COMPLETED, viewModel.uiState.value.status)
        assertFalse(settings.current.onboardingCompleted ?: false)
    }

    @Test
    fun `retry after write failure completes correctly`() = runTest(dispatcher) {
        val viewModel = createViewModel()
        advanceUntilIdle()
        settings.failOnboardingWrite = true
        viewModel.onContinue()
        advanceUntilIdle()

        settings.failOnboardingWrite = false
        viewModel.onContinue()
        advanceUntilIdle()

        assertEquals(WelcomeStatus.COMPLETED, viewModel.uiState.value.status)
        assertEquals(true, settings.current.onboardingCompleted)
    }

    @Test
    fun `initialization failure can be retried`() = runTest(dispatcher) {
        settings.failOnboardingRead = true
        val viewModel = createViewModel()
        advanceUntilIdle()

        assertEquals(WelcomeStatus.ERROR, viewModel.uiState.value.status)
        assertEquals(WelcomeError.INITIALIZATION, viewModel.uiState.value.error)

        settings.failOnboardingRead = false
        viewModel.onContinue()
        advanceUntilIdle()

        assertEquals(WelcomeStatus.READY, viewModel.uiState.value.status)
        assertEquals(false, settings.current.onboardingCompleted)
    }

    @Test
    fun `second tap while completing is ignored`() = runTest(dispatcher) {
        val viewModel = createViewModel()
        advanceUntilIdle()

        viewModel.onContinue()
        viewModel.onContinue()
        advanceUntilIdle()

        assertEquals(WelcomeStatus.COMPLETED, viewModel.uiState.value.status)
        assertTrue(settings.current.onboardingCompleted == true)
    }

    private fun createViewModel(): WelcomeViewModel = WelcomeViewModel(
        observeOnboardingCompleted = ObserveOnboardingCompletedUseCase(settings),
        resolveOnboardingCompleted = ResolveOnboardingCompletedUseCase(settings, history),
        completeOnboarding = CompleteOnboardingUseCase(settings),
    )
}
