package com.example.quipu.fakes

import com.example.quipu.domain.model.Note
import com.example.quipu.domain.model.NoteFilter
import com.example.quipu.domain.repository.NoteRepository
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.update

class FakeNoteRepository(initialNotes: List<Note> = emptyList()) : NoteRepository {

    private val notes = MutableStateFlow(initialNotes.associateBy { it.id })

    var failWrites: Boolean = false

    val currentNotes: List<Note> get() = notes.value.values.toList()

    fun getStored(id: String): Note? = notes.value[id]

    fun preload(vararg note: Note) {
        notes.update { current -> current + note.associateBy { it.id } }
    }

    override fun observeNotes(filter: NoteFilter): Flow<List<Note>> =
        notes.map { stored -> stored.values.filter { it.matches(filter) }.sortedFor(filter) }

    override fun observeNote(id: String): Flow<Note?> =
        notes.map { stored -> stored[id] }

    override suspend fun getNote(id: String): Note? = notes.value[id]

    override fun searchNotes(query: String, filter: NoteFilter): Flow<List<Note>> =
        notes.map { stored ->
            stored.values
                .filter { it.matches(filter) }
                .filter {
                    it.title.contains(query, ignoreCase = true) ||
                        it.content.contains(query, ignoreCase = true)
                }
                .sortedFor(filter)
        }

    override fun observeRecentNotes(limit: Int): Flow<List<Note>> =
        notes.map { stored ->
            stored.values
                .filter { it.matches(NoteFilter.ACTIVE) }
                .sortedByDescending { it.updatedAt }
                .take(limit)
        }

    override fun observePinnedNotes(limit: Int): Flow<List<Note>> =
        notes.map { stored ->
            stored.values
                .filter { it.matches(NoteFilter.ACTIVE) && it.isPinned }
                .sortedByDescending { it.updatedAt }
                .take(limit)
        }

    override fun observeActiveNotesCount(): Flow<Int> =
        notes.map { stored -> stored.values.count { it.matches(NoteFilter.ACTIVE) } }

    override suspend fun upsertNote(note: Note) {
        if (failWrites) error("Fallo de escritura simulado")
        notes.update { it + (note.id to note) }
    }

    override suspend fun setPinned(id: String, pinned: Boolean, updatedAt: Long) {
        if (failWrites) error("Fallo de escritura simulado")
        update(id) { it.copy(isPinned = pinned, updatedAt = updatedAt) }
    }

    override suspend fun setArchivedAt(id: String, archivedAt: Long?, updatedAt: Long) {
        update(id) { it.copy(archivedAt = archivedAt, updatedAt = updatedAt) }
    }

    override suspend fun setDeletedAt(id: String, deletedAt: Long?, updatedAt: Long) {
        update(id) { it.copy(deletedAt = deletedAt, updatedAt = updatedAt) }
    }

    override suspend fun deleteNotePermanently(id: String) {
        notes.update { it - id }
    }

    override suspend fun emptyTrash() {
        notes.update { stored -> stored.filterValues { it.deletedAt == null } }
    }

    private fun update(id: String, transform: (Note) -> Note) {
        notes.update { stored ->
            val note = stored[id] ?: return@update stored
            stored + (id to transform(note))
        }
    }

    private fun Note.matches(filter: NoteFilter): Boolean = when (filter) {
        NoteFilter.ACTIVE -> deletedAt == null && archivedAt == null
        NoteFilter.ARCHIVED -> deletedAt == null && archivedAt != null
        NoteFilter.TRASH -> deletedAt != null
    }

    private fun List<Note>.sortedFor(filter: NoteFilter): List<Note> = when (filter) {
        NoteFilter.ACTIVE -> sortedWith(
            compareByDescending<Note> { it.isPinned }.thenByDescending { it.updatedAt },
        )

        NoteFilter.ARCHIVED -> sortedByDescending { it.archivedAt }
        NoteFilter.TRASH -> sortedByDescending { it.deletedAt }
    }
}
