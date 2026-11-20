package io.github.dijksys.quipu.core.datastore

import android.content.Context
import androidx.datastore.core.handlers.ReplaceFileCorruptionHandler
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.emptyPreferences
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import io.github.dijksys.quipu.domain.model.AppSettings
import io.github.dijksys.quipu.domain.model.ThemeMode
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.map
import java.io.File
import java.io.IOException
import javax.inject.Inject
import javax.inject.Singleton

private const val SETTINGS_DATASTORE_NAME = "quipu_settings"
private const val SETTINGS_PREFERENCES_FILE = "$SETTINGS_DATASTORE_NAME.preferences_pb"

private val Context.settingsDataStore by preferencesDataStore(
    name = SETTINGS_DATASTORE_NAME,
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
    /**
     * Se captura antes de que el flujo abra el archivo por primera vez. Así una
     * instalación nueva no se confunde con una actualización solo porque
     * DataStore haya creado el archivo durante este arranque.
     */
    internal val hadStoredPreferencesFile: Boolean =
        File(context.filesDir, "datastore/$SETTINGS_PREFERENCES_FILE").exists()

    private object Keys {
        val themeMode = stringPreferencesKey("theme_mode")
        val notificationsEnabled = booleanPreferencesKey("notifications_enabled")
        val onboardingCompleted = booleanPreferencesKey("onboarding_completed")
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
                onboardingCompleted = preferences[Keys.onboardingCompleted],
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

    suspend fun setOnboardingCompleted(completed: Boolean) {
        context.settingsDataStore.edit { preferences ->
            preferences[Keys.onboardingCompleted] = completed
        }
    }

    /**
     * Persiste la decisión inicial solo si la clave sigue ausente. Devuelve el
     * valor realmente almacenado para resolver posibles carreras.
     */
    suspend fun initializeOnboardingCompleted(completed: Boolean): Boolean {
        val preferences = context.settingsDataStore.edit { current ->
            if (current[Keys.onboardingCompleted] == null) {
                current[Keys.onboardingCompleted] = completed
            }
        }
        return preferences[Keys.onboardingCompleted] ?: completed
    }
}
