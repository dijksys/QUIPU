package io.github.dijksys.quipu.domain.usecase.reminders

import io.github.dijksys.quipu.core.alarms.ReminderAlarmScheduler
import io.github.dijksys.quipu.core.notifications.ReminderNotifier
import io.github.dijksys.quipu.domain.repository.ReminderRepository
import javax.inject.Inject

class DeleteReminderUseCase @Inject constructor(
    private val reminderRepository: ReminderRepository,
    private val alarmScheduler: ReminderAlarmScheduler,
    private val notifier: ReminderNotifier,
) {
    suspend operator fun invoke(reminderId: String) {
        alarmScheduler.cancel(reminderId)
        notifier.cancel(reminderId)
        reminderRepository.deleteReminder(reminderId)
    }
}
