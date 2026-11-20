package io.github.dijksys.quipu.core.installation

import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class InstallationEvidenceTest {

    @Test
    fun `fresh install without local data is new`() {
        assertFalse(
            InstallationEvidence.indicatesPriorInstallation(
                hasStoredPreferences = false,
                hasDatabase = false,
                firstInstallTime = 1_000_000,
                lastUpdateTime = 1_000_000,
            ),
        )
    }

    @Test
    fun `small timestamp differences still count as fresh install`() {
        assertFalse(
            InstallationEvidence.indicatesPriorInstallation(
                hasStoredPreferences = false,
                hasDatabase = false,
                firstInstallTime = 1_000_000,
                lastUpdateTime = 1_005_000,
            ),
        )
    }

    @Test
    fun `package updated long after installation is treated as existing`() {
        assertTrue(
            InstallationEvidence.indicatesPriorInstallation(
                hasStoredPreferences = false,
                hasDatabase = false,
                firstInstallTime = 1_000_000,
                lastUpdateTime = 1_020_000,
            ),
        )
    }

    @Test
    fun `existing preferences indicate a previous installation`() {
        assertTrue(
            InstallationEvidence.indicatesPriorInstallation(
                hasStoredPreferences = true,
                hasDatabase = false,
                firstInstallTime = 1_000_000,
                lastUpdateTime = 1_000_000,
            ),
        )
    }

    @Test
    fun `existing database indicates a previous installation`() {
        assertTrue(
            InstallationEvidence.indicatesPriorInstallation(
                hasStoredPreferences = false,
                hasDatabase = true,
                firstInstallTime = 1_000_000,
                lastUpdateTime = 1_000_000,
            ),
        )
    }
}
