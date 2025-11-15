package com.bardino.dozi.core.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Medication
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
    modifier: Modifier = Modifier
) {
    Card(
        modifier = modifier
            .fillMaxWidth()
            .shadow(8.dp, RoundedCornerShape(20.dp))
            .clickable(onClick = onClick),
        shape = RoundedCornerShape(20.dp),
        colors = CardDefaults.cardColors(
            containerColor = Color.White
        ),
        elevation = CardDefaults.cardElevation(defaultElevation = 0.dp)
    ) {
        Box {
            // Sol tarafta renkli accent bar
            Box(
                modifier = Modifier
                    .width(6.dp)
                    .fillMaxHeight()
                    .background(
                        brush = Brush.verticalGradient(
                            colors = listOf(
                                DoziPrimary,
                                DoziSecondary
                            )
                        )
                    )
            )

            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(start = 6.dp)
                    .background(
                        brush = Brush.horizontalGradient(
                            colors = listOf(
                                DoziPrimaryLight.copy(alpha = 0.08f),
                                Color.White
                            )
                        )
                    )
                    .padding(16.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(
                    horizontalArrangement = Arrangement.spacedBy(14.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier.weight(1f)
                ) {
                    // İkon arka planı
                    Surface(
                        modifier = Modifier.size(56.dp),
                        shape = RoundedCornerShape(16.dp),
                        color = DoziPrimary.copy(alpha = 0.15f)
                    ) {
                        Box(
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                imageVector = Icons.Default.Medication,
                                contentDescription = null,
                                tint = DoziPrimaryDark,
                                modifier = Modifier.size(30.dp)
                            )
                        }
                    }

                    Column(
                        verticalArrangement = Arrangement.spacedBy(4.dp)
                    ) {
                        Text(
                            text = medicineName,
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold,
                            color = Gray900
                        )
                        Text(
                            text = dosage,
                            style = MaterialTheme.typography.bodyMedium,
                            color = Gray600,
                            fontWeight = FontWeight.Medium
                        )
                    }
                }

                // Stok bölümü
                Surface(
                    shape = RoundedCornerShape(14.dp),
                    color = when {
                        stockCount < 5 -> ErrorRed.copy(alpha = 0.12f)
                        stockCount < 10 -> WarningAmber.copy(alpha = 0.12f)
                        else -> SuccessGreen.copy(alpha = 0.12f)
                    }
                ) {
                    Column(
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.spacedBy(2.dp),
                        modifier = Modifier.padding(horizontal = 16.dp, vertical = 10.dp)
                    ) {
                        Text(
                            text = "Stok",
                            style = MaterialTheme.typography.labelSmall,
                            color = Gray600,
                            fontWeight = FontWeight.Medium
                        )
                        Text(
                            text = "$stockCount",
                            style = MaterialTheme.typography.headlineSmall,
                            fontWeight = FontWeight.Bold,
                            color = when {
                                stockCount < 5 -> ErrorRed
                                stockCount < 10 -> Color(0xFFF59E0B) // Daha koyu amber
                                else -> Color(0xFF059669) // Daha koyu yeşil
                            }
                        )
                    }
                }
            }
        }
    }
}