package io.github.dijksys.quipu.feature.onboarding

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dagger.hilt.android.lifecycle.HiltViewModel
import io.github.dijksys.quipu.domain.usecase.onboarding.CompleteOnboardingUseCase
import io.github.dijksys.quipu.domain.usecase.onboarding.ObserveOnboardingCompletedUseCase
import io.github.dijksys.quipu.domain.usecase.onboarding.ResolveOnboardingCompletedUseCase
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class WelcomeViewModel @Inject constructor(
    private val observeOnboardingCompleted: ObserveOnboardingCompletedUseCase,
    private val resolveOnboardingCompleted: ResolveOnboardingCompletedUseCase,
    private val completeOnboarding: CompleteOnboardingUseCase,
) : ViewModel() {

    private val _uiState = MutableStateFlow(WelcomeUiState())
    val uiState = _uiState.asStateFlow()

    private var loadJob: Job? = null
    private var completionJob: Job? = null

    init {
        resolveInitialState()
    }

    /**
     * Ejecuta la acción principal. En estado READY completa el onboarding; tras
     * un error reintenta la operación que falló, sin abandonar la pantalla.
     */
    fun onContinue() {
        val state = _uiState.value
        when (state.status) {
            WelcomeStatus.READY -> complete()
            WelcomeStatus.ERROR -> {
                if (state.error == WelcomeError.INITIALIZATION) {
                    resolveInitialState()
                } else {
                    complete()
                }
            }

            else -> Unit
        }
    }

    private fun resolveInitialState() {
        if (loadJob?.isActive == true) return
        _uiState.value = WelcomeUiState(status = WelcomeStatus.LOADING)
        loadJob = viewModelScope.launch {
            try {
                val completed = observeOnboardingCompleted().first()
                    ?: resolveOnboardingCompleted()
                _uiState.value = WelcomeUiState(
                    status = if (completed) {
                        WelcomeStatus.COMPLETED
                    } else {
                        WelcomeStatus.READY
                    },
                )
            } catch (cancellation: CancellationException) {
                throw cancellation
            } catch (_: Throwable) {
                _uiState.value = WelcomeUiState(
                    status = WelcomeStatus.ERROR,
                    error = WelcomeError.INITIALIZATION,
                )
            }
        }
    }

    private fun complete() {
        if (completionJob?.isActive == true) return
        _uiState.value = WelcomeUiState(status = WelcomeStatus.COMPLETING)
        completionJob = viewModelScope.launch {
            try {
                completeOnboarding()
                _uiState.value = WelcomeUiState(status = WelcomeStatus.COMPLETED)
            } catch (cancellation: CancellationException) {
                throw cancellation
            } catch (_: Throwable) {
                _uiState.value = WelcomeUiState(
                    status = WelcomeStatus.ERROR,
                    error = WelcomeError.COMPLETION,
                )
            }
        }
    }
}
