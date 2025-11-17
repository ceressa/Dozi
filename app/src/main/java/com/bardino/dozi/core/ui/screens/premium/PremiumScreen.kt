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
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
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
                    .padding(vertical = 40.dp, horizontal = 24.dp),
                contentAlignment = Alignment.Center
            ) {
                Column(
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    AnimatedDozi()
                    Text(
                        text = "Dozi Ekstra",
                        style = MaterialTheme.typography.headlineLarge,
                        fontWeight = FontWeight.Bold,
                        color = Color.White,
                        textAlign = TextAlign.Center
                    )
                    Text(
                        text = "Sağlık düzeninin en akıllı hali",
                        style = MaterialTheme.typography.bodyLarge,
                        color = Color.White.copy(alpha = 0.95f),
                        textAlign = TextAlign.Center
                    )
                }
            }

            Spacer(Modifier.height(32.dp))

            // ✨ Özellikler - Grid Layout
            Column(
                modifier = Modifier.padding(horizontal = 24.dp),
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                Text(
                    text = "✨ Premium Özellikler",
                    style = MaterialTheme.typography.titleLarge,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onSurface
                )

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    FeatureCard(
                        icon = Icons.Default.Analytics,
                        title = "Gelişmiş İstatistikler",
                        modifier = Modifier.weight(1f)
                    )
                    FeatureCard(
                        icon = Icons.Default.CloudUpload,
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
                        title = "Akıllı Hatırlatmalar",
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

            Spacer(Modifier.height(40.dp))

            // 💰 Planlar - Yeni Modern Tasarım
            Column(
                modifier = Modifier.padding(horizontal = 20.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                Text(
                    text = "🎯 Planını Seç",
                    style = MaterialTheme.typography.titleLarge,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onSurface
                )

                // En Popüler - Yıllık Plan
                ModernPricingCard(
                    planType = PlanType.YEARLY,
                    isSelected = selectedPlan == PlanType.YEARLY,
                    onSelect = { selectedPlan = PlanType.YEARLY },
                    badge = "⭐ En Popüler",
                    badgeColor = DoziCoral
                )

                // Aile Paketi - Özel Vurgu
                FamilyPlanCard(
                    planType = PlanType.FAMILY,
                    isSelected = selectedPlan == PlanType.FAMILY,
                    onSelect = { selectedPlan = PlanType.FAMILY }
                )

                // Ömür Boyu
                ModernPricingCard(
                    planType = PlanType.LIFETIME,
                    isSelected = selectedPlan == PlanType.LIFETIME,
                    onSelect = { selectedPlan = PlanType.LIFETIME },
                    badge = "💎 Tek Seferlik",
                    badgeColor = DoziPurple
                )

                // Diğer Planlar - Kompakt
                CompactPlanRow(
                    plans = listOf(PlanType.MONTHLY, PlanType.WEEKLY),
                    selectedPlan = selectedPlan,
                    onSelectPlan = { selectedPlan = it }
                )
            }

            Spacer(Modifier.height(32.dp))

            // 🎯 Satın Al Butonu
            Column(
                modifier = Modifier
                    .padding(horizontal = 24.dp)
                    .padding(bottom = 24.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                Button(
                    onClick = { onPurchase(selectedPlan) },
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(56.dp),
                    shape = RoundedCornerShape(16.dp),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = DoziCoral
                    )
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Default.Bolt,
                            contentDescription = null,
                            tint = Color.White
                        )
                        Text(
                            text = "Başlat",
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold,
                            color = Color.White
                        )
                    }
                }

                Text(
                    text = "✓ 7 gün ücretsiz deneme\n✓ İstediğin zaman iptal et",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    textAlign = TextAlign.Center,
                    modifier = Modifier.fillMaxWidth()
                )
            }
        }
    }
}

// 🩺 Dozi animasyonu
@Composable
private fun AnimatedDozi() {
    val infiniteTransition = rememberInfiniteTransition(label = "dozi_anim")
    val scale by infiniteTransition.animateFloat(
        initialValue = 1f,
        targetValue = 1.08f,
        animationSpec = infiniteRepeatable(
            animation = tween(1800, easing = EaseInOutSine),
            repeatMode = RepeatMode.Reverse
        ),
        label = "scale"
    )
    Image(
        painter = painterResource(R.drawable.dozi_king),
        contentDescription = "Dozi Premium",
        modifier = Modifier
            .size(100.dp)
            .scale(scale)
    )
}

// ✨ Feature Card
@Composable
private fun FeatureCard(
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    title: String,
    modifier: Modifier = Modifier
) {
    Card(
        modifier = modifier
            .height(90.dp),
        colors = CardDefaults.cardColors(
            containerColor = DoziTurquoise.copy(alpha = 0.08f)
        ),
        shape = RoundedCornerShape(16.dp)
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(12.dp),
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
                color = MaterialTheme.colorScheme.onSurface,
                textAlign = TextAlign.Center,
                fontSize = 11.sp
            )
        }
    }
}

// 💳 Modern Plan Card
@Composable
private fun ModernPricingCard(
    planType: PlanType,
    isSelected: Boolean,
    onSelect: () -> Unit,
    badge: String? = null,
    badgeColor: Color = DoziCoral
) {
    val borderBrush = if (isSelected)
        Brush.horizontalGradient(listOf(DoziTurquoise, DoziBlue))
    else
        Brush.linearGradient(listOf(LightGray.copy(alpha = 0.3f), LightGray.copy(alpha = 0.3f)))

    Card(
        modifier = Modifier
            .fillMaxWidth()
            .clickable { onSelect() },
        shape = RoundedCornerShape(20.dp),
        colors = CardDefaults.cardColors(
            containerColor = if (isSelected)
                DoziTurquoise.copy(alpha = 0.1f)
            else
                MaterialTheme.colorScheme.surface
        ),
        elevation = CardDefaults.cardElevation(
            defaultElevation = if (isSelected) 4.dp else 0.dp
        )
    ) {
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .border(
                    width = if (isSelected) 2.dp else 1.dp,
                    brush = borderBrush,
                    shape = RoundedCornerShape(20.dp)
                )
                .padding(20.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column(
                    verticalArrangement = Arrangement.spacedBy(4.dp)
                ) {
                    badge?.let {
                        Surface(
                            color = badgeColor,
                            shape = RoundedCornerShape(8.dp)
                        ) {
                            Text(
                                text = it,
                                style = MaterialTheme.typography.labelSmall,
                                color = Color.White,
                                fontWeight = FontWeight.Bold,
                                modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                            )
                        }
                        Spacer(Modifier.height(4.dp))
                    }

                    Text(
                        text = planType.title,
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onSurface
                    )

                    planType.equivalentPrice?.let {
                        Text(
                            text = it,
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }

                Column(
                    horizontalAlignment = Alignment.End,
                    verticalArrangement = Arrangement.spacedBy(2.dp)
                ) {
                    Row(
                        verticalAlignment = Alignment.Bottom
                    ) {
                        Text(
                            text = "₺${planType.price}",
                            style = MaterialTheme.typography.headlineSmall,
                            fontWeight = FontWeight.Bold,
                            color = DoziCoral
                        )
                        Text(
                            text = "/${planType.period}",
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            modifier = Modifier.padding(start = 2.dp, bottom = 2.dp)
                        )
                    }

                    planType.originalPrice?.let {
                        Text(
                            text = "₺$it",
                            style = MaterialTheme.typography.bodySmall.copy(
                                textDecoration = TextDecoration.LineThrough
                            ),
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
            }
        }
    }
}

// 👨‍👩‍👧‍👦 Aile Paketi Özel Kart
@Composable
private fun FamilyPlanCard(
    planType: PlanType,
    isSelected: Boolean,
    onSelect: () -> Unit
) {
    val borderBrush = if (isSelected)
        Brush.horizontalGradient(listOf(DoziCoral, DoziPink))
    else
        Brush.linearGradient(listOf(DoziCoral.copy(alpha = 0.3f), DoziPink.copy(alpha = 0.3f)))

    Card(
        modifier = Modifier
            .fillMaxWidth()
            .clickable { onSelect() },
        shape = RoundedCornerShape(20.dp),
        colors = CardDefaults.cardColors(
            containerColor = if (isSelected)
                DoziCoral.copy(alpha = 0.12f)
            else
                DoziCoral.copy(alpha = 0.05f)
        ),
        elevation = CardDefaults.cardElevation(
            defaultElevation = if (isSelected) 6.dp else 2.dp
        )
    ) {
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .border(
                    width = if (isSelected) 2.dp else 1.dp,
                    brush = borderBrush,
                    shape = RoundedCornerShape(20.dp)
                )
                .background(
                    Brush.verticalGradient(
                        colors = listOf(
                            DoziCoral.copy(alpha = 0.08f),
                            DoziPink.copy(alpha = 0.08f)
                        )
                    )
                )
                .padding(20.dp)
        ) {
            Column(
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                // Badge ve Başlık
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Surface(
                        color = DoziCoral,
                        shape = RoundedCornerShape(12.dp)
                    ) {
                        Row(
                            modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp),
                            horizontalArrangement = Arrangement.spacedBy(4.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Icon(
                                imageVector = Icons.Default.FamilyRestroom,
                                contentDescription = null,
                                tint = Color.White,
                                modifier = Modifier.size(18.dp)
                            )
                            Text(
                                text = "👨‍👩‍👧‍👦 Aile İçin",
                                style = MaterialTheme.typography.labelMedium,
                                color = Color.White,
                                fontWeight = FontWeight.Bold
                            )
                        }
                    }

                    // Fiyat
                    Column(
                        horizontalAlignment = Alignment.End
                    ) {
                        Row(
                            verticalAlignment = Alignment.Bottom
                        ) {
                            Text(
                                text = "₺${planType.price}",
                                style = MaterialTheme.typography.headlineMedium,
                                fontWeight = FontWeight.Bold,
                                color = DoziCoral
                            )
                            Text(
                                text = "/${planType.period}",
                                style = MaterialTheme.typography.bodyMedium,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                                modifier = Modifier.padding(start = 2.dp, bottom = 4.dp)
                            )
                        }
                        planType.originalPrice?.let {
                            Text(
                                text = "₺$it",
                                style = MaterialTheme.typography.bodySmall.copy(
                                    textDecoration = TextDecoration.LineThrough
                                ),
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }
                }

                // Açıklama
                Text(
                    text = planType.title,
                    style = MaterialTheme.typography.titleLarge,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onSurface
                )

                // Özellikler
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    FamilyFeatureChip("6 kişiye kadar")
                    FamilyFeatureChip("Sadece ₺74.99/ay")
                }

                // Öne Çıkan Özellikler
                Column(
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    FamilyFeatureRow("Aynı ekrandan tüm aileyi yönet")
                    FamilyFeatureRow("Her üye için ayrı profil")
                    FamilyFeatureRow("Paylaşımlı ilaç takibi")
                }
            }
        }
    }
}

@Composable
private fun FamilyFeatureChip(text: String) {
    Surface(
        color = DoziCoral.copy(alpha = 0.2f),
        shape = RoundedCornerShape(8.dp)
    ) {
        Text(
            text = text,
            style = MaterialTheme.typography.bodySmall,
            fontWeight = FontWeight.SemiBold,
            color = DoziCoral,
            modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp)
        )
    }
}

@Composable
private fun FamilyFeatureRow(text: String) {
    Row(
        horizontalArrangement = Arrangement.spacedBy(6.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Icon(
            imageVector = Icons.Default.CheckCircle,
            contentDescription = null,
            tint = DoziCoral,
            modifier = Modifier.size(16.dp)
        )
        Text(
            text = text,
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurface
        )
    }
}

// 📦 Kompakt Plan Row
@Composable
private fun CompactPlanRow(
    plans: List<PlanType>,
    selectedPlan: PlanType,
    onSelectPlan: (PlanType) -> Unit
) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        plans.forEach { plan ->
            CompactPlanCard(
                planType = plan,
                isSelected = selectedPlan == plan,
                onSelect = { onSelectPlan(plan) },
                modifier = Modifier.weight(1f)
            )
        }
    }
}

@Composable
private fun CompactPlanCard(
    planType: PlanType,
    isSelected: Boolean,
    onSelect: () -> Unit,
    modifier: Modifier = Modifier
) {
    Card(
        modifier = modifier
            .clickable { onSelect() },
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(
            containerColor = if (isSelected)
                DoziTurquoise.copy(alpha = 0.1f)
            else
                MaterialTheme.colorScheme.surface
        ),
        border = if (isSelected)
            BorderStroke(2.dp, DoziTurquoise)
        else
            BorderStroke(1.dp, LightGray.copy(alpha = 0.3f))
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(6.dp)
        ) {
            Text(
                text = planType.title,
                style = MaterialTheme.typography.titleSmall,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.onSurface
            )

            Text(
                text = "₺${planType.price}",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold,
                color = DoziCoral
            )

            Text(
                text = planType.period,
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
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
        "Yıllık Plan",
        "599.99",
        "yıl",
        originalPrice = "839.88",
        equivalentPrice = "Sadece ₺49.99/ay"
    ),
    FAMILY(
        "Aile Paketi",
        "899.99",
        "yıl",
        originalPrice = "1259.88",
        equivalentPrice = "6 kişiye kadar"
    ),
    LIFETIME(
        "Ömür Boyu",
        "1999.99",
        "tek seferlik",
        originalPrice = "7199.64",
        equivalentPrice = "Bir kez öde, sonsuza kadar kullan"
    )
}
