package com.example.quipu.core.datastore

import android.content.Context
import androidx.datastore.core.handlers.ReplaceFileCorruptionHandler
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.emptyPreferences
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import com.example.quipu.domain.model.AppSettings
import com.example.quipu.domain.model.ThemeMode
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.map
import java.io.IOException
import javax.inject.Inject
import javax.inject.Singleton

private val Context.settingsDataStore by preferencesDataStore(
    name = "quipu_settings",
    corruptionHandler = ReplaceFileCorruptionHandler { emptyPreferences() },
)

/**
 * Fuente de datos de preferencias. Mantiene un único DataStore por proceso y
 * expone un modelo de dominio, sin claves de Preferences hacia capas superiores.
 */
@Singleton
class AppSettingsDataStore @Inject constructor(
    @param:ApplicationContext private val context: Context,
) {
    private object Keys {
        val themeMode = stringPreferencesKey("theme_mode")
        val notificationsEnabled = booleanPreferencesKey("notifications_enabled")
    }

    val settings: Flow<AppSettings> = context.settingsDataStore.data
        .catch { throwable ->
            if (throwable is IOException) emit(emptyPreferences()) else throw throwable
        }
        .map { preferences ->
            AppSettings(
                themeMode = preferences[Keys.themeMode]
                    ?.let { stored -> ThemeMode.entries.firstOrNull { it.name == stored } }
                    ?: ThemeMode.SYSTEM,
                notificationsEnabled = preferences[Keys.notificationsEnabled] ?: true,
            )
        }

    suspend fun setThemeMode(mode: ThemeMode) {
        context.settingsDataStore.edit { preferences ->
            preferences[Keys.themeMode] = mode.name
        }
    }

    suspend fun setNotificationsEnabled(enabled: Boolean) {
        context.settingsDataStore.edit { preferences ->
            preferences[Keys.notificationsEnabled] = enabled
        }
    }
}
