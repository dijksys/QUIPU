package com.example.quipu.core.alarms

import com.example.quipu.domain.model.Reminder

enum class AlarmPrecision {
    EXACT,
    INEXACT,
}

data class AlarmScheduleResult(
    val scheduled: Boolean,
    val precision: AlarmPrecision? = null,
)

/**
 * Abstracción de programación de alarmas independiente de la interfaz y de Android.
 * La implementación real vive en [AndroidReminderAlarmScheduler].
 */
interface ReminderAlarmScheduler {
    fun schedule(reminder: Reminder): AlarmScheduleResult
    fun cancel(reminderId: String)
    fun canScheduleExactAlarms(): Boolean
}
