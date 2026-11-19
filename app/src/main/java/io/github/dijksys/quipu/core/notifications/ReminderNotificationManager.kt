package io.github.dijksys.quipu.core.notifications

import android.Manifest
import android.annotation.SuppressLint
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Build
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import androidx.core.content.ContextCompat
import androidx.core.net.toUri
import io.github.dijksys.quipu.MainActivity
import io.github.dijksys.quipu.R
import io.github.dijksys.quipu.domain.model.Reminder
import dagger.hilt.android.qualifiers.ApplicationContext
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class ReminderNotificationManager @Inject constructor(
    @param:ApplicationContext private val context: Context,
) : ReminderNotifier {

    private val notificationManager: NotificationManagerCompat =
        NotificationManagerCompat.from(context)

    @SuppressLint("MissingPermission")
    override fun show(reminder: Reminder): Boolean {
        if (!canPostNotifications()) return false

        val contentIntent = Intent(context, MainActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_CLEAR_TOP or Intent.FLAG_ACTIVITY_SINGLE_TOP
            // El data URI hace único el PendingIntent de cada notificación.
            data = "quipu://notification/${reminder.id}".toUri()
            putExtra(EXTRA_OPEN_REMINDER_ID, reminder.id)
        }
        val pendingIntent = PendingIntent.getActivity(
            context,
            0,
            contentIntent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE,
        )

        val description = reminder.description?.trim().orEmpty()
        val notification = NotificationCompat.Builder(context, NotificationChannels.CHANNEL_REMINDERS)
            .setSmallIcon(R.drawable.ic_notification)
            .setContentTitle(reminder.title)
            .setContentText(description.ifBlank { context.getString(R.string.notification_default_text) })
            .setStyle(
                NotificationCompat.BigTextStyle().bigText(
                    description.ifBlank { context.getString(R.string.notification_default_text) },
                ),
            )
            .setPriority(NotificationCompat.PRIORITY_HIGH)
            .setCategory(NotificationCompat.CATEGORY_REMINDER)
            .setWhen(reminder.scheduledAt)
            .setShowWhen(true)
            .setAutoCancel(true)
            .setContentIntent(pendingIntent)
            .build()

        notificationManager.notify(notificationId(reminder.id), notification)
        return true
    }

    override fun cancel(reminderId: String) {
        notificationManager.cancel(notificationId(reminderId))
    }

    private fun canPostNotifications(): Boolean {
        val permissionGranted = Build.VERSION.SDK_INT < Build.VERSION_CODES.TIRAMISU ||
            ContextCompat.checkSelfPermission(
                context,
                Manifest.permission.POST_NOTIFICATIONS,
            ) == PackageManager.PERMISSION_GRANTED
        return permissionGranted && NotificationManagerCompat.from(context).areNotificationsEnabled()
    }

    companion object {
        const val EXTRA_OPEN_REMINDER_ID = "extra_open_reminder_id"

        fun notificationId(reminderId: String): Int = reminderId.hashCode() and 0x7FFFFFFF
    }
}
