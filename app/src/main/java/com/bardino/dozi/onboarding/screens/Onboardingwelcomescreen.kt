package com.bardino.dozi.onboarding.screens

import androidx.compose.animation.core.*
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.bardino.dozi.R
import com.bardino.dozi.core.ui.theme.*

@Composable
fun OnboardingWelcomeScreen(
    onStartTour: () -> Unit,
    onSkip: () -> Unit
) {
    // Float animasyonu
    val infiniteTransition = rememberInfiniteTransition(label = "float")
    val floatOffset by infiniteTransition.animateFloat(
        initialValue = 0f,
        targetValue = 15f,
        animationSpec = infiniteRepeatable(
            animation = tween(2000, easing = EaseInOutSine),
            repeatMode = RepeatMode.Reverse
        ),
        label = "float"
    )

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(
                Brush.verticalGradient(
                    colors = listOf(
                        DoziTurquoise.copy(alpha = 0.1f),
                        DoziBlue.copy(alpha = 0.2f),
                        BackgroundLight
                    )
                )
            )
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(32.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {
            Spacer(Modifier.weight(1f))

            // Dozi logosu
            Image(
                painter = painterResource(id = R.drawable.dozi_hosgeldin),
                contentDescription = "Dozi Logo",
                modifier = Modifier
                    .size(200.dp)
                    .offset(y = floatOffset.dp)
            )

            Spacer(Modifier.height(32.dp))

            // Hoş geldin mesajı
            Text(
                text = "Dozi'ye Hoş Geldin! 💧",
                style = MaterialTheme.typography.headlineMedium,
                fontWeight = FontWeight.Bold,
                color = DoziTurquoise,
                textAlign = TextAlign.Center
            )

            Spacer(Modifier.height(16.dp))

            Text(
                text = "Sağlıklı yaşam asistanın",
                style = MaterialTheme.typography.titleMedium,
                color = TextSecondary,
                textAlign = TextAlign.Center
            )

            Spacer(Modifier.weight(1f))

            // Başlat butonu
            Button(
                onClick = onStartTour,
                modifier = Modifier
                    .fillMaxWidth()
                    .height(56.dp),
                colors = ButtonDefaults.buttonColors(
                    containerColor = DoziTurquoise
                ),
                shape = RoundedCornerShape(16.dp)
            ) {
                Text(
                    text = "Turu Başlat 🚀",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold
                )
            }

            Spacer(Modifier.height(16.dp))

            // Atla butonu
            TextButton(
                onClick = onSkip,
                modifier = Modifier.fillMaxWidth()
            ) {
                Text(
                    text = "Atla →",
                    style = MaterialTheme.typography.titleSmall,
                    color = TextSecondary
                )
            }
        }
    }
}