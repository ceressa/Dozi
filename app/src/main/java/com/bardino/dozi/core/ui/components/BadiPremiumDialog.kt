package com.bardino.dozi.core.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import com.bardino.dozi.core.ui.theme.DoziPurple
import com.bardino.dozi.core.ui.theme.DoziTurquoise

/**
 * Badi sistemi için özel premium bilgilendirme dialog'u
 * Badi'nin ne olduğunu örneklerle açıklar
 */
@Composable
fun BadiPremiumDialog(
    onDismiss: () -> Unit,
    onUpgrade: () -> Unit
) {
    Dialog(
        onDismissRequest = onDismiss,
        properties = DialogProperties(usePlatformDefaultWidth = false)
    ) {
        Card(
            modifier = Modifier
                .fillMaxWidth(0.95f)
                .padding(16.dp),
            shape = RoundedCornerShape(24.dp),
            colors = CardDefaults.cardColors(containerColor = Color.White),
            elevation = CardDefaults.cardElevation(defaultElevation = 8.dp)
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .verticalScroll(rememberScrollState())
            ) {
                // Gradient Header
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .background(
                            Brush.horizontalGradient(
                                listOf(DoziTurquoise, DoziPurple)
                            )
                        )
                        .padding(24.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Column(
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Default.People,
                            contentDescription = null,
                            tint = Color.White,
                            modifier = Modifier.size(48.dp)
                        )
                        Text(
                            text = "Badi Sistemi",
                            style = MaterialTheme.typography.headlineSmall,
                            fontWeight = FontWeight.Bold,
                            color = Color.White
                        )
                        Text(
                            text = "Dozi Ekstra Özelliği",
                            style = MaterialTheme.typography.bodyMedium,
                            color = Color.White.copy(alpha = 0.9f)
                        )
                    }
                }

                // Content
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(24.dp),
                    verticalArrangement = Arrangement.spacedBy(20.dp)
                ) {
                    // Badi Nedir?
                    Text(
                        text = "Badi Nedir?",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold,
                        color = DoziTurquoise
                    )

                    Text(
                        text = "Badi, sevdiklerinizin ilaç takibini birlikte yapmanızı sağlayan bir sistemdir. " +
                               "Aileniz veya arkadaşlarınızla ilaç hatırlatmalarını paylaşabilir, " +
                               "birbirinizin sağlığını takip edebilirsiniz.",
                        style = MaterialTheme.typography.bodyMedium,
                        color = Color(0xFF546E7A),
                        lineHeight = 22.sp
                    )

                    Divider(color = Color(0xFFE0E0E0))

                    // Örnek Kullanım Senaryoları
                    Text(
                        text = "Örnek Kullanım Senaryoları",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold,
                        color = DoziTurquoise
                    )

                    // Senaryo 1
                    BadiExampleCard(
                        icon = Icons.Default.FamilyRestroom,
                        title = "Yaşlı Ebeveyn Takibi",
                        description = "Anneniz veya babanızın ilaçlarını uzaktan takip edin. " +
                                     "İlaç alınmadığında size bildirim gelsin."
                    )

                    // Senaryo 2
                    BadiExampleCard(
                        icon = Icons.Default.ChildCare,
                        title = "Çocuk İlaç Takibi",
                        description = "Çocuğunuzun ilaçlarını eşinizle birlikte takip edin. " +
                                     "Kim ilaç verdiyse diğeri bilgilendirilsin."
                    )

                    // Senaryo 3
                    BadiExampleCard(
                        icon = Icons.Default.Favorite,
                        title = "Kronik Hasta Desteği",
                        description = "Diyabet, tansiyon gibi kronik hastalığı olan " +
                                     "bir yakınınızı destekleyin. Kritik ilaçlar için anında uyarı alın."
                    )

                    Divider(color = Color(0xFFE0E0E0))

                    // Özellikler
                    Text(
                        text = "Badi ile Neler Yapabilirsiniz?",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold,
                        color = DoziTurquoise
                    )

                    BadiFeatureItem(
                        icon = Icons.Default.Notifications,
                        text = "İlaç alınmadığında bildirim alın"
                    )
                    BadiFeatureItem(
                        icon = Icons.Default.Visibility,
                        text = "Sevdiklerinizin ilaç geçmişini görün"
                    )
                    BadiFeatureItem(
                        icon = Icons.Default.Warning,
                        text = "Kritik ilaçlar için acil uyarı"
                    )
                    BadiFeatureItem(
                        icon = Icons.Default.Chat,
                        text = "İlaç notları ve mesajlar paylaşın"
                    )

                    Spacer(modifier = Modifier.height(8.dp))

                    // Upgrade Button
                    Button(
                        onClick = onUpgrade,
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(56.dp),
                        shape = RoundedCornerShape(16.dp),
                        colors = ButtonDefaults.buttonColors(
                            containerColor = DoziTurquoise
                        )
                    ) {
                        Icon(
                            imageVector = Icons.Default.Star,
                            contentDescription = null,
                            tint = Color.White
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            text = "Dozi Ekstra'ya Geç",
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold,
                            color = Color.White
                        )
                    }

                    // Dismiss Button
                    TextButton(
                        onClick = onDismiss,
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Text(
                            text = "Daha Sonra",
                            color = Color(0xFF9E9E9E)
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun BadiExampleCard(
    icon: ImageVector,
    title: String,
    description: String
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(12.dp),
        colors = CardDefaults.cardColors(
            containerColor = Color(0xFFF5F5F5)
        )
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(12.dp),
            verticalAlignment = Alignment.Top
        ) {
            Icon(
                imageVector = icon,
                contentDescription = null,
                tint = DoziPurple,
                modifier = Modifier.size(24.dp)
            )
            Spacer(modifier = Modifier.width(12.dp))
            Column {
                Text(
                    text = title,
                    style = MaterialTheme.typography.bodyMedium,
                    fontWeight = FontWeight.Bold,
                    color = Color(0xFF1A237E)
                )
                Spacer(modifier = Modifier.height(4.dp))
                Text(
                    text = description,
                    style = MaterialTheme.typography.bodySmall,
                    color = Color(0xFF546E7A),
                    lineHeight = 18.sp
                )
            }
        }
    }
}

@Composable
private fun BadiFeatureItem(
    icon: ImageVector,
    text: String
) {
    Row(
        verticalAlignment = Alignment.CenterVertically,
        modifier = Modifier.padding(vertical = 4.dp)
    ) {
        Icon(
            imageVector = icon,
            contentDescription = null,
            tint = DoziTurquoise,
            modifier = Modifier.size(20.dp)
        )
        Spacer(modifier = Modifier.width(12.dp))
        Text(
            text = text,
            style = MaterialTheme.typography.bodyMedium,
            color = Color(0xFF546E7A)
        )
    }
}
