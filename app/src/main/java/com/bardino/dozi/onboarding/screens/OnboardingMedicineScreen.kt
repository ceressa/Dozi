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
 * Onboarding - İlaç Ekleme Anlatımı (Dozi ile - İnteraktif)
 */
@Composable
fun OnboardingMedicineScreen(
    onNext: () -> Unit,
    onTryNow: () -> Unit
) {
    val context = LocalContext.current

    // Onboarding'e geri dönme kontrolü
    LaunchedEffect(Unit) {
        if (OnboardingPreferences.isInOnboarding(context) &&
            OnboardingPreferences.getOnboardingStep(context) == "medicine_completed") {
            // İlaç eklendi, state'i temizle ve devam et
            OnboardingPreferences.clearOnboardingState(context)
            onNext()
        }
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(BackgroundLight)
            .padding(24.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Spacer(Modifier.height(40.dp))

        // Dozi karakteri - Öğretici Dozi (transparan arka plan)
        Image(
            painter = painterResource(id = R.drawable.dozi_teach1),
            contentDescription = "Dozi",
            modifier = Modifier.size(140.dp)
        )

        Spacer(Modifier.height(24.dp))

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
                    text = "İlaç Ekleme 💊",
                    style = MaterialTheme.typography.headlineSmall,
                    fontWeight = FontWeight.Bold,
                    color = DoziBlue,
                    textAlign = TextAlign.Center
                )

                Spacer(Modifier.height(16.dp))

                // Ana açıklama
                Text(
                    text = "İlaçlarını eklemek çok kolay! Sana 3 farklı yol sunuyorum:",
                    style = MaterialTheme.typography.bodyLarge,
                    color = TextSecondary,
                    textAlign = TextAlign.Center,
                    lineHeight = 24.sp
                )

                Spacer(Modifier.height(24.dp))

                // Yöntemler
                MethodCard(
                    icon = Icons.Default.Search,
                    title = "İsimle Ara",
                    description = "İlaç adını yaz, listeden seç",
                    color = DoziTurquoise
                )

                Spacer(Modifier.height(12.dp))

                MethodCard(
                    icon = Icons.Default.Mic,
                    title = "Sesle Söyle",
                    description = "Mikrofona söyle, ben bulayım",
                    color = DoziCoral
                )

                Spacer(Modifier.height(12.dp))

                MethodCard(
                    icon = Icons.Default.QrCodeScanner,
                    title = "Barkod Tara",
                    description = "İlaç kutusunu tara, otomatik ekle",
                    color = DoziPurple
                )

                Spacer(Modifier.height(20.dp))

                // İpucu
                Card(
                    colors = CardDefaults.cardColors(
                        containerColor = DoziBlue.copy(alpha = 0.1f)
                    ),
                    shape = RoundedCornerShape(12.dp)
                ) {
                    Row(
                        modifier = Modifier.padding(12.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(
                            Icons.Default.Lightbulb,
                            contentDescription = null,
                            tint = DoziBlue,
                            modifier = Modifier.size(20.dp)
                        )
                        Spacer(Modifier.width(8.dp))
                        Text(
                            text = "İlaç ekledikten sonra stok sayısını da girmeyi unutma!",
                            style = MaterialTheme.typography.bodySmall,
                            color = DoziBlue,
                            fontWeight = FontWeight.Medium
                        )
                    }
                }
            }
        }

        Spacer(Modifier.height(32.dp))

        // Şimdi Dene butonu (interaktif)
        Button(
            onClick = {
                // Onboarding state'ini kaydet
                OnboardingPreferences.setOnboardingStep(context, "medicine")
                onTryNow()
            },
            modifier = Modifier
                .fillMaxWidth()
                .height(56.dp),
            colors = ButtonDefaults.buttonColors(
                containerColor = DoziBlue
            ),
            shape = RoundedCornerShape(16.dp)
        ) {
            Icon(Icons.Default.Add, "Ekle")
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
                contentColor = DoziBlue
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
private fun MethodCard(
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    title: String,
    description: String,
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
            Box(
                modifier = Modifier
                    .size(48.dp)
                    .background(color.copy(alpha = 0.2f), CircleShape),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = icon,
                    contentDescription = null,
                    tint = color,
                    modifier = Modifier.size(24.dp)
                )
            }

            Spacer(Modifier.width(16.dp))

            Column {
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
        }
    }
}
