package io.github.dijksys.quipu.domain.usecase.reminders

import io.github.dijksys.quipu.core.alarms.AlarmScheduleResult
import io.github.dijksys.quipu.core.alarms.ReminderAlarmScheduler
import io.github.dijksys.quipu.core.common.AppError
import io.github.dijksys.quipu.core.common.AppResult
import io.github.dijksys.quipu.core.common.Clock
import io.github.dijksys.quipu.domain.model.Reminder
import io.github.dijksys.quipu.domain.model.ReminderStatus
import io.github.dijksys.quipu.domain.repository.ReminderRepository
import io.github.dijksys.quipu.domain.repository.SettingsRepository
import kotlinx.coroutines.flow.first
import javax.inject.Inject

class UpdateReminderUseCase @Inject constructor(
    private val reminderRepository: ReminderRepository,
    private val settingsRepository: SettingsRepository,
    private val alarmScheduler: ReminderAlarmScheduler,
    private val clock: Clock,
) {
    suspend operator fun invoke(
        existing: Reminder,
        draft: ReminderDraft,
    ): AppResult<ReminderSaveResult> {
        val now = clock.nowMillis()
        val title = draft.title.trim()
        if (title.isBlank()) return AppResult.Failure(AppError.TitleRequired)
        if (draft.scheduledAt <= now) return AppResult.Failure(AppError.ScheduledTimeInPast)

        val updated = existing.copy(
            title = title,
            description = draft.description?.trim()?.ifBlank { null },
            noteId = draft.noteId,
            scheduledAt = draft.scheduledAt,
            updatedAt = now,
            completedAt = null,
            notifiedAt = null,
            // Reprogramar un recordatorio completado o cancelado lo devuelve a pendiente.
            status = ReminderStatus.PENDING,
        )

        // La alarma anterior se cancela antes de reemplazarla.
        alarmScheduler.cancel(existing.id)
        reminderRepository.upsertReminder(updated)

        val notificationsEnabled = settingsRepository.observeSettings().first().notificationsEnabled
        val alarm = if (notificationsEnabled) {
            alarmScheduler.schedule(updated)
        } else {
            AlarmScheduleResult(scheduled = false)
        }
        return AppResult.Success(ReminderSaveResult(updated, alarm))
    }
}
