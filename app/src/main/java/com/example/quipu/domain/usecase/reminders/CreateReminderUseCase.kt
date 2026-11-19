package com.example.quipu.domain.usecase.reminders

import com.example.quipu.core.alarms.AlarmScheduleResult
import com.example.quipu.core.alarms.ReminderAlarmScheduler
import com.example.quipu.core.common.AppError
import com.example.quipu.core.common.AppResult
import com.example.quipu.core.common.Clock
import com.example.quipu.domain.model.Reminder
import com.example.quipu.domain.model.ReminderStatus
import com.example.quipu.domain.repository.ReminderRepository
import com.example.quipu.domain.repository.SettingsRepository
import kotlinx.coroutines.flow.first
import java.util.UUID
import javax.inject.Inject

class CreateReminderUseCase @Inject constructor(
    private val reminderRepository: ReminderRepository,
    private val settingsRepository: SettingsRepository,
    private val alarmScheduler: ReminderAlarmScheduler,
    private val clock: Clock,
) {
    suspend operator fun invoke(
        draft: ReminderDraft,
        id: String = UUID.randomUUID().toString(),
    ): AppResult<ReminderSaveResult> {
        val now = clock.nowMillis()
        val title = draft.title.trim()
        if (title.isBlank()) return AppResult.Failure(AppError.TitleRequired)
        if (draft.scheduledAt <= now) return AppResult.Failure(AppError.ScheduledTimeInPast)

        val reminder = Reminder(
            id = id,
            title = title,
            description = draft.description?.trim()?.ifBlank { null },
            noteId = draft.noteId,
            scheduledAt = draft.scheduledAt,
            createdAt = now,
            updatedAt = now,
            status = ReminderStatus.PENDING,
        )

        // La base de datos es la fuente de verdad: se guarda antes de programar la alarma.
        reminderRepository.upsertReminder(reminder)

        val notificationsEnabled = settingsRepository.observeSettings().first().notificationsEnabled
        val alarm = if (notificationsEnabled) {
            alarmScheduler.schedule(reminder)
        } else {
            AlarmScheduleResult(scheduled = false)
        }
        return AppResult.Success(ReminderSaveResult(reminder, alarm))
    }
}
