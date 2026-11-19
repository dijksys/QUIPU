package com.example.quipu.data.local.mapper

import com.example.quipu.data.local.entity.NoteEntity
import com.example.quipu.domain.model.Note

fun NoteEntity.toDomain(): Note = Note(
    id = id,
    title = title,
    content = content,
    createdAt = createdAt,
    updatedAt = updatedAt,
    isPinned = isPinned,
    archivedAt = archivedAt,
    deletedAt = deletedAt,
)

fun Note.toEntity(): NoteEntity = NoteEntity(
    id = id,
    title = title,
    content = content,
    createdAt = createdAt,
    updatedAt = updatedAt,
    isPinned = isPinned,
    archivedAt = archivedAt,
    deletedAt = deletedAt,
)
