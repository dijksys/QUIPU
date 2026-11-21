package io.github.dijksys.quipu.data.repository

import android.content.Context
import android.util.Log
import androidx.room.Room
import androidx.room.withTransaction
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import io.github.dijksys.quipu.core.database.AppDatabase
import io.github.dijksys.quipu.data.local.entity.NoteEntity
import io.github.dijksys.quipu.data.local.entity.NoteFtsEntity
import io.github.dijksys.quipu.data.local.entity.NoteTagCrossRef
import io.github.dijksys.quipu.data.local.entity.NoteTagEntity
import io.github.dijksys.quipu.data.local.entity.ReminderEntity
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.runTest
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class TrashEmptyingRepositoryTest {

    private lateinit var database: AppDatabase
    private lateinit var repository: NoteRepositoryImpl

    @Before
    fun setUp() {
        val context = ApplicationProvider.getApplicationContext<Context>()
        database = Room.inMemoryDatabaseBuilder(context, AppDatabase::class.java).build()
        repository = NoteRepositoryImpl(
            database = database,
            noteDao = database.noteDao(),
            noteFtsDao = database.noteFtsDao(),
            noteTagDao = database.noteTagDao(),
        )
    }

    @After
    fun tearDown() {
        database.close()
    }

    @Test
    fun emptyTrashRemovesOnlyTrashedNotesAndKeepsIndexConsistent() = runTest {
        seedScenario(active = 20, archived = 5, trashed = 10, withTagsAndReminders = true)

        repository.emptyTrash()

        assertEquals(20, database.noteDao().countActiveNotes())
        assertEquals(5, database.noteDao().observeArchivedNotes().first().size)
        assertTrue(database.noteDao().observeTrashedNotes().first().isEmpty())
        assertEquals(25, database.noteFtsDao().count())
        assertEquals(0, countAllTagLinks())
        assertEquals(10, countAllReminders())
        assertEquals(0, countLinkedReminders())
        assertDatabaseIntegrity()
    }

    @Test
    fun emptyTrashIsIdempotent() = runTest {
        seedScenario(active = 2, archived = 1, trashed = 3, withTagsAndReminders = false)

        repository.emptyTrash()
        repository.emptyTrash()
        repository.emptyTrash()

        assertEquals(2, database.noteDao().countActiveNotes())
        assertEquals(3, database.noteFtsDao().count())
        assertTrue(database.noteDao().observeTrashedNotes().first().isEmpty())
        assertDatabaseIntegrity()
    }

    @Test
    fun emptyTrashOfEmptyTrashIsSafe() = runTest {
        seedScenario(active = 3, archived = 0, trashed = 0, withTagsAndReminders = false)

        repository.emptyTrash()

        assertEquals(3, database.noteDao().countActiveNotes())
        assertEquals(3, database.noteFtsDao().count())
        assertDatabaseIntegrity()
    }

    @Test
    fun emptyTrashWithOneThousandTrashedNotesCompletesAndScales() = runTest {
        seedScenario(active = 3_000, archived = 0, trashed = 1_000, withTagsAndReminders = true)

        val started = System.currentTimeMillis()
        repository.emptyTrash()
        val elapsed = System.currentTimeMillis() - started
        Log.i(
            "QuipuTrashPerf",
            "trashed=1000 active=3000 elapsedMs=$elapsed tags=50 reminders=50",
        )

        assertTrue("Vaciado demasiado lento: ${elapsed}ms", elapsed < 10_000)
        assertEquals(3_000, database.noteDao().countActiveNotes())
        assertEquals(3_000, database.noteFtsDao().count())
        assertTrue(database.noteDao().observeTrashedNotes().first().isEmpty())
        assertDatabaseIntegrity()
    }

    private suspend fun seedScenario(
        active: Int,
        archived: Int,
        trashed: Int,
        withTagsAndReminders: Boolean,
    ) {
        database.withTransaction {
            val tagId = if (withTagsAndReminders) {
                database.noteTagDao().insertTag(
                    NoteTagEntity(name = "papelera", normalizedName = "papelera"),
                )
            } else {
                null
            }

            repeat(active) { index ->
                insertNote("active-$index", deletedAt = null, archivedAt = null)
            }
            repeat(archived) { index ->
                insertNote("archived-$index", deletedAt = null, archivedAt = 500L)
            }
            repeat(trashed) { index ->
                val id = "trashed-$index"
                insertNote(id, deletedAt = 1_000L, archivedAt = null)
                if (tagId != null && index < 50) {
                    database.noteTagDao().insertCrossRef(NoteTagCrossRef(id, tagId))
                    database.reminderDao().upsert(reminder("reminder-$id", id))
                }
            }
        }
    }

    private suspend fun insertNote(id: String, deletedAt: Long?, archivedAt: Long?) {
        database.noteDao().upsert(
            NoteEntity(
                id = id,
                title = "Nota $id",
                content = "contenido de prueba $id",
                createdAt = 1_000,
                updatedAt = 1_000,
                isPinned = false,
                archivedAt = archivedAt,
                deletedAt = deletedAt,
            ),
        )
        database.noteFtsDao().insert(
            NoteFtsEntity(
                noteId = id,
                title = "Nota $id",
                content = "contenido de prueba $id",
                tags = "",
            ),
        )
    }

    private fun reminder(id: String, noteId: String) = ReminderEntity(
        id = id,
        title = "Recordatorio",
        description = null,
        noteId = noteId,
        scheduledAt = 2_000,
        createdAt = 1_000,
        updatedAt = 1_000,
        completedAt = null,
        notifiedAt = null,
        status = "PENDING",
    )

    private fun countAllTagLinks(): Int = rawCount("SELECT COUNT(*) FROM note_tag_cross_ref")

    private fun countAllReminders(): Int = rawCount("SELECT COUNT(*) FROM reminders")

    private fun countLinkedReminders(): Int =
        rawCount("SELECT COUNT(*) FROM reminders WHERE noteId IS NOT NULL")

    private fun rawCount(sql: String): Int =
        database.openHelper.readableDatabase.query(sql).use { cursor ->
            cursor.moveToFirst()
            cursor.getInt(0)
        }

    private fun assertDatabaseIntegrity() {
        val db = database.openHelper.readableDatabase
        db.query("PRAGMA integrity_check").use { cursor ->
            cursor.moveToFirst()
            assertEquals("ok", cursor.getString(0))
        }
        db.query("PRAGMA foreign_key_check").use { cursor ->
            assertEquals(0, cursor.count)
        }
    }
}
