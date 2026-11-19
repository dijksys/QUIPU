package com.example.quipu.core.notifications

import android.app.NotificationChannel
import android.app.NotificationManager
import android.content.Context
import com.example.quipu.R
import dagger.hilt.android.qualifiers.ApplicationContext
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class NotificationChannels @Inject constructor(
    @param:ApplicationContext private val context: Context,
) {
    fun createChannels() {
        val manager = context.getSystemService(NotificationManager::class.java)
        val channel = NotificationChannel(
            CHANNEL_REMINDERS,
            context.getString(R.string.notification_channel_reminders),
            NotificationManager.IMPORTANCE_HIGH,
        ).apply {
            description = context.getString(R.string.notification_channel_reminders_description)
            enableVibration(true)
            setShowBadge(true)
        }
        manager.createNotificationChannel(channel)
    }

    companion object {
        const val CHANNEL_REMINDERS = "reminders"
    }
}
