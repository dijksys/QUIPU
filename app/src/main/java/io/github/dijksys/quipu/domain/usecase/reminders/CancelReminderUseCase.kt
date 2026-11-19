package io.github.dijksys.quipu.domain.usecase.reminders

import io.github.dijksys.quipu.core.alarms.ReminderAlarmScheduler
import io.github.dijksys.quipu.core.common.Clock
import io.github.dijksys.quipu.core.notifications.ReminderNotifier
import io.github.dijksys.quipu.domain.repository.ReminderRepository
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
