package io.github.dijksys.quipu.domain.usecase.reminders

import io.github.dijksys.quipu.core.alarms.AlarmScheduleResult
import io.github.dijksys.quipu.domain.model.Reminder

data class ReminderSaveResult(
    val reminder: Reminder,
    val alarm: AlarmScheduleResult,
)
