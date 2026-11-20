package io.github.dijksys.quipu.core.installation

import android.content.Context
import android.content.pm.PackageManager
import android.os.Build
import dagger.hilt.android.qualifiers.ApplicationContext
import io.github.dijksys.quipu.core.database.AppDatabase
import io.github.dijksys.quipu.core.datastore.AppSettingsDataStore
import io.github.dijksys.quipu.domain.installation.InstallationHistory
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class AndroidInstallationHistory @Inject constructor(
    @param:ApplicationContext private val context: Context,
    private val settingsDataStore: AppSettingsDataStore,
) : InstallationHistory {

    override fun hadPriorInstallation(): Boolean {
        if (settingsDataStore.hadStoredPreferencesFile) return true
        if (context.getDatabasePath(AppDatabase.DATABASE_NAME).exists()) return true

        val packageInfo = runCatching {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                context.packageManager.getPackageInfo(
                    context.packageName,
                    PackageManager.PackageInfoFlags.of(0),
                )
            } else {
                @Suppress("DEPRECATION")
                context.packageManager.getPackageInfo(context.packageName, 0)
            }
        }.getOrNull() ?: return false

        return InstallationEvidence.indicatesPriorInstallation(
            hasStoredPreferences = false,
            hasDatabase = false,
            firstInstallTime = packageInfo.firstInstallTime,
            lastUpdateTime = packageInfo.lastUpdateTime,
        )
    }
}
