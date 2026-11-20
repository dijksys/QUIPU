package io.github.dijksys.quipu.feature.settings

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Image
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.OpenInNew
import androidx.compose.material.icons.rounded.Code
import androidx.compose.material.icons.rounded.Person
import androidx.compose.material.icons.rounded.Security
import androidx.compose.material.icons.rounded.WifiOff
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.unit.dp
import io.github.dijksys.quipu.BuildConfig
import io.github.dijksys.quipu.R
import io.github.dijksys.quipu.core.designsystem.components.QuipuCard
import io.github.dijksys.quipu.core.designsystem.theme.QuipuSpacing

internal const val DEVELOPER_PROFILE_URL = "https://github.com/dijksys"
internal const val REPOSITORY_URL = "https://github.com/dijksys/QUIPU"

@Composable
internal fun AboutCard(modifier: Modifier = Modifier) {
    QuipuCard(
        modifier = modifier
            .fillMaxWidth()
            .testTag("settings_about_card"),
        contentPadding = PaddingValues(QuipuSpacing.xl),
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Image(
                painter = painterResource(R.drawable.quipu_logo),
                contentDescription = stringResource(R.string.settings_about_logo_description),
                modifier = Modifier
                    .size(52.dp)
                    .testTag("settings_about_logo"),
                contentScale = ContentScale.Fit,
            )
            Spacer(Modifier.width(QuipuSpacing.md))
            Column {
                Text(
                    text = stringResource(R.string.app_name),
                    style = MaterialTheme.typography.titleLarge,
                    color = MaterialTheme.colorScheme.onSurface,
                )
                Text(
                    text = stringResource(
                        R.string.settings_version,
                        BuildConfig.VERSION_NAME,
                    ),
                    modifier = Modifier.testTag("settings_about_version"),
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
        }

        HorizontalDivider(
            modifier = Modifier.padding(vertical = QuipuSpacing.lg),
            color = MaterialTheme.colorScheme.outlineVariant,
        )

        AboutInfoRow(
            icon = Icons.Rounded.Security,
            title = stringResource(R.string.settings_about_privacy_title),
            message = stringResource(R.string.settings_privacy_message),
        )
        Spacer(Modifier.size(QuipuSpacing.md))
        AboutInfoRow(
            icon = Icons.Rounded.WifiOff,
            title = stringResource(R.string.settings_about_offline_title),
            message = stringResource(R.string.settings_offline_message),
        )
    }
}

@Composable
private fun AboutInfoRow(
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    title: String,
    message: String,
) {
    Row(verticalAlignment = Alignment.Top) {
        Icon(
            imageVector = icon,
            contentDescription = null,
            tint = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.size(18.dp),
        )
        Spacer(Modifier.width(QuipuSpacing.sm))
        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = title,
                style = MaterialTheme.typography.titleSmall,
                color = MaterialTheme.colorScheme.onSurface,
            )
            Spacer(Modifier.size(QuipuSpacing.xxs))
            Text(
                text = message,
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
    }
}

@Composable
internal fun ProjectLinksSection(
    onOpenLink: (String) -> Unit,
    modifier: Modifier = Modifier,
) {
    Column(
        modifier = modifier.fillMaxWidth(),
        verticalArrangement = Arrangement.spacedBy(QuipuSpacing.md),
    ) {
        ProjectLinkCard(
            title = stringResource(R.string.settings_project_profile_title),
            description = stringResource(R.string.settings_project_profile_description),
            icon = Icons.Rounded.Person,
            onClick = { onOpenLink(DEVELOPER_PROFILE_URL) },
            modifier = Modifier.testTag("settings_project_profile"),
        )
        ProjectLinkCard(
            title = stringResource(R.string.settings_project_repository_title),
            description = stringResource(R.string.settings_project_repository_description),
            icon = Icons.Rounded.Code,
            onClick = { onOpenLink(REPOSITORY_URL) },
            modifier = Modifier.testTag("settings_project_repository"),
        )
    }
}

@Composable
private fun ProjectLinkCard(
    title: String,
    description: String,
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val openLinkLabel = stringResource(R.string.settings_project_external_link)
    Surface(
        modifier = modifier
            .fillMaxWidth()
            .clickable(
                role = Role.Button,
                onClickLabel = openLinkLabel,
                onClick = onClick,
            ),
        shape = RoundedCornerShape(22.dp),
        color = MaterialTheme.colorScheme.surface,
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant),
    ) {
        Row(
            modifier = Modifier.padding(horizontal = QuipuSpacing.lg, vertical = 15.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            SettingsIconBadge(
                icon = icon,
                containerColor = MaterialTheme.colorScheme.secondaryContainer.copy(alpha = 0.6f),
                contentColor = MaterialTheme.colorScheme.onSecondaryContainer,
            )
            Spacer(Modifier.width(QuipuSpacing.md))
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = title,
                    style = MaterialTheme.typography.bodyLarge,
                    color = MaterialTheme.colorScheme.onSurface,
                )
                Text(
                    text = description,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
            Spacer(Modifier.width(QuipuSpacing.sm))
            Icon(
                imageVector = Icons.AutoMirrored.Rounded.OpenInNew,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.size(16.dp),
            )
        }
    }
}
