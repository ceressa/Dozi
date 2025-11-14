package com.bardino.dozi.core.ui.components

import androidx.compose.foundation.layout.size
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.bardino.dozi.core.ui.theme.*
import com.google.firebase.auth.FirebaseAuth

sealed class BottomNavItem(
    val route: String,
    val icon: ImageVector,
    val label: String
) {
    object Home : BottomNavItem("home", Icons.Default.Home, "Ana Sayfa")
    object Medicines : BottomNavItem("medicine_list", Icons.Default.MedicalServices, "İlaçlarım")
    object Reminders : BottomNavItem("reminder_list", Icons.Default.Notifications, "Hatırlatmalar")
    object Buddies : BottomNavItem("buddy_list", Icons.Default.People, "Buddy")

    // Dinamik - auth durumuna göre değişir
    class ProfileOrLogin(isLoggedIn: Boolean) : BottomNavItem(
        route = "profile",
        icon = if (isLoggedIn) Icons.Default.Person else Icons.Default.Login,
        label = if (isLoggedIn) "Profil" else "Giriş"
    )
}

@Composable
fun DoziBottomBar(
    currentRoute: String?,
    onNavigate: (String) -> Unit,
    onLoginRequired: () -> Unit = {}
) {
    // Firebase Auth durumunu gerçek zamanlı kontrol et
    val auth = remember { FirebaseAuth.getInstance() }
    var isLoggedIn by remember { mutableStateOf(auth.currentUser != null) }

    // Auth durumu değiştiğinde otomatik güncelle
    DisposableEffect(Unit) {
        val listener = FirebaseAuth.AuthStateListener { firebaseAuth ->
            isLoggedIn = firebaseAuth.currentUser != null
        }
        auth.addAuthStateListener(listener)
        onDispose { auth.removeAuthStateListener(listener) }
    }

    val items = listOf(
        BottomNavItem.Home,
        BottomNavItem.Medicines,
        BottomNavItem.Reminders,
        BottomNavItem.Buddies,
        BottomNavItem.ProfileOrLogin(isLoggedIn)
    )

    NavigationBar(
        containerColor = Color(0xFF1A237E), // Koyu mavi - kontrast
        contentColor = Color.White,
        tonalElevation = 8.dp
    ) {
        items.forEach { item ->
            val selected = currentRoute == item.route

            // İlaçlarım, Hatırlatmalar ve Buddy için login kontrolü
            val requiresLogin = (item is BottomNavItem.Medicines || item is BottomNavItem.Reminders || item is BottomNavItem.Buddies)

            NavigationBarItem(
                icon = {
                    Icon(
                        item.icon,
                        contentDescription = item.label,
                        modifier = Modifier.size(if (selected) 24.dp else 22.dp)
                    )
                },
                label = {
                    Text(
                        item.label,
                        style = MaterialTheme.typography.labelSmall.copy(
                            fontSize = 11.sp,
                            lineHeight = 12.sp
                        ),
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                },
                selected = selected,
                onClick = {
                    if (currentRoute != item.route) {
                        // Login gerektiren özelliklere tıklandıysa ve login değilse
                        if (requiresLogin && !isLoggedIn) {
                            onLoginRequired()
                        } else {
                            onNavigate(item.route)
                        }
                    }
                },
                colors = NavigationBarItemDefaults.colors(
                    selectedIconColor = DoziTurquoise,
                    selectedTextColor = DoziTurquoise,
                    unselectedIconColor = if (requiresLogin && !isLoggedIn) {
                        Color.White.copy(alpha = 0.4f)
                    } else {
                        Color.White.copy(alpha = 0.6f)
                    },
                    unselectedTextColor = if (requiresLogin && !isLoggedIn) {
                        Color.White.copy(alpha = 0.4f)
                    } else {
                        Color.White.copy(alpha = 0.6f)
                    },
                    indicatorColor = Color.White.copy(alpha = 0.15f)
                )
            )
        }
    }
}