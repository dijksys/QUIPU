package com.example.quipu.domain.usecase.notes

import com.example.quipu.core.common.Clock
import com.example.quipu.domain.repository.NoteRepository
import javax.inject.Inject

class ArchiveNoteUseCase @Inject constructor(
    private val noteRepository: NoteRepository,
    private val clock: Clock,
) {
    /** Archiva la nota o la desarchiva si ya estaba archivada. */
    suspend operator fun invoke(noteId: String, archive: Boolean) {
        noteRepository.setArchivedAt(
            id = noteId,
            archivedAt = if (archive) clock.nowMillis() else null,
            updatedAt = clock.nowMillis(),
        )
    }
}
