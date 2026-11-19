package com.example.quipu.domain.usecase.notes

import com.example.quipu.domain.model.Note
import com.example.quipu.domain.model.NoteFilter
import com.example.quipu.domain.repository.NoteRepository
import kotlinx.coroutines.flow.Flow
import javax.inject.Inject

class SearchNotesUseCase @Inject constructor(
    private val noteRepository: NoteRepository,
) {
    operator fun invoke(query: String, filter: NoteFilter): Flow<List<Note>> {
        val trimmed = query.trim()
        return if (trimmed.isEmpty()) {
            noteRepository.observeNotes(filter)
        } else {
            noteRepository.searchNotes(trimmed, filter)
        }
    }
}
