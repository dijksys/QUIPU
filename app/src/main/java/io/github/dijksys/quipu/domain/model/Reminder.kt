package io.github.dijksys.quipu.domain.model

data class Reminder(
    val id: String,
    val title: String,
    val description: String?,
    val noteId: String?,
    val scheduledAt: Long,
    val createdAt: Long,
    val updatedAt: Long,
    val completedAt: Long? = null,
    val notifiedAt: Long? = null,
    val status: ReminderStatus = ReminderStatus.PENDING,
) {
    fun isOverdue(nowMillis: Long): Boolean =
        status == ReminderStatus.PENDING && scheduledAt < nowMillis

    fun isUpcoming(nowMillis: Long): Boolean =
        status == ReminderStatus.PENDING && scheduledAt >= nowMillis
}
