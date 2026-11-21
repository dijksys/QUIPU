package io.github.dijksys.quipu.feature.notes

import io.github.dijksys.quipu.domain.model.Note
import io.github.dijksys.quipu.domain.model.NoteDateFilter
import io.github.dijksys.quipu.domain.model.NoteDateRange
import io.github.dijksys.quipu.domain.model.NoteFilter
import io.github.dijksys.quipu.domain.model.NoteSortOrder

data class NotesUiState(
    val filter: NoteFilter = NoteFilter.ACTIVE,
    val query: String = "",
    val notes: List<Note> = emptyList(),
    val isLoading: Boolean = true,
    val dateFilter: NoteDateFilter = NoteDateFilter.ALL,
    val customDateRange: NoteDateRange? = null,
    val sortOrder: NoteSortOrder = NoteSortOrder.RELEVANCE,
    val isEmptyingTrash: Boolean = false,
) {
    val isActive: Boolean get() = filter == NoteFilter.ACTIVE
    val isArchived: Boolean get() = filter == NoteFilter.ARCHIVED
    val isTrash: Boolean get() = filter == NoteFilter.TRASH
    val isEmpty: Boolean get() = !isLoading && notes.isEmpty()
    val isSearching: Boolean get() = query.isNotBlank()
    val hasActiveFilters: Boolean
        get() = dateFilter != NoteDateFilter.ALL || sortOrder != NoteSortOrder.RELEVANCE
}

sealed interface NotesEvent {
    data class PinChanged(val pinned: Boolean) : NotesEvent
    data class ArchiveChanged(val archived: Boolean) : NotesEvent
    data class Trashed(val noteId: String) : NotesEvent
    data object Restored : NotesEvent
    data object PermanentlyDeleted : NotesEvent
    data object TrashEmptied : NotesEvent
    data object Failure : NotesEvent
}

enum class NoteSaveStatus {
    EMPTY,
    UNSAVED,
    SAVING,
    SAVED,
    ERROR,
}

data class NoteEditorUiState(
    val noteId: String,
    val isLoading: Boolean = true,
    val isNewSession: Boolean = true,
    val isPersisted: Boolean = false,
    val title: String = "",
    val content: String = "",
    val isPinned: Boolean = false,
    val isArchived: Boolean = false,
    val saveStatus: NoteSaveStatus = NoteSaveStatus.EMPTY,
    val createdAt: Long? = null,
    val updatedAt: Long? = null,
    val justCreated: Boolean = false,
    val isMissing: Boolean = false,
    val tags: List<String> = emptyList(),
    val availableTags: List<String> = emptyList(),
) {
    val hasText: Boolean get() = title.isNotBlank() || content.isNotBlank()
}

sealed interface NoteEditorEvent {
    data class PinChanged(val pinned: Boolean) : NoteEditorEvent
    data class ArchiveChanged(val archived: Boolean) : NoteEditorEvent
    data object Deleted : NoteEditorEvent
    data object Failure : NoteEditorEvent
}
