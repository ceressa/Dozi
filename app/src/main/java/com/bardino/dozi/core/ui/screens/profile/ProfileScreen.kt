package com.bardino.dozi.core.ui.screens.profile

import android.os.Build
import androidx.annotation.RequiresApi
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material.icons.outlined.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.bardino.dozi.core.data.model.User
import com.bardino.dozi.core.data.repository.UserRepository
import com.bardino.dozi.core.ui.components.DoziTopBar
import com.bardino.dozi.core.ui.screens.login.LoginScreen
import com.bardino.dozi.core.ui.theme.*
import com.google.firebase.auth.FirebaseAuth
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.util.*

@RequiresApi(Build.VERSION_CODES.O)
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ProfileScreen(
    onNavigateToLocations: () -> Unit = {},
    onNavigateToPremium: () -> Unit = {},
    onNavigateToFamilyManagement: () -> Unit = {},
    onNavigateToSettings: () -> Unit = {},
    onNavigateToNotifications: () -> Unit = {},
    onNavigateToAbout: () -> Unit = {},
    onNavigateToSupport: () -> Unit = {},
    onNavigateToHome: () -> Unit = {},
    onGoogleSignInClick: () -> Unit
) {
    val auth = remember { FirebaseAuth.getInstance() }
    var currentUser by remember { mutableStateOf(auth.currentUser) }

    DisposableEffect(Unit) {
        val listener = FirebaseAuth.AuthStateListener { firebaseAuth ->
            currentUser = firebaseAuth.currentUser
        }
        auth.addAuthStateListener(listener)
        onDispose { auth.removeAuthStateListener(listener) }
    }

    if (currentUser == null) {
        LoginScreen(
            onLoginSuccess = onNavigateToHome,
            onSkip = onNavigateToHome
        )
    } else {
        ProfileContent(
            user = currentUser,
            onNavigateToLocations = onNavigateToLocations,
            onNavigateToPremium = onNavigateToPremium,
            onNavigateToFamilyManagement = onNavigateToFamilyManagement,
            onNavigateToSettings = onNavigateToSettings,
            onNavigateToNotifications = onNavigateToNotifications,
            onNavigateToAbout = onNavigateToAbout,
            onNavigateToSupport = onNavigateToSupport,
            onLogout = {
                auth.signOut()
                onNavigateToHome()
            }
        )
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun ProfileContent(
    user: com.google.firebase.auth.FirebaseUser?,
    onNavigateToLocations: () -> Unit,
    onNavigateToPremium: () -> Unit,
    onNavigateToFamilyManagement: () -> Unit,
    onNavigateToSettings: () -> Unit,
    onNavigateToNotifications: () -> Unit,
    onNavigateToAbout: () -> Unit,
    onNavigateToSupport: () -> Unit,
    onLogout: () -> Unit
) {
    val context = LocalContext.current
    val userRepository = remember { UserRepository() }
    val scope = rememberCoroutineScope()

    var firestoreUser by remember { mutableStateOf<User?>(null) }
    var isLoading by remember { mutableStateOf(true) }
    var showLogoutDialog by remember { mutableStateOf(false) }

    LaunchedEffect(user?.uid) {
        scope.launch {
            try {
                firestoreUser = userRepository.getUserData()
                // Debug logging
                firestoreUser?.let { u ->
                    android.util.Log.d("ProfileScreen", "📊 User loaded:")
                    android.util.Log.d("ProfileScreen", "  - isPremium: ${u.isPremium}")
                    android.util.Log.d("ProfileScreen", "  - isTrial: ${u.isTrial}")
                    android.util.Log.d("ProfileScreen", "  - planType: ${u.planType}")
                    android.util.Log.d("ProfileScreen", "  - premiumExpiryDate: ${u.premiumExpiryDate}")
                    android.util.Log.d("ProfileScreen", "  - isCurrentlyPremium(): ${u.isCurrentlyPremium()}")
                    android.util.Log.d("ProfileScreen", "  - premiumDaysRemaining(): ${u.premiumDaysRemaining()}")
                    android.util.Log.d("ProfileScreen", "  - getPremiumPlanType(): ${u.getPremiumPlanType()}")
                }
                isLoading = false
            } catch (e: Exception) {
                android.util.Log.e("ProfileScreen", "Error loading user", e)
                isLoading = false
            }
        }
    }

    if (showLogoutDialog) {
        AlertDialog(
            onDismissRequest = { showLogoutDialog = false },
            title = { Text("Çıkış Yap") },
            text = { Text("Çıkış yapmak istediğinize emin misiniz?") },
            confirmButton = {
                TextButton(
                    onClick = {
                        showLogoutDialog = false
                        onLogout()
                    }
                ) {
                    Text("Çıkış Yap", color = Color.Red)
                }
            },
            dismissButton = {
                TextButton(onClick = { showLogoutDialog = false }) {
                    Text("İptal")
                }
            }
        )
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Text(
                        text = "Profil",
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                },
                actions = {
                    IconButton(onClick = { showLogoutDialog = true }) {
                        Icon(
                            Icons.Default.Logout,
                            contentDescription = "Çıkış Yap",
                            tint = Color.Red
                        )
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.surface
                )
            )
        },
        containerColor = MaterialTheme.colorScheme.background
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .verticalScroll(rememberScrollState())
        ) {
            // User Profile Header with Gradient
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(
                        brush = Brush.verticalGradient(
                            colors = listOf(
                                DoziTurquoise,
                                DoziTurquoise.copy(alpha = 0.8f)
                            )
                        )
                    )
                    .padding(24.dp)
            ) {
                Column(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    // Avatar
                    Box(
                        modifier = Modifier
                            .size(80.dp)
                            .clip(CircleShape)
                            .background(Color.White.copy(alpha = 0.2f))
                            .border(3.dp, Color.White, CircleShape),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            text = (firestoreUser?.name?.firstOrNull()?.uppercase()
                                ?: user?.email?.firstOrNull()?.uppercase()
                                ?: "U").toString(),
                            fontSize = 32.sp,
                            fontWeight = FontWeight.Bold,
                            color = Color.White
                        )
                    }

                    Spacer(modifier = Modifier.height(12.dp))

                    // Name
                    Text(
                        text = firestoreUser?.name ?: user?.displayName ?: "Kullanıcı",
                        style = MaterialTheme.typography.titleLarge,
                        fontWeight = FontWeight.Bold,
                        color = Color.White
                    )

                    // Email
                    Text(
                        text = user?.email ?: "",
                        style = MaterialTheme.typography.bodyMedium,
                        color = Color.White.copy(alpha = 0.8f)
                    )

                    // Premium Badge
                    Spacer(modifier = Modifier.height(12.dp))

                    val isCurrentlyPremium = firestoreUser?.isCurrentlyPremium() == true
                    val isTrial = firestoreUser?.isTrial == true
                    val daysRemaining = firestoreUser?.premiumDaysRemaining() ?: 0

                    Surface(
                        shape = RoundedCornerShape(20.dp),
                        color = if (isCurrentlyPremium) Color.White.copy(alpha = 0.2f) else Color.White.copy(alpha = 0.15f)
                    ) {
                        Row(
                            modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Icon(
                                imageVector = when {
                                    isLoading -> Icons.Default.Sync
                                    isCurrentlyPremium -> Icons.Default.Star
                                    else -> Icons.Default.StarBorder
                                },
                                contentDescription = null,
                                tint = Color.White,
                                modifier = Modifier.size(16.dp)
                            )
                            Spacer(modifier = Modifier.width(6.dp))
                            Text(
                                text = when {
                                    isLoading -> "Yükleniyor..."
                                    isCurrentlyPremium && isTrial -> {
                                        if (daysRemaining > 0) "Deneme - $daysRemaining gün kaldı"
                                        else "Deneme Sürümü"
                                    }
                                    isCurrentlyPremium -> {
                                        if (daysRemaining > 0) "Dozi Ekstra - $daysRemaining gün"
                                        else "Dozi Ekstra"
                                    }
                                    else -> "Ücretsiz Plan"
                                },
                                style = MaterialTheme.typography.bodySmall,
                                fontWeight = FontWeight.SemiBold,
                                color = Color.White
                            )
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(16.dp))

            // Quick Actions Grid
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp),
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                QuickActionCard(
                    icon = Icons.Default.HelpOutline,
                    title = "Destek",
                    backgroundColor = DoziTurquoise.copy(alpha = 0.1f),
                    iconColor = DoziTurquoise,
                    onClick = onNavigateToSupport,
                    modifier = Modifier.weight(1f)
                )
                QuickActionCard(
                    icon = Icons.Default.Star,
                    title = "Ekstra",
                    backgroundColor = Color(0xFFFFF3E0),
                    iconColor = Color(0xFFFF9800),
                    onClick = onNavigateToPremium,
                    modifier = Modifier.weight(1f)
                )
            }

            Spacer(modifier = Modifier.height(12.dp))

            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp),
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                QuickActionCard(
                    icon = Icons.Default.LocationOn,
                    title = "Konumlar",
                    backgroundColor = Color(0xFFE3F2FD),
                    iconColor = Color(0xFF2196F3),
                    onClick = onNavigateToLocations,
                    modifier = Modifier.weight(1f)
                )
                QuickActionCard(
                    icon = Icons.Default.Settings,
                    title = "Ayarlar",
                    backgroundColor = Color(0xFFF3E5F5),
                    iconColor = Color(0xFF9C27B0),
                    onClick = onNavigateToSettings,
                    modifier = Modifier.weight(1f)
                )
            }

            Spacer(modifier = Modifier.height(20.dp))

            // Other Options Card
            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp),
                colors = CardDefaults.cardColors(
                    containerColor = MaterialTheme.colorScheme.surface
                ),
                shape = RoundedCornerShape(16.dp)
            ) {
                Column {
                    if (firestoreUser?.familyPlanId != null) {
                        CompactMenuItem(
                            icon = Icons.Default.FamilyRestroom,
                            title = "Aile Yönetimi",
                            onClick = onNavigateToFamilyManagement
                        )
                        HorizontalDivider(
                            modifier = Modifier.padding(horizontal = 16.dp),
                            color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f)
                        )
                    }

                    CompactMenuItem(
                        icon = Icons.Default.Notifications,
                        title = "Bildirimler",
                        onClick = onNavigateToNotifications
                    )

                    HorizontalDivider(
                        modifier = Modifier.padding(horizontal = 16.dp),
                        color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f)
                    )

                    CompactMenuItem(
                        icon = Icons.Default.Info,
                        title = "Hakkında",
                        onClick = onNavigateToAbout
                    )
                }
            }

            // Bottom padding for navigation bar
            Spacer(modifier = Modifier.height(100.dp))
        }
    }
}

@Composable
private fun QuickActionCard(
    icon: ImageVector,
    title: String,
    backgroundColor: Color,
    iconColor: Color,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    Card(
        modifier = modifier,
        colors = CardDefaults.cardColors(
            containerColor = backgroundColor
        ),
        shape = RoundedCornerShape(16.dp),
        onClick = onClick
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Box(
                modifier = Modifier
                    .size(48.dp)
                    .clip(CircleShape)
                    .background(iconColor.copy(alpha = 0.15f)),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = icon,
                    contentDescription = null,
                    tint = iconColor,
                    modifier = Modifier.size(24.dp)
                )
            }
            Spacer(modifier = Modifier.height(8.dp))
            Text(
                text = title,
                style = MaterialTheme.typography.bodyMedium,
                fontWeight = FontWeight.SemiBold,
                color = MaterialTheme.colorScheme.onSurface
            )
        }
    }
}

@Composable
private fun CompactMenuItem(
    icon: ImageVector,
    title: String,
    onClick: () -> Unit
) {
    Surface(
        modifier = Modifier.fillMaxWidth(),
        onClick = onClick,
        color = Color.Transparent
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 14.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Icon(
                imageVector = icon,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.size(22.dp)
            )
            Spacer(modifier = Modifier.width(16.dp))
            Text(
                text = title,
                style = MaterialTheme.typography.bodyLarge,
                color = MaterialTheme.colorScheme.onSurface,
                modifier = Modifier.weight(1f)
            )
            Icon(
                imageVector = Icons.Default.ChevronRight,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.5f),
                modifier = Modifier.size(20.dp)
            )
        }
    }
}

