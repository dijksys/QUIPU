package com.example.quipu.domain.usecase.reminders

import com.example.quipu.domain.model.Reminder
import com.example.quipu.domain.repository.ReminderRepository
import javax.inject.Inject

class GetReminderUseCase @Inject constructor(
    private val reminderRepository: ReminderRepository,
) {
    suspend operator fun invoke(reminderId: String): Reminder? =
        reminderRepository.getReminder(reminderId)
}
