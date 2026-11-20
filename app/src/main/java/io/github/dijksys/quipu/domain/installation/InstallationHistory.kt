package io.github.dijksys.quipu.domain.installation

/**
 * Evidencia de una instalación previa de QUIPU. Se mantiene detrás de una
 * abstracción para poder probar la compatibilidad del onboarding sin depender
 * de PackageManager ni del sistema de archivos.
 */
interface InstallationHistory {
    fun hadPriorInstallation(): Boolean
}
