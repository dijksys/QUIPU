package io.github.dijksys.quipu.feature.notes

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import io.github.dijksys.quipu.domain.model.Note
import io.github.dijksys.quipu.domain.search.SearchText
import io.github.dijksys.quipu.domain.usecase.notes.ArchiveNoteUseCase
import io.github.dijksys.quipu.domain.usecase.notes.CreateNoteUseCase
import io.github.dijksys.quipu.domain.usecase.notes.DeleteNoteUseCase
import io.github.dijksys.quipu.domain.usecase.notes.GetNoteUseCase
import io.github.dijksys.quipu.domain.usecase.notes.ObserveTagsUseCase
import io.github.dijksys.quipu.domain.usecase.notes.SetNoteTagsUseCase
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
    private val setNoteTags: SetNoteTagsUseCase,
    private val observeTags: ObserveTagsUseCase,
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
        NoteEditorUiState(
            noteId = noteId,
            isNewSession = initialIsNew,
            isLoading = true,
        ),
    )
    val uiState = _uiState.asStateFlow()

    private val eventChannel = Channel<NoteEditorEvent>(Channel.BUFFERED)
    val events = eventChannel.receiveAsFlow()

    private var currentNote: Note? = null
    private var debounceJob: Job? = null
    private val saveMutex = Mutex()

    init {
        viewModelScope.launch {
            observeTags().collect { tags ->
                _uiState.update { it.copy(availableTags = tags) }
            }
        }
        viewModelScope.launch {
            val storedNote = getNote(noteId)
            val draftTitle = savedStateHandle.get<String>(KEY_DRAFT_TITLE)
            val draftContent = savedStateHandle.get<String>(KEY_DRAFT_CONTENT)
            val draftTags = savedStateHandle.get<ArrayList<String>>(KEY_DRAFT_TAGS)

            if (storedNote != null) {
                currentNote = storedNote
                val restoredTitle = draftTitle ?: storedNote.title
                val restoredContent = draftContent ?: storedNote.content
                val restoredTags = draftTags ?: ArrayList(storedNote.tags)
                val hasPendingDraft = restoredTitle.trim() != storedNote.title ||
                    restoredContent != storedNote.content
                _uiState.update {
                    it.copy(
                        isLoading = false,
                        isNewSession = initialIsNew,
                        isPersisted = true,
                        title = restoredTitle,
                        content = restoredContent,
                        tags = restoredTags,
                        isPinned = storedNote.isPinned,
                        isArchived = storedNote.isArchived,
                        saveStatus = if (hasPendingDraft) {
                            NoteSaveStatus.UNSAVED
                        } else {
                            NoteSaveStatus.SAVED
                        },
                        createdAt = storedNote.createdAt,
                        updatedAt = storedNote.updatedAt,
                    )
                }
                if (hasPendingDraft) ensureAutoSaveScheduled()
            } else if (initialIsNew) {
                val restoredTitle = draftTitle.orEmpty()
                val restoredContent = draftContent.orEmpty()
                val hasText = restoredTitle.isNotBlank() || restoredContent.isNotBlank()
                _uiState.update {
                    it.copy(
                        isLoading = false,
                        isNewSession = true,
                        isPersisted = false,
                        title = restoredTitle,
                        content = restoredContent,
                        tags = draftTags.orEmpty(),
                        saveStatus = if (hasText) {
                            NoteSaveStatus.UNSAVED
                        } else {
                            NoteSaveStatus.EMPTY
                        },
                    )
                }
                if (hasText) ensureAutoSaveScheduled()
            } else {
                _uiState.update {
                    it.copy(
                        isLoading = false,
                        isMissing = true,
                        isNewSession = false,
                    )
                }
            }
        }
    }

    fun onTitleChange(value: String) {
        applyEdit { it.copy(title = value) }
        savedStateHandle[KEY_DRAFT_TITLE] = value
    }

    fun onContentChange(value: String) {
        applyEdit { it.copy(content = value) }
        savedStateHandle[KEY_DRAFT_CONTENT] = value
    }

    /** Las etiquetas se guardan aparte del contenido y sin tocar updatedAt. */
    fun setTags(tags: List<String>) {
        val cleaned = tags
            .map { it.trim() }
            .filter { it.isNotEmpty() }
            .distinctBy { SearchText.normalizeTag(it) }
        _uiState.update { it.copy(tags = cleaned) }
        savedStateHandle[KEY_DRAFT_TAGS] = ArrayList(cleaned)

        val note = currentNote ?: return
        viewModelScope.launch {
            runCatching {
                setNoteTags(note.id, cleaned)
                getNote(note.id)
            }.onSuccess { refreshed ->
                currentNote = refreshed ?: note.copy(tags = cleaned)
                _uiState.update { it.copy(tags = currentNote?.tags ?: cleaned) }
            }.onFailure {
                eventChannel.send(NoteEditorEvent.Failure)
            }
        }
    }

    /** Cancela el debounce y escribe inmediatamente (al salir de la pantalla). */
    suspend fun saveNow(): String? {
        cancelDebounce()
        persist()
        return if (_uiState.value.saveStatus == NoteSaveStatus.ERROR) {
            null
        } else {
            currentNote?.id
        }
    }

    /** Persiste al pasar la aplicación a segundo plano sin cancelar una escritura activa. */
    fun flush() {
        cancelDebounce()
        viewModelScope.launch { persist() }
    }

    /** Reintenta explícitamente después de un error de persistencia. */
    fun retrySave() {
        cancelDebounce()
        viewModelScope.launch { persist() }
    }

    fun togglePin() {
        cancelDebounce()
        viewModelScope.launch {
            val note = persist() ?: return@launch
            if (_uiState.value.saveStatus != NoteSaveStatus.SAVED) return@launch

            val pinned = !note.isPinned
            runCatching {
                togglePin(note.id, pinned)
                val refreshed = getNote(note.id)
                currentNote = refreshed ?: note.copy(isPinned = pinned)
                _uiState.update {
                    it.copy(
                        isPinned = pinned,
                        isArchived = refreshed?.isArchived ?: it.isArchived,
                        createdAt = refreshed?.createdAt ?: it.createdAt,
                        updatedAt = refreshed?.updatedAt ?: it.updatedAt,
                    )
                }
                eventChannel.send(NoteEditorEvent.PinChanged(pinned))
            }.onFailure { eventChannel.send(NoteEditorEvent.Failure) }
        }
    }

    fun setArchived(archive: Boolean) {
        cancelDebounce()
        viewModelScope.launch {
            val note = persist() ?: return@launch
            if (_uiState.value.saveStatus != NoteSaveStatus.SAVED) return@launch

            runCatching {
                archiveNote(note.id, archive)
                val refreshed = getNote(note.id)
                currentNote = refreshed ?: note
                _uiState.update {
                    it.copy(
                        isArchived = refreshed?.isArchived ?: archive,
                        isPinned = refreshed?.isPinned ?: it.isPinned,
                        createdAt = refreshed?.createdAt ?: it.createdAt,
                        updatedAt = refreshed?.updatedAt ?: it.updatedAt,
                    )
                }
                eventChannel.send(NoteEditorEvent.ArchiveChanged(archive))
            }.onFailure { eventChannel.send(NoteEditorEvent.Failure) }
        }
    }

    fun moveToTrash() {
        cancelDebounce()
        viewModelScope.launch {
            if (_uiState.value.hasText) {
                val saved = persist() ?: return@launch
                if (_uiState.value.saveStatus == NoteSaveStatus.ERROR) return@launch
                currentNote = currentNote ?: saved
            }
            runCatching {
                currentNote?.let { deleteNote(it.id) }
                eventChannel.send(NoteEditorEvent.Deleted)
            }.onFailure { eventChannel.send(NoteEditorEvent.Failure) }
        }
    }

    private fun applyEdit(transform: (NoteEditorUiState) -> NoteEditorUiState) {
        _uiState.update { current ->
            val edited = transform(current).copy(justCreated = false)
            edited.copy(
                saveStatus = if (!edited.isPersisted && !edited.hasText) {
                    NoteSaveStatus.EMPTY
                } else {
                    NoteSaveStatus.UNSAVED
                },
            )
        }

        if (_uiState.value.saveStatus == NoteSaveStatus.UNSAVED) {
            scheduleAutoSave()
        } else {
            cancelDebounce()
        }
    }

    private fun scheduleAutoSave() {
        debounceJob?.cancel()
        startDebounce()
    }

    private fun ensureAutoSaveScheduled() {
        if (debounceJob?.isActive == true) return
        startDebounce()
    }

    private fun startDebounce() {
        debounceJob = viewModelScope.launch {
            delay(AUTO_SAVE_DELAY_MILLIS)
            // La escritura queda fuera del job de debounce para que nuevas
            // pulsaciones no cancelen una operación ya iniciada.
            debounceJob = null
            persist()
        }
    }

    private fun cancelDebounce() {
        debounceJob?.cancel()
        debounceJob = null
    }

    /**
     * Persiste únicamente si existe una diferencia real. El título se normaliza
     * igual que en los casos de uso para evitar escrituras y timestamps falsos.
     */
    private suspend fun persist(): Note? = saveMutex.withLock {
        val snapshot = _uiState.value
        if (snapshot.isMissing) return@withLock null

        val existing = currentNote
        if (existing == null && !snapshot.hasText) {
            _uiState.update {
                it.copy(
                    isPersisted = false,
                    saveStatus = NoteSaveStatus.EMPTY,
                )
            }
            return@withLock null
        }

        val normalizedTitle = snapshot.title.trim()
        if (existing != null &&
            normalizedTitle == existing.title &&
            snapshot.content == existing.content
        ) {
            _uiState.update {
                it.copy(
                    isPersisted = true,
                    saveStatus = NoteSaveStatus.SAVED,
                    isPinned = existing.isPinned,
                    isArchived = existing.isArchived,
                    createdAt = existing.createdAt,
                    updatedAt = existing.updatedAt,
                )
            }
            return@withLock existing
        }

        _uiState.update { it.copy(saveStatus = NoteSaveStatus.SAVING) }
        return try {
            val saved = if (existing == null) {
                createNote(snapshot.title, snapshot.content, noteId)
            } else {
                updateNote(existing.copy(title = snapshot.title, content = snapshot.content))
            }
            val desiredTags = snapshot.tags
            val savedWithTags = if (desiredTags != saved.tags) {
                setNoteTags(saved.id, desiredTags)
                saved.copy(tags = desiredTags)
            } else {
                saved
            }
            currentNote = savedWithTags

            val latest = _uiState.value
            val stillPending = latest.title.trim() != savedWithTags.title ||
                latest.content != savedWithTags.content
            _uiState.update {
                it.copy(
                    isPersisted = true,
                    isPinned = savedWithTags.isPinned,
                    isArchived = savedWithTags.isArchived,
                    tags = savedWithTags.tags,
                    saveStatus = if (stillPending) {
                        NoteSaveStatus.UNSAVED
                    } else {
                        NoteSaveStatus.SAVED
                    },
                    createdAt = savedWithTags.createdAt,
                    updatedAt = savedWithTags.updatedAt,
                    justCreated = existing == null && !stillPending,
                )
            }
            if (stillPending) ensureAutoSaveScheduled()
            savedWithTags
        } catch (cancellation: CancellationException) {
            throw cancellation
        } catch (_: Throwable) {
            _uiState.update { it.copy(saveStatus = NoteSaveStatus.ERROR) }
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
        private const val KEY_DRAFT_TAGS = "draft_tags"
        private const val AUTO_SAVE_DELAY_MILLIS = 600L
    }
}
