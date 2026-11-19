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
class ReminderDaoTest {

    private lateinit var database: AppDatabase
    private lateinit var reminderDao: ReminderDao
    private lateinit var noteDao: NoteDao

    @Before
    fun setUp() {
        val context = ApplicationProvider.getApplicationContext<Context>()
        database = Room.inMemoryDatabaseBuilder(context, AppDatabase::class.java).build()
        reminderDao = database.reminderDao()
        noteDao = database.noteDao()
    }

    @After
    fun tearDown() {
        database.close()
    }

    @Test
    fun remindersAreSortedByScheduledTime() = runTest {
        reminderDao.upsert(reminder("r2", scheduledAt = 2_000))
        reminderDao.upsert(reminder("r1", scheduledAt = 1_000))

        val reminders = reminderDao.observeAll().first()

        assertEquals(listOf("r1", "r2"), reminders.map { it.id })
    }

    @Test
    fun pendingQueryExcludesCompletedAndCancelled() = runTest {
        reminderDao.upsert(reminder("pending", scheduledAt = 1_000))
        reminderDao.upsert(reminder("done", scheduledAt = 1_000).copy(status = "COMPLETED"))
        reminderDao.upsert(reminder("cancelled", scheduledAt = 1_000).copy(status = "CANCELLED"))

        val pending = reminderDao.getPending()

        assertEquals(listOf("pending"), pending.map { it.id })
    }

    @Test
    fun statusUpdatesPersistTimestamps() = runTest {
        reminderDao.upsert(reminder("r1", scheduledAt = 1_000))

        reminderDao.markNotified("r1", 1_500)
        var stored = reminderDao.getById("r1")!!
        assertEquals(1_500L, stored.notifiedAt)

        reminderDao.markCompleted("r1", 2_000)
        stored = reminderDao.getById("r1")!!
        assertEquals("COMPLETED", stored.status)
        assertEquals(2_000L, stored.completedAt)

        reminderDao.markCancelled("r1", 3_000)
        stored = reminderDao.getById("r1")!!
        assertEquals("CANCELLED", stored.status)
        assertNull(stored.completedAt)
        assertEquals(3_000L, stored.updatedAt)
    }

    @Test
    fun observeRemindersForNote() = runTest {
        noteDao.upsert(noteEntity("n1"))
        noteDao.upsert(noteEntity("n2"))
        reminderDao.upsert(reminder("r1", scheduledAt = 1_000, noteId = "n1"))
        reminderDao.upsert(reminder("r2", scheduledAt = 2_000, noteId = "n2"))

        val linked = reminderDao.observeForNote("n1").first()

        assertEquals(listOf("r1"), linked.map { it.id })
    }

    @Test
    fun deleteReminder() = runTest {
        reminderDao.upsert(reminder("r1", scheduledAt = 1_000))

        reminderDao.deleteById("r1")

        assertNull(reminderDao.getById("r1"))
        assertTrue(reminderDao.observeAll().first().isEmpty())
    }

    private fun reminder(
        id: String,
        scheduledAt: Long,
        noteId: String? = null,
    ) = ReminderEntity(
        id = id,
        title = "Recordatorio $id",
        description = null,
        noteId = noteId,
        scheduledAt = scheduledAt,
        createdAt = 100,
        updatedAt = 100,
        completedAt = null,
        notifiedAt = null,
        status = "PENDING",
    )

    private fun noteEntity(id: String) = NoteEntity(
        id = id,
        title = "Nota $id",
        content = "",
        createdAt = 100,
        updatedAt = 100,
        isPinned = false,
        archivedAt = null,
        deletedAt = null,
    )
}
