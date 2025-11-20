package com.bardino.dozi.onboarding.screens

import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Info
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.bardino.dozi.R
import com.bardino.dozi.core.ui.theme.*

@Composable
fun OnboardingLoginScreen(
    onGoogleSignIn: () -> Unit,
    onSkip: () -> Unit
) {
    var isLoading by remember { mutableStateOf(false) }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(BackgroundLight)
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 24.dp, vertical = 16.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Spacer(Modifier.height(32.dp))

            Image(
                painter = painterResource(id = R.drawable.dozi_hosgeldin),
                contentDescription = "Dozi",
                modifier = Modifier.size(120.dp)
            )

            Spacer(Modifier.height(24.dp))

            Text(
                text = "Dozi'ye Hoş Geldin!",
                style = MaterialTheme.typography.headlineLarge,
                fontWeight = FontWeight.ExtraBold,
                color = DoziTurquoise,
                textAlign = TextAlign.Center,
                fontSize = 28.sp
            )

            Spacer(Modifier.height(8.dp))

            Text(
                text = "Sana özel hediyemiz var!",
                style = MaterialTheme.typography.titleLarge,
                color = TextPrimary,
                textAlign = TextAlign.Center,
                fontWeight = FontWeight.Medium
            )

            Spacer(Modifier.height(32.dp))

            // 3 Gün Hediye kartı
            Card(
                colors = CardDefaults.cardColors(
                    containerColor = DoziCoral
                ),
                shape = RoundedCornerShape(20.dp),
                elevation = CardDefaults.cardElevation(defaultElevation = 8.dp)
            ) {
                Column(
                    modifier = Modifier.padding(24.dp),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Text(
                        text = "🎁",
                        fontSize = 48.sp
                    )
                    Spacer(Modifier.height(12.dp))
                    Text(
                        text = "3 Gün Ücretsiz",
                        style = MaterialTheme.typography.headlineSmall,
                        fontWeight = FontWeight.ExtraBold,
                        color = Color.White,
                        fontSize = 24.sp
                    )
                    Spacer(Modifier.height(4.dp))
                    Text(
                        text = "Dozi Ekstra",
                        style = MaterialTheme.typography.titleLarge,
                        fontWeight = FontWeight.Bold,
                        color = Color.White.copy(alpha = 0.95f)
                    )
                    Spacer(Modifier.height(8.dp))
                    Text(
                        text = "Tüm premium özellikleri dene!",
                        style = MaterialTheme.typography.bodyMedium,
                        color = Color.White.copy(alpha = 0.9f),
                        textAlign = TextAlign.Center
                    )
                }
            }

            Spacer(Modifier.height(32.dp))

            // Google ile Giriş butonu
            Button(
                onClick = {
                    isLoading = true
                    onGoogleSignIn()
                },
                modifier = Modifier
                    .fillMaxWidth()
                    .height(64.dp),
                colors = ButtonDefaults.buttonColors(
                    containerColor = DoziTurquoise
                ),
                shape = RoundedCornerShape(16.dp),
                enabled = !isLoading,
                elevation = ButtonDefaults.buttonElevation(
                    defaultElevation = 8.dp,
                    pressedElevation = 12.dp
                )
            ) {
                if (isLoading) {
                    CircularProgressIndicator(
                        modifier = Modifier.size(28.dp),
                        color = Color.White,
                        strokeWidth = 3.dp
                    )
                    Spacer(Modifier.width(16.dp))
                    Text(
                        text = "Giriş yapılıyor...",
                        style = MaterialTheme.typography.titleLarge,
                        fontWeight = FontWeight.Bold,
                        fontSize = 18.sp
                    )
                } else {
                    Icon(
                        painter = painterResource(id = R.drawable.ic_google),
                        contentDescription = "Google",
                        modifier = Modifier.size(28.dp),
                        tint = Color.White
                    )
                    Spacer(Modifier.width(16.dp))
                    Text(
                        text = "Google ile Giriş Yap",
                        style = MaterialTheme.typography.titleLarge,
                        fontWeight = FontWeight.Bold,
                        fontSize = 18.sp
                    )
                }
            }

            Spacer(Modifier.height(16.dp))

            // Bilgi kartı
            Card(
                colors = CardDefaults.cardColors(
                    containerColor = DoziBlue.copy(alpha = 0.1f)
                ),
                shape = RoundedCornerShape(12.dp)
            ) {
                Row(
                    modifier = Modifier.padding(16.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    Icon(
                        Icons.Default.Info,
                        contentDescription = null,
                        tint = DoziBlue,
                        modifier = Modifier.size(24.dp)
                    )
                    Text(
                        text = "Verilerini bulutta güvende tut ve tüm cihazlarında senkronize et.",
                        style = MaterialTheme.typography.bodyMedium,
                        color = TextPrimary,
                        lineHeight = 20.sp
                    )
                }
            }

            Spacer(Modifier.height(24.dp))

            // Atla butonu
            TextButton(
                onClick = onSkip,
                modifier = Modifier.fillMaxWidth()
            ) {
                Text(
                    text = "Şimdilik Atla",
                    style = MaterialTheme.typography.titleSmall,
                    color = TextSecondary
                )
            }

            Spacer(Modifier.height(16.dp))
        }
    }
}
