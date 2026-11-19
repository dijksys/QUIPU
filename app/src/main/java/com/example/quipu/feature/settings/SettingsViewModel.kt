package com.example.quipu.feature.settings

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.quipu.domain.model.ThemeMode
import com.example.quipu.domain.usecase.settings.ObserveSettingsUseCase
import com.example.quipu.domain.usecase.settings.SetNotificationsEnabledUseCase
import com.example.quipu.domain.usecase.settings.SetThemeModeUseCase
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.receiveAsFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class SettingsViewModel @Inject constructor(
    observeSettings: ObserveSettingsUseCase,
    private val setThemeModeUseCase: SetThemeModeUseCase,
    private val setNotificationsEnabledUseCase: SetNotificationsEnabledUseCase,
) : ViewModel() {

    private val eventChannel = Channel<SettingsEvent>(Channel.BUFFERED)
    val events = eventChannel.receiveAsFlow()

    val uiState = observeSettings()
        .map { settings ->
            SettingsUiState(
                isLoading = false,
                themeMode = settings.themeMode,
                notificationsEnabled = settings.notificationsEnabled,
            )
        }
        .catch { emit(SettingsUiState(isLoading = false)) }
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5_000),
            initialValue = SettingsUiState(),
        )

    fun setThemeMode(mode: ThemeMode) {
        viewModelScope.launch {
            runCatching { setThemeModeUseCase(mode) }
                .onFailure { eventChannel.send(SettingsEvent.Failure) }
        }
    }

    fun setNotificationsEnabled(enabled: Boolean) {
        viewModelScope.launch {
            runCatching { setNotificationsEnabledUseCase(enabled) }
                .onFailure { eventChannel.send(SettingsEvent.Failure) }
        }
    }
}
