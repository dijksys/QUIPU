package io.github.dijksys.quipu.core.common

/**
 * Resultado explícito para operaciones de negocio que pueden fallar por validación
 * o por disponibilidad de la plataforma.
 */
sealed interface AppResult<out T> {
    data class Success<T>(val value: T) : AppResult<T>
    data class Failure(val error: AppError) : AppResult<Nothing>
}

sealed interface AppError {
    data object TitleRequired : AppError
    data object ScheduledTimeRequired : AppError
    data object ScheduledTimeInPast : AppError
    data object NoteNotFound : AppError
    data object ReminderNotFound : AppError
    data class Unexpected(val cause: Throwable? = null) : AppError
}
