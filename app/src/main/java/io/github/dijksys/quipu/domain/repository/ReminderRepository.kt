package io.github.dijksys.quipu.domain.repository

import io.github.dijksys.quipu.domain.model.Reminder
import kotlinx.coroutines.flow.Flow

interface ReminderRepository {
    fun observeAllReminders(): Flow<List<Reminder>>
    fun observeReminder(id: String): Flow<Reminder?>
    fun observeRemindersForNote(noteId: String): Flow<List<Reminder>>
    suspend fun getReminder(id: String): Reminder?
    suspend fun getPendingReminders(): List<Reminder>

    suspend fun upsertReminder(reminder: Reminder)
    suspend fun deleteReminder(id: String)
    suspend fun markCompleted(id: String, completedAt: Long)
    suspend fun markCancelled(id: String, updatedAt: Long)
    suspend fun markNotified(id: String, notifiedAt: Long)
}
