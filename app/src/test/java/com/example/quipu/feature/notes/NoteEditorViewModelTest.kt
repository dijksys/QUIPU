package com.example.quipu.feature.notes

import androidx.lifecycle.SavedStateHandle
import com.example.quipu.domain.model.Note
import com.example.quipu.domain.usecase.notes.ArchiveNoteUseCase
import com.example.quipu.domain.usecase.notes.CreateNoteUseCase
import com.example.quipu.domain.usecase.notes.DeleteNoteUseCase
import com.example.quipu.domain.usecase.notes.GetNoteUseCase
import com.example.quipu.domain.usecase.notes.TogglePinNoteUseCase
import com.example.quipu.domain.usecase.notes.UpdateNoteUseCase
import com.example.quipu.fakes.FakeNoteRepository
import com.example.quipu.fakes.TestClock
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.async
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runCurrent
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test

@OptIn(ExperimentalCoroutinesApi::class)
class NoteEditorViewModelTest {

    private val dispatcher = StandardTestDispatcher()
    private val clock = TestClock()
    private lateinit var repository: FakeNoteRepository

    @Before
    fun setUp() {
        Dispatchers.setMain(dispatcher)
        repository = FakeNoteRepository()
    }

    @After
    fun tearDown() {
        Dispatchers.resetMain()
    }

    @Test
    fun `new note is saved automatically after debounce`() = runTest(dispatcher) {
        val viewModel = createViewModel()
        runCurrent()

        viewModel.onTitleChange("Mi título")
        viewModel.onContentChange("Contenido de la nota")
        advanceUntilIdle()

        val stored = repository.currentNotes.single()
        assertEquals("Mi título", stored.title)
        assertEquals("Contenido de la nota", stored.content)
        assertFalse(viewModel.uiState.value.isSaving)
        assertNotNull(viewModel.uiState.value.lastSavedAt)
    }

    @Test
    fun `blank new note is not persisted`() = runTest(dispatcher) {
        val viewModel = createViewModel()
        runCurrent()

        viewModel.onTitleChange("   ")
        viewModel.onContentChange("  ")
        advanceUntilIdle()

        assertTrue(repository.currentNotes.isEmpty())
    }

    @Test
    fun `saveNow flushes pending changes immediately`() = runTest(dispatcher) {
        val viewModel = createViewModel()
        runCurrent()
        viewModel.onTitleChange("Sin esperar")
        viewModel.onContentChange("Texto")

        val id = viewModel.saveNow()

        assertNotNull(id)
        assertEquals("Sin esperar", repository.getStored(id!!)!!.title)
    }

    @Test
    fun `existing note is loaded and updated`() = runTest(dispatcher) {
        val existing = note(id = "n1", title = "Antes", content = "Texto")
        repository.preload(existing)
        val viewModel = createViewModel(noteId = "n1")
        runCurrent()

        assertEquals("Antes", viewModel.uiState.value.title)
        assertFalse(viewModel.uiState.value.isNew)

        clock.currentTime += 2_000
        viewModel.onTitleChange("Después")
        advanceUntilIdle()

        assertEquals("Después", repository.getStored("n1")!!.title)
        assertTrue(viewModel.uiState.value.createdAt == existing.createdAt)
    }

    @Test
    fun `pin action persists note and emits event`() = runTest(dispatcher) {
        repository.preload(note(id = "n1", title = "Nota", content = "Texto"))
        val viewModel = createViewModel(noteId = "n1")
        runCurrent()
        val event = async { viewModel.events.first() }

        viewModel.togglePin()
        advanceUntilIdle()

        assertTrue(repository.getStored("n1")!!.isPinned)
        assertEquals(true, (event.await() as NoteEditorEvent.PinChanged).pinned)
    }

    @Test
    fun `archive action saves content and emits state`() = runTest(dispatcher) {
        repository.preload(note(id = "n1", title = "Nota", content = "Texto"))
        val viewModel = createViewModel(noteId = "n1")
        runCurrent()
        val event = async { viewModel.events.first() }

        viewModel.onContentChange("Contenido editado")
        viewModel.setArchived(true)
        advanceUntilIdle()

        assertTrue(repository.getStored("n1")!!.isArchived)
        assertEquals("Contenido editado", repository.getStored("n1")!!.content)
        assertEquals(true, (event.await() as NoteEditorEvent.ArchiveChanged).archived)
    }

    @Test
    fun `missing note exposes missing state`() = runTest(dispatcher) {
        val viewModel = createViewModel(noteId = "no-existe")
        runCurrent()

        assertTrue(viewModel.uiState.value.isMissing)
    }

    private fun createViewModel(noteId: String = NoteEditorViewModel.NEW_NOTE_ID): NoteEditorViewModel {
        val savedState = SavedStateHandle(
            mapOf(NoteEditorViewModel.ARG_NOTE_ID to noteId),
        )
        return NoteEditorViewModel(
            savedStateHandle = savedState,
            getNote = GetNoteUseCase(repository),
            createNote = CreateNoteUseCase(repository, clock),
            updateNote = UpdateNoteUseCase(repository, clock),
            togglePin = TogglePinNoteUseCase(repository, clock),
            archiveNote = ArchiveNoteUseCase(repository, clock),
            deleteNote = DeleteNoteUseCase(repository, clock),
        )
    }

    private fun note(
        id: String,
        title: String,
        content: String,
    ) = Note(
        id = id,
        title = title,
        content = content,
        createdAt = clock.currentTime,
        updatedAt = clock.currentTime,
    )
}
