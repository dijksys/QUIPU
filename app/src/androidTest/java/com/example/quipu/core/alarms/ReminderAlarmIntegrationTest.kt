package com.example.quipu.core.alarms

import android.Manifest
import android.app.NotificationManager
import android.content.Context
import android.content.pm.PackageManager
import android.os.Build
import androidx.core.app.NotificationManagerCompat
import androidx.core.content.ContextCompat
import androidx.room.Room
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import com.example.quipu.core.database.AppDatabase
import com.example.quipu.core.notifications.NotificationChannels
import com.example.quipu.core.notifications.ReminderNotificationManager
import com.example.quipu.data.local.mapper.toEntity
import com.example.quipu.domain.model.Reminder
import kotlinx.coroutines.delay
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith

/**
 * Integración real de AlarmManager + BroadcastReceiver + NotificationManager.
 * Requiere un emulador o dispositivo con la app instalada.
 */
@RunWith(AndroidJUnit4::class)
class ReminderAlarmIntegrationTest {

    @Test
    fun exactAlarmPostsNotificationAndMarksItNotified() = runBlocking {
        val context = ApplicationProvider.getApplicationContext<Context>()
        val instrumentation = InstrumentationRegistry.getInstrumentation()

        grantNotificationPermission(context, instrumentation)
        allowExactAlarms(context, instrumentation)

        val now = System.currentTimeMillis()
        val reminderId = "integration-alarm-${now}"
        val reminder = Reminder(
            id = reminderId,
            title = "Alarma de integración",
            description = "Notificación de prueba",
            noteId = null,
            scheduledAt = now + 8_000,
            createdAt = now,
            updatedAt = now,
        )

        val database = Room.databaseBuilder(
            context,
            AppDatabase::class.java,
            AppDatabase.DATABASE_NAME,
        ).build()
        database.reminderDao().upsert(reminder.toEntity())
        database.close()

        val scheduler = AndroidReminderAlarmScheduler(context)
        assertTrue(scheduler.canScheduleExactAlarms())
        val scheduleResult = scheduler.schedule(reminder)
        assertTrue(scheduleResult.scheduled)

        val notificationManager = context.getSystemService(NotificationManager::class.java)
        val expectedId = ReminderNotificationManager.notificationId(reminderId)
        val deadline = System.currentTimeMillis() + 60_000
        var notificationFound = false
        while (System.currentTimeMillis() < deadline && !notificationFound) {
            notificationFound = notificationManager
                .activeNotifications
                .any { it.id == expectedId }
            if (!notificationFound) delay(1_000)
        }

        assertTrue(
            "No se publicó la notificación del recordatorio",
            notificationFound,
        )

        delay(500)
        val verificationDb = Room.databaseBuilder(
            context,
            AppDatabase::class.java,
            AppDatabase.DATABASE_NAME,
        ).build()
        val stored = verificationDb.reminderDao().getById(reminderId)
        assertNotNull(stored?.notifiedAt)
        verificationDb.close()

        scheduler.cancel(reminderId)
        NotificationManagerCompat.from(context).cancel(expectedId)
        val cleanupDb = Room.databaseBuilder(
            context,
            AppDatabase::class.java,
            AppDatabase.DATABASE_NAME,
        ).build()
        cleanupDb.reminderDao().deleteById(reminderId)
        cleanupDb.close()
    }

    private fun grantNotificationPermission(
        context: Context,
        instrumentation: android.app.Instrumentation,
    ) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU &&
            ContextCompat.checkSelfPermission(
                context,
                Manifest.permission.POST_NOTIFICATIONS,
            ) != PackageManager.PERMISSION_GRANTED
        ) {
            instrumentation.uiAutomation.grantRuntimePermission(
                context.packageName,
                Manifest.permission.POST_NOTIFICATIONS,
            )
        }
        NotificationChannels(context).createChannels()
    }

    private fun allowExactAlarms(
        context: Context,
        instrumentation: android.app.Instrumentation,
    ) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            instrumentation.uiAutomation
                .executeShellCommand(
                    "appops set ${context.packageName} SCHEDULE_EXACT_ALARM allow",
                )
                .close()
        }
    }
}
