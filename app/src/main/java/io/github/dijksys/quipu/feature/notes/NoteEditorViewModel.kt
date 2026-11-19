package io.github.dijksys.quipu.feature.notes

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import io.github.dijksys.quipu.domain.model.Note
import io.github.dijksys.quipu.domain.usecase.notes.ArchiveNoteUseCase
import io.github.dijksys.quipu.domain.usecase.notes.CreateNoteUseCase
import io.github.dijksys.quipu.domain.usecase.notes.DeleteNoteUseCase
import io.github.dijksys.quipu.domain.usecase.notes.GetNoteUseCase
import io.github.dijksys.quipu.domain.usecase.notes.TogglePinNoteUseCase
import io.github.dijksys.quipu.domain.usecase.notes.UpdateNoteUseCase
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.Job
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.receiveAsFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import java.util.UUID
import javax.inject.Inject

/**
 * Editor con guardado automático (debounce) y escritura forzada al salir.
 * El borrador también se conserva en SavedStateHandle para sobrevivir a la
 * muerte del proceso antes de la siguiente escritura.
 */
@HiltViewModel
class NoteEditorViewModel @Inject constructor(
    private val savedStateHandle: SavedStateHandle,
    private val getNote: GetNoteUseCase,
    private val createNote: CreateNoteUseCase,
    private val updateNote: UpdateNoteUseCase,
    private val togglePin: TogglePinNoteUseCase,
    private val archiveNote: ArchiveNoteUseCase,
    private val deleteNote: DeleteNoteUseCase,
) : ViewModel() {

    private val noteIdArg: String = savedStateHandle.get<String>(ARG_NOTE_ID) ?: NEW_NOTE_ID
    private val initialIsNew: Boolean = noteIdArg == NEW_NOTE_ID
    private val noteId: String = if (initialIsNew) {
        savedStateHandle.get<String>(KEY_DRAFT_ID)
            ?: UUID.randomUUID().toString().also { savedStateHandle[KEY_DRAFT_ID] = it }
    } else {
        noteIdArg
    }

    private val _uiState = MutableStateFlow(
        NoteEditorUiState(noteId = noteId, isNew = initialIsNew, isLoading = true),
    )
    val uiState = _uiState.asStateFlow()

    private val eventChannel = Channel<NoteEditorEvent>(Channel.BUFFERED)
    val events = eventChannel.receiveAsFlow()

    private var currentNote: Note? = null
    private var autoSaveJob: Job? = null
    private val saveMutex = Mutex()

    init {
        viewModelScope.launch {
            val storedNote = getNote(noteId)
            val draftTitle = savedStateHandle.get<String>(KEY_DRAFT_TITLE)
            val draftContent = savedStateHandle.get<String>(KEY_DRAFT_CONTENT)

            if (storedNote != null) {
                currentNote = storedNote
                _uiState.update {
                    it.copy(
                        isLoading = false,
                        isNew = false,
                        title = draftTitle ?: storedNote.title,
                        content = draftContent ?: storedNote.content,
                        isPinned = storedNote.isPinned,
                        isArchived = storedNote.isArchived,
                        lastSavedAt = storedNote.updatedAt,
                        createdAt = storedNote.createdAt,
                    )
                }
            } else if (initialIsNew) {
                _uiState.update {
                    it.copy(
                        isLoading = false,
                        isNew = true,
                        title = draftTitle.orEmpty(),
                        content = draftContent.orEmpty(),
                    )
                }
            } else {
                _uiState.update { it.copy(isLoading = false, isMissing = true) }
            }
        }
    }

    fun onTitleChange(value: String) {
        _uiState.update { it.copy(title = value) }
        savedStateHandle[KEY_DRAFT_TITLE] = value
        scheduleAutoSave()
    }

    fun onContentChange(value: String) {
        _uiState.update { it.copy(content = value) }
        savedStateHandle[KEY_DRAFT_CONTENT] = value
        scheduleAutoSave()
    }

    /** Cancela el debounce y escribe inmediatamente (al salir de la pantalla). */
    suspend fun saveNow(): String? {
        autoSaveJob?.cancel()
        persist()
        return currentNote?.id
    }

    fun flush() {
        autoSaveJob?.cancel()
        viewModelScope.launch { persist() }
    }

    fun togglePin() {
        autoSaveJob?.cancel()
        viewModelScope.launch {
            persist()
            val note = currentNote ?: return@launch
            val pinned = !note.isPinned
            runCatching {
                togglePin(note.id, pinned)
                currentNote = note.copy(isPinned = pinned)
                _uiState.update { it.copy(isPinned = pinned) }
                eventChannel.send(NoteEditorEvent.PinChanged(pinned))
            }.onFailure { eventChannel.send(NoteEditorEvent.Failure) }
        }
    }

    fun setArchived(archive: Boolean) {
        autoSaveJob?.cancel()
        viewModelScope.launch {
            persist()
            val note = currentNote ?: return@launch
            runCatching {
                archiveNote(note.id, archive)
                // Se relee para que el siguiente autoguardado no revierta el archivado.
                currentNote = getNote(note.id)
                _uiState.update { it.copy(isArchived = archive) }
                eventChannel.send(NoteEditorEvent.ArchiveChanged(archive))
            }.onFailure { eventChannel.send(NoteEditorEvent.Failure) }
        }
    }

    fun moveToTrash() {
        autoSaveJob?.cancel()
        viewModelScope.launch {
            if (_uiState.value.hasText) persist()
            runCatching {
                currentNote?.let { deleteNote(it.id) }
                eventChannel.send(NoteEditorEvent.Deleted)
            }.onFailure { eventChannel.send(NoteEditorEvent.Failure) }
        }
    }

    private fun scheduleAutoSave() {
        autoSaveJob?.cancel()
        autoSaveJob = viewModelScope.launch {
            delay(AUTO_SAVE_DELAY_MILLIS)
            persist()
        }
    }

    private suspend fun persist(): Note? = saveMutex.withLock {
        val state = _uiState.value
        if (state.isMissing) return@withLock null
        if (!state.hasText && currentNote == null) return@withLock null

        _uiState.update { it.copy(isSaving = true) }
        return try {
            val existing = currentNote
            val saved = if (existing == null) {
                createNote(state.title, state.content, noteId)
            } else {
                updateNote(existing.copy(title = state.title, content = state.content))
            }
            currentNote = saved
            _uiState.update {
                it.copy(
                    isNew = false,
                    isPinned = saved.isPinned,
                    isArchived = saved.isArchived,
                    isSaving = false,
                    lastSavedAt = saved.updatedAt,
                    createdAt = saved.createdAt,
                )
            }
            saved
        } catch (cancellation: CancellationException) {
            throw cancellation
        } catch (_: Throwable) {
            _uiState.update { it.copy(isSaving = false) }
            eventChannel.send(NoteEditorEvent.Failure)
            null
        }
    }

    companion object {
        const val ARG_NOTE_ID = "noteId"
        const val NEW_NOTE_ID = "new"
        private const val KEY_DRAFT_ID = "draft_note_id"
        private const val KEY_DRAFT_TITLE = "draft_title"
        private const val KEY_DRAFT_CONTENT = "draft_content"
        private const val AUTO_SAVE_DELAY_MILLIS = 600L
    }
}
