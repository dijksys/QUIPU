package io.github.dijksys.quipu.data.local.dao

import androidx.room.Dao
import androidx.room.Query
import androidx.room.Upsert
import io.github.dijksys.quipu.data.local.entity.NoteEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface NoteDao {

    @Query(
        """
        SELECT * FROM notes
        WHERE deletedAt IS NULL AND archivedAt IS NULL
        ORDER BY isPinned DESC, updatedAt DESC
        """,
    )
    fun observeActiveNotes(): Flow<List<NoteEntity>>

    @Query(
        """
        SELECT * FROM notes
        WHERE deletedAt IS NULL AND archivedAt IS NOT NULL
        ORDER BY archivedAt DESC
        """,
    )
    fun observeArchivedNotes(): Flow<List<NoteEntity>>

    @Query(
        """
        SELECT * FROM notes
        WHERE deletedAt IS NOT NULL
        ORDER BY deletedAt DESC
        """,
    )
    fun observeTrashedNotes(): Flow<List<NoteEntity>>

    @Query(
        """
        SELECT * FROM notes
        WHERE deletedAt IS NULL AND archivedAt IS NULL
        ORDER BY updatedAt DESC
        LIMIT :limit
        """,
    )
    fun observeRecentNotes(limit: Int): Flow<List<NoteEntity>>

    @Query(
        """
        SELECT * FROM notes
        WHERE deletedAt IS NULL AND archivedAt IS NULL AND isPinned = 1
        ORDER BY updatedAt DESC
        LIMIT :limit
        """,
    )
    fun observePinnedNotes(limit: Int): Flow<List<NoteEntity>>

    @Query(
        """
        SELECT * FROM notes
        WHERE deletedAt IS NULL AND archivedAt IS NULL
          AND (title LIKE '%' || :query || '%' OR content LIKE '%' || :query || '%')
        ORDER BY isPinned DESC, updatedAt DESC
        """,
    )
    fun searchActiveNotes(query: String): Flow<List<NoteEntity>>

    @Query(
        """
        SELECT * FROM notes
        WHERE deletedAt IS NULL AND archivedAt IS NOT NULL
          AND (title LIKE '%' || :query || '%' OR content LIKE '%' || :query || '%')
        ORDER BY archivedAt DESC
        """,
    )
    fun searchArchivedNotes(query: String): Flow<List<NoteEntity>>

    @Query(
        """
        SELECT * FROM notes
        WHERE deletedAt IS NOT NULL
          AND (title LIKE '%' || :query || '%' OR content LIKE '%' || :query || '%')
        ORDER BY deletedAt DESC
        """,
    )
    fun searchTrashedNotes(query: String): Flow<List<NoteEntity>>

    @Query("SELECT * FROM notes WHERE id = :id LIMIT 1")
    fun observeNote(id: String): Flow<NoteEntity?>

    @Query("SELECT * FROM notes WHERE id = :id LIMIT 1")
    suspend fun getNote(id: String): NoteEntity?

    @Query("SELECT COUNT(*) FROM notes WHERE deletedAt IS NULL AND archivedAt IS NULL")
    suspend fun countActiveNotes(): Int

    @Query("SELECT COUNT(*) FROM notes WHERE deletedAt IS NULL AND archivedAt IS NULL")
    fun observeActiveNotesCount(): Flow<Int>

    @Upsert
    suspend fun upsert(note: NoteEntity)

    @Query("UPDATE notes SET isPinned = :pinned, updatedAt = :updatedAt WHERE id = :id")
    suspend fun setPinned(id: String, pinned: Boolean, updatedAt: Long)

    @Query("UPDATE notes SET archivedAt = :archivedAt, updatedAt = :updatedAt WHERE id = :id")
    suspend fun setArchivedAt(id: String, archivedAt: Long?, updatedAt: Long)

    @Query("UPDATE notes SET deletedAt = :deletedAt, updatedAt = :updatedAt WHERE id = :id")
    suspend fun setDeletedAt(id: String, deletedAt: Long?, updatedAt: Long)

    @Query("DELETE FROM notes WHERE id = :id")
    suspend fun deleteById(id: String)

    @Query("DELETE FROM notes WHERE deletedAt IS NOT NULL")
    suspend fun deleteAllTrashed()
}
