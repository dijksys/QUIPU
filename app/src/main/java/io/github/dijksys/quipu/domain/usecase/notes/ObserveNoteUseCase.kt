package io.github.dijksys.quipu.domain.usecase.notes

import io.github.dijksys.quipu.domain.model.Note
import io.github.dijksys.quipu.domain.repository.NoteRepository
import kotlinx.coroutines.flow.Flow
import javax.inject.Inject

class ObserveNoteUseCase @Inject constructor(
    private val noteRepository: NoteRepository,
) {
    operator fun invoke(noteId: String): Flow<Note?> = noteRepository.observeNote(noteId)
}
