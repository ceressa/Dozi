package com.bardino.dozi.core.ui.components

import androidx.compose.foundation.layout.size
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.unit.dp
import com.bardino.dozi.core.ui.theme.*

sealed class BottomNavItem(
    val route: String,
    val icon: ImageVector,
    val label: String
) {
    object Home : BottomNavItem("home", Icons.Default.Home, "Ana Sayfa")
    object Medicines : BottomNavItem("medicine_list", Icons.Default.MedicalServices, "İlaçlarım")
    object Reminders : BottomNavItem("reminder_list", Icons.Default.Notifications, "Hatırlatmalar")
    object Profile : BottomNavItem("profile", Icons.Default.Person, "Profil")
}

@Composable
fun DoziBottomBar(
    currentRoute: String?,
    onNavigate: (String) -> Unit
) {
    val items = listOf(
        BottomNavItem.Home,
        BottomNavItem.Medicines,
        BottomNavItem.Reminders,
        BottomNavItem.Profile
    )

    NavigationBar(
        containerColor = Color(0xFF1A237E), // Koyu mavi - kontrast
        contentColor = Color.White,
        tonalElevation = 8.dp
    ) {
        items.forEach { item ->
            val selected = currentRoute == item.route

            NavigationBarItem(
                icon = {
                    Icon(
                        item.icon,
                        contentDescription = item.label,
                        modifier = Modifier.size(if (selected) 28.dp else 24.dp)
                    )
                },
                label = {
                    Text(
                        item.label,
                        style = MaterialTheme.typography.labelSmall
                    )
                },
                selected = selected,
                onClick = {
                    if (currentRoute != item.route) {
                        onNavigate(item.route)
                    }
                },
                colors = NavigationBarItemDefaults.colors(
                    selectedIconColor = DoziTurquoise,
                    selectedTextColor = DoziTurquoise,
                    unselectedIconColor = Color.White.copy(alpha = 0.6f),
                    unselectedTextColor = Color.White.copy(alpha = 0.6f),
                    indicatorColor = Color.White.copy(alpha = 0.15f)
                )
            )
        }
    }
}