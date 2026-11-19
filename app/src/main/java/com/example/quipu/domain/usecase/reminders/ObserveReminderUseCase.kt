package com.example.quipu.domain.usecase.reminders

import com.example.quipu.domain.model.Reminder
import com.example.quipu.domain.repository.ReminderRepository
import kotlinx.coroutines.flow.Flow
import javax.inject.Inject

class ObserveReminderUseCase @Inject constructor(
    private val reminderRepository: ReminderRepository,
) {
    operator fun invoke(reminderId: String): Flow<Reminder?> =
        reminderRepository.observeReminder(reminderId)
}
