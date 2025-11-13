package com.bardino.dozi.core.ui.screens.login

import android.app.Activity
import android.content.Intent
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInVertically
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.google.android.gms.auth.api.signin.GoogleSignIn
import com.google.android.gms.auth.api.signin.GoogleSignInOptions
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.GoogleAuthProvider
import com.bardino.dozi.R
import com.bardino.dozi.core.data.repository.UserRepository
import com.bardino.dozi.core.ui.theme.*
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

@Composable
fun LoginScreen(onLoginSuccess: () -> Unit) {
    val context = androidx.compose.ui.platform.LocalContext.current
    val auth = FirebaseAuth.getInstance()
    val repo = UserRepository()
    val scope = rememberCoroutineScope()

    var isLoading by remember { mutableStateOf(false) }
    var showContent by remember { mutableStateOf(false) }

    // Animasyon için
    LaunchedEffect(Unit) {
        delay(200)
        showContent = true
    }

    val launcher = rememberLauncherForActivityResult(ActivityResultContracts.StartActivityForResult()) { result ->
        if (result.resultCode == Activity.RESULT_OK) {
            val task = GoogleSignIn.getSignedInAccountFromIntent(result.data)
            val account = task.result
            val credential = GoogleAuthProvider.getCredential(account.idToken, null)
            isLoading = true
            auth.signInWithCredential(credential).addOnSuccessListener {
                scope.launch {
                    repo.createUserIfNotExists()
                    isLoading = false
                    onLoginSuccess()
                }
            }.addOnFailureListener {
                isLoading = false
            }
        } else {
            isLoading = false
        }
    }

    val googleSignInClient = GoogleSignIn.getClient(
        context,
        GoogleSignInOptions.Builder(GoogleSignInOptions.DEFAULT_SIGN_IN)
            .requestIdToken(context.getString(R.string.default_web_client_id))
            .requestEmail()
            .build()
    )

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(
                brush = Brush.verticalGradient(
                    colors = listOf(
                        Color(0xFF00BCD4),
                        Color(0xFF0097A7),
                        Color(0xFF006064)
                    )
                )
            ),
        contentAlignment = Alignment.Center
    ) {
        AnimatedVisibility(
            visible = showContent,
            enter = fadeIn() + slideInVertically(initialOffsetY = { it / 2 }),
            exit = fadeOut()
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(32.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(24.dp)
            ) {
                // Dozi Logo
                Surface(
                    modifier = Modifier.size(120.dp),
                    shape = RoundedCornerShape(30.dp),
                    color = Color.White,
                    tonalElevation = 8.dp
                ) {
                    Image(
                        painter = painterResource(R.drawable.dozi),
                        contentDescription = "Dozi Logo",
                        modifier = Modifier
                            .fillMaxSize()
                            .padding(24.dp)
                    )
                }

                Spacer(modifier = Modifier.height(8.dp))

                // Başlık
                Text(
                    text = "Dozi'ye Hoş Geldin!",
                    style = MaterialTheme.typography.headlineMedium,
                    fontWeight = FontWeight.Bold,
                    color = Color.White,
                    textAlign = TextAlign.Center
                )

                // Alt Başlık
                Text(
                    text = "İlaç takibini kolaylaştır,\nsağlığını kontrol altında tut",
                    style = MaterialTheme.typography.bodyLarge,
                    color = Color.White.copy(alpha = 0.9f),
                    textAlign = TextAlign.Center,
                    lineHeight = 24.sp
                )

                Spacer(modifier = Modifier.height(24.dp))

                // Google Sign-In Butonu
                Button(
                    onClick = {
                        if (!isLoading) {
                            launcher.launch(googleSignInClient.signInIntent)
                        }
                    },
                    modifier = Modifier
                        .fillMaxWidth(0.85f)
                        .height(56.dp),
                    shape = RoundedCornerShape(16.dp),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = Color.White,
                        contentColor = Color.Black
                    ),
                    elevation = ButtonDefaults.buttonElevation(
                        defaultElevation = 4.dp,
                        pressedElevation = 8.dp
                    ),
                    enabled = !isLoading
                ) {
                    if (isLoading) {
                        CircularProgressIndicator(
                            modifier = Modifier.size(24.dp),
                            color = DoziTurquoise,
                            strokeWidth = 3.dp
                        )
                    } else {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.Center
                        ) {
                            Image(
                                painter = painterResource(id = R.drawable.ic_google),
                                contentDescription = "Google logo",
                                modifier = Modifier.size(24.dp)
                            )
                            Spacer(modifier = Modifier.width(12.dp))
                            Text(
                                text = "Google ile Giriş Yap",
                                style = MaterialTheme.typography.bodyLarge,
                                fontWeight = FontWeight.SemiBold
                            )
                        }
                    }
                }

                Spacer(modifier = Modifier.height(40.dp))

                // Özellikler
                Column(
                    modifier = Modifier.alpha(0.9f),
                    verticalArrangement = Arrangement.spacedBy(16.dp)
                ) {
                    FeatureItem(
                        icon = Icons.Default.Notifications,
                        text = "Zamanında hatırlatmalar"
                    )
                    FeatureItem(
                        icon = Icons.Default.MedicalServices,
                        text = "İlaç takibi ve yönetimi"
                    )
                    FeatureItem(
                        icon = Icons.Default.CloudSync,
                        text = "Tüm cihazlarında senkronize"
                    )
                }
            }
        }
    }
}

@Composable
private fun FeatureItem(icon: androidx.compose.ui.graphics.vector.ImageVector, text: String) {
    Row(
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        Icon(
            imageVector = icon,
            contentDescription = null,
            tint = Color.White,
            modifier = Modifier.size(20.dp)
        )
        Text(
            text = text,
            style = MaterialTheme.typography.bodyMedium,
            color = Color.White,
            fontWeight = FontWeight.Medium
        )
    }
}
