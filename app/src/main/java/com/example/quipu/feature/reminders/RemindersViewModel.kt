package com.example.quipu.feature.reminders

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.quipu.core.common.Clock
import com.example.quipu.domain.model.Reminder
import com.example.quipu.domain.model.ReminderFilter
import com.example.quipu.domain.model.ReminderStatus
import com.example.quipu.domain.usecase.reminders.CancelReminderUseCase
import com.example.quipu.domain.usecase.reminders.CompleteReminderUseCase
import com.example.quipu.domain.usecase.reminders.DeleteReminderUseCase
import com.example.quipu.domain.usecase.reminders.ObserveRemindersUseCase
import com.example.quipu.domain.usecase.settings.ObserveSettingsUseCase
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.receiveAsFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class RemindersViewModel @Inject constructor(
    private val savedStateHandle: SavedStateHandle,
    observeReminders: ObserveRemindersUseCase,
    observeSettings: ObserveSettingsUseCase,
    private val completeReminder: CompleteReminderUseCase,
    private val cancelReminder: CancelReminderUseCase,
    private val deleteReminder: DeleteReminderUseCase,
    private val clock: Clock,
) : ViewModel() {

    private val filter = MutableStateFlow(
        ReminderFilter.entries
            .firstOrNull { it.name == savedStateHandle.get<String>(KEY_FILTER) }
            ?: ReminderFilter.PENDING,
    )

    private val eventChannel = Channel<RemindersEvent>(Channel.BUFFERED)
    val events = eventChannel.receiveAsFlow()

    /**
     * Actualiza la clasificación de vencidos aunque no haya cambios en la base de datos.
     */
    private val ticker = flow {
        while (true) {
            emit(clock.nowMillis())
            delay(TICK_INTERVAL_MILLIS)
        }
    }

    val uiState = combine(
        observeReminders(),
        ticker,
        filter,
        observeSettings(),
    ) { reminders, now, currentFilter, settings ->
        val pending = reminders
            .filter { it.status == ReminderStatus.PENDING && it.scheduledAt >= now }
            .sortedBy { it.scheduledAt }
        val overdue = reminders
            .filter { it.status == ReminderStatus.PENDING && it.scheduledAt < now }
            .sortedByDescending { it.scheduledAt }
        val completed = reminders
            .filter { it.status == ReminderStatus.COMPLETED }
            .sortedByDescending { it.completedAt ?: it.updatedAt }
        val cancelled = reminders
            .filter { it.status == ReminderStatus.CANCELLED }
            .sortedByDescending { it.updatedAt }

        RemindersUiState(
            filter = currentFilter,
            isLoading = false,
            nowMillis = now,
            pending = pending,
            overdue = overdue,
            completed = completed,
            cancelled = cancelled,
            notificationsEnabled = settings.notificationsEnabled,
        )
    }.catch {
        eventChannel.send(RemindersEvent.Failure)
        emit(RemindersUiState(filter = filter.value, isLoading = false))
    }.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5_000),
        initialValue = RemindersUiState(filter = filter.value),
    )

    fun setFilter(newFilter: ReminderFilter) {
        filter.value = newFilter
        savedStateHandle[KEY_FILTER] = newFilter.name
    }

    fun complete(reminder: Reminder) {
        viewModelScope.launch {
            runCatching {
                completeReminder(reminder.id)
                eventChannel.send(RemindersEvent.Completed)
            }.onFailure { eventChannel.send(RemindersEvent.Failure) }
        }
    }

    fun cancel(reminder: Reminder) {
        viewModelScope.launch {
            runCatching {
                cancelReminder(reminder.id)
                eventChannel.send(RemindersEvent.Cancelled)
            }.onFailure { eventChannel.send(RemindersEvent.Failure) }
        }
    }

    fun delete(reminder: Reminder) {
        viewModelScope.launch {
            runCatching {
                deleteReminder(reminder.id)
                eventChannel.send(RemindersEvent.Deleted)
            }.onFailure { eventChannel.send(RemindersEvent.Failure) }
        }
    }

    companion object {
        private const val KEY_FILTER = "reminder_filter"
        private const val TICK_INTERVAL_MILLIS = 60_000L
    }
}
