package com.example.quipu.core.notifications

import com.example.quipu.domain.model.Reminder

/**
 * Abstracción de notificaciones para que los casos de uso no dependan de Android.
 */
interface ReminderNotifier {
    /** Devuelve true si la notificación pudo publicarse. */
    fun show(reminder: Reminder): Boolean
    fun cancel(reminderId: String)
}
