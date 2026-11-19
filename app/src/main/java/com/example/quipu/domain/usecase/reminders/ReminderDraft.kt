package com.example.quipu.domain.usecase.reminders

data class ReminderDraft(
    val title: String,
    val description: String?,
    val noteId: String?,
    val scheduledAt: Long,
)
