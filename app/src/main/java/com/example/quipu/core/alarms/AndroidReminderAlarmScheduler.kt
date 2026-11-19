package com.example.quipu.core.alarms

import android.app.AlarmManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.net.Uri
import android.os.Build
import com.example.quipu.domain.model.Reminder
import dagger.hilt.android.qualifiers.ApplicationContext
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class AndroidReminderAlarmScheduler @Inject constructor(
    @param:ApplicationContext private val context: Context,
) : ReminderAlarmScheduler {

    private val alarmManager: AlarmManager =
        context.getSystemService(AlarmManager::class.java)

    override fun schedule(reminder: Reminder): AlarmScheduleResult {
        val pendingIntent = createPendingIntent(context, reminder.id)

        if (reminder.scheduledAt <= System.currentTimeMillis()) {
            alarmManager.cancel(pendingIntent)
            return AlarmScheduleResult(scheduled = false)
        }

        return if (canScheduleExactAlarms()) {
            alarmManager.setExactAndAllowWhileIdle(
                AlarmManager.RTC_WAKEUP,
                reminder.scheduledAt,
                pendingIntent,
            )
            AlarmScheduleResult(scheduled = true, precision = AlarmPrecision.EXACT)
        } else {
            // Alternativa explícita cuando no hay permiso de alarmas exactas:
            // Android puede retrasar la entrega para agrupar alarmas.
            alarmManager.setAndAllowWhileIdle(
                AlarmManager.RTC_WAKEUP,
                reminder.scheduledAt,
                pendingIntent,
            )
            AlarmScheduleResult(scheduled = true, precision = AlarmPrecision.INEXACT)
        }
    }

    override fun cancel(reminderId: String) {
        alarmManager.cancel(createPendingIntent(context, reminderId))
    }

    override fun canScheduleExactAlarms(): Boolean =
        Build.VERSION.SDK_INT < Build.VERSION_CODES.S || alarmManager.canScheduleExactAlarms()

    companion object {
        const val ACTION_FIRE_REMINDER = "com.example.quipu.action.FIRE_REMINDER"
        const val EXTRA_REMINDER_ID = "extra_reminder_id"

        fun createIntent(context: Context, reminderId: String): Intent =
            Intent(context, ReminderReceiver::class.java).apply {
                action = ACTION_FIRE_REMINDER
                // El data URI hace único cada PendingIntent aunque se reutilice el requestCode.
                data = Uri.Builder()
                    .scheme("quipu")
                    .authority("reminder")
                    .appendPath(reminderId)
                    .build()
                putExtra(EXTRA_REMINDER_ID, reminderId)
            }

        fun createPendingIntent(context: Context, reminderId: String): PendingIntent =
            PendingIntent.getBroadcast(
                context,
                0,
                createIntent(context, reminderId),
                PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE,
            )
    }
}
