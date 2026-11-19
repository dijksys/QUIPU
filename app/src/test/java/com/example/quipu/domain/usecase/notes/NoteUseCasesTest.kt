package com.example.quipu.domain.usecase.notes

import com.example.quipu.domain.model.NoteFilter
import com.example.quipu.fakes.FakeNoteRepository
import com.example.quipu.fakes.TestClock
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class NoteUseCasesTest {

    private val clock = TestClock()
    private val repository = FakeNoteRepository()

    private val createNote = CreateNoteUseCase(repository, clock)
    private val updateNote = UpdateNoteUseCase(repository, clock)
    private val observeNotes = ObserveNotesUseCase(repository)
    private val searchNotes = SearchNotesUseCase(repository)
    private val togglePin = TogglePinNoteUseCase(repository, clock)
    private val archiveNote = ArchiveNoteUseCase(repository, clock)
    private val deleteNote = DeleteNoteUseCase(repository, clock)
    private val restoreNote = RestoreNoteUseCase(repository, clock)
    private val permanentlyDelete = PermanentlyDeleteNoteUseCase(repository)
    private val emptyTrash = EmptyTrashUseCase(repository)

    @Test
    fun `create note trims title and persists timestamps`() = runTest {
        val note = createNote(title = "  Mi nota  ", content = "Contenido", id = "n1")

        assertEquals("Mi nota", note.title)
        assertEquals("Contenido", note.content)
        assertEquals(clock.currentTime, note.createdAt)
        assertEquals(clock.currentTime, note.updatedAt)
        assertEquals(note, repository.getStored("n1"))
    }

    @Test
    fun `update note changes content and updatedAt`() = runTest {
        val created = createNote("Original", "Texto", "n1")
        clock.currentTime += 5_000

        val updated = updateNote(created.copy(title = " Cambiado ", content = "Nuevo texto"))

        assertEquals("Cambiado", updated.title)
        assertEquals("Nuevo texto", updated.content)
        assertEquals(clock.currentTime, updated.updatedAt)
        assertEquals(created.createdAt, updated.createdAt)
        assertEquals(updated, repository.getStored("n1"))
    }

    @Test
    fun `pin archive trash restore and delete permanently`() = runTest {
        createNote("Nota", "Contenido", "n1")

        togglePin("n1", true)
        assertTrue(repository.getStored("n1")!!.isPinned)

        archiveNote("n1", archive = true)
        assertNotNull(repository.getStored("n1")!!.archivedAt)

        archiveNote("n1", archive = false)
        assertNull(repository.getStored("n1")!!.archivedAt)

        deleteNote("n1")
        assertNotNull(repository.getStored("n1")!!.deletedAt)

        restoreNote("n1")
        val restored = repository.getStored("n1")!!
        assertNull(restored.deletedAt)
        assertNull(restored.archivedAt)

        permanentlyDelete("n1")
        assertNull(repository.getStored("n1"))
    }

    @Test
    fun `empty trash preserves active notes`() = runTest {
        createNote("Activa", "", "n1")
        createNote("Eliminada 1", "", "n2")
        createNote("Eliminada 2", "", "n3")
        deleteNote("n2")
        deleteNote("n3")

        emptyTrash()

        assertEquals(listOf("n1"), repository.currentNotes.map { it.id })
    }

    @Test
    fun `observe notes filters by state`() = runTest {
        createNote("Activa", "", "n1")
        createNote("Archivada", "", "n2")
        createNote("Eliminada", "", "n3")
        archiveNote("n2", archive = true)
        deleteNote("n3")

        assertEquals(listOf("n1"), observeNotes(NoteFilter.ACTIVE).first().map { it.id })
        assertEquals(listOf("n2"), observeNotes(NoteFilter.ARCHIVED).first().map { it.id })
        assertEquals(listOf("n3"), observeNotes(NoteFilter.TRASH).first().map { it.id })
    }

    @Test
    fun `pinned notes appear first`() = runTest {
        createNote("Primera", "", "n1")
        clock.currentTime += 1_000
        createNote("Segunda", "", "n2")
        clock.currentTime += 1_000
        createNote("Tercera", "", "n3")
        togglePin("n1", true)

        val result = observeNotes(NoteFilter.ACTIVE).first()
        assertEquals(listOf("n1", "n3", "n2"), result.map { it.id })
    }

    @Test
    fun `search matches title and content`() = runTest {
        createNote("Lista de compras", "Pan y leche", "n1")
        createNote("Ideas", "Escribir un artículo", "n2")

        val byTitle = searchNotes("compras", NoteFilter.ACTIVE).first()
        val byContent = searchNotes("artículo", NoteFilter.ACTIVE).first()
        val noResults = searchNotes("inexistente", NoteFilter.ACTIVE).first()

        assertEquals(listOf("n1"), byTitle.map { it.id })
        assertEquals(listOf("n2"), byContent.map { it.id })
        assertTrue(noResults.isEmpty())
    }
}
