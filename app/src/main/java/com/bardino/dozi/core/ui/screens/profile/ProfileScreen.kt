package com.bardino.dozi.core.ui.screens.profile

import android.os.Build
import androidx.annotation.RequiresApi
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
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

/**
 * Kullanıcı Hesap Ayarları Ekranı
 * Login/Logout ve temel kullanıcı bilgileri
 */
@RequiresApi(Build.VERSION_CODES.O)
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ProfileScreen(
    onNavigateToLocations: () -> Unit = {},
    onNavigateToPremium: () -> Unit = {},
    onNavigateToSettings: () -> Unit = {},
    onNavigateToNotifications: () -> Unit = {},
    onNavigateToAbout: () -> Unit = {},
    onNavigateToHome: () -> Unit = {},
    onGoogleSignInClick: () -> Unit
) {
    val auth = remember { FirebaseAuth.getInstance() }
    var currentUser by remember { mutableStateOf(auth.currentUser) }

    // Auth listener
    DisposableEffect(Unit) {
        val listener = FirebaseAuth.AuthStateListener { firebaseAuth ->
            currentUser = firebaseAuth.currentUser
        }
        auth.addAuthStateListener(listener)
        onDispose { auth.removeAuthStateListener(listener) }
    }

    if (currentUser == null) {
        // Giriş yapılmamış - Login ekranını göster
        LoginScreen(
            onLoginSuccess = onNavigateToHome,
            onSkip = onNavigateToHome
        )
    } else {
        // Giriş yapılmış - Profil ekranını göster
        ProfileContent(
            user = currentUser,
            onNavigateToLocations = onNavigateToLocations,
            onNavigateToPremium = onNavigateToPremium,
            onNavigateToSettings = onNavigateToSettings,
            onNavigateToNotifications = onNavigateToNotifications,
            onNavigateToAbout = onNavigateToAbout,
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
    onNavigateToSettings: () -> Unit,
    onNavigateToNotifications: () -> Unit,
    onNavigateToAbout: () -> Unit,
    onLogout: () -> Unit
) {
    val context = LocalContext.current
    val userRepository = remember { UserRepository() }
    val scope = rememberCoroutineScope()

    var firestoreUser by remember { mutableStateOf<User?>(null) }
    var isLoading by remember { mutableStateOf(true) }

    LaunchedEffect(user?.uid) {
        scope.launch {
            try {
                firestoreUser = userRepository.getUserData()
                isLoading = false
            } catch (e: Exception) {
                isLoading = false
            }
        }
    }

    Scaffold(
        topBar = {
            DoziTopBar(
                title = "Profil",
                canNavigateBack = false,
                backgroundColor = MaterialTheme.colorScheme.surface
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
            // Kullanıcı Bilgisi Kartı
            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(16.dp),
                colors = CardDefaults.cardColors(
                    containerColor = MaterialTheme.colorScheme.surface
                )
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(16.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    // Avatar
                    Box(
                        modifier = Modifier
                            .size(64.dp)
                            .clip(CircleShape)
                            .background(DoziTurquoise),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            text = (firestoreUser?.name?.firstOrNull()?.uppercase() ?: user?.email?.firstOrNull()?.uppercase() ?: "U").toString(),
                            fontSize = 24.sp,
                            fontWeight = FontWeight.Bold,
                            color = Color.White
                        )
                    }

                    Spacer(modifier = Modifier.width(16.dp))

                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = firestoreUser?.name ?: user?.displayName ?: "Kullanıcı",
                            style = MaterialTheme.typography.titleLarge,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.onSurface
                        )
                        Spacer(modifier = Modifier.height(4.dp))
                        Text(
                            text = user?.email ?: "",
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(8.dp))

            // Menü Seçenekleri
            ProfileMenuItem(
                icon = Icons.Default.LocationOn,
                title = "Konumlar",
                description = "Konum tabanlı hatırlatmalar",
                onClick = onNavigateToLocations
            )

            ProfileMenuItem(
                icon = Icons.Default.Star,
                title = "Premium",
                description = "Premium özelliklere erişin",
                onClick = onNavigateToPremium
            )

            ProfileMenuItem(
                icon = Icons.Default.Settings,
                title = "Ayarlar",
                description = "Uygulama ayarları",
                onClick = onNavigateToSettings
            )

            ProfileMenuItem(
                icon = Icons.Default.Notifications,
                title = "Bildirimler",
                description = "Bildirim ayarları",
                onClick = onNavigateToNotifications
            )

            ProfileMenuItem(
                icon = Icons.Default.Info,
                title = "Hakkında",
                description = "Uygulama bilgileri",
                onClick = onNavigateToAbout
            )

            Spacer(modifier = Modifier.height(16.dp))

            // Çıkış Yap Butonu
            Button(
                onClick = onLogout,
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp),
                colors = ButtonDefaults.buttonColors(
                    containerColor = Color.Red.copy(alpha = 0.1f),
                    contentColor = Color.Red
                )
            ) {
                Icon(Icons.Default.Logout, contentDescription = null)
                Spacer(modifier = Modifier.width(8.dp))
                Text("Çıkış Yap")
            }

            Spacer(modifier = Modifier.height(32.dp))
        }
    }
}

@Composable
private fun ProfileMenuItem(
    icon: ImageVector,
    title: String,
    description: String,
    onClick: () -> Unit
) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 4.dp),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surface
        ),
        onClick = onClick
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Icon(
                imageVector = icon,
                contentDescription = null,
                tint = DoziTurquoise,
                modifier = Modifier.size(24.dp)
            )
            Spacer(modifier = Modifier.width(16.dp))
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = title,
                    style = MaterialTheme.typography.bodyLarge,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onSurface
                )
                Text(
                    text = description,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
            Icon(
                imageVector = Icons.Default.ChevronRight,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }
}
