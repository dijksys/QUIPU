package com.example.quipu.core.designsystem.theme

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.quipu.domain.model.ThemeMode
import com.example.quipu.domain.usecase.settings.ObserveSettingsUseCase
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import javax.inject.Inject

/** Observa el tema global para aplicarlo en la raíz de la aplicación. */
@HiltViewModel
class AppThemeViewModel @Inject constructor(
    observeSettings: ObserveSettingsUseCase,
) : ViewModel() {
    val themeMode = observeSettings()
        .map { it.themeMode }
        .catch { emit(ThemeMode.SYSTEM) }
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5_000),
            initialValue = ThemeMode.SYSTEM,
        )
}
