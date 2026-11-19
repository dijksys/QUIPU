package com.example.quipu.feature.settings

import com.example.quipu.domain.model.Reminder
import com.example.quipu.domain.model.ThemeMode
import com.example.quipu.domain.usecase.reminders.RescheduleRemindersUseCase
import com.example.quipu.domain.usecase.settings.ObserveSettingsUseCase
import com.example.quipu.domain.usecase.settings.SetNotificationsEnabledUseCase
import com.example.quipu.domain.usecase.settings.SetThemeModeUseCase
import com.example.quipu.fakes.FakeReminderAlarmScheduler
import com.example.quipu.fakes.FakeReminderRepository
import com.example.quipu.fakes.FakeSettingsRepository
import com.example.quipu.fakes.TestClock
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

        viewModel.setThemeMode(ThemeMode.DARK)
        advanceUntilIdle()

        assertEquals(ThemeMode.DARK, settings.current.themeMode)
        assertEquals(ThemeMode.DARK, viewModel.uiState.value.themeMode)
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
