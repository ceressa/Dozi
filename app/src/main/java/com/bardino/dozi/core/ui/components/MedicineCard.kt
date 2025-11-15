package com.bardino.dozi.core.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Medication
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.bardino.dozi.core.ui.theme.*

@Composable
fun MedicineCard(
    medicineName: String,
    dosage: String,
    stockCount: Int,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    onEdit: (() -> Unit)? = null,
    onDelete: (() -> Unit)? = null
) {
    Card(
        modifier = modifier
            .fillMaxWidth()
            .shadow(8.dp, RoundedCornerShape(24.dp))
            .clickable(onClick = onClick),
        shape = RoundedCornerShape(24.dp),
        colors = CardDefaults.cardColors(
            containerColor = Color.White
        ),
        elevation = CardDefaults.cardElevation(defaultElevation = 0.dp)
    ) {
        Box {
            // Üstte renkli gradient border (Coral-Amber-Primary)
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(6.dp)
                    .background(
                        brush = Brush.horizontalGradient(
                            listOf(
                                DoziSecondary,
                                DoziAccent,
                                DoziPrimary
                            )
                        ),
                        shape = RoundedCornerShape(topStart = 24.dp, topEnd = 24.dp)
                    )
            )

            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(top = 6.dp)
                    .background(
                        brush = Brush.verticalGradient(
                            colors = listOf(
                                DoziSecondaryLight.copy(alpha = 0.05f),
                                Color.White
                            )
                        )
                    )
                    .padding(20.dp),
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                // Üst satır: İkon, isim, butonlar
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.Top
                ) {
                    Row(
                        horizontalArrangement = Arrangement.spacedBy(14.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        modifier = Modifier.weight(1f)
                    ) {
                        // İkon arka planı - gradient
                        Surface(
                            modifier = Modifier.size(56.dp),
                            shape = RoundedCornerShape(16.dp),
                            color = Color.Transparent
                        ) {
                            Box(
                                modifier = Modifier.background(
                                    brush = Brush.linearGradient(
                                        colors = listOf(
                                            DoziSecondary.copy(alpha = 0.2f),
                                            DoziAccent.copy(alpha = 0.15f)
                                        )
                                    )
                                ),
                                contentAlignment = Alignment.Center
                            ) {
                                Icon(
                                    imageVector = Icons.Default.Medication,
                                    contentDescription = null,
                                    tint = DoziSecondaryDark,
                                    modifier = Modifier.size(30.dp)
                                )
                            }
                        }

                        Column(
                            verticalArrangement = Arrangement.spacedBy(4.dp)
                        ) {
                            Text(
                                text = medicineName,
                                style = MaterialTheme.typography.titleLarge,
                                fontWeight = FontWeight.Bold,
                                color = Gray900,
                                maxLines = 1,
                                overflow = androidx.compose.ui.text.style.TextOverflow.Ellipsis
                            )
                            Text(
                                text = dosage,
                                style = MaterialTheme.typography.bodyMedium,
                                color = Gray600,
                                fontWeight = FontWeight.Medium
                            )
                        }
                    }

                    // Edit ve Delete butonları
                    if (onEdit != null || onDelete != null) {
                        Row(
                            horizontalArrangement = Arrangement.spacedBy(6.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            // Edit butonu
                            if (onEdit != null) {
                                Surface(
                                    shape = RoundedCornerShape(12.dp),
                                    color = DoziSecondary.copy(alpha = 0.15f)
                                ) {
                                    IconButton(onClick = onEdit) {
                                        Icon(
                                            Icons.Default.Edit,
                                            contentDescription = "Düzenle",
                                            tint = DoziSecondaryDark,
                                            modifier = Modifier.size(20.dp)
                                        )
                                    }
                                }
                            }

                            // Delete butonu
                            if (onDelete != null) {
                                Surface(
                                    shape = RoundedCornerShape(12.dp),
                                    color = ErrorRed.copy(alpha = 0.15f)
                                ) {
                                    IconButton(onClick = onDelete) {
                                        Icon(
                                            Icons.Default.Delete,
                                            contentDescription = "Sil",
                                            tint = ErrorRed,
                                            modifier = Modifier.size(20.dp)
                                        )
                                    }
                                }
                            }
                        }
                    }
                }

                // Stok bilgisi
                Surface(
                    shape = RoundedCornerShape(14.dp),
                    color = when {
                        stockCount < 5 -> ErrorRed.copy(alpha = 0.15f)
                        stockCount < 10 -> WarningAmber.copy(alpha = 0.15f)
                        else -> Color(0xFF059669).copy(alpha = 0.15f)
                    },
                    border = BorderStroke(
                        1.dp,
                        when {
                            stockCount < 5 -> ErrorRed.copy(alpha = 0.3f)
                            stockCount < 10 -> WarningAmber.copy(alpha = 0.3f)
                            else -> Color(0xFF059669).copy(alpha = 0.3f)
                        }
                    ),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Row(
                        modifier = Modifier.padding(horizontal = 16.dp, vertical = 12.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = "Stok Durumu",
                            style = MaterialTheme.typography.labelLarge,
                            color = Gray700,
                            fontWeight = FontWeight.Bold
                        )
                        Row(
                            horizontalArrangement = Arrangement.spacedBy(6.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                text = "$stockCount adet",
                                style = MaterialTheme.typography.titleLarge,
                                fontWeight = FontWeight.Bold,
                                color = when {
                                    stockCount < 5 -> ErrorRed
                                    stockCount < 10 -> Color(0xFFF59E0B) // Daha koyu amber
                                    else -> Color(0xFF059669) // Daha koyu yeşil
                                }
                            )
                            if (stockCount < 10) {
                                Text(
                                    text = "⚠️",
                                    style = MaterialTheme.typography.titleMedium
                                )
                            }
                        }
                    }
                }
            }
        }
    }
}