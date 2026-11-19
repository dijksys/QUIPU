package io.github.dijksys.quipu.core.notifications

import io.github.dijksys.quipu.domain.model.Reminder

/**
 * Abstracción de notificaciones para que los casos de uso no dependan de Android.
 */
interface ReminderNotifier {
    /** Devuelve true si la notificación pudo publicarse. */
    fun show(reminder: Reminder): Boolean
    fun cancel(reminderId: String)
}
