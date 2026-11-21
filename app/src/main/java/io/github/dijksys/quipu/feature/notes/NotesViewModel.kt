package io.github.dijksys.quipu.feature.notes

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import io.github.dijksys.quipu.core.common.Clock
import io.github.dijksys.quipu.domain.model.Note
import io.github.dijksys.quipu.domain.model.NoteDateFilter
import io.github.dijksys.quipu.domain.model.NoteDateRange
import io.github.dijksys.quipu.domain.model.NoteFilter
import io.github.dijksys.quipu.domain.model.NoteSortOrder
import io.github.dijksys.quipu.domain.model.resolveRange
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
    private val savedStateHandle: SavedStateHandle,
    private val searchNotes: SearchNotesUseCase,
    private val togglePin: TogglePinNoteUseCase,
    private val archiveNote: ArchiveNoteUseCase,
    private val deleteNote: DeleteNoteUseCase,
    private val restoreNote: RestoreNoteUseCase,
    private val permanentlyDeleteNote: PermanentlyDeleteNoteUseCase,
    private val emptyTrashUseCase: EmptyTrashUseCase,
    private val clock: Clock,
) : ViewModel() {

    private val filter: NoteFilter = NoteFilter.entries
        .firstOrNull { it.name == savedStateHandle.get<String>(ARG_FILTER) }
        ?: NoteFilter.ACTIVE

    private val query = MutableStateFlow(
        savedStateHandle.get<String>(ARG_QUERY).orEmpty(),
    )
    private val loading = MutableStateFlow(true)
    private val dateFilter = MutableStateFlow(NoteDateFilter.ALL)
    private val customRange = MutableStateFlow<NoteDateRange?>(null)
    private val sortOrder = MutableStateFlow(NoteSortOrder.RELEVANCE)
    private val isEmptyingTrash = MutableStateFlow(false)
    private val eventsChannel = Channel<NotesEvent>(Channel.BUFFERED)
    val events = eventsChannel.receiveAsFlow()

    private data class SearchParams(
        val query: String,
        val dateRange: NoteDateRange?,
        val sortOrder: NoteSortOrder,
    )

    private val searchParams = combine(
        query
            .debounce { if (it.isEmpty()) 0L else SEARCH_DEBOUNCE_MILLIS }
            .distinctUntilChanged(),
        dateFilter,
        customRange,
        sortOrder,
    ) { text, date, custom, sort ->
        SearchParams(
            query = text,
            dateRange = date.resolveRange(clock.nowMillis(), custom),
            sortOrder = sort,
        )
    }

    private val notesFlow = searchParams
        .flatMapLatest { params ->
            searchNotes(
                query = params.query,
                filter = filter,
                dateRange = params.dateRange,
                sort = params.sortOrder,
            )
        }
        .catch {
            eventsChannel.send(NotesEvent.Failure)
            emit(emptyList())
        }

    private val filterState = combine(dateFilter, customRange, sortOrder) { date, custom, sort ->
        Triple(date, custom, sort)
    }

    val uiState = combine(
        notesFlow,
        query,
        loading,
        filterState,
        isEmptyingTrash,
    ) { notes, currentQuery, isLoading, filters, emptying ->
        NotesUiState(
            filter = filter,
            query = currentQuery,
            notes = notes,
            isLoading = isLoading,
            dateFilter = filters.first,
            customDateRange = filters.second,
            sortOrder = filters.third,
            isEmptyingTrash = emptying,
        )
    }.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5_000),
        initialValue = NotesUiState(filter = filter, query = query.value),
    )

    init {
        viewModelScope.launch {
            notesFlow.collect { loading.value = false }
        }
    }

    fun onQueryChange(value: String) {
        query.value = value
        savedStateHandle[ARG_QUERY] = value
    }

    fun onDateFilterChange(value: NoteDateFilter) {
        dateFilter.value = value
    }

    fun onCustomRangeChange(range: NoteDateRange) {
        customRange.value = range
        dateFilter.value = NoteDateFilter.CUSTOM
    }

    fun onSortOrderChange(value: NoteSortOrder) {
        sortOrder.value = value
    }

    fun clearSearch() {
        onQueryChange("")
        dateFilter.value = NoteDateFilter.ALL
        customRange.value = null
        sortOrder.value = NoteSortOrder.RELEVANCE
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
        if (!isEmptyingTrash.compareAndSet(expect = false, update = true)) return
        viewModelScope.launch {
            try {
                runCatching {
                    emptyTrashUseCase()
                    eventsChannel.send(NotesEvent.TrashEmptied)
                }.onFailure { eventsChannel.send(NotesEvent.Failure) }
            } finally {
                isEmptyingTrash.value = false
            }
        }
    }

    companion object {
        const val ARG_FILTER = "filter"
        const val ARG_QUERY = "query"
        private const val SEARCH_DEBOUNCE_MILLIS = 250L
    }
}
