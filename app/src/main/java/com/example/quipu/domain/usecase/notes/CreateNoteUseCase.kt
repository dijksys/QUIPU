package com.example.quipu.domain.usecase.notes

import com.example.quipu.core.common.Clock
import com.example.quipu.domain.model.Note
import com.example.quipu.domain.repository.NoteRepository
import java.util.UUID
import javax.inject.Inject

class CreateNoteUseCase @Inject constructor(
    private val noteRepository: NoteRepository,
    private val clock: Clock,
) {
    suspend operator fun invoke(
        title: String,
        content: String,
        id: String = UUID.randomUUID().toString(),
    ): Note {
        val now = clock.nowMillis()
        val note = Note(
            id = id,
            title = title.trim(),
            content = content,
            createdAt = now,
            updatedAt = now,
        )
        noteRepository.upsertNote(note)
        return note
    }
}
