package com.example.quipu.feature.notes

import androidx.lifecycle.SavedStateHandle
import com.example.quipu.domain.model.Note
import com.example.quipu.domain.usecase.notes.ArchiveNoteUseCase
import com.example.quipu.domain.usecase.notes.DeleteNoteUseCase
import com.example.quipu.domain.usecase.notes.EmptyTrashUseCase
import com.example.quipu.domain.usecase.notes.PermanentlyDeleteNoteUseCase
import com.example.quipu.domain.usecase.notes.RestoreNoteUseCase
import com.example.quipu.domain.usecase.notes.SearchNotesUseCase
import com.example.quipu.domain.usecase.notes.TogglePinNoteUseCase
import com.example.quipu.fakes.FakeNoteRepository
import com.example.quipu.fakes.TestClock
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.async
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.advanceTimeBy
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runCurrent
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test

@OptIn(ExperimentalCoroutinesApi::class)
class NotesViewModelTest {

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
    fun `active notes are exposed and update reactively`() = runTest(dispatcher) {
        repository.preload(note("n1", "Primera"))
        val viewModel = createViewModel()

        backgroundScope.launch { viewModel.uiState.collect { } }
        advanceUntilIdle()

        assertEquals(listOf("n1"), viewModel.uiState.value.notes.map { it.id })

        repository.preload(note("n2", "Segunda"))
        advanceUntilIdle()

        assertTrue(viewModel.uiState.value.notes.any { it.id == "n2" })
        assertFalse(viewModel.uiState.value.isLoading)
    }

    @Test
    fun `search filters notes after debounce`() = runTest(dispatcher) {
        repository.preload(note("n1", "Lista de compras"), note("n2", "Ideas"))
        val viewModel = createViewModel()
        backgroundScope.launch { viewModel.uiState.collect { } }
        advanceUntilIdle()

        viewModel.onQueryChange("compras")
        advanceTimeBy(300)
        runCurrent()

        assertEquals(listOf("n1"), viewModel.uiState.value.notes.map { it.id })
        assertEquals("compras", viewModel.uiState.value.query)
    }

    @Test
    fun `toggle pin updates repository and emits event`() = runTest(dispatcher) {
        val target = note("n1", "Nota")
        repository.preload(target)
        val viewModel = createViewModel()
        val event = async { viewModel.events.first() }

        viewModel.togglePin(target)
        advanceUntilIdle()

        assertTrue(repository.getStored("n1")!!.isPinned)
        assertTrue(event.await() is NotesEvent.PinChanged)
    }

    @Test
    fun `moving to trash emits note id for undo`() = runTest(dispatcher) {
        val target = note("n1", "Nota")
        repository.preload(target)
        val viewModel = createViewModel()
        val event = async { viewModel.events.first() }

        viewModel.moveToTrash(target)
        advanceUntilIdle()

        val trashed = repository.getStored("n1")!!
        assertTrue(trashed.isDeleted)
        assertEquals("n1", (event.await() as NotesEvent.Trashed).noteId)
    }

    @Test
    fun `archive and restore work from the view model`() = runTest(dispatcher) {
        val target = note("n1", "Nota")
        repository.preload(target)
        val viewModel = createViewModel()

        viewModel.setArchived(target, true)
        advanceUntilIdle()
        assertTrue(repository.getStored("n1")!!.isArchived)

        viewModel.restore("n1")
        advanceUntilIdle()
        assertFalse(repository.getStored("n1")!!.isDeleted)
        assertFalse(repository.getStored("n1")!!.isArchived)
    }

    @Test
    fun `trash filter exposes deleted notes`() = runTest(dispatcher) {
        repository.preload(note("n1", "Eliminada", deletedAt = clock.currentTime))
        val viewModel = createViewModel(filter = "TRASH")
        backgroundScope.launch { viewModel.uiState.collect { } }
        advanceUntilIdle()

        assertEquals(listOf("n1"), viewModel.uiState.value.notes.map { it.id })
        assertTrue(viewModel.uiState.value.isTrash)
    }

    @Test
    fun `failure while pinning is reported as event`() = runTest(dispatcher) {
        val target = note("n1", "Nota")
        repository.preload(target)
        repository.failWrites = true
        val viewModel = createViewModel()
        val event = async { viewModel.events.first() }

        viewModel.togglePin(target)
        advanceUntilIdle()

        assertEquals(NotesEvent.Failure, event.await())
    }

    private fun createViewModel(filter: String = "ACTIVE"): NotesViewModel {
        val savedState = SavedStateHandle(mapOf(NotesViewModel.ARG_FILTER to filter))
        return NotesViewModel(
            savedStateHandle = savedState,
            searchNotes = SearchNotesUseCase(repository),
            togglePin = TogglePinNoteUseCase(repository, clock),
            archiveNote = ArchiveNoteUseCase(repository, clock),
            deleteNote = DeleteNoteUseCase(repository, clock),
            restoreNote = RestoreNoteUseCase(repository, clock),
            permanentlyDeleteNote = PermanentlyDeleteNoteUseCase(repository),
            emptyTrash = EmptyTrashUseCase(repository),
        )
    }

    private fun note(
        id: String,
        title: String,
        deletedAt: Long? = null,
    ) = Note(
        id = id,
        title = title,
        content = "",
        createdAt = clock.currentTime,
        updatedAt = clock.currentTime,
        deletedAt = deletedAt,
    )
}
