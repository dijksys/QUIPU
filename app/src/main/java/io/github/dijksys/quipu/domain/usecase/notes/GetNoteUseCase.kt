package io.github.dijksys.quipu.domain.usecase.notes

import io.github.dijksys.quipu.domain.model.Note
import io.github.dijksys.quipu.domain.repository.NoteRepository
import javax.inject.Inject

class GetNoteUseCase @Inject constructor(
    private val noteRepository: NoteRepository,
) {
    suspend operator fun invoke(noteId: String): Note? = noteRepository.getNote(noteId)
}
