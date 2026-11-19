package io.github.dijksys.quipu.domain.usecase.reminders

import io.github.dijksys.quipu.domain.model.Reminder
import io.github.dijksys.quipu.domain.repository.ReminderRepository
import kotlinx.coroutines.flow.Flow
import javax.inject.Inject

class ObserveRemindersUseCase @Inject constructor(
    private val reminderRepository: ReminderRepository,
) {
    operator fun invoke(): Flow<List<Reminder>> = reminderRepository.observeAllReminders()
}
