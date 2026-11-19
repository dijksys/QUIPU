package io.github.dijksys.quipu.domain.usecase.notes

import io.github.dijksys.quipu.core.common.Clock
import io.github.dijksys.quipu.domain.repository.NoteRepository
import javax.inject.Inject

class TogglePinNoteUseCase @Inject constructor(
    private val noteRepository: NoteRepository,
    private val clock: Clock,
) {
    suspend operator fun invoke(noteId: String, pinned: Boolean) {
        noteRepository.setPinned(noteId, pinned, clock.nowMillis())
    }
}
