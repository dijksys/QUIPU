package io.github.dijksys.quipu.data.local.dao

import android.content.Context
import androidx.room.Room
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import io.github.dijksys.quipu.core.database.AppDatabase
import io.github.dijksys.quipu.data.local.entity.NoteEntity
import io.github.dijksys.quipu.data.local.entity.ReminderEntity
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.runTest
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class NoteDaoTest {

    private lateinit var database: AppDatabase
    private lateinit var noteDao: NoteDao
    private lateinit var reminderDao: ReminderDao

    @Before
    fun setUp() {
        val context = ApplicationProvider.getApplicationContext<Context>()
        database = Room.inMemoryDatabaseBuilder(context, AppDatabase::class.java).build()
        noteDao = database.noteDao()
        reminderDao = database.reminderDao()
    }

    @After
    fun tearDown() {
        database.close()
    }

    @Test
    fun insertAndObserveActiveNotes() = runTest {
        noteDao.upsert(note("n1", "Primera", updatedAt = 100))

        val notes = noteDao.observeActiveNotes().first()

        assertEquals(1, notes.size)
        assertEquals("Primera", notes.single().title)
        assertEquals(1, noteDao.observeActiveNotesCount().first())
    }

    @Test
    fun pinnedNotesAreOrderedFirst() = runTest {
        noteDao.upsert(note("n1", "Normal", updatedAt = 200, pinned = false))
        noteDao.upsert(note("n2", "Fijada", updatedAt = 100, pinned = true))

        val notes = noteDao.observeActiveNotes().first()

        assertEquals(listOf("n2", "n1"), notes.map { it.id })
        assertEquals(listOf("n2"), noteDao.observePinnedNotes(4).first().map { it.id })
        assertEquals(listOf("n1", "n2"), noteDao.observeRecentNotes(4).first().map { it.id })
    }

    @Test
    fun archiveAndTrashFiltersAreExclusive() = runTest {
        noteDao.upsert(note("active", "Activa", deletedAt = null, archivedAt = null))
        noteDao.upsert(note("archived", "Archivada", archivedAt = 500))
        noteDao.upsert(note("trashed", "Eliminada", deletedAt = 900))

        assertEquals(listOf("active"), noteDao.observeActiveNotes().first().map { it.id })
        assertEquals(listOf("archived"), noteDao.observeArchivedNotes().first().map { it.id })
        assertEquals(listOf("trashed"), noteDao.observeTrashedNotes().first().map { it.id })
    }

    @Test
    fun searchFindsTitleAndContent() = runTest {
        noteDao.upsert(note("n1", "Lista de compras", content = "Pan y leche"))
        noteDao.upsert(note("n2", "Ideas", content = "Escribir un artículo"))

        assertEquals(
            listOf("n1"),
            noteDao.searchActiveNotes("compras").first().map { it.id },
        )
        assertEquals(
            listOf("n2"),
            noteDao.searchActiveNotes("artículo").first().map { it.id },
        )
        assertTrue(noteDao.searchActiveNotes("inexistente").first().isEmpty())
    }

    @Test
    fun partialUpdatesAndPermanentDelete() = runTest {
        noteDao.upsert(note("n1", "Nota", updatedAt = 100))

        noteDao.setPinned("n1", true, updatedAt = 200)
        noteDao.setArchivedAt("n1", 300, updatedAt = 300)
        noteDao.setDeletedAt("n1", 400, updatedAt = 400)

        val trashed = noteDao.getNote("n1")!!
        assertTrue(trashed.isPinned)
        assertEquals(300L, trashed.archivedAt)
        assertEquals(400L, trashed.deletedAt)

        noteDao.deleteById("n1")
        assertNull(noteDao.getNote("n1"))
    }

    @Test
    fun deletingNotePermanentlyDetachesReminder() = runTest {
        noteDao.upsert(note("n1", "Nota vinculada"))
        reminderDao.upsert(reminder("r1", noteId = "n1"))

        noteDao.deleteById("n1")

        val reminder = reminderDao.getById("r1")
        assertEquals("r1", reminder?.id)
        assertNull(reminder?.noteId)
    }

    @Test
    fun emptyTrashRemovesOnlyTrashedNotes() = runTest {
        noteDao.upsert(note("n1", "Activa"))
        noteDao.upsert(note("n2", "Eliminada", deletedAt = 100))
        reminderDao.upsert(reminder("r1", noteId = "n2"))

        noteDao.deleteAllTrashed()

        assertEquals(listOf("n1"), noteDao.observeActiveNotes().first().map { it.id })
        assertNull(noteDao.getNote("n2"))
        assertEquals("r1", reminderDao.getById("r1")?.id)
        assertNull(reminderDao.getById("r1")?.noteId)
    }

    private fun note(
        id: String,
        title: String,
        content: String = "",
        updatedAt: Long = 100,
        pinned: Boolean = false,
        archivedAt: Long? = null,
        deletedAt: Long? = null,
    ) = NoteEntity(
        id = id,
        title = title,
        content = content,
        createdAt = 100,
        updatedAt = updatedAt,
        isPinned = pinned,
        archivedAt = archivedAt,
        deletedAt = deletedAt,
    )

    private fun reminder(id: String, noteId: String?) = ReminderEntity(
        id = id,
        title = "Recordatorio",
        description = null,
        noteId = noteId,
        scheduledAt = 1_000,
        createdAt = 100,
        updatedAt = 100,
        completedAt = null,
        notifiedAt = null,
        status = "PENDING",
    )
}
