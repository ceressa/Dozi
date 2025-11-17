package com.bardino.dozi.onboarding.screens

import androidx.compose.animation.core.*
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.bardino.dozi.R
import com.bardino.dozi.core.ui.theme.*

data class PremiumPlan(
    val id: String,
    val title: String,
    val price: String,
    val period: String,
    val badge: String? = null,
    val features: List<String>
)

@Composable
fun OnboardingPremiumScreen(
    onGoogleSignIn: () -> Unit
) {
    var isLoading by remember { mutableStateOf(false) }
    var selectedPlan by remember { mutableStateOf("yearly") }

    val plans = listOf(
        PremiumPlan(
            id = "weekly",
            title = "Haftalık",
            price = "49₺",
            period = "hafta",
            features = listOf(
                "Sınırsız ilaç ekleme",
                "Bulut yedekleme",
                "Sesli hatırlatıcılar"
            )
        ),
        PremiumPlan(
            id = "monthly",
            title = "Aylık",
            price = "149₺",
            period = "ay",
            badge = "POPÜLER",
            features = listOf(
                "Sınırsız ilaç ekleme",
                "Bulut yedekleme",
                "Sesli hatırlatıcılar",
                "Öncelikli destek"
            )
        ),
        PremiumPlan(
            id = "yearly",
            title = "Yıllık Aile",
            price = "999₺",
            period = "yıl",
            badge = "EN AVANTAJLI",
            features = listOf(
                "3 kişilik aile paketi",
                "Sınırsız ilaç ekleme",
                "Bulut yedekleme",
                "Sesli hatırlatıcılar",
                "Öncelikli destek",
                "Aile takip sistemi"
            )
        )
    )

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(BackgroundLight)
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(horizontal = 24.dp, vertical = 16.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Spacer(Modifier.height(16.dp))

            // Başlık
            Text(
                text = "Dozi Ekstra 💧",
                style = MaterialTheme.typography.headlineMedium,
                fontWeight = FontWeight.Bold,
                color = DoziTurquoise
            )

            Spacer(Modifier.height(8.dp))

            Text(
                text = "7 gün ücretsiz dene, sonra devam et",
                style = MaterialTheme.typography.bodyLarge,
                color = TextSecondary,
                textAlign = TextAlign.Center
            )

            Spacer(Modifier.height(24.dp))

            // Paket seçenekleri
            plans.forEach { plan ->
                PremiumPlanCard(
                    plan = plan,
                    isSelected = selectedPlan == plan.id,
                    onClick = { selectedPlan = plan.id }
                )
                Spacer(Modifier.height(12.dp))
            }

            Spacer(Modifier.weight(1f))

            // Devam butonu
            Button(
                onClick = {
                    isLoading = true
                    onGoogleSignIn()
                },
                modifier = Modifier
                    .fillMaxWidth()
                    .height(56.dp),
                colors = ButtonDefaults.buttonColors(
                    containerColor = DoziTurquoise
                ),
                shape = RoundedCornerShape(16.dp),
                enabled = !isLoading
            ) {
                if (isLoading) {
                    CircularProgressIndicator(
                        modifier = Modifier.size(24.dp),
                        color = Color.White,
                        strokeWidth = 3.dp
                    )
                    Spacer(Modifier.width(12.dp))
                    Text(
                        text = "Başlatılıyor...",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold
                    )
                } else {
                    Text(
                        text = "Ücretsiz Denemeyi Başlat",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold
                    )
                }
            }

            Spacer(Modifier.height(8.dp))

            Text(
                text = "İstediğin zaman iptal edebilirsin",
                style = MaterialTheme.typography.bodySmall,
                color = TextSecondary,
                textAlign = TextAlign.Center
            )

            Spacer(Modifier.height(16.dp))
        }
    }
}

@Composable
private fun PremiumPlanCard(
    plan: PremiumPlan,
    isSelected: Boolean,
    onClick: () -> Unit
) {
    val backgroundColor = when {
        plan.id == "yearly" && isSelected -> DoziTurquoise.copy(alpha = 0.15f)
        plan.id == "yearly" -> DoziTurquoise.copy(alpha = 0.08f)
        isSelected -> DoziBlue.copy(alpha = 0.15f)
        else -> Gray100
    }

    val borderColor = when {
        plan.id == "yearly" && isSelected -> DoziTurquoise
        isSelected -> DoziBlue
        else -> Color.Transparent
    }

    Box(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(20.dp))
            .background(backgroundColor)
            .border(
                width = if (isSelected) 2.dp else 0.dp,
                color = borderColor,
                shape = RoundedCornerShape(20.dp)
            )
            .clickable(onClick = onClick)
            .padding(16.dp)
    ) {
        Column {
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.Top
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    // Badge (opsiyonel)
                    if (plan.badge != null) {
                        Box(
                            modifier = Modifier
                                .clip(RoundedCornerShape(8.dp))
                                .background(
                                    if (plan.id == "yearly") DoziTurquoise else DoziCoral
                                )
                                .padding(horizontal = 8.dp, vertical = 4.dp)
                        ) {
                            Text(
                                text = plan.badge,
                                style = MaterialTheme.typography.labelSmall,
                                fontWeight = FontWeight.Bold,
                                color = Color.White,
                                fontSize = 10.sp
                            )
                        }
                        Spacer(Modifier.height(8.dp))
                    }

                    Text(
                        text = plan.title,
                        style = MaterialTheme.typography.titleLarge,
                        fontWeight = FontWeight.Bold,
                        color = TextPrimary
                    )

                    Spacer(Modifier.height(4.dp))

                    Row(
                        verticalAlignment = Alignment.Bottom,
                        horizontalArrangement = Arrangement.spacedBy(4.dp)
                    ) {
                        Text(
                            text = plan.price,
                            style = MaterialTheme.typography.headlineSmall,
                            fontWeight = FontWeight.ExtraBold,
                            color = if (plan.id == "yearly") DoziTurquoise else DoziBlue
                        )
                        Text(
                            text = "/ ${plan.period}",
                            style = MaterialTheme.typography.bodyMedium,
                            color = TextSecondary
                        )
                    }
                }

                // Aile paketi için özel görsel
                if (plan.id == "yearly") {
                    Image(
                        painter = painterResource(id = R.drawable.dozi_family),
                        contentDescription = "Aile Paketi",
                        modifier = Modifier.size(80.dp)
                    )
                }

                // Seçim göstergesi
                if (plan.id != "yearly") {
                    RadioButton(
                        selected = isSelected,
                        onClick = onClick,
                        colors = RadioButtonDefaults.colors(
                            selectedColor = DoziBlue,
                            unselectedColor = TextSecondary
                        )
                    )
                }
            }

            Spacer(Modifier.height(12.dp))

            // Özellikler
            plan.features.forEach { feature ->
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    modifier = Modifier.padding(vertical = 4.dp)
                ) {
                    Icon(
                        Icons.Default.CheckCircle,
                        contentDescription = null,
                        tint = SuccessGreen,
                        modifier = Modifier.size(18.dp)
                    )
                    Text(
                        text = feature,
                        style = MaterialTheme.typography.bodyMedium,
                        color = TextPrimary
                    )
                }
            }
        }

        // Aile paketinde RadioButton sağ üstte
        if (plan.id == "yearly") {
            RadioButton(
                selected = isSelected,
                onClick = onClick,
                colors = RadioButtonDefaults.colors(
                    selectedColor = DoziTurquoise,
                    unselectedColor = TextSecondary
                ),
                modifier = Modifier.align(Alignment.TopEnd)
            )
        }
    }
}
