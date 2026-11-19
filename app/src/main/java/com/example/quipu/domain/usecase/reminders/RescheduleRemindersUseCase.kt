package com.example.quipu.domain.usecase.reminders

import com.example.quipu.core.alarms.ReminderAlarmScheduler
import com.example.quipu.core.common.Clock
import com.example.quipu.domain.repository.ReminderRepository
import com.example.quipu.domain.repository.SettingsRepository
import kotlinx.coroutines.flow.first
import javax.inject.Inject

/**
 * Reprograma todas las alarmas pendientes. Se usa al arrancar el dispositivo,
 * al cambiar la hora/zona horaria, al actualizar la app y al activar notificaciones.
 */
class RescheduleRemindersUseCase @Inject constructor(
    private val reminderRepository: ReminderRepository,
    private val settingsRepository: SettingsRepository,
    private val alarmScheduler: ReminderAlarmScheduler,
    private val clock: Clock,
) {
    suspend operator fun invoke() {
        val notificationsEnabled =
            settingsRepository.observeSettings().first().notificationsEnabled
        val pending = reminderRepository.getPendingReminders()
        val now = clock.nowMillis()

        pending.forEach { reminder ->
            when {
                !notificationsEnabled -> alarmScheduler.cancel(reminder.id)
                reminder.scheduledAt > now -> alarmScheduler.schedule(reminder)
                // Un recordatorio vencido sigue pendiente, pero no se dispara al reprogramar.
                else -> alarmScheduler.cancel(reminder.id)
            }
        }
    }
}
