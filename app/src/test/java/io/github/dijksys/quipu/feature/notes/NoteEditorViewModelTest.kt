package io.github.dijksys.quipu.feature.notes

import androidx.lifecycle.SavedStateHandle
import io.github.dijksys.quipu.domain.model.Note
import io.github.dijksys.quipu.domain.formatting.MarkdownChecklist
import io.github.dijksys.quipu.domain.formatting.MarkdownFormatting
import io.github.dijksys.quipu.domain.formatting.MarkdownSelection
import io.github.dijksys.quipu.domain.usecase.notes.ArchiveNoteUseCase
import io.github.dijksys.quipu.domain.usecase.notes.CreateNoteUseCase
import io.github.dijksys.quipu.domain.usecase.notes.DeleteNoteUseCase
import io.github.dijksys.quipu.domain.usecase.notes.GetNoteUseCase
import io.github.dijksys.quipu.domain.usecase.notes.ObserveTagsUseCase
import io.github.dijksys.quipu.domain.usecase.notes.SetNoteTagsUseCase
import io.github.dijksys.quipu.domain.usecase.notes.TogglePinNoteUseCase
import io.github.dijksys.quipu.domain.usecase.notes.UpdateNoteUseCase
import io.github.dijksys.quipu.fakes.FakeNoteRepository
import io.github.dijksys.quipu.fakes.TestClock
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
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
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
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
    fun `new empty note starts empty without metadata`() = runTest(dispatcher) {
        val viewModel = createViewModel()
        runCurrent()

        val state = viewModel.uiState.value
        assertEquals(NoteSaveStatus.EMPTY, state.saveStatus)
        assertTrue(state.isNewSession)
        assertFalse(state.isPersisted)
        assertNull(state.createdAt)
        assertNull(state.updatedAt)
        assertFalse(state.justCreated)
        assertTrue(repository.currentNotes.isEmpty())
    }

    @Test
    fun `typing marks unsaved immediately and stays pending during debounce`() =
        runTest(dispatcher) {
            val viewModel = createViewModel()
            runCurrent()

            viewModel.onTitleChange("Mi título")

            assertEquals(NoteSaveStatus.UNSAVED, viewModel.uiState.value.saveStatus)
            advanceTimeBy(300)
            runCurrent()
            assertEquals(NoteSaveStatus.UNSAVED, viewModel.uiState.value.saveStatus)
            assertTrue(repository.currentNotes.isEmpty())

            advanceUntilIdle()

            val saved = viewModel.uiState.value
            assertEquals(NoteSaveStatus.SAVED, saved.saveStatus)
            assertTrue(saved.justCreated)
            assertTrue(saved.isPersisted)
            assertTrue(saved.isNewSession)
            assertNotNull(saved.createdAt)
            assertNotNull(saved.updatedAt)
            assertEquals(1, repository.upsertCount)
        }

    @Test
    fun `saving state is exposed while write is in progress`() = runTest(dispatcher) {
        repository.writeGate = CompletableDeferred()
        val viewModel = createViewModel()
        runCurrent()

        viewModel.onTitleChange("Nota")
        advanceTimeBy(601)
        runCurrent()

        assertEquals(NoteSaveStatus.SAVING, viewModel.uiState.value.saveStatus)
        assertFalse(viewModel.uiState.value.justCreated)

        repository.writeGate?.complete(Unit)
        advanceUntilIdle()

        assertEquals(NoteSaveStatus.SAVED, viewModel.uiState.value.saveStatus)
        assertTrue(viewModel.uiState.value.justCreated)
    }

    @Test
    fun `first creation shows created state and subsequent save shows saved`() =
        runTest(dispatcher) {
            val viewModel = createViewModel()
            runCurrent()

            viewModel.onTitleChange("Primera")
            viewModel.onContentChange("Contenido")
            advanceUntilIdle()

            assertEquals(NoteSaveStatus.SAVED, viewModel.uiState.value.saveStatus)
            assertTrue(viewModel.uiState.value.justCreated)

            clock.currentTime += 2_000
            viewModel.onContentChange("Contenido editado")
            assertEquals(NoteSaveStatus.UNSAVED, viewModel.uiState.value.saveStatus)
            assertFalse(viewModel.uiState.value.justCreated)
            advanceUntilIdle()

            assertEquals(NoteSaveStatus.SAVED, viewModel.uiState.value.saveStatus)
            assertFalse(viewModel.uiState.value.justCreated)
            assertEquals(clock.currentTime, repository.currentNotes.single().updatedAt)
        }

    @Test
    fun `existing note starts saved in edit session and updates timestamp`() =
        runTest(dispatcher) {
            val originalUpdatedAt = clock.currentTime
            repository.preload(note(id = "n1", title = "Antes", content = "Texto"))
            val viewModel = createViewModel(noteId = "n1")
            runCurrent()

            val initial = viewModel.uiState.value
            assertEquals(NoteSaveStatus.SAVED, initial.saveStatus)
            assertFalse(initial.isNewSession)
            assertTrue(initial.isPersisted)
            assertEquals(originalUpdatedAt, initial.updatedAt)

            clock.currentTime += 2_000
            viewModel.onContentChange("Texto editado")
            assertEquals(NoteSaveStatus.UNSAVED, viewModel.uiState.value.saveStatus)
            advanceUntilIdle()

            val saved = viewModel.uiState.value
            assertEquals(NoteSaveStatus.SAVED, saved.saveStatus)
            assertEquals(clock.currentTime, saved.updatedAt)
            assertEquals("Texto editado", repository.getStored("n1")!!.content)
        }

    @Test
    fun `closing without changes does not update timestamp or write`() = runTest(dispatcher) {
        val originalUpdatedAt = clock.currentTime
        repository.preload(note(id = "n1", title = "Sin cambios", content = "Texto"))
        val viewModel = createViewModel(noteId = "n1")
        runCurrent()

        clock.currentTime += 5_000
        val id = viewModel.saveNow()

        assertEquals("n1", id)
        assertEquals(0, repository.upsertCount)
        assertEquals(originalUpdatedAt, viewModel.uiState.value.updatedAt)
        assertEquals(originalUpdatedAt, repository.getStored("n1")!!.updatedAt)
        assertEquals(NoteSaveStatus.SAVED, viewModel.uiState.value.saveStatus)
    }

    @Test
    fun `closing with pending changes persists them immediately`() = runTest(dispatcher) {
        val viewModel = createViewModel()
        runCurrent()
        viewModel.onTitleChange("Salida")
        viewModel.onContentChange("Contenido pendiente")

        val id = viewModel.saveNow()

        assertNotNull(id)
        assertEquals("Salida", repository.getStored(id!!)!!.title)
        assertEquals("Contenido pendiente", repository.getStored(id)!!.content)
        assertEquals(NoteSaveStatus.SAVED, viewModel.uiState.value.saveStatus)
    }

    @Test
    fun `blank new note remains empty and is not persisted`() = runTest(dispatcher) {
        val viewModel = createViewModel()
        runCurrent()

        viewModel.onTitleChange("   ")
        viewModel.onContentChange("  ")
        advanceUntilIdle()

        assertEquals(NoteSaveStatus.EMPTY, viewModel.uiState.value.saveStatus)
        assertTrue(repository.currentNotes.isEmpty())
        assertEquals(0, repository.upsertCount)
    }

    @Test
    fun `failed save keeps content and retry recovers`() = runTest(dispatcher) {
        repository.failWrites = true
        val viewModel = createViewModel()
        runCurrent()
        viewModel.onTitleChange("No se guarda")
        viewModel.onContentChange("Contenido intacto")

        advanceUntilIdle()

        assertEquals(NoteSaveStatus.ERROR, viewModel.uiState.value.saveStatus)
        assertEquals("No se guarda", viewModel.uiState.value.title)
        assertEquals("Contenido intacto", viewModel.uiState.value.content)
        assertNull(viewModel.uiState.value.createdAt)
        assertEquals(0, repository.upsertCount)

        repository.failWrites = false
        viewModel.retrySave()
        advanceUntilIdle()

        assertEquals(NoteSaveStatus.SAVED, viewModel.uiState.value.saveStatus)
        assertTrue(viewModel.uiState.value.justCreated)
        assertNotNull(viewModel.uiState.value.createdAt)
        assertEquals("No se guarda", repository.currentNotes.single().title)
    }

    @Test
    fun `changes during an active save stay unsaved and are saved next`() =
        runTest(dispatcher) {
            repository.writeGate = CompletableDeferred()
            val viewModel = createViewModel()
            runCurrent()
            viewModel.onTitleChange("A")
            advanceTimeBy(601)
            runCurrent()
            assertEquals(NoteSaveStatus.SAVING, viewModel.uiState.value.saveStatus)

            viewModel.onContentChange("B")
            assertEquals(NoteSaveStatus.UNSAVED, viewModel.uiState.value.saveStatus)
            repository.writeGate?.complete(Unit)
            runCurrent()

            assertEquals(NoteSaveStatus.UNSAVED, viewModel.uiState.value.saveStatus)
            assertEquals("A", repository.currentNotes.single().title)

            advanceUntilIdle()

            assertEquals(NoteSaveStatus.SAVED, viewModel.uiState.value.saveStatus)
            assertEquals("B", repository.currentNotes.single().content)
            assertEquals(2, repository.upsertCount)
        }

    @Test
    fun `pin does not write content and refreshes metadata`() = runTest(dispatcher) {
        repository.preload(note(id = "n1", title = "Nota", content = "Texto"))
        val viewModel = createViewModel(noteId = "n1")
        runCurrent()
        clock.currentTime += 2_000

        viewModel.togglePin()
        advanceUntilIdle()

        assertTrue(repository.getStored("n1")!!.isPinned)
        assertEquals(0, repository.upsertCount)
        assertEquals(1, repository.pinCount)
        assertEquals(clock.currentTime, viewModel.uiState.value.updatedAt)
        assertEquals(NoteSaveStatus.SAVED, viewModel.uiState.value.saveStatus)
    }

    @Test
    fun `archive does not write content and refreshes metadata`() = runTest(dispatcher) {
        repository.preload(note(id = "n1", title = "Nota", content = "Texto"))
        val viewModel = createViewModel(noteId = "n1")
        runCurrent()
        clock.currentTime += 2_000

        viewModel.setArchived(true)
        advanceUntilIdle()

        assertTrue(repository.getStored("n1")!!.isArchived)
        assertEquals(0, repository.upsertCount)
        assertEquals(1, repository.archiveCount)
        assertEquals(clock.currentTime, viewModel.uiState.value.updatedAt)
        assertEquals(NoteSaveStatus.SAVED, viewModel.uiState.value.saveStatus)
    }

    @Test
    fun `background flush without changes does not update timestamp`() = runTest(dispatcher) {
        val originalUpdatedAt = clock.currentTime
        repository.preload(note(id = "n1", title = "Sin cambios", content = "Texto"))
        val viewModel = createViewModel(noteId = "n1")
        runCurrent()
        clock.currentTime += 5_000

        viewModel.flush()
        advanceUntilIdle()

        assertEquals(0, repository.upsertCount)
        assertEquals(originalUpdatedAt, repository.getStored("n1")!!.updatedAt)
        assertEquals(originalUpdatedAt, viewModel.uiState.value.updatedAt)
        assertEquals(NoteSaveStatus.SAVED, viewModel.uiState.value.saveStatus)
    }

    @Test
    fun `background flush persists pending edits immediately`() = runTest(dispatcher) {
        val viewModel = createViewModel()
        runCurrent()
        viewModel.onTitleChange("Segundo plano")
        viewModel.onContentChange("Borrador pendiente")

        viewModel.flush()
        advanceUntilIdle()

        val stored = repository.currentNotes.single()
        assertEquals("Segundo plano", stored.title)
        assertEquals("Borrador pendiente", stored.content)
        assertEquals(NoteSaveStatus.SAVED, viewModel.uiState.value.saveStatus)
        assertEquals(1, repository.upsertCount)
    }

    @Test
    fun `whitespace-only title change is not persisted`() = runTest(dispatcher) {
        val originalUpdatedAt = clock.currentTime
        repository.preload(note(id = "n1", title = "Original", content = "Texto"))
        val viewModel = createViewModel(noteId = "n1")
        runCurrent()

        clock.currentTime += 5_000
        viewModel.onTitleChange("Original   ")
        advanceUntilIdle()

        assertEquals(0, repository.upsertCount)
        assertEquals(originalUpdatedAt, repository.getStored("n1")!!.updatedAt)
        assertEquals(NoteSaveStatus.SAVED, viewModel.uiState.value.saveStatus)
    }

    @Test
    fun `restored draft marks unsaved and autosaves`() = runTest(dispatcher) {
        repository.preload(note(id = "n1", title = "Original", content = "Texto"))
        val viewModel = createViewModel(
            noteId = "n1",
            draftTitle = "Borrador restaurado",
            draftContent = "Contenido restaurado",
        )
        runCurrent()

        assertEquals(NoteSaveStatus.UNSAVED, viewModel.uiState.value.saveStatus)
        assertEquals("Borrador restaurado", viewModel.uiState.value.title)

        advanceUntilIdle()

        assertEquals("Borrador restaurado", repository.getStored("n1")!!.title)
        assertEquals("Contenido restaurado", repository.getStored("n1")!!.content)
        assertEquals(NoteSaveStatus.SAVED, viewModel.uiState.value.saveStatus)
    }

    @Test
    fun `markdown and checkbox edits are autosaved`() = runTest(dispatcher) {
        val viewModel = createViewModel()
        runCurrent()
        viewModel.onTitleChange("Markdown")
        viewModel.onContentChange("# Título\n\n- [ ] Uno")
        advanceUntilIdle()

        val noteId = repository.currentNotes.single().id
        assertEquals("# Título\n\n- [ ] Uno", repository.getStored(noteId)!!.content)

        val checked = MarkdownChecklist.toggleItem(viewModel.uiState.value.content, 2)
        viewModel.onContentChange(checked)
        advanceUntilIdle()

        assertEquals("# Título\n\n- [x] Uno", repository.getStored(noteId)!!.content)
        assertEquals(NoteSaveStatus.SAVED, viewModel.uiState.value.saveStatus)
    }

    @Test
    fun `all formatting operations persist the original markdown`() = runTest(dispatcher) {
        val viewModel = createViewModel()
        runCurrent()
        viewModel.onTitleChange("Formatos")
        viewModel.onContentChange("texto")
        advanceUntilIdle()

        val noteId = repository.currentNotes.single().id
        val selection = MarkdownSelection(0, 5)
        val results = listOf(
            MarkdownFormatting.toggleBold("texto", selection),
            MarkdownFormatting.toggleItalic("texto", selection),
            MarkdownFormatting.applyHeading("texto", selection, 1),
            MarkdownFormatting.applyHeading("texto", selection, 2),
            MarkdownFormatting.applyHeading("texto", selection, 3),
            MarkdownFormatting.applyUnorderedList("texto", selection),
            MarkdownFormatting.applyOrderedList("texto", selection),
            MarkdownFormatting.applyChecklist("texto", selection),
            MarkdownFormatting.insertCodeBlock("texto", selection),
        )

        results.forEach { result ->
            viewModel.onContentChange(result.text)
            advanceTimeBy(700)
            runCurrent()
            advanceUntilIdle()

            assertEquals(result.text, repository.getStored(noteId)!!.content)
            assertEquals(NoteSaveStatus.SAVED, viewModel.uiState.value.saveStatus)
        }

        val pasted = "Texto pegado https://example.com/path"
        viewModel.onContentChange(pasted)
        advanceUntilIdle()

        assertEquals(pasted, repository.getStored(noteId)!!.content)
        assertEquals(pasted, viewModel.uiState.value.content)
        assertEquals(NoteSaveStatus.SAVED, viewModel.uiState.value.saveStatus)
    }

    @Test
    fun `missing note exposes missing state`() = runTest(dispatcher) {
        val viewModel = createViewModel(noteId = "no-existe")
        runCurrent()

        assertTrue(viewModel.uiState.value.isMissing)
    }

    @Test
    fun `tags added before the first save persist with the note`() = runTest(dispatcher) {
        val viewModel = createViewModel()
        runCurrent()

        viewModel.setTags(listOf("Ideas", "ideas", "Trabajo"))
        viewModel.onTitleChange("Nota con etiquetas")
        viewModel.onContentChange("Contenido")
        advanceTimeBy(700)
        runCurrent()
        advanceUntilIdle()

        val stored = repository.currentNotes.single()
        assertEquals(listOf("Ideas", "Trabajo"), stored.tags)
        assertEquals(listOf("Ideas", "Trabajo"), viewModel.uiState.value.tags)
    }

    @Test
    fun `tags on a persisted note update immediately without touching updatedAt`() =
        runTest(dispatcher) {
            val stored = note("n1", "Nota", "Contenido")
            repository.preload(stored)
            val viewModel = createViewModel(noteId = "n1")
            advanceUntilIdle()

            viewModel.setTags(listOf("Trabajo"))
            advanceUntilIdle()

            assertEquals(listOf("Trabajo"), repository.getStored("n1")!!.tags)
            assertEquals(stored.updatedAt, repository.getStored("n1")!!.updatedAt)
            assertEquals(listOf("Trabajo"), viewModel.uiState.value.tags)
        }

    private fun createViewModel(
        noteId: String = NoteEditorViewModel.NEW_NOTE_ID,
        draftTitle: String? = null,
        draftContent: String? = null,
    ): NoteEditorViewModel {
        val arguments = buildMap {
            put(NoteEditorViewModel.ARG_NOTE_ID, noteId)
            draftTitle?.let { put("draft_title", it) }
            draftContent?.let { put("draft_content", it) }
        }
        val savedState = SavedStateHandle(arguments)
        return NoteEditorViewModel(
            savedStateHandle = savedState,
            getNote = GetNoteUseCase(repository),
            createNote = CreateNoteUseCase(repository, clock),
            updateNote = UpdateNoteUseCase(repository, clock),
            togglePin = TogglePinNoteUseCase(repository, clock),
            archiveNote = ArchiveNoteUseCase(repository, clock),
            deleteNote = DeleteNoteUseCase(repository, clock),
            setNoteTags = SetNoteTagsUseCase(repository),
            observeTags = ObserveTagsUseCase(repository),
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
