package com.example.quipu.feature.settings

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.selection.selectable
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Alarm
import androidx.compose.material.icons.rounded.DarkMode
import androidx.compose.material.icons.rounded.Info
import androidx.compose.material.icons.rounded.LightMode
import androidx.compose.material.icons.rounded.Notifications
import androidx.compose.material.icons.rounded.Security
import androidx.compose.material.icons.rounded.SettingsBrightness
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.RadioButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Surface
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.quipu.BuildConfig
import com.example.quipu.R
import com.example.quipu.core.designsystem.components.QuipuCard
import com.example.quipu.core.designsystem.components.QuipuInfoPill
import com.example.quipu.core.designsystem.components.QuipuSectionHeader
import com.example.quipu.core.designsystem.components.QuipuSelectableRow
import com.example.quipu.core.designsystem.components.QuipuTextButton
import com.example.quipu.core.designsystem.components.QuipuTopAppBar
import com.example.quipu.core.designsystem.theme.QuipuSpacing
import com.example.quipu.core.permissions.rememberExactAlarmPermissionController
import com.example.quipu.core.permissions.rememberNotificationPermissionController
import com.example.quipu.domain.model.ThemeMode

@Composable
fun SettingsScreen(
    viewModel: SettingsViewModel = hiltViewModel(),
) {
    val state by viewModel.uiState.collectAsStateWithLifecycle()
    val snackbarHostState = remember { SnackbarHostState() }
    val notificationPermission = rememberNotificationPermissionController()
    val exactAlarmPermission = rememberExactAlarmPermissionController()
    val messageError = stringResource(R.string.error_generic)

    LaunchedEffect(viewModel) {
        viewModel.events.collect { event ->
            when (event) {
                SettingsEvent.Failure -> snackbarHostState.showSnackbar(messageError)
            }
        }
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
                QuipuCard(contentPadding = PaddingValues(vertical = QuipuSpacing.sm)) {
                    ThemeOption(
                        title = stringResource(R.string.settings_theme_light),
                        icon = Icons.Rounded.LightMode,
                        selected = state.themeMode == ThemeMode.LIGHT,
                        onClick = { viewModel.setThemeMode(ThemeMode.LIGHT) },
                    )
                    ThemeOption(
                        title = stringResource(R.string.settings_theme_dark),
                        icon = Icons.Rounded.DarkMode,
                        selected = state.themeMode == ThemeMode.DARK,
                        onClick = { viewModel.setThemeMode(ThemeMode.DARK) },
                    )
                    ThemeOption(
                        title = stringResource(R.string.settings_theme_system),
                        icon = Icons.Rounded.SettingsBrightness,
                        selected = state.themeMode == ThemeMode.SYSTEM,
                        onClick = { viewModel.setThemeMode(ThemeMode.SYSTEM) },
                    )
                }
            }

            item {
                QuipuSectionHeader(title = stringResource(R.string.settings_notifications_title))
            }
            item {
                QuipuCard(contentPadding = PaddingValues(QuipuSpacing.lg)) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(
                            imageVector = Icons.Rounded.Notifications,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.primary,
                        )
                        Spacer(Modifier.width(QuipuSpacing.md))
                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                text = stringResource(R.string.settings_notifications_enable),
                                style = MaterialTheme.typography.bodyLarge,
                            )
                            Text(
                                text = stringResource(R.string.settings_notifications_enable_hint),
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                            )
                        }
                        Switch(
                            checked = state.notificationsEnabled,
                            onCheckedChange = viewModel::setNotificationsEnabled,
                        )
                    }

                    HorizontalDivider(
                        modifier = Modifier.padding(vertical = QuipuSpacing.md),
                        color = MaterialTheme.colorScheme.outlineVariant,
                    )

                    PermissionRow(
                        title = stringResource(R.string.settings_notification_permission),
                        granted = notificationPermission.granted,
                        grantedLabel = stringResource(R.string.settings_permission_granted),
                        missingLabel = stringResource(R.string.settings_permission_missing),
                        onRequest = notificationPermission.request,
                        onOpenSettings = notificationPermission.openSettings,
                    )

                    HorizontalDivider(
                        modifier = Modifier.padding(vertical = QuipuSpacing.md),
                        color = MaterialTheme.colorScheme.outlineVariant,
                    )

                    PermissionRow(
                        title = stringResource(R.string.settings_exact_alarm_permission),
                        granted = exactAlarmPermission.granted,
                        grantedLabel = if (exactAlarmPermission.isSupported) {
                            stringResource(R.string.settings_permission_granted)
                        } else {
                            stringResource(R.string.settings_permission_not_needed)
                        },
                        missingLabel = stringResource(R.string.settings_permission_missing),
                        onRequest = exactAlarmPermission.request,
                        onOpenSettings = exactAlarmPermission.request,
                        requestLabel = stringResource(R.string.settings_permission_allow),
                    )

                    Spacer(Modifier.height(QuipuSpacing.md))
                    Text(
                        text = stringResource(R.string.settings_exact_alarm_explanation),
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
            }

            item {
                QuipuSectionHeader(title = stringResource(R.string.settings_about_title))
            }
            item {
                QuipuCard(contentPadding = PaddingValues(QuipuSpacing.lg)) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Surface(
                            shape = CircleShape,
                            color = MaterialTheme.colorScheme.primaryContainer,
                        ) {
                            Icon(
                                imageVector = Icons.Rounded.Info,
                                contentDescription = null,
                                tint = MaterialTheme.colorScheme.primary,
                                modifier = Modifier
                                    .padding(10.dp)
                                    .size(22.dp),
                            )
                        }
                        Spacer(Modifier.width(QuipuSpacing.md))
                        Column {
                            Text(
                                text = stringResource(R.string.app_name),
                                style = MaterialTheme.typography.titleMedium,
                            )
                            Text(
                                text = stringResource(
                                    R.string.settings_version,
                                    BuildConfig.VERSION_NAME,
                                ),
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                            )
                        }
                    }
                    HorizontalDivider(
                        modifier = Modifier.padding(vertical = QuipuSpacing.md),
                        color = MaterialTheme.colorScheme.outlineVariant,
                    )
                    Row {
                        Icon(
                            imageVector = Icons.Rounded.Security,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.onSurfaceVariant,
                            modifier = Modifier.size(18.dp),
                        )
                        Spacer(Modifier.width(QuipuSpacing.sm))
                        Text(
                            text = stringResource(R.string.settings_privacy_message),
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                        )
                    }
                    Spacer(Modifier.height(QuipuSpacing.sm))
                    Row {
                        Icon(
                            imageVector = Icons.Rounded.Alarm,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.onSurfaceVariant,
                            modifier = Modifier.size(18.dp),
                        )
                        Spacer(Modifier.width(QuipuSpacing.sm))
                        Text(
                            text = stringResource(R.string.settings_offline_message),
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun ThemeOption(
    title: String,
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    selected: Boolean,
    onClick: () -> Unit,
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .selectable(selected = selected, onClick = onClick)
            .padding(horizontal = QuipuSpacing.lg, vertical = 10.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Icon(
            imageVector = icon,
            contentDescription = null,
            tint = if (selected) {
                MaterialTheme.colorScheme.primary
            } else {
                MaterialTheme.colorScheme.onSurfaceVariant
            },
            modifier = Modifier.size(21.dp),
        )
        Spacer(Modifier.width(QuipuSpacing.md))
        Text(
            text = title,
            style = MaterialTheme.typography.bodyLarge,
            modifier = Modifier.weight(1f),
        )
        RadioButton(selected = selected, onClick = onClick)
    }
}

@Composable
private fun PermissionRow(
    title: String,
    granted: Boolean,
    grantedLabel: String,
    missingLabel: String,
    onRequest: () -> Unit,
    onOpenSettings: () -> Unit,
    requestLabel: String = stringResource(R.string.settings_permission_request),
) {
    Column {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Text(
                text = title,
                style = MaterialTheme.typography.bodyLarge,
                modifier = Modifier.weight(1f),
            )
            QuipuInfoPill(
                text = if (granted) grantedLabel else missingLabel,
                containerColor = if (granted) {
                    MaterialTheme.colorScheme.primaryContainer
                } else {
                    MaterialTheme.colorScheme.errorContainer
                },
                contentColor = if (granted) {
                    MaterialTheme.colorScheme.onPrimaryContainer
                } else {
                    MaterialTheme.colorScheme.onErrorContainer
                },
            )
        }
        if (!granted) {
            Row(horizontalArrangement = Arrangement.spacedBy(QuipuSpacing.xs)) {
                QuipuTextButton(text = requestLabel, onClick = onRequest)
                QuipuTextButton(
                    text = stringResource(R.string.reminders_open_settings),
                    onClick = onOpenSettings,
                )
            }
        }
    }
}
