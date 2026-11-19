package io.github.dijksys.quipu.data.local.entity

import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import androidx.room.PrimaryKey

@Entity(
    tableName = "reminders",
    foreignKeys = [
        ForeignKey(
            entity = NoteEntity::class,
            parentColumns = ["id"],
            childColumns = ["noteId"],
            onDelete = ForeignKey.SET_NULL,
        ),
    ],
    indices = [
        Index(value = ["noteId"]),
        Index(value = ["status", "scheduledAt"]),
        Index(value = ["scheduledAt"]),
        Index(value = ["completedAt"]),
    ],
)
data class ReminderEntity(
    @PrimaryKey val id: String,
    val title: String,
    val description: String?,
    val noteId: String?,
    val scheduledAt: Long,
    val createdAt: Long,
    val updatedAt: Long,
    val completedAt: Long?,
    val notifiedAt: Long?,
    val status: String,
)
