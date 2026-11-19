package io.github.dijksys.quipu.fakes

import io.github.dijksys.quipu.core.notifications.ReminderNotifier
import io.github.dijksys.quipu.domain.model.Reminder

class FakeReminderNotifier : ReminderNotifier {

    var canShow: Boolean = true
    val shown = mutableListOf<String>()
    val cancelled = mutableListOf<String>()

    override fun show(reminder: Reminder): Boolean {
        if (!canShow) return false
        shown += reminder.id
        return true
    }

    override fun cancel(reminderId: String) {
        cancelled += reminderId
    }
}
