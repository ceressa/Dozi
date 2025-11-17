package com.bardino.dozi.onboarding.screens

import androidx.compose.animation.*
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
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.bardino.dozi.R
import com.bardino.dozi.core.ui.theme.*

/**
 * Onboarding - Ana Ekran Tanıtımı (Dozi Anlatımı)
 * Spotlight yerine Dozi character ile samimi anlatım
 */
@Composable
fun OnboardingHomeTourScreen(
    onNext: () -> Unit
) {
    var currentStep by remember { mutableIntStateOf(0) }

    val steps = listOf(
        OnboardingStep(
            title = "Ana Ekran 🏠",
            description = "Burası ana ekranın! Bugünkü ilaçlarını, hatırlatmalarını ve sağlık durumunu burada görebilirsin.",
            icon = Icons.Default.Home,
            color = DoziTurquoise,
            premiumFeature = "Premium'da gelişmiş istatistikler ve sağlık raporları!"
        ),
        OnboardingStep(
            title = "Akıllı Hatırlatmalar ⏰",
            description = "Her ilaç için sana hatırlatma göndereceğim. AL, ATLA veya ERTELE diyebilirsin.",
            icon = Icons.Default.Notifications,
            color = DoziCoral,
            premiumFeature = "Premium'da sesli hatırlatmalar ve akıllı öneriler!"
        ),
        OnboardingStep(
            title = "İlaç Yönetimi 💊",
            description = "Tüm ilaçların burada. Yeni ilaç ekleyebilir, düzenleyebilir ve takip edebilirsin.",
            icon = Icons.Default.MedicalServices,
            color = DoziBlue,
            premiumFeature = "Premium'da sınırsız ilaç ve bulut yedekleme!"
        ),
        OnboardingStep(
            title = "Aile Paketi 👨‍👩‍👧",
            description = "Sevdiklerini ekleyebilirsin. Onlar da seni takip edip destek olabilir! Premium Aile Paketi ile 3 kişiye kadar.",
            icon = Icons.Default.People,
            color = SuccessGreen,
            premiumFeature = "Aile Paketi ile aile sağlık raporları ve takip sistemi!"
        )
    )

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(BackgroundLight)
            .padding(24.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.SpaceBetween
    ) {
        Spacer(Modifier.height(16.dp))

        // Adım göstergesi
        Text(
            text = "Adım 3/3",
            style = MaterialTheme.typography.labelLarge,
            color = DoziTurquoise,
            fontWeight = FontWeight.Bold
        )

        Spacer(Modifier.height(24.dp))

        // Dozi karakteri - Değişen ifadeler (transparan arka plan)
        AnimatedContent(
            targetState = currentStep,
            transitionSpec = {
                fadeIn() + scaleIn() togetherWith fadeOut() + scaleOut()
            },
            label = "dozi_animation"
        ) { step ->
            Image(
                painter = painterResource(
                    id = when (step) {
                        0 -> R.drawable.dozi_teach3  // Ana ekran
                        1 -> R.drawable.dozi_teach4  // Hatırlatmalar
                        2 -> R.drawable.dozi_teach3  // İlaç listesi
                        else -> R.drawable.dozi_teach4 // Badi
                    }
                ),
                contentDescription = "Dozi",
                modifier = Modifier.size(140.dp)
            )
        }

        Spacer(Modifier.height(32.dp))

        // Step indicator
        Row(
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            steps.forEachIndexed { index, _ ->
                Box(
                    modifier = Modifier
                        .width(if (index == currentStep) 32.dp else 8.dp)
                        .height(8.dp)
                        .background(
                            color = if (index == currentStep) DoziTurquoise else Gray200,
                            shape = CircleShape
                        )
                )
            }
        }

        Spacer(Modifier.height(24.dp))

        // Ana içerik kartı
        AnimatedContent(
            targetState = currentStep,
            transitionSpec = {
                slideInHorizontally { it } + fadeIn() togetherWith
                        slideOutHorizontally { -it } + fadeOut()
            },
            label = "step_content"
        ) { step ->
            OnboardingStepCard(steps[step])
        }

        Spacer(Modifier.weight(1f))

        // Alt butonlar
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            if (currentStep > 0) {
                OutlinedButton(
                    onClick = { currentStep-- },
                    modifier = Modifier.weight(1f).height(56.dp),
                    shape = RoundedCornerShape(16.dp)
                ) {
                    Icon(Icons.Default.ArrowBack, "Geri")
                    Spacer(Modifier.width(8.dp))
                    Text("Geri", fontWeight = FontWeight.Bold)
                }
            }

            Button(
                onClick = {
                    if (currentStep < steps.size - 1) {
                        currentStep++
                    } else {
                        onNext()
                    }
                },
                modifier = Modifier
                    .weight(if (currentStep > 0) 1f else 1f)
                    .height(56.dp),
                colors = ButtonDefaults.buttonColors(
                    containerColor = steps[currentStep].color
                ),
                shape = RoundedCornerShape(16.dp)
            ) {
                Text(
                    text = if (currentStep < steps.size - 1) "Devam" else "Başlayalım!",
                    fontWeight = FontWeight.Bold
                )
                Spacer(Modifier.width(8.dp))
                Icon(
                    if (currentStep < steps.size - 1) Icons.Default.ArrowForward else Icons.Default.Check,
                    "İleri"
                )
            }
        }
    }
}

@Composable
private fun OnboardingStepCard(step: OnboardingStep) {
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
            // İkon
            Box(
                modifier = Modifier
                    .size(80.dp)
                    .background(step.color.copy(alpha = 0.15f), CircleShape),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = step.icon,
                    contentDescription = null,
                    tint = step.color,
                    modifier = Modifier.size(40.dp)
                )
            }

            Spacer(Modifier.height(20.dp))

            // Başlık
            Text(
                text = step.title,
                style = MaterialTheme.typography.headlineSmall,
                fontWeight = FontWeight.Bold,
                color = TextPrimary,
                textAlign = TextAlign.Center
            )

            Spacer(Modifier.height(12.dp))

            // Açıklama
            Text(
                text = step.description,
                style = MaterialTheme.typography.bodyLarge.copy(
                    fontSize = 16.sp,
                    lineHeight = 24.sp
                ),
                color = TextSecondary,
                textAlign = TextAlign.Center
            )

            Spacer(Modifier.height(16.dp))

            // Premium özellik vurgusu
            if (step.premiumFeature != null) {
                Card(
                    colors = CardDefaults.cardColors(
                        containerColor = DoziTurquoise.copy(alpha = 0.1f)
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
                            text = step.premiumFeature,
                            style = MaterialTheme.typography.bodySmall,
                            color = DoziTurquoise,
                            fontWeight = FontWeight.Bold
                        )
                    }
                }
            }
        }
    }
}

private data class OnboardingStep(
    val title: String,
    val description: String,
    val icon: ImageVector,
    val color: Color,
    val premiumFeature: String? = null
)
