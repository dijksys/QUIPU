package com.example.quipu.domain.usecase.notes

import com.example.quipu.domain.model.Note
import com.example.quipu.domain.model.NoteFilter
import com.example.quipu.domain.repository.NoteRepository
import kotlinx.coroutines.flow.Flow
import javax.inject.Inject

class ObserveNotesUseCase @Inject constructor(
    private val noteRepository: NoteRepository,
) {
    operator fun invoke(filter: NoteFilter = NoteFilter.ACTIVE): Flow<List<Note>> =
        noteRepository.observeNotes(filter)
}
