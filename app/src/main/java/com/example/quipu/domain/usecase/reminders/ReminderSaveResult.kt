package com.example.quipu.domain.usecase.reminders

import com.example.quipu.core.alarms.AlarmScheduleResult
import com.example.quipu.domain.model.Reminder

data class ReminderSaveResult(
    val reminder: Reminder,
    val alarm: AlarmScheduleResult,
)
