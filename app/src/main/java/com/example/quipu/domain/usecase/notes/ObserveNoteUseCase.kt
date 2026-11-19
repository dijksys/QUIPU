package com.example.quipu.domain.usecase.notes

import com.example.quipu.domain.model.Note
import com.example.quipu.domain.repository.NoteRepository
import kotlinx.coroutines.flow.Flow
import javax.inject.Inject

class ObserveNoteUseCase @Inject constructor(
    private val noteRepository: NoteRepository,
) {
    operator fun invoke(noteId: String): Flow<Note?> = noteRepository.observeNote(noteId)
}
