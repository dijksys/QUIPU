package com.example.quipu.core.alarms

import android.app.AlarmManager
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.os.Build
import com.example.quipu.domain.usecase.reminders.RescheduleRemindersUseCase
import com.example.quipu.di.ApplicationScope
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.launch
import javax.inject.Inject

/**
 * Reprograma recordatorios después de reiniciar el dispositivo, al cambiar la
 * hora/zona horaria, al actualizar la app o al conceder alarmas exactas.
 */
@AndroidEntryPoint
class BootReceiver : BroadcastReceiver() {

    @Inject
    lateinit var rescheduleReminders: RescheduleRemindersUseCase

    @Inject
    @ApplicationScope
    lateinit var applicationScope: CoroutineScope

    override fun onReceive(context: Context, intent: Intent) {
        val action = intent.action ?: return
        if (!isRescheduleEvent(action)) return

        val pendingResult = goAsync()
        applicationScope.launch {
            try {
                rescheduleReminders()
            } finally {
                pendingResult.finish()
            }
        }
    }

    private fun isRescheduleEvent(action: String): Boolean {
        if (action == Intent.ACTION_BOOT_COMPLETED) return true
        if (action == Intent.ACTION_TIME_CHANGED) return true
        if (action == Intent.ACTION_TIMEZONE_CHANGED) return true
        if (action == Intent.ACTION_MY_PACKAGE_REPLACED) return true
        return Build.VERSION.SDK_INT >= Build.VERSION_CODES.S &&
            action == AlarmManager.ACTION_SCHEDULE_EXACT_ALARM_PERMISSION_STATE_CHANGED
    }
}
