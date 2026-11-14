package com.example.multipromptchat.ui.navigation

import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Face
import androidx.compose.material.icons.filled.MailOutline
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.NavigationBarItemDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.unit.dp
import com.example.multipromptchat.ui.Strings

private data class BottomTab(
    val route: String,
    val label: String,
    val icon: ImageVector,
)

private val bottomTabs = listOf(
    BottomTab(Routes.CONVERSATIONS, Strings.tabConversations, Icons.Filled.MailOutline),
    BottomTab(Routes.PROMPTS, Strings.tabPrompts, Icons.Filled.Face),
    BottomTab(Routes.SETTINGS, Strings.tabSettings, Icons.Filled.Settings),
)

val bottomTabRoutes: List<String> = bottomTabs.map { it.route }

@Composable
fun BottomBar(
    currentRoute: String?,
    onSelect: (String) -> Unit,
) {
    NavigationBar(
        containerColor = MaterialTheme.colorScheme.surface,
        tonalElevation = 0.dp,
    ) {
        bottomTabs.forEach { tab ->
            NavigationBarItem(
                selected = currentRoute == tab.route,
                onClick = { onSelect(tab.route) },
                icon = {
                    Icon(imageVector = tab.icon, contentDescription = tab.label)
                },
                label = {
                    Text(text = tab.label, style = MaterialTheme.typography.labelSmall)
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
