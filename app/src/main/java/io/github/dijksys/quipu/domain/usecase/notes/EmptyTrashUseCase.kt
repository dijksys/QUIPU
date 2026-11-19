package io.github.dijksys.quipu.domain.usecase.notes

import io.github.dijksys.quipu.domain.repository.NoteRepository
import javax.inject.Inject

class EmptyTrashUseCase @Inject constructor(
    private val noteRepository: NoteRepository,
) {
    suspend operator fun invoke() {
        noteRepository.emptyTrash()
    }
}
