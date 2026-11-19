package com.example.quipu.data.repository

import com.example.quipu.data.local.dao.NoteDao
import com.example.quipu.data.local.mapper.toDomain
import com.example.quipu.data.local.mapper.toEntity
import com.example.quipu.domain.model.Note
import com.example.quipu.domain.model.NoteFilter
import com.example.quipu.domain.repository.NoteRepository
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class NoteRepositoryImpl @Inject constructor(
    private val noteDao: NoteDao,
) : NoteRepository {

    override fun observeNotes(filter: NoteFilter): Flow<List<Note>> = when (filter) {
        NoteFilter.ACTIVE -> noteDao.observeActiveNotes()
        NoteFilter.ARCHIVED -> noteDao.observeArchivedNotes()
        NoteFilter.TRASH -> noteDao.observeTrashedNotes()
    }.map { entities -> entities.map { it.toDomain() } }

    override fun observeNote(id: String): Flow<Note?> =
        noteDao.observeNote(id).map { it?.toDomain() }

    override suspend fun getNote(id: String): Note? = noteDao.getNote(id)?.toDomain()

    override fun searchNotes(query: String, filter: NoteFilter): Flow<List<Note>> =
        when (filter) {
            NoteFilter.ACTIVE -> noteDao.searchActiveNotes(query)
            NoteFilter.ARCHIVED -> noteDao.searchArchivedNotes(query)
            NoteFilter.TRASH -> noteDao.searchTrashedNotes(query)
        }.map { entities -> entities.map { it.toDomain() } }

    override fun observeRecentNotes(limit: Int): Flow<List<Note>> =
        noteDao.observeRecentNotes(limit).map { entities -> entities.map { it.toDomain() } }

    override fun observePinnedNotes(limit: Int): Flow<List<Note>> =
        noteDao.observePinnedNotes(limit).map { entities -> entities.map { it.toDomain() } }

    override fun observeActiveNotesCount(): Flow<Int> = noteDao.observeActiveNotesCount()

    override suspend fun upsertNote(note: Note) {
        noteDao.upsert(note.toEntity())
    }

    override suspend fun setPinned(id: String, pinned: Boolean, updatedAt: Long) {
        noteDao.setPinned(id, pinned, updatedAt)
    }

    override suspend fun setArchivedAt(id: String, archivedAt: Long?, updatedAt: Long) {
        noteDao.setArchivedAt(id, archivedAt, updatedAt)
    }

    override suspend fun setDeletedAt(id: String, deletedAt: Long?, updatedAt: Long) {
        noteDao.setDeletedAt(id, deletedAt, updatedAt)
    }

    override suspend fun deleteNotePermanently(id: String) {
        noteDao.deleteById(id)
    }

    override suspend fun emptyTrash() {
        noteDao.deleteAllTrashed()
    }
}
