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
                backgroundColor = Color.White,
                actions = {
                    IconButton(onClick = onNavigateToAddReminder) {
                        Icon(
                            Icons.Default.Add,
                            contentDescription = "Yeni Hatırlatma",
                            tint = DoziCoralDark
                        )
                    }
                }
            )
        },
        floatingActionButton = {
            FloatingActionButton(
                onClick = onNavigateToAddReminder,
                containerColor = DoziTurquoise,
                contentColor = Color.White,
                shape = RoundedCornerShape(18.dp),
                modifier = Modifier.shadow(10.dp, RoundedCornerShape(18.dp))
            ) {
                Icon(Icons.Default.Add, contentDescription = "Yeni Hatırlatma")
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
                // Üst Bilgi Alanı
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .background(
                            brush = Brush.horizontalGradient(
                                listOf(DoziBlue, DoziTurquoise)
                            )
                        )
                        .padding(horizontal = 24.dp, vertical = 28.dp)
                ) {
                    Column(
                        verticalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        Text(
                            text = "💡 Toplam ${medicines.size} hatırlatma",
                            color = Color.White,
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.SemiBold
                        )
                        Text(
                            text = "İlaçlarınızı zamanında almanız için hatırlatmalar burada.",
                            color = Color.White.copy(alpha = 0.9f),
                            style = MaterialTheme.typography.bodyMedium
                        )
                    }
                }

                Spacer(Modifier.height(16.dp))

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
            .shadow(6.dp, RoundedCornerShape(20.dp)),
        shape = RoundedCornerShape(20.dp),
        colors = CardDefaults.cardColors(containerColor = Color.White)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(20.dp),
            verticalArrangement = Arrangement.spacedBy(14.dp)
        ) {
            // Başlık satırı
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = "${medicine.icon} ${medicine.name}",
                        style = MaterialTheme.typography.titleLarge,
                        fontWeight = FontWeight.Bold,
                        color = DoziTurquoise
                    )
                    Spacer(Modifier.height(4.dp))
                    Text(
                        text = medicine.dosage,
                        style = MaterialTheme.typography.bodyMedium,
                        color = TextSecondary
                    )
                }

                IconButton(onClick = { showDeleteDialog = true }) {
                    Icon(
                        Icons.Default.Delete,
                        contentDescription = "Sil",
                        tint = ErrorRed
                    )
                }
            }

            Divider(color = Gray200, thickness = 1.dp)

            // Bilgi satırları
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                // Saatler
                medicine.times.forEach { time ->
                    InfoTag(
                        icon = Icons.Default.Schedule,
                        text = time,
                        color = DoziBlue
                    )
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

    // Silme onay dialogu
    if (showDeleteDialog) {
        AlertDialog(
            onDismissRequest = { showDeleteDialog = false },
            icon = {
                Icon(
                    Icons.Default.Warning,
                    contentDescription = null,
                    tint = ErrorRed,
                    modifier = Modifier.size(48.dp)
                )
            },
            title = {
                Text(
                    "Hatırlatmayı Sil?",
                    fontWeight = FontWeight.Bold
                )
            },
            text = {
                Text(
                    "${medicine.name} hatırlatmasını silmek istediğinize emin misiniz?",
                    color = TextSecondary
                )
            },
            confirmButton = {
                Button(
                    onClick = {
                        onDelete()
                        showDeleteDialog = false
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = ErrorRed)
                ) {
                    Icon(Icons.Default.Delete, contentDescription = null)
                    Spacer(Modifier.width(8.dp))
                    Text("Sil", fontWeight = FontWeight.Bold)
                }
            },
            dismissButton = {
                OutlinedButton(
                    onClick = { showDeleteDialog = false },
                    border = BorderStroke(1.dp, Gray200)
                ) {
                    Text("İptal", color = TextSecondary, fontWeight = FontWeight.Medium)
                }
            }
        )
    }
}

@Composable
private fun InfoTag(
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    text: String,
    color: Color,
    fullWidth: Boolean = false
) {
    Surface(
        color = color.copy(alpha = 0.12f),
        shape = RoundedCornerShape(12.dp),
        modifier = if (fullWidth) Modifier.fillMaxWidth() else Modifier
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 12.dp, vertical = 8.dp),
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