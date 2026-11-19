package com.example.quipu.domain.model

enum class ThemeMode {
    LIGHT,
    DARK,
    SYSTEM,
}

data class AppSettings(
    val themeMode: ThemeMode = ThemeMode.SYSTEM,
    val notificationsEnabled: Boolean = true,
)
