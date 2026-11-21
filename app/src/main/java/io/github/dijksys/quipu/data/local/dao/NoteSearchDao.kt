package io.github.dijksys.quipu.data.local.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import io.github.dijksys.quipu.data.local.entity.NoteFtsEntity
import io.github.dijksys.quipu.data.local.entity.NoteTagCrossRef
import io.github.dijksys.quipu.data.local.entity.NoteTagEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface NoteFtsDao {

    @Query("DELETE FROM notes_fts WHERE noteId = :noteId")
    suspend fun deleteByNoteId(noteId: String)

    @Insert
    suspend fun insert(entity: NoteFtsEntity)

    @Query("DELETE FROM notes_fts WHERE noteId IN (SELECT id FROM notes WHERE deletedAt IS NOT NULL)")
    suspend fun deleteTrashed()

    @Query("SELECT COUNT(*) FROM notes_fts")
    suspend fun count(): Int
}

@Dao
interface NoteTagDao {

    @Query("SELECT * FROM note_tags ORDER BY normalizedName")
    fun observeTags(): Flow<List<NoteTagEntity>>

    @Query("SELECT * FROM note_tags ORDER BY normalizedName")
    suspend fun getAll(): List<NoteTagEntity>

    @Query("SELECT * FROM note_tags WHERE normalizedName = :normalizedName LIMIT 1")
    suspend fun findByNormalizedName(normalizedName: String): NoteTagEntity?

    @Insert(onConflict = OnConflictStrategy.IGNORE)
    suspend fun insertTag(tag: NoteTagEntity): Long

    @Query(
        """
        SELECT c.noteId AS noteId, t.name AS name
        FROM note_tag_cross_ref c
        INNER JOIN note_tags t ON t.id = c.tagId
        ORDER BY t.normalizedName
        """,
    )
    fun observeNoteTags(): Flow<List<NoteTagName>>

    @Query(
        """
        SELECT t.name
        FROM note_tags t
        INNER JOIN note_tag_cross_ref c ON c.tagId = t.id
        WHERE c.noteId = :noteId
        ORDER BY t.normalizedName
        """,
    )
    suspend fun getTagNamesForNote(noteId: String): List<String>

    @Query(
        """
        SELECT t.name
        FROM note_tags t
        INNER JOIN note_tag_cross_ref c ON c.tagId = t.id
        WHERE c.noteId = :noteId
        ORDER BY t.normalizedName
        """,
    )
    fun observeTagNamesForNote(noteId: String): Flow<List<String>>

    @Query("DELETE FROM note_tag_cross_ref WHERE noteId = :noteId")
    suspend fun clearForNote(noteId: String)

    @Insert(onConflict = OnConflictStrategy.IGNORE)
    suspend fun insertCrossRef(ref: NoteTagCrossRef)
}

data class NoteTagName(
    val noteId: String,
    val name: String,
)
