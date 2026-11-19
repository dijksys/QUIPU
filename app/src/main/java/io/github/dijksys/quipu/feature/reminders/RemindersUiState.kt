package io.github.dijksys.quipu.feature.reminders

import io.github.dijksys.quipu.domain.model.Note
import io.github.dijksys.quipu.domain.model.Reminder
import io.github.dijksys.quipu.domain.model.ReminderFilter
import io.github.dijksys.quipu.domain.model.ReminderStatus

data class RemindersUiState(
    val filter: ReminderFilter = ReminderFilter.PENDING,
    val isLoading: Boolean = true,
    val nowMillis: Long = System.currentTimeMillis(),
    val pending: List<Reminder> = emptyList(),
    val overdue: List<Reminder> = emptyList(),
    val completed: List<Reminder> = emptyList(),
    val cancelled: List<Reminder> = emptyList(),
    val notificationsEnabled: Boolean = true,
) {
    val visibleReminders: List<Reminder>
        get() = when (filter) {
            ReminderFilter.PENDING -> pending
            ReminderFilter.OVERDUE -> overdue
            ReminderFilter.COMPLETED -> completed
            ReminderFilter.CANCELLED -> cancelled
        }

    fun countFor(filter: ReminderFilter): Int = when (filter) {
        ReminderFilter.PENDING -> pending.size
        ReminderFilter.OVERDUE -> overdue.size
        ReminderFilter.COMPLETED -> completed.size
        ReminderFilter.CANCELLED -> cancelled.size
    }

    val isEmpty: Boolean get() = !isLoading && visibleReminders.isEmpty()
}

sealed interface RemindersEvent {
    data object Completed : RemindersEvent
    data object Cancelled : RemindersEvent
    data object Deleted : RemindersEvent
    data object Failure : RemindersEvent
}

enum class ReminderEditorError {
    TITLE_REQUIRED,
    TIME_IN_PAST,
    SAVE_FAILED,
    NOT_FOUND,
}

data class ReminderEditorUiState(
    val reminderId: String = "",
    val isLoading: Boolean = true,
    val isNew: Boolean = true,
    val title: String = "",
    val description: String = "",
    val scheduledAt: Long = 0L,
    val noteId: String? = null,
    val linkedNoteTitle: String? = null,
    val availableNotes: List<Note> = emptyList(),
    val status: ReminderStatus = ReminderStatus.PENDING,
    val notificationsEnabled: Boolean = true,
    val isSaving: Boolean = false,
    val error: ReminderEditorError? = null,
    val isMissing: Boolean = false,
)

sealed interface ReminderEditorEvent {
    data object Saved : ReminderEditorEvent
    data object Deleted : ReminderEditorEvent
    data object Completed : ReminderEditorEvent
    data object Cancelled : ReminderEditorEvent
    data object Failure : ReminderEditorEvent
}
