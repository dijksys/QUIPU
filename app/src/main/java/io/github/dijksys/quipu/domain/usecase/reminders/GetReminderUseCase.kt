package io.github.dijksys.quipu.domain.usecase.reminders

import io.github.dijksys.quipu.domain.model.Reminder
import io.github.dijksys.quipu.domain.repository.ReminderRepository
import javax.inject.Inject

class GetReminderUseCase @Inject constructor(
    private val reminderRepository: ReminderRepository,
) {
    suspend operator fun invoke(reminderId: String): Reminder? =
        reminderRepository.getReminder(reminderId)
}
