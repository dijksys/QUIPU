package io.github.dijksys.quipu.feature.notes

import androidx.lifecycle.SavedStateHandle
import io.github.dijksys.quipu.domain.model.Note
import io.github.dijksys.quipu.domain.usecase.notes.ArchiveNoteUseCase
import io.github.dijksys.quipu.domain.usecase.notes.DeleteNoteUseCase
import io.github.dijksys.quipu.domain.usecase.notes.EmptyTrashUseCase
import io.github.dijksys.quipu.domain.usecase.notes.PermanentlyDeleteNoteUseCase
import io.github.dijksys.quipu.domain.usecase.notes.RestoreNoteUseCase
import io.github.dijksys.quipu.domain.usecase.notes.SearchNotesUseCase
import io.github.dijksys.quipu.domain.usecase.notes.TogglePinNoteUseCase
import io.github.dijksys.quipu.fakes.FakeNoteRepository
import io.github.dijksys.quipu.fakes.TestClock
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

    @Test
    fun `initial query is restored from saved state`() = runTest(dispatcher) {
        repository.preload(note("n1", "Lista de compras"), note("n2", "Ideas"))
        val viewModel = createViewModel(query = "compras")
        backgroundScope.launch { viewModel.uiState.collect { } }
        advanceUntilIdle()

        assertEquals("compras", viewModel.uiState.value.query)
        assertEquals(listOf("n1"), viewModel.uiState.value.notes.map { it.id })
    }

    @Test
    fun `date filter and sort order refine results`() = runTest(dispatcher) {
        repository.preload(
            note("n1", "Zeta compras", updatedAt = 1_000),
            note("n2", "Alfa compras", updatedAt = 3_000),
        )
        val viewModel = createViewModel()
        backgroundScope.launch { viewModel.uiState.collect { } }
        advanceUntilIdle()

        viewModel.onQueryChange("compras")
        advanceTimeBy(300)
        runCurrent()
        viewModel.onSortOrderChange(io.github.dijksys.quipu.domain.model.NoteSortOrder.TITLE)
        advanceUntilIdle()

        assertEquals(listOf("n2", "n1"), viewModel.uiState.value.notes.map { it.id })

        viewModel.onCustomRangeChange(
            io.github.dijksys.quipu.domain.model.NoteDateRange(2_000, 4_000),
        )
        advanceUntilIdle()

        assertEquals(listOf("n2"), viewModel.uiState.value.notes.map { it.id })
    }

    @Test
    fun `clear search resets query and filters`() = runTest(dispatcher) {
        repository.preload(note("n1", "Nota"))
        val viewModel = createViewModel(query = "nota")
        backgroundScope.launch { viewModel.uiState.collect { } }
        advanceUntilIdle()

        viewModel.onSortOrderChange(io.github.dijksys.quipu.domain.model.NoteSortOrder.RECENT)
        viewModel.clearSearch()
        advanceUntilIdle()

        assertEquals("", viewModel.uiState.value.query)
        assertEquals(
            io.github.dijksys.quipu.domain.model.NoteDateFilter.ALL,
            viewModel.uiState.value.dateFilter,
        )
        assertEquals(
            io.github.dijksys.quipu.domain.model.NoteSortOrder.RELEVANCE,
            viewModel.uiState.value.sortOrder,
        )
        assertFalse(viewModel.uiState.value.isSearching)
    }

    private fun createViewModel(
        filter: String = "ACTIVE",
        query: String? = null,
    ): NotesViewModel {
        val savedState = SavedStateHandle(
            buildMap {
                put(NotesViewModel.ARG_FILTER, filter)
                query?.let { put(NotesViewModel.ARG_QUERY, it) }
            },
        )
        return NotesViewModel(
            savedStateHandle = savedState,
            searchNotes = SearchNotesUseCase(repository),
            togglePin = TogglePinNoteUseCase(repository, clock),
            archiveNote = ArchiveNoteUseCase(repository, clock),
            deleteNote = DeleteNoteUseCase(repository, clock),
            restoreNote = RestoreNoteUseCase(repository, clock),
            permanentlyDeleteNote = PermanentlyDeleteNoteUseCase(repository),
            emptyTrash = EmptyTrashUseCase(repository),
            clock = clock,
        )
    }

    private fun note(
        id: String,
        title: String,
        deletedAt: Long? = null,
        updatedAt: Long = clock.currentTime,
    ) = Note(
        id = id,
        title = title,
        content = "",
        createdAt = clock.currentTime,
        updatedAt = updatedAt,
        deletedAt = deletedAt,
    )
}
