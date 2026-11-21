package io.github.dijksys.quipu.core.database

import android.content.Context
import androidx.room.Room
import androidx.sqlite.db.SupportSQLiteDatabase
import androidx.sqlite.db.SupportSQLiteOpenHelper
import androidx.sqlite.db.framework.FrameworkSQLiteOpenHelperFactory
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import io.github.dijksys.quipu.data.repository.NoteRepositoryImpl
import io.github.dijksys.quipu.domain.model.NoteFilter
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class Migration1To2Test {

    @Test
    fun migrationPreservesNotesAndBackfillsFts() = runTest {
        val context = ApplicationProvider.getApplicationContext<Context>()
        val databaseName = "migration-1-2-test.db"
        context.deleteDatabase(databaseName)

        createVersionOneDatabase(context, databaseName).apply {
            execSQL(
                """
                INSERT INTO notes
                    (id, title, content, createdAt, updatedAt, isPinned, archivedAt, deletedAt)
                VALUES (?, ?, ?, ?, ?, ?, ?, ?)
                """.trimIndent(),
                arrayOf<Any?>(
                    "n1",
                    "Organización semanal",
                    "Plan de recordatorios",
                    100L,
                    200L,
                    1,
                    null,
                    null,
                ),
            )
            execSQL(
                """
                INSERT INTO notes
                    (id, title, content, createdAt, updatedAt, isPinned, archivedAt, deletedAt)
                VALUES (?, ?, ?, ?, ?, ?, ?, ?)
                """.trimIndent(),
                arrayOf<Any?>("n2", "Archivada", "contenido", 300L, 400L, 0, 500L, null),
            )
            close()
        }

        val migrated = Room.databaseBuilder(context, AppDatabase::class.java, databaseName)
            .addMigrations(Migration1To2)
            .build()
        val repository = NoteRepositoryImpl(
            database = migrated,
            noteDao = migrated.noteDao(),
            noteFtsDao = migrated.noteFtsDao(),
            noteTagDao = migrated.noteTagDao(),
        )

        val note = migrated.noteDao().getNote("n1")
        assertNotNull(note)
        assertEquals("Organización semanal", note!!.title)
        assertEquals(1, migrated.noteDao().countActiveNotes())
        assertEquals(2, migrated.noteFtsDao().count())
        assertEquals(
            listOf("n1"),
            repository.searchNotes("organizacion", NoteFilter.ACTIVE).first().map { it.id },
        )
        assertEquals(
            listOf("n1"),
            repository.searchNotes("record", NoteFilter.ACTIVE).first().map { it.id },
        )
        assertEquals(0, migrated.noteTagDao().getAll().size)

        migrated.close()
        context.deleteDatabase(databaseName)
    }

    private fun createVersionOneDatabase(
        context: Context,
        name: String,
    ): SupportSQLiteDatabase {
        val configuration = SupportSQLiteOpenHelper.Configuration.builder(context)
            .name(name)
            .callback(object : SupportSQLiteOpenHelper.Callback(1) {
                override fun onCreate(db: SupportSQLiteDatabase) {
                    db.execSQL(
                        "CREATE TABLE IF NOT EXISTS `notes` (`id` TEXT NOT NULL, " +
                            "`title` TEXT NOT NULL, `content` TEXT NOT NULL, " +
                            "`createdAt` INTEGER NOT NULL, `updatedAt` INTEGER NOT NULL, " +
                            "`isPinned` INTEGER NOT NULL, `archivedAt` INTEGER, " +
                            "`deletedAt` INTEGER, PRIMARY KEY(`id`))",
                    )
                    db.execSQL(
                        "CREATE INDEX IF NOT EXISTS `index_notes_deletedAt_archivedAt_isPinned_updatedAt` " +
                            "ON `notes` (`deletedAt`, `archivedAt`, `isPinned`, `updatedAt`)",
                    )
                    db.execSQL(
                        "CREATE INDEX IF NOT EXISTS `index_notes_deletedAt_archivedAt_updatedAt` " +
                            "ON `notes` (`deletedAt`, `archivedAt`, `updatedAt`)",
                    )
                    db.execSQL(
                        "CREATE INDEX IF NOT EXISTS `index_notes_archivedAt` ON `notes` (`archivedAt`)",
                    )
                    db.execSQL(
                        "CREATE INDEX IF NOT EXISTS `index_notes_updatedAt` ON `notes` (`updatedAt`)",
                    )
                    db.execSQL(
                        "CREATE TABLE IF NOT EXISTS `reminders` (`id` TEXT NOT NULL, " +
                            "`title` TEXT NOT NULL, `description` TEXT, `noteId` TEXT, " +
                            "`scheduledAt` INTEGER NOT NULL, `createdAt` INTEGER NOT NULL, " +
                            "`updatedAt` INTEGER NOT NULL, `completedAt` INTEGER, " +
                            "`notifiedAt` INTEGER, `status` TEXT NOT NULL, PRIMARY KEY(`id`), " +
                            "FOREIGN KEY(`noteId`) REFERENCES `notes`(`id`) " +
                            "ON UPDATE NO ACTION ON DELETE SET NULL )",
                    )
                    db.execSQL(
                        "CREATE INDEX IF NOT EXISTS `index_reminders_noteId` ON `reminders` (`noteId`)",
                    )
                    db.execSQL(
                        "CREATE INDEX IF NOT EXISTS `index_reminders_status_scheduledAt` " +
                            "ON `reminders` (`status`, `scheduledAt`)",
                    )
                    db.execSQL(
                        "CREATE INDEX IF NOT EXISTS `index_reminders_scheduledAt` " +
                            "ON `reminders` (`scheduledAt`)",
                    )
                    db.execSQL(
                        "CREATE INDEX IF NOT EXISTS `index_reminders_completedAt` " +
                            "ON `reminders` (`completedAt`)",
                    )
                }

                override fun onUpgrade(db: SupportSQLiteDatabase, oldVersion: Int, newVersion: Int) = Unit
            })
            .build()

        return FrameworkSQLiteOpenHelperFactory().create(configuration).writableDatabase
    }
}
