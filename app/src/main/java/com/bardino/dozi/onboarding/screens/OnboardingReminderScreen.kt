package com.bardino.dozi.onboarding.screens

import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.bardino.dozi.R
import com.bardino.dozi.core.data.OnboardingPreferences
import com.bardino.dozi.core.ui.theme.*

/**
 * Onboarding - Hatırlatma Kurma Anlatımı (Dozi ile - İnteraktif)
 */
@Composable
fun OnboardingReminderScreen(
    onNext: () -> Unit,
    onTryNow: () -> Unit
) {
    val context = LocalContext.current

    // Onboarding'e geri dönme kontrolü
    LaunchedEffect(Unit) {
        if (OnboardingPreferences.isInOnboarding(context) &&
            OnboardingPreferences.getOnboardingStep(context) == "reminder_completed") {
            // Hatırlatma eklendi, state'i temizle ve devam et
            OnboardingPreferences.clearOnboardingState(context)
            onNext()
        }
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(BackgroundLight)
            .padding(horizontal = 24.dp, vertical = 16.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Spacer(Modifier.height(24.dp))

        // Dozi karakteri - Öğretici Dozi 2 (transparan arka plan)
        Image(
            painter = painterResource(id = R.drawable.dozi_teach2),
            contentDescription = "Dozi",
            modifier = Modifier.size(120.dp)
        )

        Spacer(Modifier.height(16.dp))

        // Ana kart
        Card(
            modifier = Modifier
                .fillMaxWidth()
                .shadow(4.dp, RoundedCornerShape(20.dp)),
            colors = CardDefaults.cardColors(
                containerColor = Color.White
            ),
            shape = RoundedCornerShape(20.dp)
        ) {
            Column(
                modifier = Modifier.padding(24.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                // Başlık
                Text(
                    text = "Hatırlatma Kurma ⏰",
                    style = MaterialTheme.typography.headlineSmall,
                    fontWeight = FontWeight.Bold,
                    color = DoziCoral,
                    textAlign = TextAlign.Center
                )

                Spacer(Modifier.height(16.dp))

                // Ana açıklama
                Text(
                    text = "İlaçların için hatırlatma kurmak çok basit! İşte adımlar:",
                    style = MaterialTheme.typography.bodyMedium,
                    color = TextSecondary,
                    textAlign = TextAlign.Center,
                    lineHeight = 20.sp
                )

                Spacer(Modifier.height(16.dp))

                // Adımlar - Daha kompakt
                StepCard(
                    number = "1",
                    title = "İlaç Seç",
                    description = "İlaçlardan birini seç",
                    icon = Icons.Default.MedicalServices,
                    color = DoziBlue
                )

                Spacer(Modifier.height(8.dp))

                StepCard(
                    number = "2",
                    title = "Zaman Belirle",
                    description = "Saat ve sıklık ayarla",
                    icon = Icons.Default.AccessTime,
                    color = DoziTurquoise
                )

                Spacer(Modifier.height(8.dp))

                StepCard(
                    number = "3",
                    title = "Kaydet",
                    description = "Bildirim göndereceğim!",
                    icon = Icons.Default.NotificationsActive,
                    color = DoziCoral
                )
            }
        }

        Spacer(Modifier.height(20.dp))

        // Şimdi Dene butonu (interaktif)
        Button(
            onClick = {
                // Onboarding state'ini kaydet
                OnboardingPreferences.setOnboardingStep(context, "reminder")
                onTryNow()
            },
            modifier = Modifier
                .fillMaxWidth()
                .height(56.dp),
            colors = ButtonDefaults.buttonColors(
                containerColor = DoziCoral
            ),
            shape = RoundedCornerShape(16.dp)
        ) {
            Icon(Icons.Default.NotificationsActive, "Hatırlatma")
            Spacer(Modifier.width(8.dp))
            Text(
                text = "Şimdi Dene!",
                fontWeight = FontWeight.Bold
            )
        }

        Spacer(Modifier.height(12.dp))

        // Devam butonu
        OutlinedButton(
            onClick = onNext,
            modifier = Modifier
                .fillMaxWidth()
                .height(56.dp),
            shape = RoundedCornerShape(16.dp),
            colors = ButtonDefaults.outlinedButtonColors(
                contentColor = DoziCoral
            )
        ) {
            Text(
                text = "Anladım, Devam",
                fontWeight = FontWeight.Bold
            )
            Spacer(Modifier.width(8.dp))
            Icon(Icons.Default.ArrowForward, "İleri")
        }
    }
}

@Composable
private fun StepCard(
    number: String,
    title: String,
    description: String,
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    color: Color
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(
            containerColor = color.copy(alpha = 0.08f)
        ),
        shape = RoundedCornerShape(12.dp)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            // Numara
            Box(
                modifier = Modifier
                    .size(48.dp)
                    .background(color, CircleShape),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = number,
                    style = MaterialTheme.typography.titleLarge,
                    fontWeight = FontWeight.Bold,
                    color = Color.White
                )
            }

            Spacer(Modifier.width(16.dp))

            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = title,
                    style = MaterialTheme.typography.titleSmall,
                    fontWeight = FontWeight.Bold,
                    color = color
                )
                Spacer(Modifier.height(4.dp))
                Text(
                    text = description,
                    style = MaterialTheme.typography.bodySmall,
                    color = TextSecondary
                )
            }

            Icon(
                imageVector = icon,
                contentDescription = null,
                tint = color.copy(alpha = 0.6f),
                modifier = Modifier.size(28.dp)
            )
        }
    }
}
