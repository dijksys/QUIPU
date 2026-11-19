package com.example.quipu.data.local.mapper

import com.example.quipu.data.local.entity.ReminderEntity
import com.example.quipu.domain.model.Reminder
import com.example.quipu.domain.model.ReminderStatus

fun ReminderEntity.toDomain(): Reminder = Reminder(
    id = id,
    title = title,
    description = description,
    noteId = noteId,
    scheduledAt = scheduledAt,
    createdAt = createdAt,
    updatedAt = updatedAt,
    completedAt = completedAt,
    notifiedAt = notifiedAt,
    status = runCatching { ReminderStatus.valueOf(status) }
        .getOrDefault(ReminderStatus.PENDING),
)

fun Reminder.toEntity(): ReminderEntity = ReminderEntity(
    id = id,
    title = title,
    description = description,
    noteId = noteId,
    scheduledAt = scheduledAt,
    createdAt = createdAt,
    updatedAt = updatedAt,
    completedAt = completedAt,
    notifiedAt = notifiedAt,
    status = status.name,
)
