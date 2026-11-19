package io.github.dijksys.quipu.domain.usecase.settings

import io.github.dijksys.quipu.domain.repository.SettingsRepository
import io.github.dijksys.quipu.domain.usecase.reminders.RescheduleRemindersUseCase
import javax.inject.Inject

class SetNotificationsEnabledUseCase @Inject constructor(
    private val settingsRepository: SettingsRepository,
    private val rescheduleReminders: RescheduleRemindersUseCase,
) {
    suspend operator fun invoke(enabled: Boolean) {
        settingsRepository.setNotificationsEnabled(enabled)
        // Al desactivar se cancelan las alarmas; al activar se reprograman las futuras.
        rescheduleReminders()
    }
}
