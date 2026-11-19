package io.github.dijksys.quipu.core.navigation

import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.selection.selectable
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.TextAutoSize
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Description
import androidx.compose.material.icons.rounded.Home
import androidx.compose.material.icons.rounded.Notifications
import androidx.compose.material.icons.rounded.Settings
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import io.github.dijksys.quipu.R

private data class BottomDestination(
    val route: String,
    val labelRes: Int,
    val icon: ImageVector,
    val selectionRoute: String = route,
)

private val bottomDestinations = listOf(
    BottomDestination(AppDestination.HOME, R.string.nav_home, Icons.Rounded.Home),
    BottomDestination(
        AppDestination.notes(),
        R.string.nav_notes,
        Icons.Rounded.Description,
        selectionRoute = AppDestination.NOTES,
    ),
    BottomDestination(
        AppDestination.REMINDERS,
        R.string.nav_reminders,
        Icons.Rounded.Notifications,
    ),
    BottomDestination(AppDestination.SETTINGS, R.string.nav_settings, Icons.Rounded.Settings),
)

private val CapsuleShape = RoundedCornerShape(percent = 50)
private val ItemShape = RoundedCornerShape(24.dp)
private val IndicatorWidth = 42.dp
private val IndicatorHeight = 32.dp
private val BarMinHeight = 84.dp
private val ItemMinHeight = 64.dp

@Composable
fun AppBottomBar(
    currentRoute: String?,
    onNavigate: (String) -> Unit,
) {
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .navigationBarsPadding()
            .padding(
                start = 16.dp,
                end = 16.dp,
                top = 4.dp,
                bottom = 10.dp,
            ),
        contentAlignment = Alignment.Center,
    ) {
        Surface(
            modifier = Modifier
                .widthIn(max = 560.dp)
                .fillMaxWidth()
                .heightIn(min = BarMinHeight),
            shape = CapsuleShape,
            color = MaterialTheme.colorScheme.surface,
            contentColor = MaterialTheme.colorScheme.onSurface,
            border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant),
            tonalElevation = 0.dp,
            shadowElevation = 5.dp,
        ) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 6.dp, vertical = 6.dp),
                horizontalArrangement = Arrangement.spacedBy(2.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                bottomDestinations.forEach { destination ->
                    val selected = currentRoute == destination.selectionRoute
                    CapsuleNavigationItem(
                        labelRes = destination.labelRes,
                        icon = destination.icon,
                        selected = selected,
                        onClick = { onNavigate(destination.route) },
                        modifier = Modifier.weight(1f),
                    )
                }
            }
        }
    }
}

@Composable
private fun CapsuleNavigationItem(
    labelRes: Int,
    icon: ImageVector,
    selected: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val indicatorColor by animateColorAsState(
        targetValue = if (selected) {
            MaterialTheme.colorScheme.primaryContainer
        } else {
            Color.Transparent
        },
        animationSpec = tween(durationMillis = 180),
        label = "navigation-indicator-color",
    )
    val iconColor by animateColorAsState(
        targetValue = if (selected) {
            MaterialTheme.colorScheme.onPrimaryContainer
        } else {
            MaterialTheme.colorScheme.onSurfaceVariant
        },
        animationSpec = tween(durationMillis = 180),
        label = "navigation-icon-color",
    )
    val labelColor by animateColorAsState(
        targetValue = if (selected) {
            MaterialTheme.colorScheme.primary
        } else {
            MaterialTheme.colorScheme.onSurfaceVariant
        },
        animationSpec = tween(durationMillis = 180),
        label = "navigation-label-color",
    )
    val label = stringResource(labelRes)

    Column(
        modifier = modifier
            .heightIn(min = ItemMinHeight)
            .clip(ItemShape)
            .selectable(
                selected = selected,
                onClick = onClick,
                role = Role.Tab,
            )
            .padding(horizontal = 2.dp, vertical = 4.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center,
    ) {
        Box(
            modifier = Modifier
                .size(width = IndicatorWidth, height = IndicatorHeight)
                .clip(CircleShape)
                .background(indicatorColor),
            contentAlignment = Alignment.Center,
        ) {
            Icon(
                imageVector = icon,
                contentDescription = null,
                modifier = Modifier.size(22.dp),
                tint = iconColor,
            )
        }
        Spacer(Modifier.height(2.dp))
        Text(
            text = label,
            modifier = Modifier.fillMaxWidth(),
            color = labelColor,
            style = MaterialTheme.typography.labelSmall,
            autoSize = TextAutoSize.StepBased(
                minFontSize = 7.sp,
                maxFontSize = 11.sp,
                stepSize = 0.25.sp,
            ),
            fontSize = 11.sp,
            lineHeight = 13.sp,
            letterSpacing = 0.sp,
            fontWeight = FontWeight.Medium,
            textAlign = TextAlign.Center,
            maxLines = 1,
            softWrap = false,
        )
    }
}
