package io.github.dijksys.quipu

import android.app.Application
import io.github.dijksys.quipu.core.notifications.NotificationChannels
import dagger.hilt.android.HiltAndroidApp
import javax.inject.Inject

@HiltAndroidApp
class NotesApplication : Application() {

    @Inject
    lateinit var notificationChannels: NotificationChannels

    override fun onCreate() {
        super.onCreate()
        notificationChannels.createChannels()
    }
}
