package io.github.dijksys.quipu.domain.usecase.notes

import io.github.dijksys.quipu.domain.repository.NoteRepository
import javax.inject.Inject

class SetNoteTagsUseCase @Inject constructor(
    private val noteRepository: NoteRepository,
) {
    suspend operator fun invoke(noteId: String, tags: List<String>) {
        noteRepository.setNoteTags(noteId, tags)
    }
}

class ObserveTagsUseCase @Inject constructor(
    private val noteRepository: NoteRepository,
) {
    operator fun invoke() = noteRepository.observeTags()
}
