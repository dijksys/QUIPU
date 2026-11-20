package io.github.dijksys.quipu.domain.model

enum class ThemeMode {
    LIGHT,
    DARK,
    SYSTEM,
}

data class AppSettings(
    val themeMode: ThemeMode = ThemeMode.SYSTEM,
    val notificationsEnabled: Boolean = true,
    /** `null` significa que la decisión inicial todavía no se ha resuelto. */
    val onboardingCompleted: Boolean? = null,
)
