package io.github.dijksys.quipu.domain.repository

import io.github.dijksys.quipu.domain.model.Note
import io.github.dijksys.quipu.domain.model.NoteDateRange
import io.github.dijksys.quipu.domain.model.NoteFilter
import io.github.dijksys.quipu.domain.model.NoteSortOrder
import kotlinx.coroutines.flow.Flow

interface NoteRepository {
    fun observeNotes(filter: NoteFilter): Flow<List<Note>>
    fun observeNote(id: String): Flow<Note?>
    suspend fun getNote(id: String): Note?
    fun searchNotes(
        query: String,
        filter: NoteFilter,
        dateRange: NoteDateRange? = null,
        sort: NoteSortOrder = NoteSortOrder.RELEVANCE,
    ): Flow<List<Note>>
    fun observeRecentNotes(limit: Int): Flow<List<Note>>
    fun observePinnedNotes(limit: Int): Flow<List<Note>>
    fun observeActiveNotesCount(): Flow<Int>
    fun observeTags(): Flow<List<String>>

    suspend fun upsertNote(note: Note)
    suspend fun setNoteTags(noteId: String, tags: List<String>)
    suspend fun setPinned(id: String, pinned: Boolean, updatedAt: Long)
    suspend fun setArchivedAt(id: String, archivedAt: Long?, updatedAt: Long)
    suspend fun setDeletedAt(id: String, deletedAt: Long?, updatedAt: Long)
    suspend fun deleteNotePermanently(id: String)
    suspend fun emptyTrash()
}
