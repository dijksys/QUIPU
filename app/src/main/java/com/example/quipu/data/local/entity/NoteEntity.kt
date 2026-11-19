package com.example.quipu.data.local.entity

import androidx.room.Entity
import androidx.room.Index
import androidx.room.PrimaryKey

@Entity(
    tableName = "notes",
    indices = [
        Index(value = ["deletedAt", "archivedAt", "isPinned", "updatedAt"]),
        Index(value = ["deletedAt", "archivedAt", "updatedAt"]),
        Index(value = ["archivedAt"]),
        Index(value = ["updatedAt"]),
    ],
)
data class NoteEntity(
    @PrimaryKey val id: String,
    val title: String,
    val content: String,
    val createdAt: Long,
    val updatedAt: Long,
    val isPinned: Boolean,
    val archivedAt: Long?,
    val deletedAt: Long?,
)
