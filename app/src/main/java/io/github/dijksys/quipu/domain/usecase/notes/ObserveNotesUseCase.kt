package io.github.dijksys.quipu.domain.usecase.notes

import io.github.dijksys.quipu.domain.model.Note
import io.github.dijksys.quipu.domain.model.NoteFilter
import io.github.dijksys.quipu.domain.repository.NoteRepository
import kotlinx.coroutines.flow.Flow
import javax.inject.Inject

class ObserveNotesUseCase @Inject constructor(
    private val noteRepository: NoteRepository,
) {
    operator fun invoke(filter: NoteFilter = NoteFilter.ACTIVE): Flow<List<Note>> =
        noteRepository.observeNotes(filter)
}
