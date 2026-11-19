package io.github.dijksys.quipu.fakes

import io.github.dijksys.quipu.core.alarms.AlarmPrecision
import io.github.dijksys.quipu.core.alarms.AlarmScheduleResult
import io.github.dijksys.quipu.core.alarms.ReminderAlarmScheduler
import io.github.dijksys.quipu.domain.model.Reminder

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
