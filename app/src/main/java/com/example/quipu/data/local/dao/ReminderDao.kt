package com.example.quipu.data.local.dao

import androidx.room.Dao
import androidx.room.Query
import androidx.room.Upsert
import com.example.quipu.data.local.entity.ReminderEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface ReminderDao {

    @Query("SELECT * FROM reminders ORDER BY scheduledAt ASC")
    fun observeAll(): Flow<List<ReminderEntity>>

    @Query("SELECT * FROM reminders WHERE id = :id LIMIT 1")
    fun observeById(id: String): Flow<ReminderEntity?>

    @Query("SELECT * FROM reminders WHERE id = :id LIMIT 1")
    suspend fun getById(id: String): ReminderEntity?

    @Query("SELECT * FROM reminders WHERE status = 'PENDING' ORDER BY scheduledAt ASC")
    suspend fun getPending(): List<ReminderEntity>

    @Query("SELECT * FROM reminders WHERE noteId = :noteId ORDER BY scheduledAt ASC")
    fun observeForNote(noteId: String): Flow<List<ReminderEntity>>

    @Upsert
    suspend fun upsert(reminder: ReminderEntity)

    @Query("DELETE FROM reminders WHERE id = :id")
    suspend fun deleteById(id: String)

    @Query(
        """
        UPDATE reminders
        SET status = 'COMPLETED', completedAt = :completedAt, updatedAt = :completedAt
        WHERE id = :id
        """,
    )
    suspend fun markCompleted(id: String, completedAt: Long)

    @Query(
        """
        UPDATE reminders
        SET status = 'CANCELLED', completedAt = NULL, updatedAt = :updatedAt
        WHERE id = :id
        """,
    )
    suspend fun markCancelled(id: String, updatedAt: Long)

    @Query("UPDATE reminders SET notifiedAt = :notifiedAt WHERE id = :id")
    suspend fun markNotified(id: String, notifiedAt: Long)
}
