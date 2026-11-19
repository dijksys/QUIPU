package com.example.quipu.domain.usecase.notes

import com.example.quipu.core.common.Clock
import com.example.quipu.domain.repository.NoteRepository
import javax.inject.Inject

class TogglePinNoteUseCase @Inject constructor(
    private val noteRepository: NoteRepository,
    private val clock: Clock,
) {
    suspend operator fun invoke(noteId: String, pinned: Boolean) {
        noteRepository.setPinned(noteId, pinned, clock.nowMillis())
    }
}
