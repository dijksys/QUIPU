package com.example.quipu.domain.usecase.notes

import com.example.quipu.domain.repository.NoteRepository
import javax.inject.Inject

class EmptyTrashUseCase @Inject constructor(
    private val noteRepository: NoteRepository,
) {
    suspend operator fun invoke() {
        noteRepository.emptyTrash()
    }
}
