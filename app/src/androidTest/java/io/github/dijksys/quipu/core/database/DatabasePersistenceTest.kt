package io.github.dijksys.quipu.core.database

import android.content.Context
import androidx.room.Room
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import io.github.dijksys.quipu.data.local.entity.NoteEntity
import io.github.dijksys.quipu.data.local.entity.ReminderEntity
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class DatabasePersistenceTest {

    @Test
    fun notesAndRemindersSurviveDatabaseReopen() = runTest {
        val context = ApplicationProvider.getApplicationContext<Context>()
        val databaseName = "quipu-persistence-${System.currentTimeMillis()}.db"
        context.deleteDatabase(databaseName)

        val firstInstance = Room.databaseBuilder(
            context,
            AppDatabase::class.java,
            databaseName,
        ).build()
        firstInstance.noteDao().upsert(
            NoteEntity(
                id = "note-1",
                title = "Persistente",
                content = "Contenido",
                createdAt = 100,
                updatedAt = 100,
                isPinned = false,
                archivedAt = null,
                deletedAt = null,
            ),
        )
        firstInstance.reminderDao().upsert(
            ReminderEntity(
                id = "reminder-1",
                title = "Recordatorio persistente",
                description = null,
                noteId = "note-1",
                scheduledAt = 1_000,
                createdAt = 100,
                updatedAt = 100,
                completedAt = null,
                notifiedAt = null,
                status = "PENDING",
            ),
        )
        firstInstance.close()

        val secondInstance = Room.databaseBuilder(
            context,
            AppDatabase::class.java,
            databaseName,
        ).build()
        val note = secondInstance.noteDao().getNote("note-1")
        val reminder = secondInstance.reminderDao().getById("reminder-1")

        assertNotNull(note)
        assertEquals("Persistente", note!!.title)
        assertNotNull(reminder)
        assertEquals("note-1", reminder!!.noteId)
        secondInstance.close()
        context.deleteDatabase(databaseName)
    }
}
