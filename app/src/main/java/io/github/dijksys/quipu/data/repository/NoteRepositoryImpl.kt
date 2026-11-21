package io.github.dijksys.quipu.data.repository

import androidx.room.withTransaction
import androidx.sqlite.db.SimpleSQLiteQuery
import io.github.dijksys.quipu.core.database.AppDatabase
import io.github.dijksys.quipu.data.local.dao.NoteDao
import io.github.dijksys.quipu.data.local.dao.NoteFtsDao
import io.github.dijksys.quipu.data.local.dao.NoteTagDao
import io.github.dijksys.quipu.data.local.entity.NoteFtsEntity
import io.github.dijksys.quipu.data.local.entity.NoteTagCrossRef
import io.github.dijksys.quipu.data.local.entity.NoteTagEntity
import io.github.dijksys.quipu.data.local.mapper.toDomain
import io.github.dijksys.quipu.data.local.mapper.toEntity
import io.github.dijksys.quipu.data.local.search.NoteSearchQueryBuilder
import io.github.dijksys.quipu.domain.model.Note
import io.github.dijksys.quipu.domain.model.NoteDateRange
import io.github.dijksys.quipu.domain.model.NoteFilter
import io.github.dijksys.quipu.domain.model.NoteSortOrder
import io.github.dijksys.quipu.domain.repository.NoteRepository
import io.github.dijksys.quipu.domain.search.SearchText
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.map
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class NoteRepositoryImpl @Inject constructor(
    private val database: AppDatabase,
    private val noteDao: NoteDao,
    private val noteFtsDao: NoteFtsDao,
    private val noteTagDao: NoteTagDao,
) : NoteRepository {

    override fun observeNotes(filter: NoteFilter): Flow<List<Note>> = notesWithTags(
        when (filter) {
            NoteFilter.ACTIVE -> noteDao.observeActiveNotes()
            NoteFilter.ARCHIVED -> noteDao.observeArchivedNotes()
            NoteFilter.TRASH -> noteDao.observeTrashedNotes()
        },
    )

    override fun observeNote(id: String): Flow<Note?> = combine(
        noteDao.observeNote(id),
        noteTagDao.observeTagNamesForNote(id),
    ) { entity, tags -> entity?.toDomain(tags) }

    override suspend fun getNote(id: String): Note? = database.withTransaction {
        noteDao.getNote(id)?.toDomain(noteTagDao.getTagNamesForNote(id))
    }

    override fun searchNotes(
        query: String,
        filter: NoteFilter,
        dateRange: NoteDateRange?,
        sort: NoteSortOrder,
    ): Flow<List<Note>> {
        val built = NoteSearchQueryBuilder.build(query, filter, dateRange, sort)
            ?: return observeNotes(filter)
        return notesWithTags(
            noteDao.searchNotes(SimpleSQLiteQuery(built.sql, built.args.toTypedArray())),
        )
    }

    override fun observeRecentNotes(limit: Int): Flow<List<Note>> =
        notesWithTags(noteDao.observeRecentNotes(limit))

    override fun observePinnedNotes(limit: Int): Flow<List<Note>> =
        notesWithTags(noteDao.observePinnedNotes(limit))

    override fun observeActiveNotesCount(): Flow<Int> = noteDao.observeActiveNotesCount()

    override fun observeTags(): Flow<List<String>> =
        noteTagDao.observeTags().map { tags -> tags.map { it.name } }

    override suspend fun upsertNote(note: Note) {
        database.withTransaction {
            noteDao.upsert(note.toEntity())
            reindex(note.id)
        }
    }

    override suspend fun setNoteTags(noteId: String, tags: List<String>) {
        val cleaned = tags
            .map { it.trim() }
            .filter { it.isNotEmpty() }
            .distinctBy { SearchText.normalizeTag(it) }

        database.withTransaction {
            noteTagDao.clearForNote(noteId)
            cleaned.forEach { name ->
                val normalized = SearchText.normalizeTag(name)
                if (normalized.isEmpty()) return@forEach
                val existing = noteTagDao.findByNormalizedName(normalized)
                val tagId = existing?.id ?: run {
                    val inserted = noteTagDao.insertTag(
                        NoteTagEntity(name = name, normalizedName = normalized),
                    )
                    if (inserted != -1L) {
                        inserted
                    } else {
                        noteTagDao.findByNormalizedName(normalized)?.id
                    }
                }
                if (tagId != null) {
                    noteTagDao.insertCrossRef(NoteTagCrossRef(noteId, tagId))
                }
            }
            reindex(noteId)
        }
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
        database.withTransaction {
            noteFtsDao.deleteByNoteId(id)
            noteDao.deleteById(id)
        }
    }

    override suspend fun emptyTrash() {
        database.withTransaction {
            noteFtsDao.deleteTrashed()
            noteDao.deleteAllTrashed()
        }
    }

    private fun notesWithTags(
        source: Flow<List<io.github.dijksys.quipu.data.local.entity.NoteEntity>>,
    ): Flow<List<Note>> = combine(
        source,
        noteTagDao.observeNoteTags(),
    ) { entities, links ->
        val tagsByNote = links.groupBy({ it.noteId }, { it.name })
        entities.map { entity -> entity.toDomain(tagsByNote[entity.id].orEmpty()) }
    }

    private suspend fun reindex(noteId: String) {
        noteFtsDao.deleteByNoteId(noteId)
        val entity = noteDao.getNote(noteId) ?: return
        val tags = noteTagDao.getTagNamesForNote(noteId)
        noteFtsDao.insert(
            NoteFtsEntity(
                noteId = entity.id,
                title = entity.title,
                content = entity.content,
                tags = tags.joinToString(" "),
            ),
        )
    }
}
