package com.bardino.dozi.core.ui.screens.premium

import androidx.compose.animation.*
import androidx.compose.animation.core.*
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
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
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.bardino.dozi.R
import com.bardino.dozi.core.ui.components.DoziTopBar
import com.bardino.dozi.core.ui.theme.*

data class PremiumPlan(
    val id: String,
    val title: String,
    val price: String,
    val period: String,
    val badge: String? = null,
    val features: List<String>,
    val badgeColor: Color = DoziCoral
)

@Composable
fun PremiumScreen(
    onNavigateBack: () -> Unit,
    onPurchase: (PlanType) -> Unit
) {
    var selectedPlan by remember { mutableStateOf("monthly") }

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
            badgeColor = DoziCoral,
            features = listOf(
                "Sınırsız ilaç ekleme",
                "Bulut yedekleme",
                "Sesli hatırlatıcılar",
                "Öncelikli destek"
            )
        ),
        PremiumPlan(
            id = "yearly",
            title = "Yıllık",
            price = "999₺",
            period = "yıl",
            badge = "EN AVANTAJLI",
            badgeColor = DoziTurquoise,
            features = listOf(
                "Sınırsız ilaç ekleme",
                "Bulut yedekleme",
                "Sesli hatırlatıcılar",
                "Öncelikli destek",
                "%44 tasarruf"
            )
        ),
        PremiumPlan(
            id = "monthly_family",
            title = "Aylık Aile",
            price = "249₺",
            period = "ay",
            badge = "AİLE PAKETİ",
            badgeColor = DoziPurple,
            features = listOf(
                "4 kişiye kadar",
                "Sınırsız ilaç ekleme",
                "Bulut yedekleme",
                "Sesli hatırlatıcılar",
                "Öncelikli destek",
                "Aile takip sistemi"
            )
        ),
        PremiumPlan(
            id = "yearly_family",
            title = "Yıllık Aile",
            price = "1999₺",
            period = "yıl",
            badge = "EN AVANTAJLI AİLE",
            badgeColor = DoziBlue,
            features = listOf(
                "4 kişiye kadar",
                "Sınırsız ilaç ekleme",
                "Bulut yedekleme",
                "Sesli hatırlatıcılar",
                "Öncelikli destek",
                "Aile takip sistemi",
                "%33 tasarruf"
            )
        )
    )

    Scaffold(
        topBar = {
            DoziTopBar(
                title = "Dozi Ekstra",
                canNavigateBack = true,
                onNavigateBack = onNavigateBack
            )
        },
        containerColor = BackgroundLight
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .verticalScroll(rememberScrollState())
        ) {
            // Hero Bölümü
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(BackgroundLight)
                    .padding(vertical = 32.dp, horizontal = 24.dp),
                contentAlignment = Alignment.Center
            ) {
                Column(
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.spacedBy(16.dp)
                ) {
                    AnimatedDozi()
                    Text(
                        text = "Dozi Ekstra 💧",
                        style = MaterialTheme.typography.headlineMedium,
                        fontWeight = FontWeight.Bold,
                        color = DoziTurquoise,
                        textAlign = TextAlign.Center
                    )
                    Text(
                        text = "7 gün ücretsiz dene, sonra devam et",
                        style = MaterialTheme.typography.bodyLarge,
                        color = TextSecondary,
                        textAlign = TextAlign.Center
                    )
                }
            }

            Spacer(Modifier.height(8.dp))

            // Özellikler Başlık
            Text(
                text = "✨ Premium Özellikler",
                style = MaterialTheme.typography.titleLarge,
                fontWeight = FontWeight.Bold,
                color = TextPrimary,
                modifier = Modifier.padding(horizontal = 24.dp)
            )

            Spacer(Modifier.height(16.dp))

            // Özellik Grid
            Column(
                modifier = Modifier.padding(horizontal = 24.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    FeatureCard(
                        icon = Icons.Default.AllInclusive,
                        title = "Sınırsız İlaç",
                        modifier = Modifier.weight(1f)
                    )
                    FeatureCard(
                        icon = Icons.Default.Cloud,
                        title = "Bulut Yedekleme",
                        modifier = Modifier.weight(1f)
                    )
                }

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    FeatureCard(
                        icon = Icons.Default.NotificationsActive,
                        title = "Akıllı Hatırlatma",
                        modifier = Modifier.weight(1f)
                    )
                    FeatureCard(
                        icon = Icons.Default.FamilyRestroom,
                        title = "Aile Yönetimi",
                        modifier = Modifier.weight(1f)
                    )
                }

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    FeatureCard(
                        icon = Icons.Default.MusicNote,
                        title = "Özel Sesler",
                        modifier = Modifier.weight(1f)
                    )
                    FeatureCard(
                        icon = Icons.Default.SupportAgent,
                        title = "Öncelikli Destek",
                        modifier = Modifier.weight(1f)
                    )
                }
            }

            Spacer(Modifier.height(32.dp))

            // Planlar Başlık
            Text(
                text = "🎯 Planını Seç",
                style = MaterialTheme.typography.titleLarge,
                fontWeight = FontWeight.Bold,
                color = TextPrimary,
                modifier = Modifier.padding(horizontal = 24.dp)
            )

            Spacer(Modifier.height(16.dp))

            // Paket seçenekleri
            Column(
                modifier = Modifier.padding(horizontal = 24.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                plans.forEach { plan ->
                    PremiumPlanCard(
                        plan = plan,
                        isSelected = selectedPlan == plan.id,
                        onClick = { selectedPlan = plan.id }
                    )
                }
            }

            Spacer(Modifier.height(32.dp))

            // Satın Al Butonu
            Column(
                modifier = Modifier
                    .padding(horizontal = 24.dp)
                    .padding(bottom = 24.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                Button(
                    onClick = {
                        val planType = when (selectedPlan) {
                            "ekstra_monthly" -> PlanType.EKSTRA_MONTHLY
                            "ekstra_yearly" -> PlanType.EKSTRA_YEARLY
                            "aile_monthly" -> PlanType.AILE_MONTHLY
                            "aile_yearly" -> PlanType.AILE_YEARLY
                            else -> PlanType.EKSTRA_MONTHLY
                        }
                        onPurchase(planType)
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
                        text = "Ücretsiz Denemeyi Başlat",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold
                    )
                }

                Text(
                    text = "İstediğin zaman iptal edebilirsin",
                    style = MaterialTheme.typography.bodySmall,
                    color = TextSecondary,
                    textAlign = TextAlign.Center,
                    modifier = Modifier.fillMaxWidth()
                )
            }
        }
    }
}

@Composable
private fun AnimatedDozi() {
    // Sabit Dozi King - Nefes almayan versiyon
    Image(
        painter = painterResource(R.drawable.dozi_king),
        contentDescription = "Dozi Premium",
        modifier = Modifier.size(120.dp)
    )
}

@Composable
private fun FeatureCard(
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    title: String,
    modifier: Modifier = Modifier
) {
    Box(
        modifier = modifier
            .height(90.dp)
            .clip(RoundedCornerShape(16.dp))
            .background(DoziTurquoise.copy(alpha = 0.08f))
            .padding(12.dp),
        contentAlignment = Alignment.Center
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {
            Icon(
                imageVector = icon,
                contentDescription = null,
                tint = DoziTurquoise,
                modifier = Modifier.size(28.dp)
            )
            Spacer(Modifier.height(6.dp))
            Text(
                text = title,
                style = MaterialTheme.typography.bodySmall,
                fontWeight = FontWeight.SemiBold,
                color = TextPrimary,
                textAlign = TextAlign.Center,
                fontSize = 11.sp
            )
        }
    }
}

@Composable
private fun PremiumPlanCard(
    plan: PremiumPlan,
    isSelected: Boolean,
    onClick: () -> Unit
) {
    val isFamilyPlan = plan.id.contains("family")

    val backgroundColor = when {
        isSelected -> plan.badgeColor.copy(alpha = 0.15f)
        else -> Gray100
    }

    val borderColor = when {
        isSelected -> plan.badgeColor
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
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.Top
        ) {
            // Sol kısım - Bilgiler ve Özellikler
            Column(modifier = Modifier.weight(1f)) {
                // Badge (opsiyonel)
                if (plan.badge != null) {
                    Box(
                        modifier = Modifier
                            .clip(RoundedCornerShape(8.dp))
                            .background(plan.badgeColor)
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
                        color = plan.badgeColor
                    )
                    Text(
                        text = "/ ${plan.period}",
                        style = MaterialTheme.typography.bodyMedium,
                        color = TextSecondary
                    )
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

            Spacer(Modifier.width(12.dp))

            // Sağ kısım - Dozi King Karakteri ve Radio Button
            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                // Radio Button - En üstte
                RadioButton(
                    selected = isSelected,
                    onClick = onClick,
                    colors = RadioButtonDefaults.colors(
                        selectedColor = plan.badgeColor,
                        unselectedColor = TextSecondary
                    )
                )

                // Dozi Karakter - Her plan için farklı
                Image(
                    painter = painterResource(
                        id = when (plan.id) {
                            "weekly" -> R.drawable.dozi_idea
                            "monthly" -> R.drawable.dozi_kalp
                            "yearly" -> R.drawable.dozi_perfect
                            "monthly_family" -> R.drawable.dozi_family
                            "yearly_family" -> R.drawable.dozi_king
                            else -> R.drawable.dozi_bravo
                        }
                    ),
                    contentDescription = "Dozi",
                    modifier = Modifier.size(70.dp)
                )
            }
        }
    }
}

// Plan Modeli - Yeni 3 kademeli yapı
enum class PlanType(
    val title: String,
    val price: String,
    val period: String
) {
    EKSTRA_MONTHLY("Ekstra Aylık", "149", "ay"),
    EKSTRA_YEARLY("Ekstra Yıllık", "999", "yıl"),
    AILE_MONTHLY("Aile Aylık", "249", "ay"),
    AILE_YEARLY("Aile Yıllık", "1999", "yıl")
}
