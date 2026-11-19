package com.example.quipu.domain.usecase.notes

import com.example.quipu.core.common.Clock
import com.example.quipu.domain.repository.NoteRepository
import javax.inject.Inject

class RestoreNoteUseCase @Inject constructor(
    private val noteRepository: NoteRepository,
    private val clock: Clock,
) {
    /** Restaura una nota de la papelera y la devuelve a la lista activa. */
    suspend operator fun invoke(noteId: String) {
        val now = clock.nowMillis()
        noteRepository.setDeletedAt(noteId, null, now)
        noteRepository.setArchivedAt(noteId, null, now)
    }
}
