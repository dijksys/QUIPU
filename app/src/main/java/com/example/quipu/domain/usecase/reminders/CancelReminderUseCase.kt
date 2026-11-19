package com.example.quipu.domain.usecase.reminders

import com.example.quipu.core.alarms.ReminderAlarmScheduler
import com.example.quipu.core.common.Clock
import com.example.quipu.core.notifications.ReminderNotifier
import com.example.quipu.domain.repository.ReminderRepository
import javax.inject.Inject

class CancelReminderUseCase @Inject constructor(
    private val reminderRepository: ReminderRepository,
    private val alarmScheduler: ReminderAlarmScheduler,
    private val notifier: ReminderNotifier,
    private val clock: Clock,
) {
    suspend operator fun invoke(reminderId: String) {
        alarmScheduler.cancel(reminderId)
        notifier.cancel(reminderId)
        reminderRepository.markCancelled(reminderId, clock.nowMillis())
    }
}
