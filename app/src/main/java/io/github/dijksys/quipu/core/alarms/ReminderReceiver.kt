package io.github.dijksys.quipu.core.alarms

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import io.github.dijksys.quipu.domain.usecase.reminders.HandleReminderAlarmUseCase
import io.github.dijksys.quipu.di.ApplicationScope
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.launch
import javax.inject.Inject

/**
 * Recibe la alarma de un recordatorio y delega la validación en un caso de uso.
 * El estado se consulta en Room antes de publicar la notificación.
 */
@AndroidEntryPoint
class ReminderReceiver : BroadcastReceiver() {

    @Inject
    lateinit var handleReminderAlarm: HandleReminderAlarmUseCase

    @Inject
    @ApplicationScope
    lateinit var applicationScope: CoroutineScope

    override fun onReceive(context: Context, intent: Intent) {
        if (intent.action != AndroidReminderAlarmScheduler.ACTION_FIRE_REMINDER) return
        val reminderId = intent.getStringExtra(AndroidReminderAlarmScheduler.EXTRA_REMINDER_ID)
            ?: return

        val pendingResult = goAsync()
        applicationScope.launch {
            try {
                handleReminderAlarm(reminderId)
            } finally {
                pendingResult.finish()
            }
        }
    }
}
