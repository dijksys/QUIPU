package io.github.dijksys.quipu.fakes

import io.github.dijksys.quipu.domain.model.Note
import io.github.dijksys.quipu.domain.model.NoteDateRange
import io.github.dijksys.quipu.domain.model.NoteFilter
import io.github.dijksys.quipu.domain.model.NoteSortOrder
import io.github.dijksys.quipu.domain.repository.NoteRepository
import io.github.dijksys.quipu.domain.search.SearchText
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.update

class FakeNoteRepository(initialNotes: List<Note> = emptyList()) : NoteRepository {

    private val notes = MutableStateFlow(initialNotes.associateBy { it.id })

    var failWrites: Boolean = false
    var writeGate: CompletableDeferred<Unit>? = null
    var emptyTrashGate: CompletableDeferred<Unit>? = null
    var upsertCount: Int = 0
        private set
    var pinCount: Int = 0
        private set
    var archiveCount: Int = 0
        private set
    var emptyTrashCount: Int = 0
        private set

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

    override fun searchNotes(
        query: String,
        filter: NoteFilter,
        dateRange: NoteDateRange?,
        sort: NoteSortOrder,
    ): Flow<List<Note>> = notes.map { stored ->
        val terms = SearchText.terms(query)
        stored.values
            .filter { it.matches(filter) }
            .filter { note -> dateRange == null || dateRange.contains(note.updatedAt) }
            .filter { note ->
                val title = SearchText.normalize(note.title)
                val content = SearchText.normalize(note.content)
                val tags = SearchText.normalize(note.tags.joinToString(" "))
                terms.all { term ->
                    title.contains(term) || content.contains(term) || tags.contains(term)
                }
            }
            .sortedWith(sortComparator(sort, terms))
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

    override fun observeTags(): Flow<List<String>> =
        notes.map { stored -> stored.values.flatMap { it.tags }.distinct().sorted() }

    override suspend fun upsertNote(note: Note) {
        writeGate?.await()
        if (failWrites) error("Fallo de escritura simulado")
        upsertCount++
        notes.update { it + (note.id to note) }
    }

    override suspend fun setNoteTags(noteId: String, tags: List<String>) {
        if (failWrites) error("Fallo de escritura simulado")
        update(noteId) { it.copy(tags = tags) }
    }

    override suspend fun setPinned(id: String, pinned: Boolean, updatedAt: Long) {
        if (failWrites) error("Fallo de escritura simulado")
        pinCount++
        update(id) { it.copy(isPinned = pinned, updatedAt = updatedAt) }
    }

    override suspend fun setArchivedAt(id: String, archivedAt: Long?, updatedAt: Long) {
        archiveCount++
        update(id) { it.copy(archivedAt = archivedAt, updatedAt = updatedAt) }
    }

    override suspend fun setDeletedAt(id: String, deletedAt: Long?, updatedAt: Long) {
        update(id) { it.copy(deletedAt = deletedAt, updatedAt = updatedAt) }
    }

    override suspend fun deleteNotePermanently(id: String) {
        notes.update { it - id }
    }

    override suspend fun emptyTrash() {
        emptyTrashGate?.await()
        emptyTrashCount++
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

    private fun sortComparator(
        sort: NoteSortOrder,
        terms: List<String>,
    ): Comparator<Note> = when (sort) {
        NoteSortOrder.RECENT -> compareByDescending { it.updatedAt }
        NoteSortOrder.TITLE -> Comparator<Note> { first, second ->
            val byTitle = first.displayTitle.compareTo(second.displayTitle, ignoreCase = true)
            if (byTitle != 0) byTitle else second.updatedAt.compareTo(first.updatedAt)
        }
        NoteSortOrder.RELEVANCE -> compareBy<Note> { note ->
            val title = SearchText.normalize(note.title)
            val tags = SearchText.normalize(note.tags.joinToString(" "))
            when {
                terms.isNotEmpty() && title == terms.joinToString(" ") -> 0
                terms.any { title.startsWith(it) } -> 1
                terms.any { tags.contains(it) } -> 2
                else -> 3
            }
        }.thenByDescending { it.updatedAt }
    }
}
