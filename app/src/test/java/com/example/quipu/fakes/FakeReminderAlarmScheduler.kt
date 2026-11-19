package com.example.quipu.fakes

import com.example.quipu.core.alarms.AlarmPrecision
import com.example.quipu.core.alarms.AlarmScheduleResult
import com.example.quipu.core.alarms.ReminderAlarmScheduler
import com.example.quipu.domain.model.Reminder

class FakeReminderAlarmScheduler : ReminderAlarmScheduler {

    var exactAlarmsAllowed: Boolean = true
    val scheduled = linkedMapOf<String, Reminder>()
    val cancelled = mutableListOf<String>()

    override fun schedule(reminder: Reminder): AlarmScheduleResult {
        scheduled[reminder.id] = reminder
        return AlarmScheduleResult(
            scheduled = true,
            precision = if (exactAlarmsAllowed) AlarmPrecision.EXACT else AlarmPrecision.INEXACT,
        )
    }

    override fun cancel(reminderId: String) {
        cancelled += reminderId
        scheduled.remove(reminderId)
    }

    override fun canScheduleExactAlarms(): Boolean = exactAlarmsAllowed
}
