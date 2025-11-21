package com.bardino.dozi.onboarding.screens

import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalLifecycleOwner
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.repeatOnLifecycle
import com.bardino.dozi.R
import com.bardino.dozi.core.data.OnboardingPreferences
import com.bardino.dozi.core.ui.theme.*

@Composable
fun OnboardingMedicineReminderScreen(
    onNext: () -> Unit,
    onTryMedicine: () -> Unit,
    onTryReminder: () -> Unit
) {
    val context = LocalContext.current
    val lifecycleOwner = LocalLifecycleOwner.current
    var currentPhase by remember { mutableStateOf("initial") }

    // Ekran ilk açıldığında onboarding state'ini başlat
    LaunchedEffect(Unit) {
        OnboardingPreferences.setOnboardingStep(context, "medicine")
    }

    // Ekran her görünür olduğunda (back navigation dahil) state'i kontrol et
    LaunchedEffect(lifecycleOwner) {
        lifecycleOwner.lifecycle.repeatOnLifecycle(Lifecycle.State.RESUMED) {
            val step = OnboardingPreferences.getOnboardingStep(context)
            when (step) {
                "medicine_completed", "reminder_completed" -> {
                    // Onboarding tamamlandı, Premium'a git
                    onNext()
                }
            }
        }
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(BackgroundLight)
    ) {
        Column(
            modifier = Modifier
                .weight(1f)
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 24.dp, vertical = 16.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Spacer(Modifier.height(16.dp))

            Text(
                text = "Adım 2/3",
                style = MaterialTheme.typography.labelLarge,
                color = DoziPurple,
                fontWeight = FontWeight.Bold
            )

            Spacer(Modifier.height(16.dp))

            Image(
                painter = painterResource(
                    id = if (currentPhase == "initial") R.drawable.dozi_teach1 else R.drawable.dozi_teach2
                ),
                contentDescription = "Dozi",
                modifier = Modifier.size(120.dp)
            )

            Spacer(Modifier.height(16.dp))

            when (currentPhase) {
                "initial" -> InitialPhaseContent()
                "reminder" -> ReminderPhaseContent()
            }

            Spacer(Modifier.height(24.dp))
        }

        // Butonlar en altta sabit
        Surface(
            shadowElevation = 8.dp,
            color = BackgroundLight
        ) {
            Column(
                modifier = Modifier.padding(24.dp)
            ) {
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
    }
}

@Composable
private fun InitialPhaseContent() {
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
            Text(
                text = "İlaç Ekleme 💊",
                style = MaterialTheme.typography.headlineMedium,
                fontWeight = FontWeight.Bold,
                color = DoziPurple,
                textAlign = TextAlign.Center
            )

            Spacer(Modifier.height(8.dp))

            Text(
                text = "İlacını ekle, ben sana hatırlatayım!",
                style = MaterialTheme.typography.bodyLarge,
                color = TextSecondary,
                textAlign = TextAlign.Center,
                fontWeight = FontWeight.Medium
            )

            Spacer(Modifier.height(16.dp))

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
                        text = "İlaç eklediğinde otomatik olarak devam edeceğiz!",
                        style = MaterialTheme.typography.bodyMedium,
                        color = DoziPurple,
                        fontWeight = FontWeight.Bold
                    )
                }
            }
        }
    }
}

@Composable
private fun ReminderPhaseContent() {
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
                text = "Harika! İlacını ekledin.",
                style = MaterialTheme.typography.bodyMedium,
                color = SuccessGreen,
                fontWeight = FontWeight.Bold
            )
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