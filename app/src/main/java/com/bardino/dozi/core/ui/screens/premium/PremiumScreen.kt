package com.bardino.dozi.core.ui.screens.premium

import androidx.compose.animation.*
import androidx.compose.animation.core.*
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
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.unit.dp
import com.bardino.dozi.R
import com.bardino.dozi.core.ui.components.DoziTopBar
import com.bardino.dozi.core.ui.theme.*

@Composable
fun PremiumScreen(
    onNavigateBack: () -> Unit,
    onPurchase: (PlanType) -> Unit
) {
    var selectedPlan by remember { mutableStateOf(PlanType.YEARLY) }
    var isVisible by remember { mutableStateOf(false) }

    LaunchedEffect(Unit) {
        isVisible = true
    }

    Scaffold(
        topBar = {
            DoziTopBar(
                title = "",
                canNavigateBack = true,
                onNavigateBack = onNavigateBack
            )
        },
        containerColor = MaterialTheme.colorScheme.background
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .verticalScroll(rememberScrollState())
        ) {
            // 🌈 Hero Bölümü
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(
                        Brush.verticalGradient(
                            colors = listOf(DoziTurquoise, DoziBlue)
                        )
                    )
                    .padding(vertical = 48.dp, horizontal = 24.dp),
                contentAlignment = Alignment.Center
            ) {
                Column(
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    AnimatedDozi()
                    Text(
                        text = "Dozi Ekstra'ya Geç",
                        style = MaterialTheme.typography.headlineMedium,
                        fontWeight = FontWeight.Bold,
                        color = Color.White,
                        textAlign = TextAlign.Center
                    )
                    Text(
                        text = "Sağlık düzeninin en akıllı hali",
                        style = MaterialTheme.typography.bodyLarge,
                        color = Color.White.copy(alpha = 0.9f),
                        textAlign = TextAlign.Center
                    )
                }
            }

            Spacer(Modifier.height(28.dp))

            // ✨ Özellikler
            Column(
                modifier = Modifier.padding(horizontal = 24.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                Text(
                    text = "Dozi Ekstra Neler Sunar",
                    style = MaterialTheme.typography.titleLarge,
                    fontWeight = FontWeight.Bold,
                    color = TextPrimary
                )

                PremiumFeature("Gelişmiş ilaç istatistikleri ve grafikler")
                PremiumFeature("Kişisel alışkanlıklara göre akıllı hatırlatmalar")
                PremiumFeature("Bulut yedekleme ve cihazlar arası senkronizasyon")
                PremiumFeature("Özel bildirim sesleri ve titreşim ayarları")
                PremiumFeature("Aile üyelerini aynı ekrandan yönetme")
                PremiumFeature("Öncelikli destek ve erken erişim")
            }

            Spacer(Modifier.height(40.dp))

            // 💰 Planlar
            Column(
                modifier = Modifier.padding(horizontal = 20.dp),
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                Text(
                    text = "Planını Seç",
                    style = MaterialTheme.typography.titleLarge,
                    fontWeight = FontWeight.Bold,
                    color = TextPrimary
                )

                PricingOption(
                    planType = PlanType.WEEKLY,
                    isSelected = selectedPlan == PlanType.WEEKLY,
                    onSelect = { selectedPlan = PlanType.WEEKLY }
                )
                PricingOption(
                    planType = PlanType.MONTHLY,
                    isSelected = selectedPlan == PlanType.MONTHLY,
                    onSelect = { selectedPlan = PlanType.MONTHLY }
                )
                PricingOption(
                    planType = PlanType.YEARLY,
                    isSelected = selectedPlan == PlanType.YEARLY,
                    onSelect = { selectedPlan = PlanType.YEARLY },
                    highlight = "En Popüler"
                )
                PricingOption(
                    planType = PlanType.LIFETIME,
                    isSelected = selectedPlan == PlanType.LIFETIME,
                    onSelect = { selectedPlan = PlanType.LIFETIME },
                    highlight = "Tek Seferlik Ödeme"
                )
            }

            Spacer(Modifier.height(40.dp))

            // 🎯 Satın Al Butonu
            Column(
                modifier = Modifier
                    .padding(horizontal = 24.dp),
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                Button(
                    onClick = { onPurchase(selectedPlan) },
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(60.dp),
                    shape = RoundedCornerShape(16.dp),
                    colors = ButtonDefaults.buttonColors(containerColor = DoziCoral)
                ) {
                    Text(
                        text = "Dozi Ekstra'yı Başlat",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold,
                        color = Color.White
                    )
                }

                Text(
                    text = "7 gün ücretsiz dene • İstediğin zaman iptal et",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                    textAlign = TextAlign.Center,
                    modifier = Modifier.fillMaxWidth()
                )
            }

            Spacer(Modifier.height(60.dp))
        }
    }
}

// 🩺 Dozi animasyonu
@Composable
private fun AnimatedDozi() {
    val infiniteTransition = rememberInfiniteTransition(label = "dozi_anim")
    val scale by infiniteTransition.animateFloat(
        initialValue = 1f,
        targetValue = 1.1f,
        animationSpec = infiniteRepeatable(
            animation = tween(1600, easing = EaseInOutSine),
            repeatMode = RepeatMode.Reverse
        ),
        label = "scale"
    )
    Image(
        painter = painterResource(R.drawable.dozi_happy2),
        contentDescription = "Dozi Ekstra",
        modifier = Modifier
            .size(120.dp)
            .scale(scale)
    )
}

// ✨ Özellik maddesi
@Composable
private fun PremiumFeature(text: String) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(8.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Icon(
            imageVector = Icons.Default.CheckCircle,
            contentDescription = null,
            tint = DoziTurquoise,
            modifier = Modifier.size(20.dp)
        )
        Text(
            text = text,
            style = MaterialTheme.typography.bodyMedium,
            color = TextPrimary
        )
    }
}

// 💳 Plan tasarımı (yeni stil)
@Composable
private fun PricingOption(
    planType: PlanType,
    isSelected: Boolean,
    onSelect: () -> Unit,
    highlight: String? = null
) {
    val border = if (isSelected)
        Brush.horizontalGradient(GradientHero)
    else
        Brush.linearGradient(listOf(LightGray.copy(alpha = 0.5f), LightGray.copy(alpha = 0.5f)))

    Box(
        modifier = Modifier
            .fillMaxWidth()
            .border(2.dp, border, RoundedCornerShape(18.dp))
            .background(
                color = if (isSelected)
                    DoziTurquoise.copy(alpha = 0.08f)
                else
                    Color.White,
                shape = RoundedCornerShape(18.dp)
            )
            .clickable { onSelect() }
            .padding(vertical = 24.dp, horizontal = 20.dp)
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            highlight?.let {
                Surface(
                    color = DoziCoral,
                    shape = RoundedCornerShape(50)
                ) {
                    Text(
                        text = it,
                        style = MaterialTheme.typography.labelSmall,
                        color = Color.White,
                        fontWeight = FontWeight.Bold,
                        modifier = Modifier.padding(horizontal = 10.dp, vertical = 4.dp)
                    )
                }
            }

            Text(
                text = planType.title,
                style = MaterialTheme.typography.titleLarge,
                fontWeight = FontWeight.Bold,
                color = TextPrimary
            )

            Row(
                verticalAlignment = Alignment.Bottom,
                horizontalArrangement = Arrangement.Center
            ) {
                Text(
                    text = "₺${planType.price}",
                    style = MaterialTheme.typography.headlineLarge,
                    fontWeight = FontWeight.Bold,
                    color = DoziCoral
                )
                Text(
                    text = " / ${planType.period}",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                    modifier = Modifier.padding(start = 4.dp)
                )
            }

            planType.originalPrice?.let {
                Text(
                    text = "₺$it",
                    style = MaterialTheme.typography.bodySmall.copy(
                        textDecoration = TextDecoration.LineThrough
                    ),
                    color = TextSecondary
                )
            }

            planType.equivalentPrice?.let {
                Text(
                    text = it,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurface
                )
            }
        }
    }
}

// 📊 Plan Modeli
enum class PlanType(
    val title: String,
    val price: String,
    val period: String,
    val originalPrice: String? = null,
    val equivalentPrice: String? = null
) {
    WEEKLY("Haftalık", "24.99", "hafta", equivalentPrice = "~₺100/ay"),
    MONTHLY("Aylık", "69.99", "ay"),
    YEARLY(
        "Yıllık",
        "599.99",
        "yıl",
        originalPrice = "839.88",
        equivalentPrice = "Sadece ₺49.99/ay"
    ),
    LIFETIME(
        "Ömür Boyu",
        "1999.99",
        "tek seferlik",
        originalPrice = "7199.64",
        equivalentPrice = "Bir kez öde, sonsuza kadar kullan"
    )
}
