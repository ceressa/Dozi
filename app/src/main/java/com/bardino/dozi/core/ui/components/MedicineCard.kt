package com.bardino.dozi.core.ui.components

import androidx.compose.animation.*
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.rotate
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.bardino.dozi.core.data.model.Medicine
import com.bardino.dozi.core.ui.theme.*

@Composable
fun MedicineCard(
    medicineName: String,
    dosage: String,
    stockCount: Int,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    reminders: List<Medicine> = emptyList(),
    onAddReminder: (() -> Unit)? = null,
    onReminderClick: ((String) -> Unit)? = null
) {
    var expanded by remember { mutableStateOf(false) }
    val rotationAngle by animateFloatAsState(
        targetValue = if (expanded) 180f else 0f,
        label = "rotation"
    )

    Card(
        modifier = modifier
            .fillMaxWidth()
            .shadow(8.dp, RoundedCornerShape(20.dp)),
        shape = RoundedCornerShape(20.dp),
        colors = CardDefaults.cardColors(
            containerColor = Color.White
        ),
        elevation = CardDefaults.cardElevation(defaultElevation = 0.dp)
    ) {
        Column {
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
                        .clickable(onClick = onClick)
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
                                    stockCount < 10 -> Color(0xFFF59E0B)
                                    else -> Color(0xFF059669)
                                }
                            )
                        }
                    }
                }
            }

            // Hatırlatmalar bölümü
            if (reminders.isNotEmpty() || onAddReminder != null) {
                HorizontalDivider(color = Gray200.copy(alpha = 0.5f))

                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clickable { expanded = !expanded }
                        .padding(horizontal = 22.dp, vertical = 12.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Row(
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(
                            Icons.Default.NotificationsActive,
                            contentDescription = null,
                            tint = DoziTurquoise,
                            modifier = Modifier.size(20.dp)
                        )
                        Text(
                            text = "Hatırlatmalar (${reminders.size})",
                            style = MaterialTheme.typography.bodyMedium,
                            fontWeight = FontWeight.Bold,
                            color = DoziTurquoise
                        )
                    }
                    Icon(
                        Icons.Default.ExpandMore,
                        contentDescription = if (expanded) "Kapat" else "Aç",
                        tint = DoziTurquoise,
                        modifier = Modifier
                            .size(24.dp)
                            .rotate(rotationAngle)
                    )
                }
            }

            // Expandable hatırlatma listesi
            AnimatedVisibility(
                visible = expanded,
                enter = expandVertically() + fadeIn(),
                exit = shrinkVertically() + fadeOut()
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .background(Gray100.copy(alpha = 0.3f))
                        .padding(horizontal = 22.dp, vertical = 12.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    if (reminders.isEmpty()) {
                        Text(
                            text = "Henüz hatırlatma eklenmemiş",
                            style = MaterialTheme.typography.bodySmall,
                            color = Gray600,
                            modifier = Modifier.padding(vertical = 8.dp)
                        )
                    } else {
                        reminders.forEach { reminder ->
                            ReminderItem(
                                reminder = reminder,
                                onClick = { onReminderClick?.invoke(reminder.id) }
                            )
                        }
                    }

                    // Yeni hatırlatma ekle butonu
                    if (onAddReminder != null) {
                        OutlinedButton(
                            onClick = onAddReminder,
                            modifier = Modifier.fillMaxWidth(),
                            colors = ButtonDefaults.outlinedButtonColors(
                                contentColor = DoziTurquoise
                            ),
                            border = BorderStroke(
                                1.dp,
                                DoziTurquoise
                            )
                        ) {
                            Icon(
                                Icons.Default.Add,
                                contentDescription = null,
                                modifier = Modifier.size(18.dp)
                            )
                            Spacer(Modifier.width(8.dp))
                            Text(
                                "Yeni Hatırlatma Ekle",
                                fontWeight = FontWeight.Medium,
                                style = MaterialTheme.typography.bodyMedium
                            )
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun ReminderItem(
    reminder: Medicine,
    onClick: () -> Unit
) {
    Surface(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick),
        shape = RoundedCornerShape(12.dp),
        color = Color.White,
        shadowElevation = 2.dp
    ) {
        Row(
            modifier = Modifier
                .padding(12.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column(
                verticalArrangement = Arrangement.spacedBy(4.dp),
                modifier = Modifier.weight(1f)
            ) {
                Row(
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(
                        Icons.Default.Schedule,
                        contentDescription = null,
                        tint = DoziCoral,
                        modifier = Modifier.size(16.dp)
                    )
                    Text(
                        text = reminder.times.joinToString(", "),
                        style = MaterialTheme.typography.bodyMedium,
                        fontWeight = FontWeight.Bold,
                        color = Gray900
                    )
                }
                Text(
                    text = "${reminder.dosage} ${reminder.unit} • ${reminder.frequency}",
                    style = MaterialTheme.typography.bodySmall,
                    color = Gray600
                )
            }
            Icon(
                Icons.Default.ChevronRight,
                contentDescription = null,
                tint = Gray400,
                modifier = Modifier.size(20.dp)
            )
        }
    }
}