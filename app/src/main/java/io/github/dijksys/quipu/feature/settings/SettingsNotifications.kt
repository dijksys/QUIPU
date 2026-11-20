package io.github.dijksys.quipu.feature.settings

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Alarm
import androidx.compose.material.icons.rounded.Notifications
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import io.github.dijksys.quipu.R
import io.github.dijksys.quipu.core.designsystem.components.QuipuCard
import io.github.dijksys.quipu.core.designsystem.components.QuipuInfoPill
import io.github.dijksys.quipu.core.designsystem.components.QuipuTextButton
import io.github.dijksys.quipu.core.designsystem.theme.QuipuSpacing

internal enum class PermissionState {
    GRANTED,
    MISSING,
    NOT_REQUIRED,
}

@Composable
internal fun NotificationToggleCard(
    title: String,
    description: String,
    checked: Boolean,
    onCheckedChange: (Boolean) -> Unit,
    modifier: Modifier = Modifier,
) {
    QuipuCard(
        modifier = modifier.fillMaxWidth(),
        contentPadding = PaddingValues(QuipuSpacing.lg),
    ) {
        BoxWithConstraints(modifier = Modifier.fillMaxWidth()) {
            val compact = maxWidth < 260.dp
            val titleStyle = if (compact) {
                MaterialTheme.typography.titleSmall
            } else {
                MaterialTheme.typography.titleMedium
            }
            Column {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    SettingsIconBadge(icon = Icons.Rounded.Notifications)
                    Spacer(Modifier.width(QuipuSpacing.md))
                    Text(
                        text = title,
                        modifier = Modifier.weight(1f),
                        style = titleStyle,
                        color = MaterialTheme.colorScheme.onSurface,
                    )
                    if (!compact) {
                        Spacer(Modifier.width(QuipuSpacing.sm))
                        Switch(
                            checked = checked,
                            onCheckedChange = onCheckedChange,
                            modifier = Modifier.testTag("settings_notifications_switch"),
                        )
                    }
                }
                if (compact) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.End,
                    ) {
                        Switch(
                            checked = checked,
                            onCheckedChange = onCheckedChange,
                            modifier = Modifier.testTag("settings_notifications_switch"),
                        )
                    }
                }
            }
        }
        Spacer(Modifier.height(QuipuSpacing.sm))
        Text(
            text = description,
            modifier = Modifier.fillMaxWidth(),
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
    }
}

@Composable
internal fun NotificationPermissionCard(
    title: String,
    description: String,
    state: PermissionState,
    onRequest: () -> Unit,
    onOpenSettings: () -> Unit,
    modifier: Modifier = Modifier,
) {
    PermissionSettingCard(
        title = title,
        description = description,
        icon = Icons.Rounded.Notifications,
        state = state,
        requestLabel = stringResource(R.string.settings_permission_allow_notifications),
        onRequest = onRequest,
        openSettingsLabel = stringResource(R.string.settings_permission_open_settings),
        onOpenSettings = onOpenSettings,
        modifier = modifier.testTag("settings_notification_permission_card"),
        requestTag = "settings_notification_permission_request",
        openSettingsTag = "settings_notification_permission_open_settings",
    )
}

@Composable
internal fun ExactAlarmPermissionCard(
    title: String,
    description: String,
    explanation: String,
    state: PermissionState,
    onRequest: () -> Unit,
    modifier: Modifier = Modifier,
) {
    PermissionSettingCard(
        title = title,
        description = description,
        icon = Icons.Rounded.Alarm,
        state = state,
        requestLabel = stringResource(R.string.settings_permission_allow_exact_alarms),
        onRequest = onRequest,
        explanation = explanation,
        modifier = modifier.testTag("settings_exact_alarm_card"),
        requestTag = "settings_exact_alarm_request",
    )
}

@Composable
private fun PermissionSettingCard(
    title: String,
    description: String,
    icon: ImageVector,
    state: PermissionState,
    requestLabel: String,
    onRequest: (() -> Unit)?,
    modifier: Modifier = Modifier,
    openSettingsLabel: String? = null,
    onOpenSettings: (() -> Unit)? = null,
    explanation: String? = null,
    requestTag: String? = null,
    openSettingsTag: String? = null,
) {
    QuipuCard(
        modifier = modifier.fillMaxWidth(),
        contentPadding = PaddingValues(QuipuSpacing.lg),
    ) {
        BoxWithConstraints(modifier = Modifier.fillMaxWidth()) {
            val compact = maxWidth < 260.dp
            val titleStyle = if (compact) {
                MaterialTheme.typography.titleSmall
            } else {
                MaterialTheme.typography.titleMedium
            }
            Column {
                if (compact) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        SettingsIconBadge(icon = icon)
                        Spacer(Modifier.width(QuipuSpacing.md))
                        Text(
                            text = title,
                            style = titleStyle,
                            color = MaterialTheme.colorScheme.onSurface,
                            modifier = Modifier.weight(1f),
                        )
                    }
                    Spacer(Modifier.height(QuipuSpacing.xs))
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.End,
                    ) {
                        NotificationStatusBadge(state = state)
                    }
                } else {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        SettingsIconBadge(icon = icon)
                        Spacer(Modifier.width(QuipuSpacing.md))
                        Text(
                            text = title,
                            style = titleStyle,
                            color = MaterialTheme.colorScheme.onSurface,
                            modifier = Modifier.weight(1f),
                        )
                        Spacer(Modifier.width(QuipuSpacing.sm))
                        NotificationStatusBadge(state = state)
                    }
                }
            }
        }

        Spacer(Modifier.height(QuipuSpacing.sm))
        Text(
            text = description,
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )

        if (explanation != null) {
            Spacer(Modifier.height(QuipuSpacing.sm))
            Text(
                text = explanation,
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }

        if (state == PermissionState.MISSING) {
            FlowRow(
                modifier = Modifier.padding(top = QuipuSpacing.xs),
                horizontalArrangement = Arrangement.spacedBy(QuipuSpacing.xs),
            ) {
                if (onRequest != null) {
                    QuipuTextButton(
                        text = requestLabel,
                        onClick = onRequest,
                        modifier = requestTag?.let { Modifier.testTag(it) } ?: Modifier,
                    )
                }
                if (openSettingsLabel != null && onOpenSettings != null) {
                    QuipuTextButton(
                        text = openSettingsLabel,
                        onClick = onOpenSettings,
                        modifier = openSettingsTag?.let { Modifier.testTag(it) } ?: Modifier,
                    )
                }
            }
        }
    }
}

@Composable
private fun NotificationStatusBadge(state: PermissionState) {
    val label = when (state) {
        PermissionState.GRANTED -> stringResource(R.string.settings_permission_granted)
        PermissionState.MISSING -> stringResource(R.string.settings_permission_missing)
        PermissionState.NOT_REQUIRED -> stringResource(R.string.settings_permission_not_needed)
    }
    val containerColor: Color
    val contentColor: Color
    when (state) {
        PermissionState.GRANTED -> {
            containerColor = MaterialTheme.colorScheme.primaryContainer
            contentColor = MaterialTheme.colorScheme.onPrimaryContainer
        }

        PermissionState.MISSING -> {
            containerColor = MaterialTheme.colorScheme.errorContainer
            contentColor = MaterialTheme.colorScheme.onErrorContainer
        }

        PermissionState.NOT_REQUIRED -> {
            containerColor = MaterialTheme.colorScheme.surfaceContainerHigh
            contentColor = MaterialTheme.colorScheme.onSurfaceVariant
        }
    }
    QuipuInfoPill(
        text = label,
        containerColor = containerColor,
        contentColor = contentColor,
    )
}
