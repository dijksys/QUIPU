package com.example.quipu.core.navigation

import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Description
import androidx.compose.material.icons.rounded.Home
import androidx.compose.material.icons.rounded.Notifications
import androidx.compose.material.icons.rounded.Settings
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.NavigationBarItemDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import com.example.quipu.R

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

@Composable
fun AppBottomBar(
    currentRoute: String?,
    onNavigate: (String) -> Unit,
) {
    NavigationBar(
        containerColor = MaterialTheme.colorScheme.surface,
        tonalElevation = 0.dp,
    ) {
        bottomDestinations.forEach { destination ->
            val selected = currentRoute == destination.selectionRoute
            NavigationBarItem(
                selected = selected,
                onClick = { onNavigate(destination.route) },
                icon = {
                    Icon(imageVector = destination.icon, contentDescription = null)
                },
                label = {
                    Text(
                        text = stringResource(destination.labelRes),
                        style = MaterialTheme.typography.labelSmall,
                    )
                },
                colors = NavigationBarItemDefaults.colors(
                    selectedIconColor = MaterialTheme.colorScheme.onPrimaryContainer,
                    selectedTextColor = MaterialTheme.colorScheme.primary,
                    indicatorColor = MaterialTheme.colorScheme.primaryContainer,
                    unselectedIconColor = MaterialTheme.colorScheme.onSurfaceVariant,
                    unselectedTextColor = MaterialTheme.colorScheme.onSurfaceVariant,
                ),
            )
        }
    }
}
