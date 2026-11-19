package com.example.quipu.domain.repository

import com.example.quipu.domain.model.Note
import com.example.quipu.domain.model.NoteFilter
import kotlinx.coroutines.flow.Flow

interface NoteRepository {
    fun observeNotes(filter: NoteFilter): Flow<List<Note>>
    fun observeNote(id: String): Flow<Note?>
    suspend fun getNote(id: String): Note?
    fun searchNotes(query: String, filter: NoteFilter): Flow<List<Note>>
    fun observeRecentNotes(limit: Int): Flow<List<Note>>
    fun observePinnedNotes(limit: Int): Flow<List<Note>>
    fun observeActiveNotesCount(): Flow<Int>

    suspend fun upsertNote(note: Note)
    suspend fun setPinned(id: String, pinned: Boolean, updatedAt: Long)
    suspend fun setArchivedAt(id: String, archivedAt: Long?, updatedAt: Long)
    suspend fun setDeletedAt(id: String, deletedAt: Long?, updatedAt: Long)
    suspend fun deleteNotePermanently(id: String)
    suspend fun emptyTrash()
}
