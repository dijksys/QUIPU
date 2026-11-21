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
import io.github.dijksys.quipu.domain.model.Note
import io.github.dijksys.quipu.domain.model.NoteDateRange
import io.github.dijksys.quipu.domain.model.NoteFilter
import io.github.dijksys.quipu.domain.model.NoteSortOrder
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.runTest
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class NoteSearchRepositoryTest {

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
    fun ftsFindsTitleContentAccentsPrefixesAndMultipleTerms() = runTest {
        repository.upsertNote(
            note("n1", "Organización semanal", "Plan de recordatorios y reuniones"),
        )
        repository.upsertNote(note("n2", "Ideas", "Comprar componentes"))

        assertEquals(
            listOf("n1"),
            repository.searchNotes("organizacion", NoteFilter.ACTIVE).first().map { it.id },
        )
        assertEquals(
            listOf("n1"),
            repository.searchNotes("ORGANIZACIÓN", NoteFilter.ACTIVE).first().map { it.id },
        )
        assertEquals(
            listOf("n1"),
            repository.searchNotes("record", NoteFilter.ACTIVE).first().map { it.id },
        )
        assertEquals(
            listOf("n1"),
            repository.searchNotes("plan reuniones", NoteFilter.ACTIVE).first().map { it.id },
        )
        assertTrue(
            repository.searchNotes("plan inexistente", NoteFilter.ACTIVE).first().isEmpty(),
        )
    }

    @Test
    fun tagsAreIndexedSearchableAndDeduplicated() = runTest {
        repository.upsertNote(note("n1", "Nota", "Contenido"))
        repository.setNoteTags("n1", listOf("Trabajo", "trabajo", "Proyectos"))

        assertEquals(2, database.noteTagDao().getAll().size)
        assertEquals(
            listOf("n1"),
            repository.searchNotes("trabajo", NoteFilter.ACTIVE).first().map { it.id },
        )
        assertEquals(
            listOf("n1"),
            repository.searchNotes("proyecto", NoteFilter.ACTIVE).first().map { it.id },
        )
        assertEquals(1, repository.searchNotes("trabajo", NoteFilter.ACTIVE).first().size)

        repository.setNoteTags("n1", emptyList())
        assertTrue(repository.searchNotes("trabajo", NoteFilter.ACTIVE).first().isEmpty())
    }

    @Test
    fun statusFiltersKeepTheIndexIntact() = runTest {
        repository.upsertNote(note("n1", "Recordatorio", "Contenido"))
        repository.setArchivedAt("n1", archivedAt = 1_000, updatedAt = 1_000)

        assertTrue(repository.searchNotes("recordatorio", NoteFilter.ACTIVE).first().isEmpty())
        assertEquals(
            listOf("n1"),
            repository.searchNotes("recordatorio", NoteFilter.ARCHIVED).first().map { it.id },
        )

        repository.setDeletedAt("n1", deletedAt = 2_000, updatedAt = 2_000)
        assertTrue(repository.searchNotes("recordatorio", NoteFilter.ARCHIVED).first().isEmpty())
        assertEquals(
            listOf("n1"),
            repository.searchNotes("recordatorio", NoteFilter.TRASH).first().map { it.id },
        )

        repository.setDeletedAt("n1", deletedAt = null, updatedAt = 3_000)
        repository.setArchivedAt("n1", archivedAt = null, updatedAt = 3_000)
        assertEquals(
            listOf("n1"),
            repository.searchNotes("recordatorio", NoteFilter.ACTIVE).first().map { it.id },
        )
    }

    @Test
    fun dateFiltersAndSortOrdersAreDeterministic() = runTest {
        repository.upsertNote(note("a", "Zeta compras", "nota", updatedAt = 1_000))
        repository.upsertNote(note("b", "Alfa compras", "nota", updatedAt = 3_000))
        repository.upsertNote(note("c", "Beta compras", "nota", updatedAt = 2_000))

        assertEquals(
            listOf("b", "c", "a"),
            repository.searchNotes("compras", NoteFilter.ACTIVE).first().map { it.id },
        )
        assertEquals(
            listOf("b", "c", "a"),
            repository.searchNotes(
                "compras",
                NoteFilter.ACTIVE,
                sort = NoteSortOrder.RECENT,
            ).first().map { it.id },
        )
        assertEquals(
            listOf("b", "c", "a"),
            repository.searchNotes(
                "compras",
                NoteFilter.ACTIVE,
                sort = NoteSortOrder.TITLE,
            ).first().map { it.id },
        )

        val range = NoteDateRange(startMillis = 1_500, endMillisExclusive = 2_500)
        assertEquals(
            listOf("c"),
            repository.searchNotes("compras", NoteFilter.ACTIVE, dateRange = range)
                .first().map { it.id },
        )
    }

    @Test
    fun exactTitleMatchComesFirst() = runTest {
        repository.upsertNote(note("n1", "Lista de compras", "nota", updatedAt = 2_000))
        repository.upsertNote(note("n2", "Compras", "nota", updatedAt = 1_000))

        assertEquals(
            listOf("n2", "n1"),
            repository.searchNotes("compras", NoteFilter.ACTIVE).first().map { it.id },
        )
    }

    @Test
    fun editingAndDeletingUpdateTheIndex() = runTest {
        repository.upsertNote(note("n1", "Nota", "manzana"))
        assertEquals(
            listOf("n1"),
            repository.searchNotes("manzana", NoteFilter.ACTIVE).first().map { it.id },
        )

        repository.upsertNote(note("n1", "Nota", "plátano"))
        assertTrue(repository.searchNotes("manzana", NoteFilter.ACTIVE).first().isEmpty())
        assertEquals(
            listOf("n1"),
            repository.searchNotes("platano", NoteFilter.ACTIVE).first().map { it.id },
        )

        repository.deleteNotePermanently("n1")
        assertTrue(repository.searchNotes("platano", NoteFilter.ACTIVE).first().isEmpty())
        assertEquals(0, database.noteFtsDao().count())
    }

    @Test
    fun emptyingTrashRemovesOnlyTrashedIndexEntries() = runTest {
        repository.upsertNote(note("n1", "Activa", "naranja"))
        repository.upsertNote(note("n2", "Eliminada", "naranja", deletedAt = 1_000))

        repository.emptyTrash()

        assertEquals(
            listOf("n1"),
            repository.searchNotes("naranja", NoteFilter.ACTIVE).first().map { it.id },
        )
        assertEquals(1, database.noteFtsDao().count())
    }

    @Test
    fun searchPerformanceWithThousandsOfNotes() = runTest {
        val count = 2_000
        database.withTransaction {
            repeat(count) { index ->
                val id = "perf-$index"
                database.noteDao().upsert(
                    NoteEntity(
                        id = id,
                        title = "Nota de rendimiento $index",
                        content = "contenido de prueba numero $index",
                        createdAt = 1_000,
                        updatedAt = 1_000L + index,
                        isPinned = false,
                        archivedAt = null,
                        deletedAt = null,
                    ),
                )
                database.noteFtsDao().insert(
                    NoteFtsEntity(
                        noteId = id,
                        title = "Nota de rendimiento $index",
                        content = "contenido de prueba numero $index",
                        tags = "",
                    ),
                )
            }
        }

        val started = System.nanoTime()
        val results = repository.searchNotes("1999", NoteFilter.ACTIVE).first()
        val elapsedMillis = (System.nanoTime() - started) / 1_000_000.0
        Log.i("QuipuPerf", "notes=$count searchMs=$elapsedMillis results=${results.size}")

        assertEquals(1, results.size)
        assertTrue("Búsqueda demasiado lenta: $elapsedMillis ms", elapsedMillis < 1_500.0)
    }

    private fun note(
        id: String,
        title: String,
        content: String,
        updatedAt: Long = 1_000,
        archivedAt: Long? = null,
        deletedAt: Long? = null,
    ) = Note(
        id = id,
        title = title,
        content = content,
        createdAt = 1_000,
        updatedAt = updatedAt,
        archivedAt = archivedAt,
        deletedAt = deletedAt,
    )
}
