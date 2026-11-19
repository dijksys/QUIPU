package io.github.dijksys.quipu.domain.usecase.reminders

import io.github.dijksys.quipu.core.common.Clock
import io.github.dijksys.quipu.core.notifications.ReminderNotifier
import io.github.dijksys.quipu.domain.model.ReminderStatus
import io.github.dijksys.quipu.domain.repository.ReminderRepository
import io.github.dijksys.quipu.domain.repository.SettingsRepository
import kotlinx.coroutines.flow.first
import javax.inject.Inject

/**
 * Valida el estado persistido antes de mostrar una notificación y evita duplicados.
 */
class HandleReminderAlarmUseCase @Inject constructor(
    private val reminderRepository: ReminderRepository,
    private val settingsRepository: SettingsRepository,
    private val notifier: ReminderNotifier,
    private val clock: Clock,
) {
    suspend operator fun invoke(reminderId: String) {
        val reminder = reminderRepository.getReminder(reminderId) ?: return

        // Solo se notifica si el recordatorio sigue vigente y no se notificó ya.
        if (reminder.status != ReminderStatus.PENDING) return
        if (reminder.notifiedAt != null && reminder.notifiedAt >= reminder.scheduledAt) return
        if (!settingsRepository.observeSettings().first().notificationsEnabled) return

        if (notifier.show(reminder)) {
            reminderRepository.markNotified(reminderId, clock.nowMillis())
        }
    }
}
