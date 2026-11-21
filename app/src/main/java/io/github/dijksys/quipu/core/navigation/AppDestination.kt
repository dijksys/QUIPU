package io.github.dijksys.quipu.core.navigation

import android.net.Uri
import io.github.dijksys.quipu.domain.model.NoteFilter

object AppDestination {
    const val HOME = "home"
    const val NOTES = "notes?filter={filter}&query={query}"
    const val NOTE_EDITOR = "note_editor/{noteId}"
    const val REMINDERS = "reminders"
    const val REMINDER_EDITOR = "reminder_editor/{reminderId}?noteId={noteId}"
    const val SETTINGS = "settings"

    const val ARG_FILTER = "filter"
    const val ARG_QUERY = "query"
    const val ARG_NOTE_ID = "noteId"
    const val ARG_REMINDER_ID = "reminderId"

    val topLevelRoutes = setOf(HOME, NOTES, REMINDERS, SETTINGS)

    fun notes(filter: NoteFilter = NoteFilter.ACTIVE, query: String? = null): String {
        val base = "notes?filter=${filter.name}"
        val trimmed = query?.trim().orEmpty()
        return if (trimmed.isEmpty()) base else "$base&query=${Uri.encode(trimmed)}"
    }

    fun noteEditor(noteId: String? = null): String =
        "note_editor/${noteId?.let(Uri::encode) ?: "new"}"

    fun reminderEditor(reminderId: String? = null, noteId: String? = null): String {
        val id = reminderId?.let(Uri::encode) ?: "new"
        return if (noteId.isNullOrBlank()) {
            "reminder_editor/$id"
        } else {
            "reminder_editor/$id?noteId=${Uri.encode(noteId)}"
        }
    }
}
