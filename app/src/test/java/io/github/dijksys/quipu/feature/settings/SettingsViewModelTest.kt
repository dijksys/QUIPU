package io.github.dijksys.quipu.feature.settings

import io.github.dijksys.quipu.domain.model.Reminder
import io.github.dijksys.quipu.domain.model.ThemeMode
import io.github.dijksys.quipu.domain.usecase.reminders.RescheduleRemindersUseCase
import io.github.dijksys.quipu.domain.usecase.settings.ObserveSettingsUseCase
import io.github.dijksys.quipu.domain.usecase.settings.SetNotificationsEnabledUseCase
import io.github.dijksys.quipu.domain.usecase.settings.SetThemeModeUseCase
import io.github.dijksys.quipu.fakes.FakeReminderAlarmScheduler
import io.github.dijksys.quipu.fakes.FakeReminderRepository
import io.github.dijksys.quipu.fakes.FakeSettingsRepository
import io.github.dijksys.quipu.fakes.TestClock
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.launch
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test

@OptIn(ExperimentalCoroutinesApi::class)
class SettingsViewModelTest {

    private val dispatcher = StandardTestDispatcher()
    private val clock = TestClock()
    private lateinit var settings: FakeSettingsRepository
    private lateinit var reminders: FakeReminderRepository
    private lateinit var scheduler: FakeReminderAlarmScheduler

    @Before
    fun setUp() {
        Dispatchers.setMain(dispatcher)
        settings = FakeSettingsRepository()
        reminders = FakeReminderRepository()
        scheduler = FakeReminderAlarmScheduler()
    }

    @After
    fun tearDown() {
        Dispatchers.resetMain()
    }

    @Test
    fun `theme mode is persisted and exposed`() = runTest(dispatcher) {
        val viewModel = createViewModel()
        backgroundScope.launch { viewModel.uiState.collect { } }
        advanceUntilIdle()

        for (mode in ThemeMode.entries) {
            viewModel.setThemeMode(mode)
            advanceUntilIdle()

            assertEquals(mode, settings.current.themeMode)
            assertEquals(mode, viewModel.uiState.value.themeMode)
        }
    }

    @Test
    fun `disabling notifications cancels pending alarms`() = runTest(dispatcher) {
        reminders.preload(
            Reminder(
                id = "r1",
                title = "Aviso",
                description = null,
                noteId = null,
                scheduledAt = clock.currentTime + 60_000,
                createdAt = clock.currentTime,
                updatedAt = clock.currentTime,
            ),
        )
        val viewModel = createViewModel()
        backgroundScope.launch { viewModel.uiState.collect { } }
        advanceUntilIdle()

        viewModel.setNotificationsEnabled(false)
        advanceUntilIdle()

        assertFalse(settings.current.notificationsEnabled)
        assertFalse(viewModel.uiState.value.notificationsEnabled)
        assertTrue(scheduler.cancelled.contains("r1"))
    }

    @Test
    fun `enabling notifications reschedules future pending reminders`() = runTest(dispatcher) {
        reminders.preload(
            Reminder(
                id = "r1",
                title = "Aviso",
                description = null,
                noteId = null,
                scheduledAt = clock.currentTime + 60_000,
                createdAt = clock.currentTime,
                updatedAt = clock.currentTime,
            ),
        )
        settings.setNotificationsEnabled(false)
        val viewModel = createViewModel()
        backgroundScope.launch { viewModel.uiState.collect { } }
        advanceUntilIdle()

        viewModel.setNotificationsEnabled(true)
        advanceUntilIdle()

        assertTrue(settings.current.notificationsEnabled)
        assertTrue(scheduler.scheduled.containsKey("r1"))
    }

    private fun createViewModel(): SettingsViewModel {
        val reschedule = RescheduleRemindersUseCase(reminders, settings, scheduler, clock)
        return SettingsViewModel(
            observeSettings = ObserveSettingsUseCase(settings),
            setThemeModeUseCase = SetThemeModeUseCase(settings),
            setNotificationsEnabledUseCase = SetNotificationsEnabledUseCase(settings, reschedule),
        )
    }
}
