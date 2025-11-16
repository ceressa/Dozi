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
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.bardino.dozi.DoziApplication
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
    onNavigateToAddReminder: () -> Unit,
    onNavigateToEditReminder: (String) -> Unit = {}
) {
    val context = LocalContext.current
    val app = context.applicationContext as com.bardino.dozi.DoziApplication
    val medicineRepository = remember { MedicineRepository(app.profileManager) }
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
        containerColor = MaterialTheme.colorScheme.background
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
                                colors = listOf(
                                    DoziBlue,
                                    DoziTurquoise
                                )
                            ),
                            shape = RoundedCornerShape(bottomStart = 24.dp, bottomEnd = 24.dp)
                        )
                        .padding(horizontal = 20.dp, vertical = 18.dp)
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column(
                            verticalArrangement = Arrangement.spacedBy(4.dp)
                        ) {
                            Text(
                                text = "Hatırlatmalarınız",
                                color = Color.White,
                                style = MaterialTheme.typography.titleLarge,
                                fontWeight = FontWeight.Bold
                            )
                            Text(
                                text = "${medicines.size} aktif hatırlatma",
                                color = Color.White.copy(alpha = 0.9f),
                                style = MaterialTheme.typography.bodyMedium
                            )
                        }
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
                                        // 🚫 Önce alarmları iptal et
                                        com.bardino.dozi.notifications.ReminderScheduler.cancelReminders(
                                            context, medicine.id, medicine.times
                                        )
                                        // Sonra veritabanından sil
                                        medicineRepository.deleteMedicine(medicine.id)
                                        android.util.Log.d("ReminderListScreen", "🗑️ ${medicine.name} silindi ve alarmları iptal edildi")
                                    }
                                },
                                onEdit = {
                                    onNavigateToEditReminder(medicine.id)
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
    onDelete: () -> Unit,
    onEdit: () -> Unit = {}
) {
    var showDeleteDialog by remember { mutableStateOf(false) }

    Card(
        modifier = Modifier
            .fillMaxWidth()
            .shadow(8.dp, RoundedCornerShape(24.dp)),
        shape = RoundedCornerShape(24.dp),
        colors = CardDefaults.cardColors(
            containerColor = Color.White
        ),
        elevation = CardDefaults.cardElevation(defaultElevation = 0.dp)
    ) {
        Box {
            // Renkli gradient top border
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(6.dp)
                    .background(
                        brush = Brush.horizontalGradient(
                            listOf(
                                DoziPrimary,
                                DoziSecondary,
                                DoziAccent
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
                                DoziPrimaryLight.copy(alpha = 0.05f),
                                Color.White
                            )
                        )
                    )
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
                            horizontalArrangement = Arrangement.spacedBy(12.dp)
                        ) {
                            // İkon arka planı - gradient
                            androidx.compose.material3.Surface(
                                shape = RoundedCornerShape(14.dp),
                                color = Color.Transparent,
                                modifier = Modifier.size(48.dp)
                            ) {
                                Box(
                                    modifier = Modifier.background(
                                        brush = Brush.linearGradient(
                                            colors = listOf(
                                                DoziPrimary.copy(alpha = 0.2f),
                                                DoziSecondary.copy(alpha = 0.15f)
                                            )
                                        )
                                    ),
                                    contentAlignment = Alignment.Center
                                ) {
                                    Text(
                                        text = medicine.icon,
                                        style = MaterialTheme.typography.headlineMedium
                                    )
                                }
                            }

                            Column(verticalArrangement = Arrangement.spacedBy(2.dp)) {
                                Text(
                                    text = medicine.name,
                                    style = MaterialTheme.typography.titleLarge,
                                    fontWeight = FontWeight.Bold,
                                    color = Gray900,
                                    maxLines = 1,
                                    overflow = androidx.compose.ui.text.style.TextOverflow.Ellipsis
                                )
                                Text(
                                    text = "${medicine.dosage} ${medicine.unit}",
                                    style = MaterialTheme.typography.bodyMedium,
                                    color = Gray600,
                                    fontWeight = FontWeight.Medium
                                )
                            }
                        }
                    }

                    Row(
                        horizontalArrangement = Arrangement.spacedBy(6.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        // Edit butonu
                        androidx.compose.material3.Surface(
                            shape = RoundedCornerShape(12.dp),
                            color = DoziPrimary.copy(alpha = 0.15f)
                        ) {
                            IconButton(onClick = onEdit) {
                                Icon(
                                    Icons.Default.Edit,
                                    contentDescription = "Düzenle",
                                    tint = DoziPrimaryDark,
                                    modifier = Modifier.size(20.dp)
                                )
                            }
                        }

                        // Delete butonu
                        androidx.compose.material3.Surface(
                            shape = RoundedCornerShape(12.dp),
                            color = ErrorRed.copy(alpha = 0.15f)
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
                            color = DoziPrimaryDark
                        )
                    }
                    if (medicine.times.size > 3) {
                        androidx.compose.material3.Surface(
                            shape = RoundedCornerShape(12.dp),
                            color = DoziPrimary.copy(alpha = 0.15f)
                        ) {
                            Box(
                                modifier = Modifier.padding(horizontal = 12.dp, vertical = 8.dp),
                                contentAlignment = Alignment.Center
                            ) {
                                Text(
                                    text = "+${medicine.times.size - 3}",
                                    style = MaterialTheme.typography.labelMedium,
                                    fontWeight = FontWeight.Bold,
                                    color = DoziPrimaryDark
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
                    color = Color(0xFF059669), // Daha koyu yeşil
                    fullWidth = true
                )

                // Başlangıç tarihi
                val startDateText = remember(medicine.startDate) {
                    try {
                        val sdf = java.text.SimpleDateFormat("dd MMM yyyy", java.util.Locale("tr"))
                        sdf.format(java.util.Date(medicine.startDate))
                    } catch (e: Exception) {
                        "Tarih belirtilmemiş"
                    }
                }
                InfoTag(
                    icon = Icons.Default.Schedule,
                    text = "Başlangıç: $startDateText",
                    color = DoziSecondaryDark,
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
                color = Color.White
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
                            color = MaterialTheme.colorScheme.onSurface
                        )

                        Text(
                            text = "${medicine.name} için ayarlanan hatırlatmayı silmek istediğinize emin misiniz?",
                            style = MaterialTheme.typography.bodyLarge,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
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
                                color = MaterialTheme.colorScheme.onSurfaceVariant
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
        color = color.copy(alpha = 0.15f),
        shape = RoundedCornerShape(12.dp),
        modifier = if (fullWidth) Modifier.fillMaxWidth() else Modifier,
        border = BorderStroke(1.dp, color.copy(alpha = 0.3f))
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
                fontWeight = FontWeight.Bold
            )
        }
    }
}