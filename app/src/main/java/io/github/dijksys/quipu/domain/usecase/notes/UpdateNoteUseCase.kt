package io.github.dijksys.quipu.domain.usecase.notes

import io.github.dijksys.quipu.core.common.Clock
import io.github.dijksys.quipu.domain.model.Note
import io.github.dijksys.quipu.domain.repository.NoteRepository
import javax.inject.Inject

class UpdateNoteUseCase @Inject constructor(
    private val noteRepository: NoteRepository,
    private val clock: Clock,
) {
    suspend operator fun invoke(note: Note): Note {
        val updated = note.copy(
            title = note.title.trim(),
            updatedAt = clock.nowMillis(),
        )
        noteRepository.upsertNote(updated)
        return updated
    }
}
