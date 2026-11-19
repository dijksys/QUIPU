package io.github.dijksys.quipu.domain.usecase.reminders

import io.github.dijksys.quipu.domain.model.Reminder
import io.github.dijksys.quipu.domain.repository.ReminderRepository
import kotlinx.coroutines.flow.Flow
import javax.inject.Inject

class ObserveReminderUseCase @Inject constructor(
    private val reminderRepository: ReminderRepository,
) {
    operator fun invoke(reminderId: String): Flow<Reminder?> =
        reminderRepository.observeReminder(reminderId)
}
