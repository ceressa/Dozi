package com.bardino.dozi.onboarding.screens

import androidx.compose.animation.core.*
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.bardino.dozi.R
import com.bardino.dozi.core.ui.theme.*

@Composable
fun OnboardingPremiumScreen(
    onGoogleSignIn: () -> Unit
) {
    // Pulse animasyonu
    val infiniteTransition = rememberInfiniteTransition(label = "pulse")
    val pulseScale by infiniteTransition.animateFloat(
        initialValue = 1f,
        targetValue = 1.05f,
        animationSpec = infiniteRepeatable(
            animation = tween(1000, easing = EaseInOutSine),
            repeatMode = RepeatMode.Reverse
        ),
        label = "pulse"
    )

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(
                Brush.verticalGradient(
                    colors = listOf(
                        DoziTurquoise.copy(alpha = 0.1f),
                        BackgroundLight
                    )
                )
            )
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(24.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Spacer(Modifier.height(40.dp))

            // Dozi karakteri - Kalp Dozi (hediye veriyor, transparan arka plan)
            Image(
                painter = painterResource(id = R.drawable.dozi_kalp),
                contentDescription = "Dozi Kalp",
                modifier = Modifier
                    .size(140.dp)
                    .scale(pulseScale)
            )

            // Hediye ikonu (kalp gibi)
            Text(
                text = "💝",
                style = MaterialTheme.typography.displayMedium,
                modifier = Modifier.offset(y = (-20).dp)
            )

            Spacer(Modifier.height(24.dp))

            // Başlık
            Text(
                text = "Tebrikler!",
                style = MaterialTheme.typography.headlineMedium,
                fontWeight = FontWeight.Bold,
                color = DoziTurquoise
            )

            Spacer(Modifier.height(8.dp))

            Text(
                text = "1 Haftalık Dozi Ekstra Hediye!",
                style = MaterialTheme.typography.titleLarge,
                fontWeight = FontWeight.Bold,
                color = TextPrimary,
                textAlign = TextAlign.Center
            )

            Spacer(Modifier.height(32.dp))

            // Özellikler
            Column(
                verticalArrangement = Arrangement.spacedBy(16.dp),
                modifier = Modifier.fillMaxWidth()
            ) {
                PremiumFeature(
                    icon = Icons.Default.AllInclusive,
                    title = "Sınırsız İlaç",
                    description = "Dilediğin kadar ilaç ekle"
                )
                PremiumFeature(
                    icon = Icons.Default.Cloud,
                    title = "Bulut Yedekleme",
                    description = "Verilerini güvenle sakla"
                )
                PremiumFeature(
                    icon = Icons.Default.FamilyRestroom,
                    title = "Aile Takibi",
                    description = "Sevdiklerini takip et"
                )
                PremiumFeature(
                    icon = Icons.Default.CameraAlt,
                    title = "Reçete OCR",
                    description = "Reçeteni fotoğrafla ekle"
                )
            }

            Spacer(Modifier.weight(1f))

            // Bilgilerini kaydet kartı
            Card(
                colors = CardDefaults.cardColors(
                    containerColor = DoziCoral.copy(alpha = 0.1f)
                ),
                shape = RoundedCornerShape(16.dp),
                modifier = Modifier.fillMaxWidth()
            ) {
                Row(
                    modifier = Modifier.padding(16.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    Icon(
                        Icons.Default.Info,
                        contentDescription = null,
                        tint = DoziCoral
                    )
                    Text(
                        text = "Bilgilerinin kaybolmaması için Google ile giriş yap",
                        style = MaterialTheme.typography.bodyMedium,
                        color = TextPrimary
                    )
                }
            }

            Spacer(Modifier.height(16.dp))

            // Google giriş butonu
            Button(
                onClick = onGoogleSignIn,
                modifier = Modifier
                    .fillMaxWidth()
                    .height(56.dp),
                colors = ButtonDefaults.buttonColors(
                    containerColor = Color.White
                ),
                border = BorderStroke(1.dp, Gray200),
                shape = RoundedCornerShape(16.dp)
            ) {
                Image(
                    painter = painterResource(id = R.drawable.ic_google),
                    contentDescription = "Google",
                    modifier = Modifier.size(24.dp)
                )
                Spacer(Modifier.width(12.dp))
                Text(
                    text = "Google ile Giriş Yap ve Başla",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                    color = TextPrimary
                )
            }

            Spacer(Modifier.height(24.dp))
        }
    }
}

@Composable
private fun PremiumFeature(
    icon: ImageVector,
    title: String,
    description: String
) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(16.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Box(
            modifier = Modifier
                .size(48.dp)
                .clip(CircleShape)
                .background(DoziTurquoise.copy(alpha = 0.15f)),
            contentAlignment = Alignment.Center
        ) {
            Icon(
                icon,
                contentDescription = null,
                tint = DoziTurquoise,
                modifier = Modifier.size(24.dp)
            )
        }

        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = title,
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold,
                color = TextPrimary
            )
            Text(
                text = description,
                style = MaterialTheme.typography.bodyMedium,
                color = TextSecondary
            )
        }

        Icon(
            Icons.Default.CheckCircle,
            contentDescription = null,
            tint = SuccessGreen
        )
    }
}