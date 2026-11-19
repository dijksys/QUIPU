package io.github.dijksys.quipu.feature.reminders

import androidx.lifecycle.SavedStateHandle
import io.github.dijksys.quipu.domain.model.AppSettings
import io.github.dijksys.quipu.domain.model.Reminder
import io.github.dijksys.quipu.domain.model.ReminderFilter
import io.github.dijksys.quipu.domain.model.ReminderStatus
import io.github.dijksys.quipu.domain.usecase.reminders.CancelReminderUseCase
import io.github.dijksys.quipu.domain.usecase.reminders.CompleteReminderUseCase
import io.github.dijksys.quipu.domain.usecase.reminders.DeleteReminderUseCase
import io.github.dijksys.quipu.domain.usecase.reminders.ObserveRemindersUseCase
import io.github.dijksys.quipu.domain.usecase.settings.ObserveSettingsUseCase
import io.github.dijksys.quipu.fakes.FakeReminderAlarmScheduler
import io.github.dijksys.quipu.fakes.FakeReminderNotifier
import io.github.dijksys.quipu.fakes.FakeReminderRepository
import io.github.dijksys.quipu.fakes.FakeSettingsRepository
import io.github.dijksys.quipu.fakes.TestClock
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.async
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.advanceTimeBy
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runCurrent
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test

@OptIn(ExperimentalCoroutinesApi::class)
class RemindersViewModelTest {

    private val dispatcher = StandardTestDispatcher()
    private val clock = TestClock()
    private lateinit var repository: FakeReminderRepository
    private lateinit var settings: FakeSettingsRepository
    private lateinit var scheduler: FakeReminderAlarmScheduler
    private lateinit var notifier: FakeReminderNotifier

    @Before
    fun setUp() {
        Dispatchers.setMain(dispatcher)
        repository = FakeReminderRepository()
        settings = FakeSettingsRepository()
        scheduler = FakeReminderAlarmScheduler()
        notifier = FakeReminderNotifier()
    }

    @After
    fun tearDown() {
        Dispatchers.resetMain()
    }

    @Test
    fun `reminders are categorized into pending overdue completed and cancelled`() =
        runTest(dispatcher) {
            repository.preload(
                reminder("future", clock.currentTime + 60_000),
                reminder("overdue", clock.currentTime - 60_000),
                reminder("done", clock.currentTime - 120_000, ReminderStatus.COMPLETED),
                reminder("cancelled", clock.currentTime + 30_000, ReminderStatus.CANCELLED),
            )
            val viewModel = createViewModel()
            backgroundScope.launch { viewModel.uiState.collect { } }
            runCurrent()

            val state = viewModel.uiState.value
            assertEquals(listOf("future"), state.pending.map { it.id })
            assertEquals(listOf("overdue"), state.overdue.map { it.id })
            assertEquals(listOf("done"), state.completed.map { it.id })
            assertEquals(listOf("cancelled"), state.cancelled.map { it.id })
            assertEquals(listOf("future"), state.visibleReminders.map { it.id })
        }

    @Test
    fun `filter changes visible list`() = runTest(dispatcher) {
        repository.preload(
            reminder("future", clock.currentTime + 60_000),
            reminder("overdue", clock.currentTime - 60_000),
        )
        val viewModel = createViewModel()
        backgroundScope.launch { viewModel.uiState.collect { } }
        runCurrent()

        viewModel.setFilter(ReminderFilter.OVERDUE)
        runCurrent()

        assertEquals(ReminderFilter.OVERDUE, viewModel.uiState.value.filter)
        assertEquals(listOf("overdue"), viewModel.uiState.value.visibleReminders.map { it.id })
    }

    @Test
    fun `completing reminder updates status and emits event`() = runTest(dispatcher) {
        val target = reminder("future", clock.currentTime + 60_000)
        repository.preload(target)
        val viewModel = createViewModel()
        backgroundScope.launch { viewModel.uiState.collect { } }
        runCurrent()
        val event = async { viewModel.events.first() }

        viewModel.complete(target)
        runCurrent()

        assertEquals(ReminderStatus.COMPLETED, repository.getStored("future")!!.status)
        assertTrue(scheduler.cancelled.contains("future"))
        assertEquals(RemindersEvent.Completed, event.await())
    }

    @Test
    fun `overdue category refreshes when time passes`() = runTest(dispatcher) {
        repository.preload(reminder("future", clock.currentTime + 60_000))
        val viewModel = createViewModel()
        backgroundScope.launch { viewModel.uiState.collect { } }
        runCurrent()
        assertEquals(1, viewModel.uiState.value.pending.size)

        clock.currentTime += 120_000
        advanceTimeBy(60_000)
        runCurrent()

        assertTrue(viewModel.uiState.value.pending.isEmpty())
        assertEquals(listOf("future"), viewModel.uiState.value.overdue.map { it.id })
    }

    @Test
    fun `settings disabled is exposed in state`() = runTest(dispatcher) {
        settings = FakeSettingsRepository(AppSettings(notificationsEnabled = false))
        val viewModel = createViewModel()
        backgroundScope.launch { viewModel.uiState.collect { } }
        runCurrent()

        assertEquals(false, viewModel.uiState.value.notificationsEnabled)
    }

    private fun createViewModel(): RemindersViewModel = RemindersViewModel(
        savedStateHandle = SavedStateHandle(),
        observeReminders = ObserveRemindersUseCase(repository),
        observeSettings = ObserveSettingsUseCase(settings),
        completeReminder = CompleteReminderUseCase(repository, scheduler, notifier, clock),
        cancelReminder = CancelReminderUseCase(repository, scheduler, notifier, clock),
        deleteReminder = DeleteReminderUseCase(repository, scheduler, notifier),
        clock = clock,
    )

    private fun reminder(
        id: String,
        scheduledAt: Long,
        status: ReminderStatus = ReminderStatus.PENDING,
    ) = Reminder(
        id = id,
        title = "Recordatorio $id",
        description = null,
        noteId = null,
        scheduledAt = scheduledAt,
        createdAt = clock.currentTime,
        updatedAt = clock.currentTime,
        completedAt = if (status == ReminderStatus.COMPLETED) clock.currentTime else null,
        status = status,
    )
}
