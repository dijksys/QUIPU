package io.github.dijksys.quipu.feature.home

import io.github.dijksys.quipu.domain.model.Note
import io.github.dijksys.quipu.domain.model.Reminder

data class HomeUiState(
    val isLoading: Boolean = true,
    val nowMillis: Long = System.currentTimeMillis(),
    val notesCount: Int = 0,
    val pendingRemindersCount: Int = 0,
    val pinnedNotes: List<Note> = emptyList(),
    val recentNotes: List<Note> = emptyList(),
    val upcomingReminders: List<Reminder> = emptyList(),
) {
    val isCompletelyEmpty: Boolean
        get() = !isLoading && notesCount == 0 && pendingRemindersCount == 0
}
