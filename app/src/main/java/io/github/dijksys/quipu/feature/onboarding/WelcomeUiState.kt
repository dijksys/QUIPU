package io.github.dijksys.quipu.feature.onboarding

enum class WelcomeStatus {
    LOADING,
    READY,
    COMPLETING,
    COMPLETED,
    ERROR,
}

enum class WelcomeError {
    INITIALIZATION,
    COMPLETION,
}

data class WelcomeUiState(
    val status: WelcomeStatus = WelcomeStatus.LOADING,
    val error: WelcomeError? = null,
)
