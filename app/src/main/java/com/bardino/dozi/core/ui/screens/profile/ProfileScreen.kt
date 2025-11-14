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
import com.bardino.dozi.core.ui.screens.onboarding.OnboardingScreen
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
            OnboardingScreen(
                onFinish = {
                    // Onboarding tamamlandı, artık bir daha gösterme
                    sharedPrefs.edit().putBoolean("has_seen_onboarding", true).apply()
                    hasSeenOnboarding = true
                    screenState = ProfileScreenState.LOGIN
                },
                onSkip = {
                    // Skip edildi, Home'a git
                    sharedPrefs.edit().putBoolean("has_seen_onboarding", true).apply()
                    hasSeenOnboarding = true
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
                    backgroundColor = Color.White,
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
            containerColor = BackgroundLight
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
                        color = TextSecondary,
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


// Profil Header Composable - Modernized
@Composable
private fun ProfileHeader(
    currentUser: FirebaseUser?,
    firestoreUser: User?
) {
    val planType = firestoreUser?.planType ?: "free"
    val isPremium = planType != "free"

    // Gradient renklerini plan durumuna göre değiştir
    val gradientColors = if (isPremium) {
        listOf(
            Color(0xFFFFB300),  // Altın
            Color(0xFFFF6F00)   // Turuncu
        )
    } else {
        listOf(
            DoziTurquoise,      // Ana tema rengi
            DoziTurquoiseDark   // Koyu Turquoise
        )
    }

    Box(
        modifier = Modifier
            .fillMaxWidth()
            .background(
                brush = Brush.verticalGradient(colors = gradientColors)
            )
            .padding(vertical = 40.dp, horizontal = 24.dp),
        contentAlignment = Alignment.Center
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            // Avatar - Premium kullanıcılar için altın kenarlık
            Surface(
                modifier = Modifier.size(120.dp),
                color = Color.White,
                shape = CircleShape
            ) {
                Box(
                    contentAlignment = Alignment.Center,
                    modifier = if (isPremium) {
                        Modifier
                            .fillMaxSize()
                            .background(
                                brush = Brush.radialGradient(
                                    colors = listOf(
                                        Color(0xFFFFD700).copy(alpha = 0.1f),
                                        Color.White
                                    )
                                )
                            )
                    } else Modifier.fillMaxSize()
                ) {
                    Image(
                        painter = painterResource(R.drawable.dozi),
                        contentDescription = "Avatar",
                        modifier = Modifier
                            .fillMaxSize()
                            .padding(24.dp)
                    )
                }
            }

            // İsim
            Text(
                text = currentUser?.displayName ?: firestoreUser?.name ?: "Kullanıcı",
                style = MaterialTheme.typography.headlineMedium,
                color = Color.White,
                fontWeight = FontWeight.ExtraBold
            )

            // Email
            Text(
                text = currentUser?.email ?: "",
                style = MaterialTheme.typography.bodyLarge,
                color = Color.White.copy(alpha = 0.9f)
            )

            // Plan Badge
            val planText = when (planType) {
                "premium" -> "⭐ Premium Plan"
                "pro" -> "👑 Pro Plan"
                else -> "🆓 Ücretsiz Plan"
            }

            Surface(
                shape = RoundedCornerShape(24.dp),
                color = Color.White.copy(alpha = if (isPremium) 0.3f else 0.2f),
                modifier = Modifier.padding(top = 4.dp)
            ) {
                Text(
                    text = planText,
                    modifier = Modifier.padding(horizontal = 20.dp, vertical = 8.dp),
                    style = MaterialTheme.typography.labelLarge,
                    color = Color.White,
                    fontWeight = FontWeight.Bold
                )
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
        colors = ButtonDefaults.buttonColors(containerColor = Color.White)
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


// 🌟 Modernize Edilmiş Menü Kartı
@Composable
private fun MenuCard(
    icon: ImageVector,
    title: String,
    desc: String,
    color: Color,
    onClick: () -> Unit
) {
    Card(
        onClick = onClick,
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(20.dp),
        colors = CardDefaults.cardColors(containerColor = Color.White)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(20.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            // Modern ikon container
            Surface(
                modifier = Modifier.size(52.dp),
                shape = RoundedCornerShape(14.dp),
                color = color.copy(alpha = 0.12f)
            ) {
                Box(contentAlignment = Alignment.Center) {
                    Icon(
                        icon,
                        contentDescription = null,
                        tint = color,
                        modifier = Modifier.size(24.dp)
                    )
                }
            }

            // Metin alanı
            Column(
                modifier = Modifier.weight(1f),
                verticalArrangement = Arrangement.spacedBy(4.dp)
            ) {
                Text(
                    text = title,
                    style = MaterialTheme.typography.titleMedium,
                    color = TextPrimary,
                    fontWeight = FontWeight.Bold
                )
                Text(
                    text = desc,
                    style = MaterialTheme.typography.bodySmall,
                    color = TextSecondary,
                    lineHeight = 18.sp
                )
            }

            // Chevron ikonu
            Surface(
                modifier = Modifier.size(32.dp),
                shape = CircleShape,
                color = color.copy(alpha = 0.08f)
            ) {
                Box(contentAlignment = Alignment.Center) {
                    Icon(
                        Icons.Default.ChevronRight,
                        contentDescription = null,
                        tint = color,
                        modifier = Modifier.size(20.dp)
                    )
                }
            }
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