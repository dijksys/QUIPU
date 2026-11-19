package io.github.dijksys.quipu.feature.reminders

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import io.github.dijksys.quipu.core.common.AppError
import io.github.dijksys.quipu.core.common.AppResult
import io.github.dijksys.quipu.core.common.Clock
import io.github.dijksys.quipu.core.common.DateTimeUtils
import io.github.dijksys.quipu.domain.model.NoteFilter
import io.github.dijksys.quipu.domain.model.Reminder
import io.github.dijksys.quipu.domain.model.ReminderStatus
import io.github.dijksys.quipu.domain.usecase.notes.GetNoteUseCase
import io.github.dijksys.quipu.domain.usecase.notes.ObserveNotesUseCase
import io.github.dijksys.quipu.domain.usecase.reminders.CancelReminderUseCase
import io.github.dijksys.quipu.domain.usecase.reminders.CompleteReminderUseCase
import io.github.dijksys.quipu.domain.usecase.reminders.CreateReminderUseCase
import io.github.dijksys.quipu.domain.usecase.reminders.DeleteReminderUseCase
import io.github.dijksys.quipu.domain.usecase.reminders.GetReminderUseCase
import io.github.dijksys.quipu.domain.usecase.reminders.ReminderDraft
import io.github.dijksys.quipu.domain.usecase.reminders.UpdateReminderUseCase
import io.github.dijksys.quipu.domain.usecase.settings.ObserveSettingsUseCase
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.receiveAsFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import java.time.LocalDate
import javax.inject.Inject

@HiltViewModel
class ReminderEditorViewModel @Inject constructor(
    savedStateHandle: SavedStateHandle,
    private val getReminder: GetReminderUseCase,
    private val getNote: GetNoteUseCase,
    private val observeNotes: ObserveNotesUseCase,
    private val observeSettings: ObserveSettingsUseCase,
    private val createReminder: CreateReminderUseCase,
    private val updateReminder: UpdateReminderUseCase,
    private val deleteReminder: DeleteReminderUseCase,
    private val completeReminder: CompleteReminderUseCase,
    private val cancelReminder: CancelReminderUseCase,
    private val clock: Clock,
) : ViewModel() {

    private val reminderIdArg: String =
        savedStateHandle.get<String>(ARG_REMINDER_ID) ?: NEW_REMINDER_ID
    private val initialIsNew: Boolean = reminderIdArg == NEW_REMINDER_ID
    private val linkedNoteIdArg: String? =
        savedStateHandle.get<String>(ARG_NOTE_ID)?.takeIf { it.isNotBlank() }

    private val _uiState = MutableStateFlow(
        ReminderEditorUiState(
            reminderId = reminderIdArg,
            isNew = initialIsNew,
            isLoading = true,
            scheduledAt = clock.nowMillis() + DEFAULT_OFFSET_MILLIS,
            noteId = linkedNoteIdArg,
        ),
    )
    val uiState = _uiState.asStateFlow()

    private val eventChannel = Channel<ReminderEditorEvent>(Channel.BUFFERED)
    val events = eventChannel.receiveAsFlow()

    private var currentReminder: Reminder? = null

    init {
        viewModelScope.launch {
            launch {
                observeNotes(NoteFilter.ACTIVE).collect { notes ->
                    _uiState.update { state ->
                        val title = notes.firstOrNull { it.id == state.noteId }?.displayTitle
                        state.copy(
                            availableNotes = notes,
                            linkedNoteTitle = title ?: state.linkedNoteTitle,
                        )
                    }
                }
            }

            launch {
                observeSettings().collect { settings ->
                    _uiState.update { it.copy(notificationsEnabled = settings.notificationsEnabled) }
                }
            }

            if (initialIsNew) {
                val note = linkedNoteIdArg?.let { getNote(it) }
                _uiState.update {
                    it.copy(
                        isLoading = false,
                        isNew = true,
                        title = note?.title.orEmpty(),
                        linkedNoteTitle = note?.displayTitle ?: it.linkedNoteTitle,
                    )
                }
            } else {
                val reminder = getReminder(reminderIdArg)
                if (reminder == null) {
                    _uiState.update {
                        it.copy(isLoading = false, isMissing = true, error = ReminderEditorError.NOT_FOUND)
                    }
                } else {
                    currentReminder = reminder
                    val note = reminder.noteId?.let { getNote(it) }
                    _uiState.update {
                        it.copy(
                            isLoading = false,
                            isNew = false,
                            title = reminder.title,
                            description = reminder.description.orEmpty(),
                            scheduledAt = reminder.scheduledAt,
                            noteId = reminder.noteId,
                            linkedNoteTitle = note?.displayTitle,
                            status = reminder.status,
                        )
                    }
                }
            }
        }
    }

    fun onTitleChange(value: String) {
        _uiState.update { it.copy(title = value, error = null) }
    }

    fun onDescriptionChange(value: String) {
        _uiState.update { it.copy(description = value) }
    }

    fun setDate(date: LocalDate) {
        _uiState.update {
            it.copy(
                scheduledAt = DateTimeUtils.withDate(it.scheduledAt, date),
                error = null,
            )
        }
    }

    fun setTime(hour: Int, minute: Int) {
        _uiState.update {
            it.copy(
                scheduledAt = DateTimeUtils.withTime(it.scheduledAt, hour, minute),
                error = null,
            )
        }
    }

    fun selectNote(noteId: String?) {
        _uiState.update { state ->
            state.copy(
                noteId = noteId,
                linkedNoteTitle = noteId?.let { id ->
                    state.availableNotes.firstOrNull { it.id == id }?.displayTitle
                },
            )
        }
    }

    fun save() {
        val state = _uiState.value
        if (state.isSaving) return

        val title = state.title.trim()
        if (title.isBlank()) {
            _uiState.update { it.copy(error = ReminderEditorError.TITLE_REQUIRED) }
            return
        }
        if (state.scheduledAt <= clock.nowMillis()) {
            _uiState.update { it.copy(error = ReminderEditorError.TIME_IN_PAST) }
            return
        }

        _uiState.update { it.copy(isSaving = true, error = null) }
        viewModelScope.launch {
            runCatching {
                // Evita referencias huérfanas si la nota se eliminó mientras editábamos.
                val safeNoteId = state.noteId?.takeIf { getNote(it) != null }
                val draft = ReminderDraft(
                    title = title,
                    description = state.description.ifBlank { null },
                    noteId = safeNoteId,
                    scheduledAt = state.scheduledAt,
                )
                val result = if (state.isNew) {
                    createReminder(draft)
                } else {
                    currentReminder?.let { updateReminder(it, draft) }
                        ?: AppResult.Failure(AppError.ReminderNotFound)
                }

                when (result) {
                    is AppResult.Success -> {
                        currentReminder = result.value.reminder
                        _uiState.update {
                            it.copy(
                                isLoading = false,
                                isNew = false,
                                isSaving = false,
                                noteId = safeNoteId,
                                status = result.value.reminder.status,
                            )
                        }
                        eventChannel.send(ReminderEditorEvent.Saved)
                    }

                    is AppResult.Failure -> {
                        _uiState.update {
                            it.copy(isSaving = false, error = result.error.toEditorError())
                        }
                    }
                }
            }.onFailure {
                _uiState.update {
                    it.copy(isSaving = false, error = ReminderEditorError.SAVE_FAILED)
                }
            }
        }
    }

    fun delete() {
        val reminder = currentReminder ?: return
        viewModelScope.launch {
            runCatching {
                deleteReminder(reminder.id)
                eventChannel.send(ReminderEditorEvent.Deleted)
            }.onFailure { eventChannel.send(ReminderEditorEvent.Failure) }
        }
    }

    fun complete() {
        val reminder = currentReminder ?: return
        viewModelScope.launch {
            runCatching {
                completeReminder(reminder.id)
                eventChannel.send(ReminderEditorEvent.Completed)
            }.onFailure { eventChannel.send(ReminderEditorEvent.Failure) }
        }
    }

    fun cancelReminder() {
        val reminder = currentReminder ?: return
        viewModelScope.launch {
            runCatching {
                cancelReminder(reminder.id)
                eventChannel.send(ReminderEditorEvent.Cancelled)
            }.onFailure { eventChannel.send(ReminderEditorEvent.Failure) }
        }
    }

    private fun AppError.toEditorError(): ReminderEditorError = when (this) {
        AppError.TitleRequired -> ReminderEditorError.TITLE_REQUIRED
        AppError.ScheduledTimeInPast, AppError.ScheduledTimeRequired ->
            ReminderEditorError.TIME_IN_PAST
        AppError.ReminderNotFound, AppError.NoteNotFound -> ReminderEditorError.NOT_FOUND
        is AppError.Unexpected -> ReminderEditorError.SAVE_FAILED
    }

    companion object {
        const val ARG_REMINDER_ID = "reminderId"
        const val ARG_NOTE_ID = "noteId"
        const val NEW_REMINDER_ID = "new"
        private const val DEFAULT_OFFSET_MILLIS = 60 * 60 * 1_000L
    }
}
