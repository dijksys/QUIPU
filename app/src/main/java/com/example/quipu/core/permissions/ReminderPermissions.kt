package com.example.quipu.core.permissions

import android.Manifest
import android.app.AlarmManager
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Build
import android.provider.Settings
import androidx.core.net.toUri
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.core.app.NotificationManagerCompat
import androidx.core.content.ContextCompat
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.lifecycle.compose.LocalLifecycleOwner
import androidx.compose.ui.platform.LocalContext

/** Estado y acciones de permisos de notificaciones. */
class NotificationPermissionController(
    val granted: Boolean,
    val request: () -> Unit,
    val openSettings: () -> Unit,
)

/** Estado y acciones del permiso de alarmas exactas. */
class ExactAlarmPermissionController(
    val granted: Boolean,
    val isSupported: Boolean,
    val request: () -> Unit,
)

@Composable
fun rememberNotificationPermissionController(): NotificationPermissionController {
    val context = LocalContext.current
    var granted by remember { mutableStateOf(context.hasNotificationPermission()) }

    val launcher = rememberLauncherForActivityResult(
        ActivityResultContracts.RequestPermission(),
    ) {
        granted = context.hasNotificationPermission()
    }

    RefreshPermissionOnResume {
        granted = context.hasNotificationPermission()
    }

    return remember(granted) {
        NotificationPermissionController(
            granted = granted,
            request = {
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                    launcher.launch(Manifest.permission.POST_NOTIFICATIONS)
                } else {
                    context.openNotificationSettings()
                }
            },
            openSettings = { context.openNotificationSettings() },
        )
    }
}

@Composable
fun rememberExactAlarmPermissionController(): ExactAlarmPermissionController {
    val context = LocalContext.current
    var granted by remember { mutableStateOf(context.canScheduleExactAlarms()) }

    RefreshPermissionOnResume {
        granted = context.canScheduleExactAlarms()
    }

    return remember(granted) {
        ExactAlarmPermissionController(
            granted = granted,
            isSupported = Build.VERSION.SDK_INT >= Build.VERSION_CODES.S,
            request = { context.openExactAlarmSettings() },
        )
    }
}

@Composable
private fun RefreshPermissionOnResume(onResume: () -> Unit) {
    val lifecycleOwner = LocalLifecycleOwner.current
    DisposableEffect(lifecycleOwner) {
        val observer = LifecycleEventObserver { _, event ->
            if (event == Lifecycle.Event.ON_RESUME) onResume()
        }
        lifecycleOwner.lifecycle.addObserver(observer)
        onDispose { lifecycleOwner.lifecycle.removeObserver(observer) }
    }
}

private fun Context.hasNotificationPermission(): Boolean {
    val permissionGranted = Build.VERSION.SDK_INT < Build.VERSION_CODES.TIRAMISU ||
        ContextCompat.checkSelfPermission(
            this,
            Manifest.permission.POST_NOTIFICATIONS,
        ) == PackageManager.PERMISSION_GRANTED
    return permissionGranted && NotificationManagerCompat.from(this).areNotificationsEnabled()
}

private fun Context.canScheduleExactAlarms(): Boolean =
    Build.VERSION.SDK_INT < Build.VERSION_CODES.S ||
        getSystemService(AlarmManager::class.java).canScheduleExactAlarms()

private fun Context.openNotificationSettings() {
    val intent = Intent(Settings.ACTION_APP_NOTIFICATION_SETTINGS).apply {
        putExtra(Settings.EXTRA_APP_PACKAGE, packageName)
        addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
    }
    runCatching { startActivity(intent) }
        .onFailure { startActivity(applicationDetailsIntent()) }
}

private fun Context.openExactAlarmSettings() {
    if (Build.VERSION.SDK_INT < Build.VERSION_CODES.S) return
    val intent = Intent(
        Settings.ACTION_REQUEST_SCHEDULE_EXACT_ALARM,
        "package:$packageName".toUri(),
    ).apply {
        addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
    }
    runCatching { startActivity(intent) }
        .onFailure { startActivity(applicationDetailsIntent()) }
}

private fun Context.applicationDetailsIntent(): Intent =
    Intent(
        Settings.ACTION_APPLICATION_DETAILS_SETTINGS,
        "package:$packageName".toUri(),
    ).apply {
        addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
    }
