package io.github.dijksys.quipu.feature.notes

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import io.github.dijksys.quipu.domain.model.Note
import io.github.dijksys.quipu.domain.model.NoteFilter
import io.github.dijksys.quipu.domain.usecase.notes.ArchiveNoteUseCase
import io.github.dijksys.quipu.domain.usecase.notes.DeleteNoteUseCase
import io.github.dijksys.quipu.domain.usecase.notes.EmptyTrashUseCase
import io.github.dijksys.quipu.domain.usecase.notes.PermanentlyDeleteNoteUseCase
import io.github.dijksys.quipu.domain.usecase.notes.RestoreNoteUseCase
import io.github.dijksys.quipu.domain.usecase.notes.SearchNotesUseCase
import io.github.dijksys.quipu.domain.usecase.notes.TogglePinNoteUseCase
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.FlowPreview
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.debounce
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.receiveAsFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import javax.inject.Inject

@OptIn(FlowPreview::class, ExperimentalCoroutinesApi::class)
@HiltViewModel
class NotesViewModel @Inject constructor(
    savedStateHandle: SavedStateHandle,
    private val searchNotes: SearchNotesUseCase,
    private val togglePin: TogglePinNoteUseCase,
    private val archiveNote: ArchiveNoteUseCase,
    private val deleteNote: DeleteNoteUseCase,
    private val restoreNote: RestoreNoteUseCase,
    private val permanentlyDeleteNote: PermanentlyDeleteNoteUseCase,
    private val emptyTrash: EmptyTrashUseCase,
) : ViewModel() {

    private val filter: NoteFilter = NoteFilter.entries
        .firstOrNull { it.name == savedStateHandle.get<String>(ARG_FILTER) }
        ?: NoteFilter.ACTIVE

    private val query = MutableStateFlow("")
    private val loading = MutableStateFlow(true)
    private val eventsChannel = Channel<NotesEvent>(Channel.BUFFERED)
    val events = eventsChannel.receiveAsFlow()

    private val notesFlow = query
        .debounce { if (it.isEmpty()) 0L else SEARCH_DEBOUNCE_MILLIS }
        .distinctUntilChanged()
        .flatMapLatest { text -> searchNotes(text, filter) }
        .catch {
            eventsChannel.send(NotesEvent.Failure)
            emit(emptyList())
        }

    val uiState = combine(notesFlow, query, loading) { notes, currentQuery, isLoading ->
        NotesUiState(
            filter = filter,
            query = currentQuery,
            notes = notes,
            isLoading = isLoading,
        )
    }.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5_000),
        initialValue = NotesUiState(filter = filter),
    )

    init {
        viewModelScope.launch {
            notesFlow.collect { loading.value = false }
        }
    }

    fun onQueryChange(value: String) {
        query.value = value
    }

    fun togglePin(note: Note) {
        viewModelScope.launch {
            runCatching {
                val pinned = !note.isPinned
                togglePin(note.id, pinned)
                eventsChannel.send(NotesEvent.PinChanged(pinned))
            }.onFailure { eventsChannel.send(NotesEvent.Failure) }
        }
    }

    fun setArchived(note: Note, archived: Boolean) {
        viewModelScope.launch {
            runCatching {
                archiveNote(note.id, archived)
                eventsChannel.send(NotesEvent.ArchiveChanged(archived))
            }.onFailure { eventsChannel.send(NotesEvent.Failure) }
        }
    }

    fun moveToTrash(note: Note) {
        viewModelScope.launch {
            runCatching {
                deleteNote(note.id)
                eventsChannel.send(NotesEvent.Trashed(note.id))
            }.onFailure { eventsChannel.send(NotesEvent.Failure) }
        }
    }

    fun restore(noteId: String) {
        viewModelScope.launch {
            runCatching {
                restoreNote(noteId)
                eventsChannel.send(NotesEvent.Restored)
            }.onFailure { eventsChannel.send(NotesEvent.Failure) }
        }
    }

    fun deletePermanently(noteId: String) {
        viewModelScope.launch {
            runCatching {
                permanentlyDeleteNote(noteId)
                eventsChannel.send(NotesEvent.PermanentlyDeleted)
            }.onFailure { eventsChannel.send(NotesEvent.Failure) }
        }
    }

    fun emptyTrash() {
        viewModelScope.launch {
            runCatching {
                emptyTrash()
                eventsChannel.send(NotesEvent.TrashEmptied)
            }.onFailure { eventsChannel.send(NotesEvent.Failure) }
        }
    }

    companion object {
        const val ARG_FILTER = "filter"
        private const val SEARCH_DEBOUNCE_MILLIS = 250L
    }
}
