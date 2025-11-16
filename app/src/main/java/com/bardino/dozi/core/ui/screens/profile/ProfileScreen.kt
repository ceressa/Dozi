package com.bardino.dozi.core.ui.screens.profile

import android.Manifest
import android.app.Activity
import android.content.pm.PackageManager
import android.os.Build
import android.util.Log
import android.widget.Toast
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.annotation.RequiresApi
import androidx.compose.animation.*
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material.icons.outlined.*
import androidx.compose.material3.*
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.content.ContextCompat
import com.bardino.dozi.R
import com.bardino.dozi.core.data.model.User
import com.bardino.dozi.core.data.repository.UserRepository
import com.bardino.dozi.core.ui.components.DoziTopBar
import com.bardino.dozi.core.ui.screens.login.LoginScreen
import com.bardino.dozi.core.ui.theme.*
import com.bardino.dozi.notifications.NotificationHelper
import com.google.android.gms.auth.api.signin.GoogleSignIn
import com.google.android.gms.auth.api.signin.GoogleSignInOptions
import com.google.android.gms.common.api.ApiException
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.FirebaseUser
import com.google.firebase.auth.GoogleAuthProvider
import kotlinx.coroutines.launch

enum class ProfileScreenState {
    ONBOARDING,
    LOGIN,
    PROFILE
}

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
    val context = LocalContext.current
    val auth = remember { FirebaseAuth.getInstance() }
    val userRepository = remember { UserRepository() }
    val scope = rememberCoroutineScope()

    var currentUser by remember { mutableStateOf(auth.currentUser) }
    var firestoreUser by remember { mutableStateOf<User?>(null) }

    // Onboarding durumunu yönet
    val sharedPrefs = context.getSharedPreferences("dozi_prefs", android.content.Context.MODE_PRIVATE)
    var hasSeenOnboarding by remember {
        mutableStateOf(sharedPrefs.getBoolean("has_seen_onboarding", false))
    }

    var screenState by remember {
        mutableStateOf(
            when {
                currentUser != null -> ProfileScreenState.PROFILE
                !hasSeenOnboarding -> ProfileScreenState.ONBOARDING
                else -> ProfileScreenState.LOGIN
            }
        )
    }

    // 🔹 Firebase Auth Listener — kullanıcı durumu değiştiğinde UI'ı günceller
    DisposableEffect(Unit) {
        val listener = FirebaseAuth.AuthStateListener { firebaseAuth ->
            currentUser = firebaseAuth.currentUser
            // Kullanıcı girişi yaptığında Firestore'dan verileri çek
            if (firebaseAuth.currentUser != null) {
                scope.launch {
                    try {
                        firestoreUser = userRepository.getUserData()
                        screenState = ProfileScreenState.PROFILE
                    } catch (e: Exception) {
                        Log.e("ProfileScreen", "Firestore veri çekme hatası: ${e.message}")
                    }
                }
            } else {
                firestoreUser = null
                screenState = if (hasSeenOnboarding) ProfileScreenState.LOGIN else ProfileScreenState.ONBOARDING
            }
        }
        auth.addAuthStateListener(listener)
        onDispose { auth.removeAuthStateListener(listener) }
    }

    // Screen state'e göre farklı ekranlar göster
    when (screenState) {
        ProfileScreenState.ONBOARDING -> {
            LoginScreen(
                onLoginSuccess = {
                    // Login başarılı olunca otomatik güncellenecek
                },
                onSkip = {
                    // Login skip edildi, Home'a git
                    onNavigateToHome()
                }
            )
        }

        ProfileScreenState.LOGIN -> {
            LoginScreen(
                onLoginSuccess = {
                    // Login başarılı olunca otomatik güncellenecek
                },
                onSkip = {
                    // Login skip edildi, Home'a git
                    onNavigateToHome()
                }
            )
        }

        ProfileScreenState.PROFILE -> {
        // Login olmuşsa Profil ekranını göster
        Scaffold(
            topBar = {
                DoziTopBar(
                    title = "Profil",
                    canNavigateBack = false,
                    backgroundColor = MaterialTheme.colorScheme.surface,
                    actions = {
                        IconButton(
                            onClick = {
                                auth.signOut()
                                GoogleSignIn.getClient(
                                    context,
                                    GoogleSignInOptions.Builder(GoogleSignInOptions.DEFAULT_SIGN_IN).build()
                                ).signOut()
                                Toast.makeText(context, "Çıkış yapıldı", Toast.LENGTH_SHORT).show()
                            }
                        ) {
                            Icon(
                                Icons.Filled.Logout,
                                contentDescription = "Çıkış Yap",
                                tint = DoziCoralDark
                            )
                        }
                    }
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
                // Profil Header
                ProfileHeader(
                    currentUser = currentUser,
                    firestoreUser = firestoreUser
                )

                Spacer(modifier = Modifier.height(24.dp))

                // Menü Seçenekleri
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp),
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    // Premium
                    MenuCard(
                        icon = Icons.Filled.Star,
                        title = "Premium'a Geç",
                        desc = "Tüm özelliklerin kilidini aç",
                        color = Color(0xFFFFB300),
                        onClick = onNavigateToPremium
                    )

                    // Ayarlar
                    MenuCard(
                        icon = Icons.Outlined.Settings,
                        title = "Ayarlar",
                        desc = "Uygulama tercihlerini düzenle",
                        color = DoziBlue,
                        onClick = onNavigateToSettings
                    )

                    // Bildirimler
                    MenuCard(
                        icon = Icons.Outlined.Notifications,
                        title = "Bildirim Ayarları",
                        desc = "Hatırlatmaları yönet",
                        color = DoziTurquoise,
                        onClick = onNavigateToNotifications
                    )

                    // Konum/Eczaneler
                    MenuCard(
                        icon = Icons.Outlined.LocationOn,
                        title = "Kayıtlı Konumlar",
                        desc = "Kayıtlı konumlarına göz at",
                        color = Color(0xFF4CAF50),
                        onClick = onNavigateToLocations
                    )

                    // Yardım & Destek
                    MenuCard(
                        icon = Icons.Outlined.HelpOutline,
                        title = "Yardım & Destek",
                        desc = "SSS ve iletişim",
                        color = Color(0xFF9C27B0),
                        onClick = { /* TODO: Yardım sayfasına yönlendir */ }
                    )

                    // Hakkında
                    MenuCard(
                        icon = Icons.Outlined.Info,
                        title = "Hakkında",
                        desc = "Versiyon bilgisi ve lisanslar",
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        onClick = onNavigateToAbout
                    )
                }

                // ✅ BottomBar için yeterli padding
                Spacer(modifier = Modifier.height(100.dp))
            }
        }
        }
    }
}


// Profil Header Composable - Redesigned Compact Version
@Composable
private fun ProfileHeader(
    currentUser: FirebaseUser?,
    firestoreUser: User?
) {
    val planType = firestoreUser?.planType ?: "free"
    val isPremium = planType != "free"

    Box(
        modifier = Modifier
            .fillMaxWidth()
            .background(MaterialTheme.colorScheme.surface)
            .padding(24.dp),
        contentAlignment = Alignment.Center
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            // Avatar - dozi için free, dozi_king için premium
            Box(
                contentAlignment = Alignment.Center
            ) {
                // Premium kullanıcılar için altın çerçeve
                if (isPremium) {
                    Surface(
                        modifier = Modifier.size(76.dp),
                        shape = CircleShape,
                        color = Color.Transparent,
                        border = androidx.compose.foundation.BorderStroke(
                            3.dp,
                            brush = Brush.linearGradient(
                                colors = listOf(
                                    Color(0xFFFFD700),
                                    Color(0xFFFFB300),
                                    Color(0xFFFFD700)
                                )
                            )
                        )
                    ) {}
                }

                // Avatar image
                Image(
                    painter = painterResource(
                        if (isPremium) R.drawable.dozi_king else R.drawable.dozi
                    ),
                    contentDescription = "Avatar",
                    modifier = Modifier
                        .size(64.dp)
                        .clip(CircleShape),
                    alpha = if (isPremium) 1f else 0.6f  // Free users: dimmed
                )
            }

            // İsim ve email
            Column(
                modifier = Modifier.weight(1f),
                verticalArrangement = Arrangement.spacedBy(4.dp)
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Text(
                        text = currentUser?.displayName ?: firestoreUser?.name ?: "Kullanıcı",
                        style = MaterialTheme.typography.titleLarge,
                        color = MaterialTheme.colorScheme.onSurface,
                        fontWeight = FontWeight.Bold,
                        fontSize = 20.sp
                    )

                    // Plan badge - compact
                    if (isPremium) {
                        Surface(
                            shape = RoundedCornerShape(8.dp),
                            color = Color(0xFFFFB300).copy(alpha = 0.15f)
                        ) {
                            Text(
                                text = "👑",
                                modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp),
                                style = MaterialTheme.typography.labelSmall,
                                fontSize = 12.sp
                            )
                        }
                    }
                }

                // Email
                if (!currentUser?.email.isNullOrEmpty()) {
                    Text(
                        text = currentUser?.email ?: "",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        fontSize = 14.sp
                    )
                }
            }
        }
    }
}

@Composable
fun GoogleSignInButton(onClick: () -> Unit) {
    Button(
        onClick = onClick,
        modifier = Modifier
            .fillMaxWidth(0.8f)
            .height(52.dp),
        shape = RoundedCornerShape(50),
        colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.surface)
    ) {
        Image(
            painter = painterResource(id = R.drawable.ic_google),
            contentDescription = "Google logo",
            modifier = Modifier
                .size(24.dp)
                .padding(end = 8.dp)
        )
        Text(
            text = "Google ile Giriş Yap",
            color = Color.Black,
            style = MaterialTheme.typography.bodyLarge,
            fontWeight = FontWeight.SemiBold
        )
    }
}

@Composable
fun ColoredDebugButton(label: String, bgColor: Color, onClick: () -> Unit) {
    Button(
        onClick = onClick,
        modifier = Modifier
            .fillMaxWidth()
            .height(48.dp),
        shape = RoundedCornerShape(12.dp),
        colors = ButtonDefaults.buttonColors(
            containerColor = bgColor,
            contentColor = Color.White,
            disabledContainerColor = bgColor.copy(alpha = 0.4f)
        )
    ) {
        Text(
            text = label,
            style = MaterialTheme.typography.bodyLarge,
            fontWeight = FontWeight.SemiBold
        )
    }
}


// 🌟 Clean Menü Kartı - No Elevation
@Composable
private fun MenuCard(
    icon: ImageVector,
    title: String,
    desc: String,
    color: Color,
    onClick: () -> Unit
) {
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(16.dp))
            .background(MaterialTheme.colorScheme.surface)
            .clickable(onClick = onClick)
            .padding(16.dp)
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(14.dp)
        ) {
            // Ikon container - daha küçük ve minimal
            Box(
                modifier = Modifier
                    .size(44.dp)
                    .clip(RoundedCornerShape(12.dp))
                    .background(color.copy(alpha = 0.1f)),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    icon,
                    contentDescription = null,
                    tint = color,
                    modifier = Modifier.size(22.dp)
                )
            }

            // Metin alanı
            Column(
                modifier = Modifier.weight(1f),
                verticalArrangement = Arrangement.spacedBy(3.dp)
            ) {
                Text(
                    text = title,
                    style = MaterialTheme.typography.titleMedium,
                    color = MaterialTheme.colorScheme.onSurface,
                    fontWeight = FontWeight.SemiBold,
                    fontSize = 16.sp
                )
                Text(
                    text = desc,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    fontSize = 13.sp
                )
            }

            // Chevron ikonu
            Icon(
                Icons.Default.ChevronRight,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.4f),
                modifier = Modifier.size(20.dp)
            )
        }
    }
}

// 🧩 Debug Buton
@Composable
fun DebugButton(label: String, onClick: () -> Unit) {
    OutlinedButton(
        onClick = onClick,
        modifier = Modifier
            .fillMaxWidth()
            .height(48.dp),
        shape = RoundedCornerShape(12.dp),
        colors = ButtonDefaults.outlinedButtonColors(contentColor = DoziBlue)
    ) {
        Text(
            label,
            style = MaterialTheme.typography.bodyLarge,
            fontWeight = FontWeight.SemiBold
        )
    }
}