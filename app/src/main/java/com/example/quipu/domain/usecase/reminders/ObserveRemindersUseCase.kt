package com.example.quipu.domain.usecase.reminders

import com.example.quipu.domain.model.Reminder
import com.example.quipu.domain.repository.ReminderRepository
import kotlinx.coroutines.flow.Flow
import javax.inject.Inject

class ObserveRemindersUseCase @Inject constructor(
    private val reminderRepository: ReminderRepository,
) {
    operator fun invoke(): Flow<List<Reminder>> = reminderRepository.observeAllReminders()
}
