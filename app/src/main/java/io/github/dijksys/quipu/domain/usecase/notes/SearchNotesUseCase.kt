package io.github.dijksys.quipu.domain.usecase.notes

import io.github.dijksys.quipu.domain.model.Note
import io.github.dijksys.quipu.domain.model.NoteDateRange
import io.github.dijksys.quipu.domain.model.NoteFilter
import io.github.dijksys.quipu.domain.model.NoteSortOrder
import io.github.dijksys.quipu.domain.repository.NoteRepository
import kotlinx.coroutines.flow.Flow
import javax.inject.Inject

class SearchNotesUseCase @Inject constructor(
    private val noteRepository: NoteRepository,
) {
    operator fun invoke(
        query: String,
        filter: NoteFilter,
        dateRange: NoteDateRange? = null,
        sort: NoteSortOrder = NoteSortOrder.RELEVANCE,
    ): Flow<List<Note>> {
        val trimmed = query.trim()
        return if (trimmed.isEmpty()) {
            noteRepository.observeNotes(filter)
        } else {
            noteRepository.searchNotes(trimmed, filter, dateRange, sort)
        }
    }
}
