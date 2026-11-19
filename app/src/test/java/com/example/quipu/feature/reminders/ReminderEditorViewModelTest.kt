package com.example.quipu.feature.reminders

import androidx.lifecycle.SavedStateHandle
import com.example.quipu.core.common.DateTimeUtils
import com.example.quipu.domain.model.Note
import com.example.quipu.domain.model.Reminder
import com.example.quipu.domain.model.ReminderStatus
import com.example.quipu.domain.usecase.notes.GetNoteUseCase
import com.example.quipu.domain.usecase.notes.ObserveNotesUseCase
import com.example.quipu.domain.usecase.reminders.CancelReminderUseCase
import com.example.quipu.domain.usecase.reminders.CompleteReminderUseCase
import com.example.quipu.domain.usecase.reminders.CreateReminderUseCase
import com.example.quipu.domain.usecase.reminders.DeleteReminderUseCase
import com.example.quipu.domain.usecase.reminders.GetReminderUseCase
import com.example.quipu.domain.usecase.reminders.UpdateReminderUseCase
import com.example.quipu.domain.usecase.settings.ObserveSettingsUseCase
import com.example.quipu.fakes.FakeNoteRepository
import com.example.quipu.fakes.FakeReminderAlarmScheduler
import com.example.quipu.fakes.FakeReminderNotifier
import com.example.quipu.fakes.FakeReminderRepository
import com.example.quipu.fakes.FakeSettingsRepository
import com.example.quipu.fakes.TestClock
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.async
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runCurrent
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test

@OptIn(ExperimentalCoroutinesApi::class)
class ReminderEditorViewModelTest {

    private val dispatcher = StandardTestDispatcher()
    private val clock = TestClock()
    private lateinit var reminders: FakeReminderRepository
    private lateinit var notes: FakeNoteRepository
    private lateinit var settings: FakeSettingsRepository
    private lateinit var scheduler: FakeReminderAlarmScheduler
    private lateinit var notifier: FakeReminderNotifier

    @Before
    fun setUp() {
        Dispatchers.setMain(dispatcher)
        reminders = FakeReminderRepository()
        notes = FakeNoteRepository()
        settings = FakeSettingsRepository()
        scheduler = FakeReminderAlarmScheduler()
        notifier = FakeReminderNotifier()
    }

    @After
    fun tearDown() {
        Dispatchers.resetMain()
    }

    @Test
    fun `new reminder linked to a note is prefilled`() = runTest(dispatcher) {
        notes.preload(note("n1", "Comprar pan"))
        val viewModel = createViewModel(noteId = "n1")
        runCurrent()

        val state = viewModel.uiState.value
        assertEquals("n1", state.noteId)
        assertEquals("Comprar pan", state.title)
        assertEquals("Comprar pan", state.linkedNoteTitle)
        assertTrue(state.scheduledAt > clock.currentTime)
    }

    @Test
    fun `title is required`() = runTest(dispatcher) {
        val viewModel = createViewModel()
        runCurrent()

        viewModel.onTitleChange("   ")
        viewModel.save()
        runCurrent()

        assertEquals(ReminderEditorError.TITLE_REQUIRED, viewModel.uiState.value.error)
        assertTrue(reminders.currentReminders.isEmpty())
    }

    @Test
    fun `past date is rejected`() = runTest(dispatcher) {
        val viewModel = createViewModel()
        runCurrent()
        viewModel.onTitleChange("Aviso")
        viewModel.setDate(DateTimeUtils.datePart(clock.currentTime - 86_400_000L))

        viewModel.save()
        runCurrent()

        assertEquals(ReminderEditorError.TIME_IN_PAST, viewModel.uiState.value.error)
        assertTrue(reminders.currentReminders.isEmpty())
    }

    @Test
    fun `new reminder is created and saved event is emitted`() = runTest(dispatcher) {
        val viewModel = createViewModel()
        runCurrent()
        viewModel.onTitleChange("Llamar al médico")
        viewModel.onDescriptionChange("Pedir cita")
        val event = async { viewModel.events.first() }

        viewModel.save()
        advanceUntilIdle()

        val stored = reminders.currentReminders.single()
        assertEquals("Llamar al médico", stored.title)
        assertEquals("Pedir cita", stored.description)
        assertEquals(ReminderStatus.PENDING, stored.status)
        assertEquals(ReminderEditorEvent.Saved, event.await())
        assertFalse(viewModel.uiState.value.isNew)
    }

    @Test
    fun `editing existing reminder updates schedule`() = runTest(dispatcher) {
        val existing = reminder("r1", clock.currentTime + 60_000)
        reminders.preload(existing)
        val viewModel = createViewModel(reminderId = "r1")
        runCurrent()
        assertEquals(existing.title, viewModel.uiState.value.title)

        clock.currentTime += 1_000
        val newTime = clock.currentTime + 120_000
        viewModel.setTime(DateTimeUtils.hour(newTime), DateTimeUtils.minute(newTime))
        viewModel.onTitleChange("Título editado")
        viewModel.save()
        advanceUntilIdle()

        val expectedTime = DateTimeUtils.withTime(
            existing.scheduledAt,
            DateTimeUtils.hour(newTime),
            DateTimeUtils.minute(newTime),
        )
        val stored = reminders.getStored("r1")!!
        assertEquals("Título editado", stored.title)
        assertEquals(expectedTime, stored.scheduledAt)
        assertNotNull(scheduler.scheduled["r1"])
    }

    @Test
    fun `complete and delete actions emit events`() = runTest(dispatcher) {
        val existing = reminder("r1", clock.currentTime + 60_000)
        reminders.preload(existing)
        val viewModel = createViewModel(reminderId = "r1")
        runCurrent()

        val completeEvent = async { viewModel.events.first() }
        viewModel.complete()
        runCurrent()
        assertEquals(ReminderEditorEvent.Completed, completeEvent.await())
        assertEquals(ReminderStatus.COMPLETED, reminders.getStored("r1")!!.status)

        val deleteEvent = async { viewModel.events.first() }
        viewModel.delete()
        runCurrent()
        assertEquals(ReminderEditorEvent.Deleted, deleteEvent.await())
        assertTrue(reminders.currentReminders.isEmpty())
    }

    @Test
    fun `save failure is exposed as editor error`() = runTest(dispatcher) {
        val viewModel = createViewModel()
        runCurrent()
        viewModel.onTitleChange("No se guardará")
        reminders.failWrites = true

        viewModel.save()
        advanceUntilIdle()

        assertEquals(ReminderEditorError.SAVE_FAILED, viewModel.uiState.value.error)
        assertFalse(viewModel.uiState.value.isSaving)
    }

    @Test
    fun `missing reminder exposes missing state`() = runTest(dispatcher) {
        val viewModel = createViewModel(reminderId = "no-existe")
        runCurrent()

        assertTrue(viewModel.uiState.value.isMissing)
        assertEquals(ReminderEditorError.NOT_FOUND, viewModel.uiState.value.error)
    }

    private fun createViewModel(
        reminderId: String = ReminderEditorViewModel.NEW_REMINDER_ID,
        noteId: String? = null,
    ): ReminderEditorViewModel {
        val args = mutableMapOf<String, String>(
            ReminderEditorViewModel.ARG_REMINDER_ID to reminderId,
        )
        noteId?.let { args[ReminderEditorViewModel.ARG_NOTE_ID] = it }
        return ReminderEditorViewModel(
            savedStateHandle = SavedStateHandle(args),
            getReminder = GetReminderUseCase(reminders),
            getNote = GetNoteUseCase(notes),
            observeNotes = ObserveNotesUseCase(notes),
            observeSettings = ObserveSettingsUseCase(settings),
            createReminder = CreateReminderUseCase(reminders, settings, scheduler, clock),
            updateReminder = UpdateReminderUseCase(reminders, settings, scheduler, clock),
            deleteReminder = DeleteReminderUseCase(reminders, scheduler, notifier),
            completeReminder = CompleteReminderUseCase(reminders, scheduler, notifier, clock),
            cancelReminder = CancelReminderUseCase(reminders, scheduler, notifier, clock),
            clock = clock,
        )
    }

    private fun note(id: String, title: String) = Note(
        id = id,
        title = title,
        content = "",
        createdAt = clock.currentTime,
        updatedAt = clock.currentTime,
    )

    private fun reminder(id: String, scheduledAt: Long) = Reminder(
        id = id,
        title = "Recordatorio",
        description = null,
        noteId = null,
        scheduledAt = scheduledAt,
        createdAt = clock.currentTime,
        updatedAt = clock.currentTime,
    )
}
