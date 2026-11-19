package io.github.dijksys.quipu.domain.usecase.notes

import io.github.dijksys.quipu.core.common.Clock
import io.github.dijksys.quipu.domain.repository.NoteRepository
import javax.inject.Inject

/** Mueve la nota a la papelera sin borrar sus datos. */
class DeleteNoteUseCase @Inject constructor(
    private val noteRepository: NoteRepository,
    private val clock: Clock,
) {
    suspend operator fun invoke(noteId: String) {
        val now = clock.nowMillis()
        noteRepository.setDeletedAt(noteId, now, now)
    }
}
