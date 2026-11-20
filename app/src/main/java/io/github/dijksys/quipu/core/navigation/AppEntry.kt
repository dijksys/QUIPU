package io.github.dijksys.quipu.core.navigation

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.safeDrawing
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import io.github.dijksys.quipu.R
import io.github.dijksys.quipu.feature.onboarding.WelcomeScreen
import io.github.dijksys.quipu.feature.onboarding.WelcomeStatus
import io.github.dijksys.quipu.feature.onboarding.WelcomeViewModel

/**
 * Decide entre la bienvenida de primera ejecución y la navegación principal.
 * El NavHost principal no se compone hasta que el onboarding está resuelto.
 */
@Composable
fun AppEntry(
    pendingReminderId: String?,
    onPendingReminderConsumed: () -> Unit,
    viewModel: WelcomeViewModel = hiltViewModel(),
) {
    val state by viewModel.uiState.collectAsStateWithLifecycle()

    when (state.status) {
        WelcomeStatus.COMPLETED -> AppNavHost(
            pendingReminderId = pendingReminderId,
            onPendingReminderConsumed = onPendingReminderConsumed,
        )

        WelcomeStatus.LOADING -> WelcomeLoadingSurface()

        else -> WelcomeScreen(
            isCompleting = state.status == WelcomeStatus.COMPLETING,
            error = state.error,
            onStart = viewModel::onContinue,
        )
    }
}

@Composable
private fun WelcomeLoadingSurface() {
    Surface(
        modifier = Modifier.fillMaxSize(),
        color = MaterialTheme.colorScheme.background,
    ) {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .windowInsetsPadding(WindowInsets.safeDrawing)
                .testTag("welcome_loading"),
            contentAlignment = Alignment.Center,
        ) {
            val loadingLabel = stringResource(R.string.welcome_loading)
            CircularProgressIndicator(
                modifier = Modifier.semantics {
                    contentDescription = loadingLabel
                },
                color = MaterialTheme.colorScheme.primary,
            )
        }
    }
}
