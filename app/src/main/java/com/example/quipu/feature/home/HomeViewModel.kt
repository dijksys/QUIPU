package com.example.quipu.feature.home

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.quipu.core.common.Clock
import com.example.quipu.domain.model.ReminderStatus
import com.example.quipu.domain.usecase.notes.ObserveNotesCountUseCase
import com.example.quipu.domain.usecase.notes.ObservePinnedNotesUseCase
import com.example.quipu.domain.usecase.notes.ObserveRecentNotesUseCase
import com.example.quipu.domain.usecase.reminders.ObserveRemindersUseCase
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import javax.inject.Inject

@HiltViewModel
class HomeViewModel @Inject constructor(
    observePinnedNotes: ObservePinnedNotesUseCase,
    observeRecentNotes: ObserveRecentNotesUseCase,
    observeNotesCount: ObserveNotesCountUseCase,
    observeReminders: ObserveRemindersUseCase,
    private val clock: Clock,
) : ViewModel() {

    val uiState = combine(
        observePinnedNotes(limit = 4),
        observeRecentNotes(limit = 8),
        observeNotesCount(),
        observeReminders(),
    ) { pinned, recent, notesCount, reminders ->
        val now = clock.nowMillis()
        val pending = reminders.filter { it.status == ReminderStatus.PENDING }
        HomeUiState(
            isLoading = false,
            nowMillis = now,
            notesCount = notesCount,
            pendingRemindersCount = pending.size,
            pinnedNotes = pinned,
            recentNotes = recent.filterNot { it.isPinned }.take(4),
            upcomingReminders = pending
                .filter { it.scheduledAt >= now }
                .sortedBy { it.scheduledAt }
                .take(4),
        )
    }.catch {
        emit(HomeUiState(isLoading = false))
    }.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5_000),
        initialValue = HomeUiState(),
    )
}
