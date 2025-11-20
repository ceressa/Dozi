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
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.bardino.dozi.R
import com.bardino.dozi.core.data.OnboardingPreferences
import com.bardino.dozi.core.ui.theme.*

@Composable
fun OnboardingWelcomeScreen(
    onStartTour: () -> Unit,
    onSkip: () -> Unit
) {
    val context = LocalContext.current
    var neverShowAgain by remember { mutableStateOf(false) }

    // Yumuşak ölçeklendirme + rotate animasyonu
    val infiniteTransition = rememberInfiniteTransition(label = "gentle_animation")
    val scale by infiniteTransition.animateFloat(
        initialValue = 0.95f,
        targetValue = 1.05f,
        animationSpec = infiniteRepeatable(
            animation = tween(3000, easing = EaseInOutSine),
            repeatMode = RepeatMode.Reverse
        ),
        label = "scale"
    )
    val rotation by infiniteTransition.animateFloat(
        initialValue = -3f,
        targetValue = 3f,
        animationSpec = infiniteRepeatable(
            animation = tween(4000, easing = EaseInOutSine),
            repeatMode = RepeatMode.Reverse
        ),
        label = "rotation"
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

            // Dozi logosu - yumuşak animasyon
            Image(
                painter = painterResource(id = R.drawable.dozi_brand),
                contentDescription = "Dozi Logo",
                modifier = Modifier
                    .size(200.dp)
                    .graphicsLayer {
                        scaleX = scale
                        scaleY = scale
                        rotationZ = rotation
                    }
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

            Spacer(Modifier.weight(1f))

            // Başlat butonu
            Button(
                onClick = {
                    // "Bir daha gösterme" tercihini kaydet
                    if (neverShowAgain) {
                        OnboardingPreferences.setNeverShowOnboardingAgain(context, true)
                    }
                    onStartTour()
                },
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

            Spacer(Modifier.height(12.dp))

            // "Bir daha gösterme" checkbox
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 4.dp),
                horizontalArrangement = Arrangement.Center,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Checkbox(
                    checked = neverShowAgain,
                    onCheckedChange = { neverShowAgain = it },
                    colors = CheckboxDefaults.colors(
                        checkedColor = DoziTurquoise,
                        uncheckedColor = TextSecondary
                    )
                )
                Spacer(Modifier.width(4.dp))
                Text(
                    text = "Bir daha gösterme",
                    style = MaterialTheme.typography.bodyMedium,
                    color = TextSecondary
                )
            }

            Spacer(Modifier.height(8.dp))

            // Atla butonu
            TextButton(
                onClick = {
                    // "Bir daha gösterme" tercihini kaydet
                    if (neverShowAgain) {
                        OnboardingPreferences.setNeverShowOnboardingAgain(context, true)
                    }
                    onSkip()
                },
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