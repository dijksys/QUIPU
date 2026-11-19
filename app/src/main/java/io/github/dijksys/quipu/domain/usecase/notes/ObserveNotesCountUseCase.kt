package io.github.dijksys.quipu.domain.usecase.notes

import io.github.dijksys.quipu.domain.repository.NoteRepository
import kotlinx.coroutines.flow.Flow
import javax.inject.Inject

class ObserveNotesCountUseCase @Inject constructor(
    private val noteRepository: NoteRepository,
) {
    operator fun invoke(): Flow<Int> = noteRepository.observeActiveNotesCount()
}
