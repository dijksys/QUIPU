package com.example.quipu.fakes

import com.example.quipu.domain.model.Reminder
import com.example.quipu.domain.model.ReminderStatus
import com.example.quipu.domain.repository.ReminderRepository
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.update

class FakeReminderRepository(initial: List<Reminder> = emptyList()) : ReminderRepository {

    private val reminders = MutableStateFlow(initial.associateBy { it.id })

    var failWrites: Boolean = false

    val currentReminders: List<Reminder> get() = reminders.value.values.toList()

    fun getStored(id: String): Reminder? = reminders.value[id]

    fun preload(vararg reminder: Reminder) {
        reminders.update { current -> current + reminder.associateBy { it.id } }
    }

    override fun observeAllReminders(): Flow<List<Reminder>> =
        reminders.map { stored -> stored.values.sortedBy { it.scheduledAt } }

    override fun observeReminder(id: String): Flow<Reminder?> =
        reminders.map { stored -> stored[id] }

    override fun observeRemindersForNote(noteId: String): Flow<List<Reminder>> =
        reminders.map { stored -> stored.values.filter { it.noteId == noteId } }

    override suspend fun getReminder(id: String): Reminder? = reminders.value[id]

    override suspend fun getPendingReminders(): List<Reminder> =
        reminders.value.values
            .filter { it.status == ReminderStatus.PENDING }
            .sortedBy { it.scheduledAt }

    override suspend fun upsertReminder(reminder: Reminder) {
        if (failWrites) error("Fallo de escritura simulado")
        reminders.update { it + (reminder.id to reminder) }
    }

    override suspend fun deleteReminder(id: String) {
        reminders.update { it - id }
    }

    override suspend fun markCompleted(id: String, completedAt: Long) {
        update(id) {
            it.copy(
                status = ReminderStatus.COMPLETED,
                completedAt = completedAt,
                updatedAt = completedAt,
            )
        }
    }

    override suspend fun markCancelled(id: String, updatedAt: Long) {
        update(id) {
            it.copy(
                status = ReminderStatus.CANCELLED,
                completedAt = null,
                updatedAt = updatedAt,
            )
        }
    }

    override suspend fun markNotified(id: String, notifiedAt: Long) {
        update(id) { it.copy(notifiedAt = notifiedAt) }
    }

    private fun update(id: String, transform: (Reminder) -> Reminder) {
        reminders.update { stored ->
            val reminder = stored[id] ?: return@update stored
            stored + (id to transform(reminder))
        }
    }
}
