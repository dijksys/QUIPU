package com.example.quipu.domain.usecase.reminders

import com.example.quipu.core.alarms.AlarmPrecision
import com.example.quipu.core.common.AppError
import com.example.quipu.core.common.AppResult
import com.example.quipu.domain.model.Reminder
import com.example.quipu.domain.model.ReminderStatus
import com.example.quipu.fakes.FakeReminderAlarmScheduler
import com.example.quipu.fakes.FakeReminderNotifier
import com.example.quipu.fakes.FakeReminderRepository
import com.example.quipu.fakes.FakeSettingsRepository
import com.example.quipu.fakes.TestClock
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class ReminderUseCasesTest {

    private val clock = TestClock()
    private val repository = FakeReminderRepository()
    private val settings = FakeSettingsRepository()
    private val scheduler = FakeReminderAlarmScheduler()
    private val notifier = FakeReminderNotifier()

    private val createReminder = CreateReminderUseCase(repository, settings, scheduler, clock)
    private val updateReminder = UpdateReminderUseCase(repository, settings, scheduler, clock)
    private val deleteReminder = DeleteReminderUseCase(repository, scheduler, notifier)
    private val completeReminder = CompleteReminderUseCase(repository, scheduler, notifier, clock)
    private val cancelReminder = CancelReminderUseCase(repository, scheduler, notifier, clock)
    private val observeReminders = ObserveRemindersUseCase(repository)
    private val rescheduleReminders =
        RescheduleRemindersUseCase(repository, settings, scheduler, clock)
    private val handleAlarm =
        HandleReminderAlarmUseCase(repository, settings, notifier, clock)

    private val future = clock.currentTime + 60_000

    @Test
    fun `create reminder validates title and future time`() = runTest {
        val blank = createReminder(
            ReminderDraft("   ", null, null, future),
            id = "r1",
        )
        val past = createReminder(
            ReminderDraft("Título", null, null, clock.currentTime - 1),
            id = "r2",
        )

        assertEquals(AppError.TitleRequired, (blank as AppResult.Failure).error)
        assertEquals(AppError.ScheduledTimeInPast, (past as AppResult.Failure).error)
        assertNull(repository.getReminder("r1"))
        assertNull(repository.getReminder("r2"))
        assertTrue(scheduler.scheduled.isEmpty())
    }

    @Test
    fun `create reminder stores it and schedules exact alarm`() = runTest {
        val result = createReminder(
            ReminderDraft("  Reunión  ", "  Detalles ", null, future),
            id = "r1",
        )

        assertTrue(result is AppResult.Success)
        val saved = (result as AppResult.Success).value
        assertEquals("Reunión", saved.reminder.title)
        assertEquals("Detalles", saved.reminder.description)
        assertEquals(ReminderStatus.PENDING, saved.reminder.status)
        assertEquals(AlarmPrecision.EXACT, saved.alarm.precision)
        assertNotNull(repository.getReminder("r1"))
        assertEquals("r1", scheduler.scheduled.keys.single())
    }

    @Test
    fun `create reminder with inexact permission saves and uses inexact alarm`() = runTest {
        scheduler.exactAlarmsAllowed = false

        val result = createReminder(ReminderDraft("Aviso", null, null, future), id = "r1")
            as AppResult.Success

        assertNotNull(repository.getReminder("r1"))
        assertEquals(AlarmPrecision.INEXACT, result.value.alarm.precision)
    }

    @Test
    fun `notifications disabled still persists reminder without scheduling`() = runTest {
        settings.setNotificationsEnabled(false)

        val result = createReminder(ReminderDraft("Aviso", null, null, future), id = "r1")
            as AppResult.Success

        assertNotNull(repository.getReminder("r1"))
        assertFalse(result.value.alarm.scheduled)
        assertTrue(scheduler.scheduled.isEmpty())
    }

    @Test
    fun `update cancels previous alarm and resets state`() = runTest {
        createReminder(ReminderDraft("Aviso", null, null, future), id = "r1")
        val existing = repository.getReminder("r1")!!.copy(
            status = ReminderStatus.COMPLETED,
            completedAt = clock.currentTime,
            notifiedAt = clock.currentTime,
        )
        repository.upsertReminder(existing)
        scheduler.cancelled.clear()
        clock.currentTime += 1_000
        val newTime = clock.currentTime + 120_000

        val result = updateReminder(
            existing,
            ReminderDraft("Aviso editado", "Nueva descripción", null, newTime),
        ) as AppResult.Success

        assertTrue(scheduler.cancelled.contains("r1"))
        assertEquals(newTime, result.value.reminder.scheduledAt)
        assertEquals(ReminderStatus.PENDING, result.value.reminder.status)
        assertNull(result.value.reminder.completedAt)
        assertNull(result.value.reminder.notifiedAt)
        assertEquals("Aviso editado", repository.getReminder("r1")!!.title)
    }

    @Test
    fun `complete and cancel update status and cancel alarms`() = runTest {
        createReminder(ReminderDraft("Uno", null, null, future), id = "r1")
        createReminder(ReminderDraft("Dos", null, null, future), id = "r2")

        completeReminder("r1")
        cancelReminder("r2")

        assertEquals(ReminderStatus.COMPLETED, repository.getReminder("r1")!!.status)
        assertNotNull(repository.getReminder("r1")!!.completedAt)
        assertEquals(ReminderStatus.CANCELLED, repository.getReminder("r2")!!.status)
        assertTrue(scheduler.cancelled.contains("r1"))
        assertTrue(scheduler.cancelled.contains("r2"))
        assertTrue(notifier.cancelled.contains("r1"))
        assertTrue(notifier.cancelled.contains("r2"))
    }

    @Test
    fun `delete removes reminder and cancels alarm`() = runTest {
        createReminder(ReminderDraft("Aviso", null, null, future), id = "r1")

        deleteReminder("r1")

        assertNull(repository.getReminder("r1"))
        assertTrue(scheduler.cancelled.contains("r1"))
        assertTrue(notifier.cancelled.contains("r1"))
    }

    @Test
    fun `observe reminders returns stored reminders sorted by time`() = runTest {
        repository.preload(
            reminder("r2", clock.currentTime + 20_000),
            reminder("r1", clock.currentTime + 10_000),
        )

        val result = observeReminders().first()

        assertEquals(listOf("r1", "r2"), result.map { it.id })
    }

    @Test
    fun `reschedule schedules future pending and cancels overdue`() = runTest {
        repository.preload(
            reminder("future", clock.currentTime + 60_000),
            reminder("overdue", clock.currentTime - 60_000),
            reminder("done", clock.currentTime + 30_000, ReminderStatus.COMPLETED),
        )

        rescheduleReminders()

        assertEquals(setOf("future"), scheduler.scheduled.keys)
        assertTrue(scheduler.cancelled.contains("overdue"))
        assertFalse(scheduler.cancelled.contains("done"))
    }

    @Test
    fun `reschedule cancels everything when notifications disabled`() = runTest {
        repository.preload(
            reminder("future", clock.currentTime + 60_000),
            reminder("overdue", clock.currentTime - 60_000),
        )
        settings.setNotificationsEnabled(false)

        rescheduleReminders()

        assertTrue(scheduler.scheduled.isEmpty())
        assertTrue(scheduler.cancelled.containsAll(listOf("future", "overdue")))
    }

    @Test
    fun `handle alarm notifies once and persists notifiedAt`() = runTest {
        repository.preload(reminder("r1", clock.currentTime))

        handleAlarm("r1")
        handleAlarm("r1")

        assertEquals(listOf("r1"), notifier.shown)
        assertEquals(clock.currentTime, repository.getReminder("r1")!!.notifiedAt)
    }

    @Test
    fun `handle alarm ignores completed or disabled reminders`() = runTest {
        repository.preload(
            reminder("done", clock.currentTime, ReminderStatus.COMPLETED),
            reminder("pending", clock.currentTime),
        )

        handleAlarm("done")
        settings.setNotificationsEnabled(false)
        handleAlarm("pending")

        assertTrue(notifier.shown.isEmpty())
    }

    @Test
    fun `handle alarm does not mark notified if notification fails`() = runTest {
        repository.preload(reminder("r1", clock.currentTime))
        notifier.canShow = false

        handleAlarm("r1")

        assertNull(repository.getReminder("r1")!!.notifiedAt)
    }

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
        status = status,
    )
}
