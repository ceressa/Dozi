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
 * Onboarding - İlaç Ekleme Ekranı
 * İlaç eklenince otomatik olarak premium ekranına geçiş yapar
 */
@Composable
fun OnboardingMedicineReminderScreen(
    onNext: () -> Unit,
    onTryMedicine: () -> Unit,
    onTryReminder: () -> Unit
) {
    val context = LocalContext.current
    var currentPhase by remember { mutableStateOf("initial") }

    // Onboarding'e geri dönme kontrolü
    LaunchedEffect(Unit) {
        val step = OnboardingPreferences.getOnboardingStep(context)
        when (step) {
            "medicine_completed" -> {
                // İlaç eklendi, premium ekranına geç (hatırlatma otomatik ekleniyor)
                OnboardingPreferences.clearOnboardingState(context)
                onNext()
            }
            "reminder_completed" -> {
                // Hatırlatma da eklendi, devam et
                OnboardingPreferences.clearOnboardingState(context)
                onNext()
            }
        }
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(BackgroundLight)
            .padding(horizontal = 24.dp, vertical = 16.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Spacer(Modifier.height(16.dp))

        // Adım göstergesi
        Text(
            text = "Adım 2/4",
            style = MaterialTheme.typography.labelLarge,
            color = DoziPurple,
            fontWeight = FontWeight.Bold
        )

        Spacer(Modifier.height(16.dp))

        // Dozi karakteri
        Image(
            painter = painterResource(
                id = if (currentPhase == "initial") R.drawable.dozi_teach1 else R.drawable.dozi_teach2
            ),
            contentDescription = "Dozi",
            modifier = Modifier.size(120.dp)
        )

        Spacer(Modifier.height(16.dp))

        when (currentPhase) {
            "initial" -> InitialPhaseContent(
                onTryMedicine = {
                    OnboardingPreferences.setOnboardingStep(context, "medicine")
                    onTryMedicine()
                },
                onNext = onNext
            )
            "reminder" -> ReminderPhaseContent(
                onTryReminder = {
                    OnboardingPreferences.setOnboardingStep(context, "reminder")
                    onTryReminder()
                },
                onNext = onNext
            )
        }
    }
}

@Composable
private fun InitialPhaseContent(
    onTryMedicine: () -> Unit,
    onNext: () -> Unit
) {
    Column(
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
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
                    style = MaterialTheme.typography.headlineMedium,
                    fontWeight = FontWeight.Bold,
                    color = DoziPurple,
                    textAlign = TextAlign.Center
                )

                Spacer(Modifier.height(8.dp))

                // Ana açıklama
                Text(
                    text = "İlacını ekle, ben sana hatırlatayım!",
                    style = MaterialTheme.typography.bodyLarge,
                    color = TextSecondary,
                    textAlign = TextAlign.Center,
                    fontWeight = FontWeight.Medium
                )

                Spacer(Modifier.height(16.dp))

                // Yöntemler
                MethodCard(
                    icon = Icons.Default.Search,
                    title = "İsimle Ara",
                    description = "İlaç adını yaz, listeden seç",
                    color = DoziTurquoise
                )

                Spacer(Modifier.height(8.dp))

                MethodCard(
                    icon = Icons.Default.Mic,
                    title = "Sesle Söyle",
                    description = "Mikrofona söyle, ben ekleyeyim",
                    color = DoziCoral
                )

                Spacer(Modifier.height(8.dp))

                MethodCard(
                    icon = Icons.Default.QrCodeScanner,
                    title = "Barkod Tara",
                    description = "İlaç kutusunu tara",
                    color = DoziPurple
                )

                Spacer(Modifier.height(16.dp))

                // Bilgilendirme
                Card(
                    colors = CardDefaults.cardColors(
                        containerColor = DoziPurple.copy(alpha = 0.1f)
                    ),
                    shape = RoundedCornerShape(12.dp)
                ) {
                    Row(
                        modifier = Modifier.padding(14.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        Icon(
                            Icons.Default.CheckCircle,
                            contentDescription = null,
                            tint = DoziPurple,
                            modifier = Modifier.size(24.dp)
                        )
                        Text(
                            text = "İlaç eklediğinde, hatırlatmaları daha sonra özelleştirebilirsin!",
                            style = MaterialTheme.typography.bodyMedium,
                            color = DoziPurple,
                            fontWeight = FontWeight.Bold
                        )
                    }
                }
            }
        }

        Spacer(Modifier.height(20.dp))

        // Şimdi Dene butonu
        Button(
            onClick = onTryMedicine,
            modifier = Modifier
                .fillMaxWidth()
                .height(60.dp),
            colors = ButtonDefaults.buttonColors(
                containerColor = DoziPurple
            ),
            shape = RoundedCornerShape(16.dp),
            elevation = ButtonDefaults.buttonElevation(
                defaultElevation = 4.dp,
                pressedElevation = 8.dp
            )
        ) {
            Icon(Icons.Default.Add, "Ekle", modifier = Modifier.size(24.dp))
            Spacer(Modifier.width(8.dp))
            Text(
                text = "İlaç Ekle!",
                fontWeight = FontWeight.Bold,
                fontSize = 18.sp
            )
        }

        Spacer(Modifier.height(12.dp))

        // Devam butonu
        TextButton(
            onClick = onNext,
            modifier = Modifier
                .fillMaxWidth()
                .height(50.dp)
        ) {
            Text(
                text = "Şimdilik Atla",
                fontWeight = FontWeight.Medium,
                color = TextSecondary,
                fontSize = 16.sp
            )
            Spacer(Modifier.width(4.dp))
            Icon(Icons.Default.ArrowForward, "İleri", tint = TextSecondary)
        }
    }
}

@Composable
private fun ReminderPhaseContent(
    onTryReminder: () -> Unit,
    onNext: () -> Unit
) {
    Column(
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        // Başarı mesajı
        Card(
            modifier = Modifier.fillMaxWidth(),
            colors = CardDefaults.cardColors(
                containerColor = SuccessGreen.copy(alpha = 0.1f)
            ),
            shape = RoundedCornerShape(16.dp)
        ) {
            Row(
                modifier = Modifier.padding(16.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                Icon(
                    Icons.Default.CheckCircle,
                    contentDescription = null,
                    tint = SuccessGreen,
                    modifier = Modifier.size(32.dp)
                )
                Text(
                    text = "Harika! İlacını ekledin. Şimdi hatırlatma kurmanın zamanı!",
                    style = MaterialTheme.typography.bodyMedium,
                    color = SuccessGreen,
                    fontWeight = FontWeight.Bold
                )
            }
        }

        Spacer(Modifier.height(20.dp))

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

                Spacer(Modifier.height(12.dp))

                // Ana açıklama
                Text(
                    text = "İlaç almayı unutmaman için hatırlatma kurman gerekiyor. Çok basit:",
                    style = MaterialTheme.typography.bodyMedium,
                    color = TextSecondary,
                    textAlign = TextAlign.Center,
                    lineHeight = 20.sp
                )

                Spacer(Modifier.height(16.dp))

                // Adımlar
                StepCard(
                    number = "1",
                    title = "İlaç Seç",
                    description = "Az önce eklediğin ilacı seç",
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
                    description = "Zamanı gelince bildirim göndereceğim!",
                    icon = Icons.Default.NotificationsActive,
                    color = DoziCoral
                )

                Spacer(Modifier.height(16.dp))

                // Premium özellik vurgusu
                Card(
                    colors = CardDefaults.cardColors(
                        containerColor = DoziTurquoise.copy(alpha = 0.08f)
                    ),
                    shape = RoundedCornerShape(12.dp)
                ) {
                    Row(
                        modifier = Modifier.padding(12.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Icon(
                            Icons.Default.Diamond,
                            contentDescription = null,
                            tint = DoziTurquoise,
                            modifier = Modifier.size(20.dp)
                        )
                        Text(
                            text = "Premium'da sesli hatırlatmalar ve daha fazlası!",
                            style = MaterialTheme.typography.bodySmall,
                            color = DoziTurquoise,
                            fontWeight = FontWeight.Bold
                        )
                    }
                }
            }
        }

        Spacer(Modifier.height(20.dp))

        // Şimdi Dene butonu
        Button(
            onClick = onTryReminder,
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
                text = "Hatırlatma Kur!",
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
                text = "Sonra Kurarım",
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
