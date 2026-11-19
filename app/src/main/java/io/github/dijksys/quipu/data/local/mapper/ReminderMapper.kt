package io.github.dijksys.quipu.data.local.mapper

import io.github.dijksys.quipu.data.local.entity.ReminderEntity
import io.github.dijksys.quipu.domain.model.Reminder
import io.github.dijksys.quipu.domain.model.ReminderStatus

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
