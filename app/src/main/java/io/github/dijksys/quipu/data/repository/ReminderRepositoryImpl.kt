package io.github.dijksys.quipu.data.repository

import io.github.dijksys.quipu.data.local.dao.ReminderDao
import io.github.dijksys.quipu.data.local.mapper.toDomain
import io.github.dijksys.quipu.data.local.mapper.toEntity
import io.github.dijksys.quipu.domain.model.Reminder
import io.github.dijksys.quipu.domain.repository.ReminderRepository
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class ReminderRepositoryImpl @Inject constructor(
    private val reminderDao: ReminderDao,
) : ReminderRepository {

    override fun observeAllReminders(): Flow<List<Reminder>> =
        reminderDao.observeAll().map { entities -> entities.map { it.toDomain() } }

    override fun observeReminder(id: String): Flow<Reminder?> =
        reminderDao.observeById(id).map { it?.toDomain() }

    override fun observeRemindersForNote(noteId: String): Flow<List<Reminder>> =
        reminderDao.observeForNote(noteId).map { entities -> entities.map { it.toDomain() } }

    override suspend fun getReminder(id: String): Reminder? =
        reminderDao.getById(id)?.toDomain()

    override suspend fun getPendingReminders(): List<Reminder> =
        reminderDao.getPending().map { it.toDomain() }

    override suspend fun upsertReminder(reminder: Reminder) {
        reminderDao.upsert(reminder.toEntity())
    }

    override suspend fun deleteReminder(id: String) {
        reminderDao.deleteById(id)
    }

    override suspend fun markCompleted(id: String, completedAt: Long) {
        reminderDao.markCompleted(id, completedAt)
    }

    override suspend fun markCancelled(id: String, updatedAt: Long) {
        reminderDao.markCancelled(id, updatedAt)
    }

    override suspend fun markNotified(id: String, notifiedAt: Long) {
        reminderDao.markNotified(id, notifiedAt)
    }
}
