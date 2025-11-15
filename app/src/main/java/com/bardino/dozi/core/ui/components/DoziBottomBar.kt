package com.bardino.dozi.core.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
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
    object More : BottomNavItem("more", Icons.Default.MoreHoriz, "Daha Fazla")
}

sealed class MoreMenuItem(
    val route: String,
    val icon: ImageVector,
    val label: String,
    val description: String
) {
    object Stats : MoreMenuItem("stats", Icons.Default.BarChart, "İstatistikler", "İlaç takip istatistikleri")
    object Badis : MoreMenuItem("badi_list", Icons.Default.People, "Badi", "Arkadaşlarınla birlikte takip et")

    class ProfileOrLogin(isLoggedIn: Boolean) : MoreMenuItem(
        route = "profile",
        icon = if (isLoggedIn) Icons.Default.Person else Icons.Default.Login,
        label = if (isLoggedIn) "Profil" else "Giriş",
        description = if (isLoggedIn) "Profil ayarları" else "Giriş yap veya kayıt ol"
    )
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun DoziBottomBar(
    currentRoute: String?,
    onNavigate: (String) -> Unit,
    onLoginRequired: () -> Unit = {}
) {
    val auth = remember { FirebaseAuth.getInstance() }
    var isLoggedIn by remember { mutableStateOf(auth.currentUser != null) }
    var showMoreMenu by remember { mutableStateOf(false) }

    // Auth durumu değiştiğinde otomatik güncelle
    DisposableEffect(Unit) {
        val listener = FirebaseAuth.AuthStateListener { firebaseAuth ->
            isLoggedIn = firebaseAuth.currentUser != null
        }
        auth.addAuthStateListener(listener)
        onDispose { auth.removeAuthStateListener(listener) }
    }

    val mainItems = listOf(
        BottomNavItem.Home,
        BottomNavItem.Medicines,
        BottomNavItem.Reminders,
        BottomNavItem.More
    )

    Box {
        NavigationBar(
            containerColor = Color(0xFF1A237E),
            contentColor = Color.White,
            tonalElevation = 8.dp
        ) {
            mainItems.forEach { item ->
                val selected = when (item) {
                    is BottomNavItem.More -> currentRoute in listOf("stats", "badi_list", "profile")
                    else -> currentRoute == item.route
                }

                val requiresLogin = item is BottomNavItem.Medicines || item is BottomNavItem.Reminders

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
                        when (item) {
                            is BottomNavItem.More -> {
                                showMoreMenu = true
                            }
                            else -> {
                                if (currentRoute != item.route) {
                                    if (requiresLogin && !isLoggedIn) {
                                        onLoginRequired()
                                    } else {
                                        onNavigate(item.route)
                                    }
                                }
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

        // More Menu BottomSheet
        if (showMoreMenu) {
            MoreMenuBottomSheet(
                isLoggedIn = isLoggedIn,
                currentRoute = currentRoute,
                onDismiss = { showMoreMenu = false },
                onNavigate = { route ->
                    showMoreMenu = false
                    onNavigate(route)
                },
                onLoginRequired = {
                    showMoreMenu = false
                    onLoginRequired()
                }
            )
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun MoreMenuBottomSheet(
    isLoggedIn: Boolean,
    currentRoute: String?,
    onDismiss: () -> Unit,
    onNavigate: (String) -> Unit,
    onLoginRequired: () -> Unit
) {
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)

    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = sheetState,
        containerColor = Color(0xFFF8F9FA),
        tonalElevation = 0.dp
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(bottom = 32.dp)
        ) {
            // Gradient başlık alanı
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(
                        androidx.compose.ui.graphics.Brush.horizontalGradient(
                            listOf(DoziTurquoise, DoziPurple)
                        )
                    )
                    .padding(horizontal = 24.dp, vertical = 20.dp)
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    Icon(
                        Icons.Default.MoreHoriz,
                        contentDescription = null,
                        tint = Color.White,
                        modifier = Modifier.size(28.dp)
                    )
                    Text(
                        text = "Daha Fazla",
                        style = MaterialTheme.typography.titleLarge.copy(
                            fontWeight = FontWeight.ExtraBold,
                            fontSize = 24.sp
                        ),
                        color = Color.White
                    )
                }
            }

            Spacer(modifier = Modifier.height(16.dp))

            val menuItems = listOf(
                MoreMenuItem.Stats,
                MoreMenuItem.Badis,
                MoreMenuItem.ProfileOrLogin(isLoggedIn)
            )

            menuItems.forEach { item ->
                val requiresLogin = item is MoreMenuItem.Badis
                val isSelected = currentRoute == item.route

                MoreMenuItemCard(
                    item = item,
                    isSelected = isSelected,
                    isEnabled = !requiresLogin || isLoggedIn,
                    onClick = {
                        if (requiresLogin && !isLoggedIn) {
                            onLoginRequired()
                        } else {
                            onNavigate(item.route)
                        }
                    }
                )
            }
        }
    }
}

@Composable
private fun MoreMenuItemCard(
    item: MoreMenuItem,
    isSelected: Boolean,
    isEnabled: Boolean,
    onClick: () -> Unit
) {
    androidx.compose.material3.Card(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 6.dp),
        shape = RoundedCornerShape(16.dp),
        colors = androidx.compose.material3.CardDefaults.cardColors(
            containerColor = if (isSelected)
                androidx.compose.ui.graphics.Brush.horizontalGradient(
                    listOf(DoziTurquoise.copy(alpha = 0.15f), DoziPurple.copy(alpha = 0.15f))
                ).let { Color.White }
            else Color.White
        ),
        elevation = androidx.compose.material3.CardDefaults.cardElevation(
            defaultElevation = if (isSelected) 6.dp else 2.dp
        ),
        onClick = { if (isEnabled) onClick() }
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .background(
                    if (isSelected) androidx.compose.ui.graphics.Brush.horizontalGradient(
                        listOf(DoziTurquoise.copy(alpha = 0.12f), DoziPurple.copy(alpha = 0.12f))
                    ) else androidx.compose.ui.graphics.Brush.horizontalGradient(
                        listOf(Color.White, Color.White)
                    )
                )
                .padding(horizontal = 16.dp, vertical = 14.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            // Icon
            Box(
                modifier = Modifier
                    .size(52.dp)
                    .background(
                        brush = if (isSelected) androidx.compose.ui.graphics.Brush.linearGradient(
                            listOf(DoziTurquoise, DoziPurple)
                        ) else androidx.compose.ui.graphics.Brush.linearGradient(
                            listOf(Color(0xFFE3F2FD), Color(0xFFE1F5FE))
                        ),
                        shape = RoundedCornerShape(14.dp)
                    ),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = item.icon,
                    contentDescription = item.label,
                    tint = if (isSelected) Color.White else DoziTurquoise,
                    modifier = Modifier.size(26.dp)
                )
            }

            Spacer(modifier = Modifier.width(16.dp))

            // Text
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = item.label,
                    style = MaterialTheme.typography.bodyLarge.copy(
                        fontWeight = if (isSelected) FontWeight.ExtraBold else FontWeight.Bold,
                        fontSize = 17.sp
                    ),
                    color = if (isEnabled) Color(0xFF1A237E) else Color.Gray
                )
                Spacer(modifier = Modifier.height(2.dp))
                Text(
                    text = item.description,
                    style = MaterialTheme.typography.bodySmall.copy(fontSize = 13.sp),
                    color = if (isEnabled) Color(0xFF546E7A) else Color.LightGray
                )
            }

            // Arrow or lock icon
            if (isEnabled) {
                Icon(
                    imageVector = Icons.Default.ChevronRight,
                    contentDescription = null,
                    tint = if (isSelected) DoziTurquoise else Color(0xFF90A4AE),
                    modifier = Modifier.size(26.dp)
                )
            } else {
                Icon(
                    imageVector = Icons.Default.Lock,
                    contentDescription = "Giriş gerekli",
                    tint = Color.Gray,
                    modifier = Modifier.size(22.dp)
                )
            }
        }
    }
}
