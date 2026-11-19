package com.example.quipu.domain.usecase.notes

import com.example.quipu.domain.model.Note
import com.example.quipu.domain.repository.NoteRepository
import javax.inject.Inject

class GetNoteUseCase @Inject constructor(
    private val noteRepository: NoteRepository,
) {
    suspend operator fun invoke(noteId: String): Note? = noteRepository.getNote(noteId)
}
