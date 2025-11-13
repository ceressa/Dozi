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

@RequiresApi(Build.VERSION_CODES.O)
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ProfileScreen(
    onNavigateToLocations: () -> Unit = {},
    onGoogleSignInClick: () -> Unit
) {
    val context = LocalContext.current
    val auth = remember { FirebaseAuth.getInstance() }
    var currentUser by remember { mutableStateOf(auth.currentUser) }

    // 🔹 Firebase Auth Listener — kullanıcı durumu değiştiğinde UI'ı günceller
    DisposableEffect(Unit) {
        val listener = FirebaseAuth.AuthStateListener { firebaseAuth ->
            currentUser = firebaseAuth.currentUser
        }
        auth.addAuthStateListener(listener)
        onDispose { auth.removeAuthStateListener(listener) }
    }

    // Login olmamışsa LoginScreen göster
    if (currentUser == null) {
        LoginScreen(onLoginSuccess = {
            // Login başarılı olunca otomatik güncellenecek
        })
    } else {
        // Login olmuşsa Profil ekranını göster
        Scaffold(
            topBar = {
                DoziTopBar(title = "Profil", canNavigateBack = false, backgroundColor = Color.White)
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
                ProfileHeader(currentUser = currentUser)

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
                        onClick = { /* TODO: Premium'a yönlendir */ }
                    )

                    // Ayarlar
                    MenuCard(
                        icon = Icons.Outlined.Settings,
                        title = "Ayarlar",
                        desc = "Uygulama tercihlerini düzenle",
                        color = DoziBlue,
                        onClick = { /* TODO: Ayarlara yönlendir */ }
                    )

                    // Bildirimler
                    MenuCard(
                        icon = Icons.Outlined.Notifications,
                        title = "Bildirim Ayarları",
                        desc = "Hatırlatmaları yönet",
                        color = DoziTurquoise,
                        onClick = { /* TODO: Bildirim ayarlarına yönlendir */ }
                    )

                    // Konum/Eczaneler
                    MenuCard(
                        icon = Icons.Outlined.LocationOn,
                        title = "Eczaneler",
                        desc = "Yakındaki eczaneleri bul",
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
                        onClick = { /* TODO: Hakkında sayfasına yönlendir */ }
                    )
                }

                Spacer(modifier = Modifier.height(24.dp))

                // Çıkış Yap Butonu
                OutlinedButton(
                    onClick = {
                        auth.signOut()
                        GoogleSignIn.getClient(
                            context,
                            GoogleSignInOptions.Builder(GoogleSignInOptions.DEFAULT_SIGN_IN).build()
                        ).signOut()
                        Toast.makeText(context, "Çıkış yapıldı", Toast.LENGTH_SHORT).show()
                    },
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp)
                        .height(50.dp),
                    shape = RoundedCornerShape(12.dp),
                    colors = ButtonDefaults.outlinedButtonColors(
                        contentColor = Color(0xFFE53935)
                    ),
                    border = androidx.compose.foundation.BorderStroke(
                        1.dp,
                        Color(0xFFE53935)
                    )
                ) {
                    Icon(
                        Icons.Filled.Logout,
                        contentDescription = null,
                        modifier = Modifier.size(20.dp)
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        "Çıkış Yap",
                        fontWeight = FontWeight.SemiBold
                    )
                }

                Spacer(modifier = Modifier.height(32.dp))
            }
        }
    }
}


// Profil Header Composable
@Composable
private fun ProfileHeader(currentUser: FirebaseUser?) {
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .background(
                brush = Brush.verticalGradient(
                    colors = listOf(
                        Color(0xFF00BCD4),
                        Color(0xFF0097A7)
                    )
                )
            )
            .padding(vertical = 32.dp, horizontal = 24.dp),
        contentAlignment = Alignment.Center
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            // Avatar
            Surface(
                modifier = Modifier.size(100.dp),
                color = Color.White,
                shape = CircleShape,
                tonalElevation = 8.dp
            ) {
                Box(contentAlignment = Alignment.Center) {
                    Image(
                        painter = painterResource(R.drawable.dozi),
                        contentDescription = "Avatar",
                        modifier = Modifier
                            .fillMaxSize()
                            .padding(20.dp)
                    )
                }
            }

            // İsim
            Text(
                text = currentUser?.displayName ?: "Kullanıcı",
                style = MaterialTheme.typography.headlineSmall,
                color = Color.White,
                fontWeight = FontWeight.Bold
            )

            // Email
            Text(
                text = currentUser?.email ?: "",
                style = MaterialTheme.typography.bodyMedium,
                color = Color.White.copy(alpha = 0.9f)
            )

            // Plan Badge
            Surface(
                shape = RoundedCornerShape(20.dp),
                color = Color.White.copy(alpha = 0.2f),
                modifier = Modifier.padding(top = 8.dp)
            ) {
                Row(
                    modifier = Modifier.padding(horizontal = 16.dp, vertical = 6.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    Icon(
                        Icons.Filled.Star,
                        contentDescription = null,
                        tint = Color(0xFFFFB300),
                        modifier = Modifier.size(16.dp)
                    )
                    Text(
                        text = "Ücretsiz Plan",
                        style = MaterialTheme.typography.labelMedium,
                        color = Color.White,
                        fontWeight = FontWeight.Medium
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
        colors = ButtonDefaults.buttonColors(containerColor = Color.White),
        elevation = ButtonDefaults.buttonElevation(defaultElevation = 3.dp)
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
        ),
        elevation = ButtonDefaults.buttonElevation(
            defaultElevation = 3.dp,
            pressedElevation = 6.dp
        )
    ) {
        Text(
            text = label,
            style = MaterialTheme.typography.bodyLarge,
            fontWeight = FontWeight.SemiBold
        )
    }
}


// 🌟 Menü Kartı
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
        shape = RoundedCornerShape(18.dp),
        colors = CardDefaults.cardColors(containerColor = Color.White),
        elevation = CardDefaults.cardElevation(defaultElevation = 1.dp)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(18.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            Surface(
                modifier = Modifier.size(46.dp),
                shape = CircleShape,
                color = color.copy(alpha = 0.15f)
            ) {
                Box(contentAlignment = Alignment.Center) {
                    Icon(icon, null, tint = color)
                }
            }

            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = title,
                    style = MaterialTheme.typography.titleMedium,
                    color = TextPrimary,
                    fontWeight = FontWeight.Bold
                )
                Text(
                    text = desc,
                    style = MaterialTheme.typography.bodyMedium,
                    color = TextSecondary
                )
            }

            Icon(
                Icons.Default.ChevronRight,
                contentDescription = null,
                tint = TextSecondary
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