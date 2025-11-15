package com.bardino.dozi.core.ui.components

import androidx.compose.animation.*
import androidx.compose.animation.core.*
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
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
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import coil.compose.AsyncImage
import com.bardino.dozi.R
import com.bardino.dozi.core.ui.theme.*

// Compatibility aliases for deprecated colors used in this file
private val DoziTurquoise = DoziPrimary
private val DoziCoral = DoziSecondary
private val DoziBlue = DoziCharacterLight

/**
 * 👑 Premium Badge Component
 *
 * Kullanıcı adının yanında premium rozeti gösterir
 */
@Composable
fun PremiumBadge(
    modifier: Modifier = Modifier,
    size: Dp = 20.dp,
    animated: Boolean = true
) {
    val infiniteTransition = rememberInfiniteTransition(label = "premium_badge")
    val scale by infiniteTransition.animateFloat(
        initialValue = 1f,
        targetValue = 1.15f,
        animationSpec = infiniteRepeatable(
            animation = tween(1200, easing = EaseInOutSine),
            repeatMode = RepeatMode.Reverse
        ),
        label = "scale"
    )

    Image(
        painter = painterResource(R.drawable.dozi_king),
        contentDescription = "Dozi Ekstra",
        modifier = modifier
            .size(size)
            .then(
                if (animated) Modifier.scale(scale) else Modifier
            )
    )
}

/**
 * ✨ Premium Avatar with Frame
 *
 * Premium kullanıcılar için özel çerçeveli avatar
 */
@Composable
fun PremiumAvatar(
    photoUrl: String?,
    isPremium: Boolean,
    size: Dp = 64.dp,
    modifier: Modifier = Modifier,
    onClick: (() -> Unit)? = null
) {
    Box(
        modifier = modifier
            .size(size)
            .then(
                if (onClick != null) Modifier.clickable { onClick() }
                else Modifier
            ),
        contentAlignment = Alignment.Center
    ) {
        // Premium gradient çerçeve
        if (isPremium) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .background(
                        brush = Brush.sweepGradient(
                            colors = listOf(
                                DoziGold,
                                DoziCoral,
                                DoziPink,
                                DoziGold
                            )
                        ),
                        shape = CircleShape
                    )
            )
        }

        // Avatar resmi
        if (photoUrl.isNullOrEmpty()) {
            // Varsayılan avatar
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(if (isPremium) 4.dp else 0.dp)
                    .background(DoziTurquoise.copy(alpha = 0.2f), CircleShape),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    Icons.Default.Person,
                    contentDescription = "Avatar",
                    tint = DoziTurquoise,
                    modifier = Modifier.size(size * 0.6f)
                )
            }
        } else {
            // Kullanıcı fotoğrafı
            AsyncImage(
                model = photoUrl,
                contentDescription = "Profil Fotoğrafı",
                modifier = Modifier
                    .fillMaxSize()
                    .padding(if (isPremium) 4.dp else 0.dp)
                    .clip(CircleShape),
                contentScale = ContentScale.Crop
            )
        }

        // Premium badge (sağ üst köşe)
        if (isPremium) {
            Box(
                modifier = Modifier
                    .align(Alignment.TopEnd)
                    .offset(x = 4.dp, y = (-4).dp)
            ) {
                PremiumBadge(size = size * 0.35f)
            }
        }
    }
}

/**
 * 🎁 Premium Feature Gate Dialog
 *
 * Premium olmayan kullanıcılara özelliği tanıtan popup
 */
@Composable
fun PremiumGateDialog(
    visible: Boolean,
    featureName: String,
    featureDescription: String,
    onDismiss: () -> Unit,
    onUpgrade: () -> Unit
) {
    if (!visible) return

    Dialog(onDismissRequest = onDismiss) {
        Card(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            shape = RoundedCornerShape(24.dp),
            colors = CardDefaults.cardColors(containerColor = Color.White)
        ) {
            Column(
                modifier = Modifier.padding(24.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                // Dozi King logosu
                Image(
                    painter = painterResource(R.drawable.dozi_king),
                    contentDescription = "Dozi Ekstra",
                    modifier = Modifier.size(80.dp)
                )

                // Başlık
                Text(
                    text = "Dozi Ekstra Özelliği",
                    style = MaterialTheme.typography.headlineSmall,
                    fontWeight = FontWeight.Bold,
                    color = TextPrimary,
                    textAlign = TextAlign.Center
                )

                // Özellik adı
                Surface(
                    color = DoziTurquoise.copy(alpha = 0.1f),
                    shape = RoundedCornerShape(12.dp)
                ) {
                    Text(
                        text = featureName,
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold,
                        color = DoziTurquoise,
                        modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp)
                    )
                }

                // Açıklama
                Text(
                    text = featureDescription,
                    style = MaterialTheme.typography.bodyMedium,
                    color = TextSecondary,
                    textAlign = TextAlign.Center
                )

                // Özellikler listesi (kısa)
                Column(
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    PremiumFeatureItem("Gelişmiş istatistikler")
                    PremiumFeatureItem("Özel bildirim sesleri")
                    PremiumFeatureItem("Bulut yedekleme")
                }

                Divider(color = Gray200)

                // Butonlar
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    // Daha sonra
                    OutlinedButton(
                        onClick = onDismiss,
                        modifier = Modifier.weight(1f),
                        shape = RoundedCornerShape(12.dp),
                        border = BorderStroke(1.dp, Gray200)
                    ) {
                        Text("Daha Sonra", color = TextSecondary)
                    }

                    // Premium'a geç
                    Button(
                        onClick = onUpgrade,
                        modifier = Modifier.weight(1f),
                        shape = RoundedCornerShape(12.dp),
                        colors = ButtonDefaults.buttonColors(
                            containerColor = DoziCoral
                        )
                    ) {
                        Icon(
                            Icons.Default.StarRate,
                            contentDescription = null,
                            modifier = Modifier.size(18.dp)
                        )
                        Spacer(Modifier.width(4.dp))
                        Text("Premium'a Geç", fontWeight = FontWeight.Bold)
                    }
                }
            }
        }
    }
}

/**
 * Premium özellik maddesi
 */
@Composable
private fun PremiumFeatureItem(text: String) {
    Row(
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        Icon(
            Icons.Default.CheckCircle,
            contentDescription = null,
            tint = SuccessGreen,
            modifier = Modifier.size(18.dp)
        )
        Text(
            text = text,
            style = MaterialTheme.typography.bodySmall,
            color = TextPrimary
        )
    }
}

/**
 * 🌟 Premium Banner
 *
 * Premium'a yükseltmeyi teşvik eden banner
 */
@Composable
fun PremiumPromoBanner(
    onUpgradeClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    Card(
        modifier = modifier
            .fillMaxWidth()
            .clickable { onUpgradeClick() },
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(
            containerColor = Color.Transparent
        )
    ) {
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .background(
                    Brush.horizontalGradient(
                        colors = listOf(
                            DoziTurquoise,
                            DoziBlue
                        )
                    )
                )
                .padding(16.dp)
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween,
                modifier = Modifier.fillMaxWidth()
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(12.dp),
                    modifier = Modifier.weight(1f)
                ) {
                    Image(
                        painter = painterResource(R.drawable.dozi_king),
                        contentDescription = "Dozi Ekstra",
                        modifier = Modifier.size(40.dp)
                    )

                    Column {
                        Text(
                            text = "Dozi Ekstra'ya Geç",
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold,
                            color = Color.White
                        )
                        Text(
                            text = "Premium özellikleri keşfet",
                            style = MaterialTheme.typography.bodySmall,
                            color = Color.White.copy(alpha = 0.9f)
                        )
                    }
                }

                Icon(
                    Icons.Default.ArrowForward,
                    contentDescription = null,
                    tint = Color.White,
                    modifier = Modifier.size(24.dp)
                )
            }
        }
    }
}

/**
 * 💎 Premium Status Card
 *
 * Kullanıcının premium durumunu gösteren kart
 */
@Composable
fun PremiumStatusCard(
    isPremium: Boolean,
    planType: String,
    daysRemaining: Int,
    onManageClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    Card(
        modifier = modifier.fillMaxWidth(),
        shape = RoundedCornerShape(20.dp),
        colors = CardDefaults.cardColors(
            containerColor = if (isPremium) DoziGold.copy(alpha = 0.1f) else Gray100
        ),
        border = if (isPremium) BorderStroke(2.dp, DoziGold) else null
    ) {
        Column(
            modifier = Modifier.padding(20.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween,
                modifier = Modifier.fillMaxWidth()
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    if (isPremium) {
                        PremiumBadge(size = 28.dp)
                    }
                    Text(
                        text = if (isPremium) "Dozi Ekstra" else "Ücretsiz Plan",
                        style = MaterialTheme.typography.titleLarge,
                        fontWeight = FontWeight.Bold,
                        color = if (isPremium) DoziGold else TextPrimary
                    )
                }

                if (isPremium) {
                    Surface(
                        color = DoziGold.copy(alpha = 0.2f),
                        shape = RoundedCornerShape(8.dp)
                    ) {
                        Text(
                            text = planType.uppercase(),
                            style = MaterialTheme.typography.labelMedium,
                            fontWeight = FontWeight.Bold,
                            color = DoziGold,
                            modifier = Modifier.padding(horizontal = 12.dp, vertical = 4.dp)
                        )
                    }
                }
            }

            if (isPremium && daysRemaining > 0) {
                Text(
                    text = "$daysRemaining gün kaldı",
                    style = MaterialTheme.typography.bodyMedium,
                    color = TextSecondary
                )
            }

            Button(
                onClick = onManageClick,
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(12.dp),
                colors = ButtonDefaults.buttonColors(
                    containerColor = if (isPremium) DoziGold else DoziCoral
                )
            ) {
                Text(
                    text = if (isPremium) "Planı Yönet" else "Premium'a Geç",
                    fontWeight = FontWeight.Bold,
                    color = Color.White
                )
            }
        }
    }
}
