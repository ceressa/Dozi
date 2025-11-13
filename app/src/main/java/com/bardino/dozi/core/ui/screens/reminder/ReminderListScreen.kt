package com.bardino.dozi.core.ui.screens.reminder

import android.content.Context
import androidx.compose.animation.*
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.bardino.dozi.core.data.model.Medicine
import com.bardino.dozi.core.data.repository.MedicineRepository
import com.bardino.dozi.core.ui.components.DoziTopBar
import com.bardino.dozi.core.ui.components.EmptyReminderList
import com.bardino.dozi.core.ui.theme.*
import kotlinx.coroutines.delay
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ReminderListScreen(
    onNavigateBack: () -> Unit,
    onNavigateToAddReminder: () -> Unit
) {
    val context = LocalContext.current
    val medicineRepository = remember { MedicineRepository() }
    var medicines by remember { mutableStateOf<List<Medicine>>(emptyList()) }
    var isVisible by remember { mutableStateOf(false) }

    // Firebase'den ilaçları sürekli dinle
    LaunchedEffect(Unit) {
        isVisible = true
        while (true) {
            try {
                medicines = medicineRepository.getAllMedicines()
            } catch (e: Exception) {
                e.printStackTrace()
            }
            delay(3000) // Her 3 saniyede bir yenile
        }
    }

    Scaffold(
        topBar = {
            DoziTopBar(
                title = "Hatırlatmalarım",
                canNavigateBack = false,
                backgroundColor = Color.Transparent,
                actions = {
                    Surface(
                        shape = androidx.compose.foundation.shape.CircleShape,
                        color = DoziCoralDark.copy(alpha = 0.15f),
                        modifier = Modifier.padding(end = 8.dp)
                    ) {
                        IconButton(onClick = onNavigateToAddReminder) {
                            Icon(
                                Icons.Default.AddCircleOutline,
                                contentDescription = "Yeni Hatırlatma",
                                tint = DoziCoralDark
                            )
                        }
                    }
                }
            )
        },
        floatingActionButton = {
            FloatingActionButton(
                onClick = onNavigateToAddReminder,
                containerColor = DoziTurquoise,
                contentColor = Color.White,
                shape = RoundedCornerShape(16.dp),
                modifier = Modifier.shadow(12.dp, RoundedCornerShape(16.dp))
            ) {
                Icon(
                    Icons.Default.Add,
                    contentDescription = "Yeni Hatırlatma",
                    modifier = Modifier.size(28.dp)
                )
            }
        },
        containerColor = BackgroundLight
    ) { padding ->

        if (medicines.isEmpty()) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(padding),
                contentAlignment = Alignment.Center
            ) {
                EmptyReminderList(onAddReminder = onNavigateToAddReminder)
            }
        } else {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(padding)
            ) {
                // Üst Bilgi Alanı - Modern Design
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .background(
                            brush = Brush.verticalGradient(
                                colors = listOf(
                                    DoziBlue,
                                    DoziBlue.copy(alpha = 0.85f)
                                )
                            ),
                            shape = RoundedCornerShape(bottomStart = 32.dp, bottomEnd = 32.dp)
                        )
                        .padding(horizontal = 24.dp, vertical = 32.dp)
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column(
                            verticalArrangement = Arrangement.spacedBy(8.dp),
                            modifier = Modifier.weight(1f)
                        ) {
                            Text(
                                text = "${medicines.size}",
                                color = Color.White,
                                style = MaterialTheme.typography.displaySmall,
                                fontWeight = FontWeight.Bold
                            )
                            Text(
                                text = "Aktif Hatırlatma",
                                color = Color.White.copy(alpha = 0.95f),
                                style = MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.Medium
                            )
                            Text(
                                text = "İlaçlarınızı zamanında almayı unutmayın",
                                color = Color.White.copy(alpha = 0.85f),
                                style = MaterialTheme.typography.bodyMedium
                            )
                        }

                        androidx.compose.material3.Surface(
                            shape = androidx.compose.foundation.shape.CircleShape,
                            color = Color.White.copy(alpha = 0.2f),
                            modifier = Modifier.size(72.dp)
                        ) {
                            Box(
                                contentAlignment = Alignment.Center
                            ) {
                                Icon(
                                    imageVector = Icons.Default.Notifications,
                                    contentDescription = null,
                                    tint = Color.White,
                                    modifier = Modifier.size(40.dp)
                                )
                            }
                        }
                    }
                }

                Spacer(Modifier.height(20.dp))

                // Hatırlatma Listesi
                LazyColumn(
                    contentPadding = PaddingValues(horizontal = 20.dp, vertical = 12.dp),
                    verticalArrangement = Arrangement.spacedBy(16.dp)
                ) {
                    items(medicines, key = { it.id }) { medicine ->
                        AnimatedVisibility(
                            visible = isVisible,
                            enter = fadeIn() + slideInVertically(initialOffsetY = { 40 })
                        ) {
                            MedicineCard(
                                medicine = medicine,
                                onDelete = {
                                    CoroutineScope(Dispatchers.IO).launch {
                                        medicineRepository.deleteMedicine(medicine.id)
                                    }
                                }
                            )
                        }
                    }

                    item { Spacer(Modifier.height(100.dp)) }
                }
            }
        }
    }
}

@Composable
private fun MedicineCard(
    medicine: Medicine,
    onDelete: () -> Unit
) {
    var showDeleteDialog by remember { mutableStateOf(false) }

    Card(
        modifier = Modifier
            .fillMaxWidth()
            .shadow(
                elevation = 8.dp,
                shape = RoundedCornerShape(24.dp),
                spotColor = DoziBlue.copy(alpha = 0.25f)
            ),
        shape = RoundedCornerShape(24.dp),
        colors = CardDefaults.cardColors(containerColor = Color.White)
    ) {
        Box {
            // Dekoratif top border
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(8.dp)
                    .background(
                        brush = Brush.horizontalGradient(
                            listOf(
                                DoziBlue.copy(alpha = 0.8f),
                                DoziTurquoise.copy(alpha = 0.6f)
                            )
                        ),
                        shape = RoundedCornerShape(topStart = 24.dp, topEnd = 24.dp)
                    )
            )

            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(top = 8.dp)
                    .padding(20.dp),
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                // Başlık satırı
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.Top
                ) {
                    Column(
                        modifier = Modifier.weight(1f),
                        verticalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            androidx.compose.material3.Surface(
                                shape = androidx.compose.foundation.shape.CircleShape,
                                color = DoziTurquoise.copy(alpha = 0.15f),
                                modifier = Modifier.size(40.dp)
                            ) {
                                Box(contentAlignment = Alignment.Center) {
                                    Text(
                                        text = medicine.icon,
                                        style = MaterialTheme.typography.titleMedium
                                    )
                                }
                            }

                            Column(verticalArrangement = Arrangement.spacedBy(2.dp)) {
                                Text(
                                    text = medicine.name,
                                    style = MaterialTheme.typography.titleLarge,
                                    fontWeight = FontWeight.Bold,
                                    color = TextPrimaryLight
                                )
                                Text(
                                    text = medicine.dosage,
                                    style = MaterialTheme.typography.bodyMedium,
                                    color = TextSecondaryLight,
                                    fontWeight = FontWeight.Medium
                                )
                            }
                        }
                    }

                    androidx.compose.material3.Surface(
                        shape = androidx.compose.foundation.shape.CircleShape,
                        color = ErrorRed.copy(alpha = 0.1f)
                    ) {
                        IconButton(onClick = { showDeleteDialog = true }) {
                            Icon(
                                Icons.Default.Delete,
                                contentDescription = "Sil",
                                tint = ErrorRed,
                                modifier = Modifier.size(20.dp)
                            )
                        }
                    }
                }

                // Saatler - Horizontal scroll eğer çok fazlaysa
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    medicine.times.take(3).forEach { time ->
                        InfoTag(
                            icon = Icons.Default.Schedule,
                            text = time,
                            color = DoziBlue
                        )
                    }
                    if (medicine.times.size > 3) {
                        androidx.compose.material3.Surface(
                            shape = androidx.compose.foundation.shape.CircleShape,
                            color = DoziBlue.copy(alpha = 0.12f)
                        ) {
                            Box(
                                modifier = Modifier.padding(8.dp),
                                contentAlignment = Alignment.Center
                            ) {
                                Text(
                                    text = "+${medicine.times.size - 3}",
                                    style = MaterialTheme.typography.labelMedium,
                                    fontWeight = FontWeight.Bold,
                                    color = DoziBlue
                                )
                            }
                        }
                    }
                }

                // Sıklık bilgisi
                InfoTag(
                    icon = Icons.Default.CalendarMonth,
                    text = when (medicine.frequency) {
                        "Her X günde bir" -> "Her ${medicine.frequencyValue} günde bir"
                        "İstediğim tarihlerde" -> "${medicine.days.size} tarih seçildi"
                        else -> medicine.frequency
                    },
                    color = SuccessGreen,
                    fullWidth = true
                )
            }
        }
    }

    // Modern Silme Onay Dialogu
    if (showDeleteDialog) {
        androidx.compose.ui.window.Dialog(
            onDismissRequest = { showDeleteDialog = false }
        ) {
            androidx.compose.material3.Surface(
                shape = RoundedCornerShape(28.dp),
                color = Color.White,
                shadowElevation = 16.dp
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(32.dp),
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.spacedBy(24.dp)
                ) {
                    // Icon
                    androidx.compose.material3.Surface(
                        modifier = Modifier.size(80.dp),
                        color = ErrorRed.copy(alpha = 0.15f),
                        shape = androidx.compose.foundation.shape.CircleShape
                    ) {
                        Box(contentAlignment = Alignment.Center) {
                            Icon(
                                Icons.Default.Warning,
                                contentDescription = null,
                                tint = ErrorRed,
                                modifier = Modifier.size(44.dp)
                            )
                        }
                    }

                    // Başlık ve Açıklama
                    Column(
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        Text(
                            text = "Hatırlatmayı Sil?",
                            style = MaterialTheme.typography.headlineSmall,
                            fontWeight = FontWeight.Bold,
                            color = TextPrimaryLight
                        )

                        Text(
                            text = "${medicine.name} için ayarlanan hatırlatmayı silmek istediğinize emin misiniz?",
                            style = MaterialTheme.typography.bodyLarge,
                            color = TextSecondaryLight,
                            textAlign = androidx.compose.ui.text.style.TextAlign.Center
                        )
                    }

                    // Butonlar
                    Column(
                        modifier = Modifier.fillMaxWidth(),
                        verticalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        Button(
                            onClick = {
                                onDelete()
                                showDeleteDialog = false
                            },
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(56.dp),
                            shape = RoundedCornerShape(16.dp),
                            colors = ButtonDefaults.buttonColors(
                                containerColor = ErrorRed
                            )
                        ) {
                            Icon(
                                Icons.Default.Delete,
                                contentDescription = null,
                                modifier = Modifier.size(22.dp)
                            )
                            Spacer(Modifier.width(10.dp))
                            Text(
                                "Evet, Sil",
                                style = MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.Bold
                            )
                        }

                        OutlinedButton(
                            onClick = { showDeleteDialog = false },
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(56.dp),
                            shape = RoundedCornerShape(16.dp),
                            border = BorderStroke(2.dp, Gray200)
                        ) {
                            Text(
                                "İptal",
                                style = MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.Bold,
                                color = TextSecondaryLight
                            )
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun InfoTag(
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    text: String,
    color: Color,
    fullWidth: Boolean = false
) {
    androidx.compose.material3.Surface(
        color = color.copy(alpha = 0.12f),
        shape = RoundedCornerShape(12.dp),
        shadowElevation = 2.dp,
        modifier = if (fullWidth) Modifier.fillMaxWidth() else Modifier
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 14.dp, vertical = 10.dp),
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Icon(
                icon,
                contentDescription = null,
                tint = color,
                modifier = Modifier.size(18.dp)
            )
            Text(
                text = text,
                color = color,
                style = MaterialTheme.typography.labelLarge,
                fontWeight = FontWeight.SemiBold
            )
        }
    }
}