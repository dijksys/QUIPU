package com.example.quipu.domain.usecase.notes

import com.example.quipu.domain.repository.NoteRepository
import javax.inject.Inject

class PermanentlyDeleteNoteUseCase @Inject constructor(
    private val noteRepository: NoteRepository,
) {
    /**
     * Elimina definitivamente la nota. Los recordatorios vinculados se conservan:
     * la clave foránea usa ON DELETE SET NULL.
     */
    suspend operator fun invoke(noteId: String) {
        noteRepository.deleteNotePermanently(noteId)
    }
}
