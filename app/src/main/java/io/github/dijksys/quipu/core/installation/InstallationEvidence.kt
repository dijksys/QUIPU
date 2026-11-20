package io.github.dijksys.quipu.core.installation

internal object InstallationEvidence {

    /**
     * Android fija ambos tiempos al instalar. Una diferencia mayor que este
     * margen indica una actualización del paquete para usuarios que todavía no
     * habían generado datos locales.
     */
    const val UPDATE_TIME_TOLERANCE_MILLIS = 10_000L

    fun indicatesPriorInstallation(
        hasStoredPreferences: Boolean,
        hasDatabase: Boolean,
        firstInstallTime: Long,
        lastUpdateTime: Long,
    ): Boolean {
        if (hasStoredPreferences || hasDatabase) return true
        return lastUpdateTime - firstInstallTime > UPDATE_TIME_TOLERANCE_MILLIS
    }
}
