package io.github.dijksys.quipu.fakes

import io.github.dijksys.quipu.domain.installation.InstallationHistory

class FakeInstallationHistory(
    var priorInstallation: Boolean = false,
) : InstallationHistory {

    override fun hadPriorInstallation(): Boolean = priorInstallation
}
