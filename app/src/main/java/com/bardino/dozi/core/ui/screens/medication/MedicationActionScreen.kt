package com.bardino.dozi.core.ui.screens.medication

import android.content.Context
import android.os.Build
import androidx.annotation.RequiresApi
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
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.bardino.dozi.core.data.model.Medicine
import com.bardino.dozi.core.data.repository.MedicineRepository
import com.bardino.dozi.core.ui.screens.home.saveMedicineStatus
import com.bardino.dozi.core.ui.screens.home.getCurrentDateString
import com.bardino.dozi.core.ui.theme.*
import com.bardino.dozi.core.utils.SoundHelper
import com.bardino.dozi.R
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

/**
 * Medication Action Screen
 * Bu ekran sadece bildirimden açılır ve aynı saatteki tüm ilaçları gösterir
 * Kullanıcı her ilaç için ayrı ayrı işlem yapabilir (Al, Atla, Ertele)
 */
@RequiresApi(Build.VERSION_CODES.O)
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MedicationActionScreen(
    time: String,
    onNavigateBack: () -> Unit
) {
    val context = LocalContext.current
    val medicineRepository = remember { MedicineRepository() }
    var medicines by remember { mutableStateOf<List<Medicine>>(emptyList()) }
    var completedMedicines by remember { mutableStateOf<Set<String>>(emptySet()) }
    var isLoading by remember { mutableStateOf(true) }

    // Aynı saatteki ilaçları yükle
    LaunchedEffect(time) {
        try {
            val allMedicines = medicineRepository.getAllMedicines()
            val filteredMedicines = allMedicines.filter { medicine ->
                medicine.times.contains(time) && medicine.reminderEnabled
            }
            medicines = filteredMedicines
            isLoading = false
        } catch (e: Exception) {
            e.printStackTrace()
            isLoading = false
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Column {
                        Text(
                            "İlaç Zamanı",
                            style = MaterialTheme.typography.titleLarge,
                            fontWeight = FontWeight.Bold
                        )
                        Text(
                            "Saat: $time",
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.7f)
                        )
                    }
                },
                navigationIcon = {
                    IconButton(onClick = onNavigateBack) {
                        Icon(Icons.Default.Close, contentDescription = "Kapat")
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = Color.White,
                    titleContentColor = MaterialTheme.colorScheme.onSurface
                )
            )
        },
        containerColor = MaterialTheme.colorScheme.background
    ) { padding ->
        when {
            isLoading -> {
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(padding),
                    contentAlignment = Alignment.Center
                ) {
                    CircularProgressIndicator(color = DoziTurquoise)
                }
            }
            medicines.isEmpty() -> {
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(padding),
                    contentAlignment = Alignment.Center
                ) {
                    Column(
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.spacedBy(16.dp)
                    ) {
                        Icon(
                            Icons.Default.CheckCircle,
                            contentDescription = null,
                            modifier = Modifier.size(64.dp),
                            tint = SuccessGreen
                        )
                        Text(
                            "Bu saatte ilaç yok",
                            style = MaterialTheme.typography.titleLarge,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.onSurface
                        )
                        Text(
                            "Tüm ilaçlarınız alındı veya bu saatte planlanmış ilaç bulunamadı",
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
            }
            else -> {
                LazyColumn(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(padding),
                    contentPadding = PaddingValues(16.dp),
                    verticalArrangement = Arrangement.spacedBy(16.dp)
                ) {
                    item {
                        Card(
                            colors = CardDefaults.cardColors(
                                containerColor = DoziTurquoise.copy(alpha = 0.1f)
                            ),
                            shape = RoundedCornerShape(16.dp),
                            elevation = CardDefaults.cardElevation(0.dp)
                        ) {
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(16.dp),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(12.dp)
                            ) {
                                Icon(
                                    Icons.Default.Info,
                                    contentDescription = null,
                                    tint = DoziTurquoise
                                )
                                Column(modifier = Modifier.weight(1f)) {
                                    Text(
                                        "${medicines.size} ilaç almanız gerekiyor",
                                        style = MaterialTheme.typography.titleSmall,
                                        fontWeight = FontWeight.Bold,
                                        color = DoziTurquoise
                                    )
                                    Text(
                                        "Her ilaç için ayrı ayrı işlem yapabilirsiniz",
                                        style = MaterialTheme.typography.bodySmall,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant
                                    )
                                }
                            }
                        }
                    }

                    items(medicines) { medicine ->
                        val isCompleted = completedMedicines.contains(medicine.id)

                        MedicationActionCard(
                            medicine = medicine,
                            time = time,
                            isCompleted = isCompleted,
                            onTaken = {
                                SoundHelper.playSound(context, SoundHelper.SoundType.HERSEY_TAMAM)
                                handleMedicineTaken(context, medicine, time, medicineRepository)
                                completedMedicines = completedMedicines + medicine.id
                            },
                            onSkipped = {
                                SoundHelper.playSound(context, SoundHelper.SoundType.PEKALA)
                                handleMedicineSkipped(context, medicine, time)
                                completedMedicines = completedMedicines + medicine.id
                            },
                            onSnoozed = { minutes ->
                                SoundHelper.playSound(context, SoundHelper.SoundType.ERTELE)
                                handleMedicineSnoozed(context, medicine, time, minutes)
                                completedMedicines = completedMedicines + medicine.id
                            }
                        )
                    }

                    item {
                        Spacer(Modifier.height(32.dp))
                    }
                }
            }
        }
    }
}

@Composable
private fun MedicationActionCard(
    medicine: Medicine,
    time: String,
    isCompleted: Boolean,
    onTaken: () -> Unit,
    onSkipped: () -> Unit,
    onSnoozed: (Int) -> Unit
) {
    var showSnoozeDialog by remember { mutableStateOf(false) }
    var showSkipDialog by remember { mutableStateOf(false) }

    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(20.dp),
        colors = CardDefaults.cardColors(
            containerColor = if (isCompleted) SuccessGreen.copy(alpha = 0.1f) else Color.White
        ),
        elevation = CardDefaults.cardElevation(0.dp)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(20.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            // Header
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                Surface(
                    shape = RoundedCornerShape(12.dp),
                    color = DoziTurquoise.copy(alpha = 0.15f),
                    modifier = Modifier.size(48.dp)
                ) {
                    Box(contentAlignment = Alignment.Center) {
                        Text(
                            medicine.icon,
                            style = MaterialTheme.typography.headlineSmall
                        )
                    }
                }

                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        medicine.name,
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                    Text(
                        medicine.dosage,
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    if (medicine.stockCount > 0) {
                        Text(
                            "Stok: ${medicine.stockCount} adet",
                            style = MaterialTheme.typography.bodySmall,
                            color = when {
                                medicine.stockCount < 5 -> DoziRed
                                medicine.stockCount < 10 -> WarningOrange
                                else -> SuccessGreen
                            }
                        )
                    }
                }

                if (isCompleted) {
                    Icon(
                        Icons.Default.CheckCircle,
                        contentDescription = "Tamamlandı",
                        tint = SuccessGreen,
                        modifier = Modifier.size(32.dp)
                    )
                }
            }

            if (!isCompleted) {
                HorizontalDivider(color = VeryLightGray)

                // Action buttons
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Button(
                        onClick = onTaken,
                        modifier = Modifier.weight(1f),
                        colors = ButtonDefaults.buttonColors(
                            containerColor = SuccessGreen
                        ),
                        shape = RoundedCornerShape(12.dp)
                    ) {
                        Column(
                            horizontalAlignment = Alignment.CenterHorizontally,
                            verticalArrangement = Arrangement.spacedBy(4.dp)
                        ) {
                            Icon(Icons.Default.Check, contentDescription = null)
                            Text("ALDIM", fontSize = 10.sp)
                        }
                    }

                    OutlinedButton(
                        onClick = { showSnoozeDialog = true },
                        modifier = Modifier.weight(1f),
                        colors = ButtonDefaults.outlinedButtonColors(
                            contentColor = WarningOrange
                        ),
                        shape = RoundedCornerShape(12.dp)
                    ) {
                        Column(
                            horizontalAlignment = Alignment.CenterHorizontally,
                            verticalArrangement = Arrangement.spacedBy(4.dp)
                        ) {
                            Icon(Icons.Default.AccessTime, contentDescription = null)
                            Text("ERTELE", fontSize = 10.sp)
                        }
                    }

                    OutlinedButton(
                        onClick = { showSkipDialog = true },
                        modifier = Modifier.weight(1f),
                        colors = ButtonDefaults.outlinedButtonColors(
                            contentColor = ErrorRed
                        ),
                        shape = RoundedCornerShape(12.dp)
                    ) {
                        Column(
                            horizontalAlignment = Alignment.CenterHorizontally,
                            verticalArrangement = Arrangement.spacedBy(4.dp)
                        ) {
                            Icon(Icons.Default.Close, contentDescription = null)
                            Text("ATLA", fontSize = 10.sp)
                        }
                    }
                }
            }
        }
    }

    // Snooze Dialog
    if (showSnoozeDialog) {
        SimpleSnoozeDialog(
            onDismiss = { showSnoozeDialog = false },
            onConfirm = { minutes ->
                onSnoozed(minutes)
                showSnoozeDialog = false
            }
        )
    }

    // Skip Dialog
    if (showSkipDialog) {
        SimpleSkipDialog(
            medicineName = medicine.name,
            onDismiss = { showSkipDialog = false },
            onConfirm = {
                onSkipped()
                showSkipDialog = false
            }
        )
    }
}

@Composable
private fun SimpleSnoozeDialog(
    onDismiss: () -> Unit,
    onConfirm: (Int) -> Unit
) {
    var selectedMinutes by remember { mutableStateOf(10) }
    val options = listOf(5, 10, 15, 30)

    AlertDialog(
        onDismissRequest = onDismiss,
        icon = {
            Icon(Icons.Default.AccessTime, contentDescription = null, tint = WarningOrange)
        },
        title = {
            Text("Ne kadar ertele?", fontWeight = FontWeight.Bold)
        },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                options.forEach { minutes ->
                    FilterChip(
                        selected = selectedMinutes == minutes,
                        onClick = { selectedMinutes = minutes },
                        label = { Text("$minutes dakika") },
                        leadingIcon = if (selectedMinutes == minutes) {
                            { Icon(Icons.Default.Check, contentDescription = null) }
                        } else null
                    )
                }
            }
        },
        confirmButton = {
            Button(
                onClick = { onConfirm(selectedMinutes) },
                colors = ButtonDefaults.buttonColors(containerColor = WarningOrange)
            ) {
                Text("Ertele")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("İptal")
            }
        }
    )
}

@Composable
private fun SimpleSkipDialog(
    medicineName: String,
    onDismiss: () -> Unit,
    onConfirm: () -> Unit
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        icon = {
            Icon(Icons.Default.Warning, contentDescription = null, tint = ErrorRed)
        },
        title = {
            Text("İlacı Atla?", fontWeight = FontWeight.Bold)
        },
        text = {
            Text("$medicineName ilacını atlamak istediğinize emin misiniz?")
        },
        confirmButton = {
            Button(
                onClick = onConfirm,
                colors = ButtonDefaults.buttonColors(containerColor = ErrorRed)
            ) {
                Text("Evet, Atla")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("İptal")
            }
        }
    )
}

// Helper functions
private fun handleMedicineTaken(
    context: Context,
    medicine: Medicine,
    time: String,
    repository: MedicineRepository
) {
    saveMedicineStatus(context, medicine.id, getCurrentDateString(), time, "taken")

    // Stok azalt
    if (medicine.stockCount > 0) {
        CoroutineScope(Dispatchers.IO).launch {
            try {
                repository.updateMedicineField(medicine.id, "stockCount", medicine.stockCount - 1)
            } catch (e: Exception) {
                e.printStackTrace()
            }
        }
    }
}

private fun handleMedicineSkipped(
    context: Context,
    medicine: Medicine,
    time: String
) {
    saveMedicineStatus(context, medicine.id, getCurrentDateString(), time, "skipped")
}

private fun handleMedicineSnoozed(
    context: Context,
    medicine: Medicine,
    time: String,
    minutes: Int
) {
    val snoozeUntil = System.currentTimeMillis() + minutes * 60_000L
    saveMedicineStatus(context, medicine.id, getCurrentDateString(), time, "snoozed_$snoozeUntil")

    // SharedPreferences'a kaydet
    context.getSharedPreferences("dozi_prefs", Context.MODE_PRIVATE).edit()
        .putInt("snooze_minutes", minutes)
        .putLong("snooze_until", snoozeUntil)
        .putLong("snooze_timestamp", System.currentTimeMillis())
        .apply()
}
