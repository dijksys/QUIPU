package io.github.dijksys.quipu.feature.settings

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalUriHandler
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import io.github.dijksys.quipu.R
import io.github.dijksys.quipu.core.designsystem.components.QuipuSectionHeader
import io.github.dijksys.quipu.core.designsystem.components.QuipuTopAppBar
import io.github.dijksys.quipu.core.designsystem.theme.QuipuSpacing
import io.github.dijksys.quipu.core.permissions.rememberExactAlarmPermissionController
import io.github.dijksys.quipu.core.permissions.rememberNotificationPermissionController
import kotlinx.coroutines.launch

@Composable
fun SettingsScreen(
    viewModel: SettingsViewModel = hiltViewModel(),
) {
    val state by viewModel.uiState.collectAsStateWithLifecycle()
    val snackbarHostState = remember { SnackbarHostState() }
    val notificationPermission = rememberNotificationPermissionController()
    val exactAlarmPermission = rememberExactAlarmPermissionController()
    val messageError = stringResource(R.string.error_generic)
    val uriHandler = LocalUriHandler.current
    val scope = rememberCoroutineScope()
    val linkOpenError = stringResource(R.string.settings_project_open_error)

    val openLink: (String) -> Unit = { url ->
        runCatching { uriHandler.openUri(url) }
            .onFailure {
                scope.launch {
                    snackbarHostState.showSnackbar(linkOpenError)
                }
            }
    }

    LaunchedEffect(viewModel) {
        viewModel.events.collect { event ->
            when (event) {
                SettingsEvent.Failure -> snackbarHostState.showSnackbar(messageError)
            }
        }
    }

    val notificationPermissionState = if (notificationPermission.granted) {
        PermissionState.GRANTED
    } else {
        PermissionState.MISSING
    }
    val exactAlarmPermissionState = when {
        !exactAlarmPermission.isSupported -> PermissionState.NOT_REQUIRED
        exactAlarmPermission.granted -> PermissionState.GRANTED
        else -> PermissionState.MISSING
    }

    Scaffold(
        containerColor = MaterialTheme.colorScheme.background,
        snackbarHost = { SnackbarHost(snackbarHostState) },
        topBar = { QuipuTopAppBar(title = stringResource(R.string.settings_title)) },
    ) { innerPadding ->
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding),
            contentPadding = PaddingValues(
                start = QuipuSpacing.screenHorizontal,
                end = QuipuSpacing.screenHorizontal,
                top = QuipuSpacing.xs,
                bottom = QuipuSpacing.xxxl,
            ),
            verticalArrangement = Arrangement.spacedBy(QuipuSpacing.md),
        ) {
            item {
                QuipuSectionHeader(title = stringResource(R.string.settings_appearance_title))
            }
            item {
                ThemeModeSelector(
                    selected = state.themeMode,
                    onSelect = viewModel::setThemeMode,
                )
            }

            item {
                QuipuSectionHeader(title = stringResource(R.string.settings_notifications_title))
            }
            item {
                NotificationToggleCard(
                    title = stringResource(R.string.settings_notifications_enable),
                    description = stringResource(R.string.settings_notifications_enable_hint),
                    checked = state.notificationsEnabled,
                    onCheckedChange = viewModel::setNotificationsEnabled,
                    modifier = Modifier.testTag("settings_notifications_card"),
                )
            }
            item {
                NotificationPermissionCard(
                    title = stringResource(R.string.settings_notification_permission),
                    description = stringResource(
                        R.string.settings_notification_permission_description,
                    ),
                    state = notificationPermissionState,
                    onRequest = notificationPermission.request,
                    onOpenSettings = notificationPermission.openSettings,
                )
            }
            item {
                ExactAlarmPermissionCard(
                    title = stringResource(R.string.settings_exact_alarm_permission),
                    description = stringResource(
                        R.string.settings_exact_alarm_permission_description,
                    ),
                    explanation = stringResource(R.string.settings_exact_alarm_explanation),
                    state = exactAlarmPermissionState,
                    onRequest = exactAlarmPermission.request,
                )
            }

            item {
                QuipuSectionHeader(title = stringResource(R.string.settings_about_title))
            }
            item {
                AboutCard()
            }

            item {
                QuipuSectionHeader(title = stringResource(R.string.settings_project_title))
            }
            item {
                ProjectLinksSection(onOpenLink = openLink)
            }
        }
    }
}
