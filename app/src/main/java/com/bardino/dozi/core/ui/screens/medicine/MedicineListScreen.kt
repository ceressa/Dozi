package com.bardino.dozi.core.ui.screens.medicine

import android.widget.Toast
import androidx.compose.animation.*
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.foundation.*
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
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
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import com.bardino.dozi.R
import com.bardino.dozi.core.data.model.Medicine
import com.bardino.dozi.core.data.repository.MedicineRepository
import com.bardino.dozi.core.ui.components.DoziTopBar
import com.bardino.dozi.core.ui.theme.*
import kotlinx.coroutines.launch

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MedicineListScreen(
    onNavigateBack: () -> Unit,
    onNavigateToDetail: (String) -> Unit,
    onNavigateToAddMedicine: (String) -> Unit = {},
    onNavigateToAddReminder: ((String) -> Unit)? = null,
    onNavigateToReminderDetail: ((String) -> Unit)? = null,
    showAddButton: Boolean = true
) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    var medicines by remember { mutableStateOf<List<Medicine>>(emptyList()) }
    var isVisible by remember { mutableStateOf(false) }

    // ✅ Firestore'dan tüm ilaçları yükle (paylaşımlı - tüm profiller)
    LaunchedEffect(Unit) {
        isVisible = true
        while (true) {
            try {
                val medicineRepository = MedicineRepository()
                medicines = medicineRepository.getAllMedicines()
                android.util.Log.d("MedicineListScreen", "✅ Loaded ${medicines.size} medicines from Firestore")
                // 🔍 Her ilacın hatırlatma bilgilerini logla
                medicines.forEach { med ->
                    android.util.Log.d("MedicineListScreen", "📋 ${med.name}: reminderEnabled=${med.reminderEnabled}, times=${med.times.joinToString(", ")}, frequency=${med.frequency}")
                }
            } catch (e: Exception) {
                android.util.Log.e("MedicineListScreen", "❌ Error loading medicines", e)
            }
            kotlinx.coroutines.delay(3000) // Refresh every 3 seconds
        }
    }

    Scaffold(
        topBar = {
            DoziTopBar(
                title = "İlaçlarım",
                canNavigateBack = false,
                backgroundColor = Color.Transparent,
                actions = {
                    Surface(
                        shape = CircleShape,
                        color = DoziCoralLight.copy(alpha = 0.15f),
                        modifier = Modifier.padding(end = 8.dp)
                    ) {
                        IconButton(
                            onClick = { onNavigateToAddMedicine("lookup") }
                        ) {
                            Icon(
                                imageVector = Icons.Default.AddCircleOutline,
                                contentDescription = "Yeni İlaç Ekle",
                                tint = DoziCoralDark
                            )
                        }
                    }
                }
            )
        },
        floatingActionButton = {
            FloatingActionButton(
                onClick = { onNavigateToAddMedicine("new") },
                containerColor = DoziTurquoise,
                contentColor = Color.White,
                shape = RoundedCornerShape(16.dp),
                modifier = Modifier.shadow(12.dp, RoundedCornerShape(16.dp))
            ) {
                Icon(
                    Icons.Default.Add,
                    contentDescription = "İlaç Ekle",
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
                EmptyMedicineState(onAddMedicine = { onNavigateToAddMedicine("new") })
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
                                    DoziTurquoise,
                                    DoziBlue
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
                                text = "İlaçlarım",
                                color = Color.White,
                                style = MaterialTheme.typography.titleLarge,
                                fontWeight = FontWeight.Bold
                            )
                            Text(
                                text = "${medicines.size} ilaç kaydedildi",
                                color = Color.White.copy(alpha = 0.9f),
                                style = MaterialTheme.typography.bodyMedium
                            )
                        }
                    }
                }

                Spacer(Modifier.height(16.dp))

                // İlaç Listesi
                LazyColumn(
                    contentPadding = PaddingValues(horizontal = 20.dp, vertical = 12.dp),
                    verticalArrangement = Arrangement.spacedBy(16.dp)
                ) {
                    items(medicines, key = { it.id }) { medicine ->
                        var dismissed by remember { mutableStateOf(false) }
                        var showDeleteConfirm by remember { mutableStateOf(false) }

                        AnimatedVisibility(
                            visible = isVisible && !dismissed,
                            enter = fadeIn() + slideInVertically(initialOffsetY = { 40 })
                        ) {
                            val dismissState = rememberSwipeToDismissBoxState(
                                confirmValueChange = { value ->
                                    when (value) {
                                        SwipeToDismissBoxValue.StartToEnd -> {
                                            showDeleteConfirm = true
                                            false
                                        }
                                        SwipeToDismissBoxValue.EndToStart -> {
                                            onNavigateToDetail(medicine.id)
                                            false
                                        }
                                        else -> false
                                    }
                                }
                            )

                            SwipeToDismissBox(
                                state = dismissState,
                                backgroundContent = {
                                    val direction = dismissState.targetValue
                                    val bgColor = when (direction) {
                                        SwipeToDismissBoxValue.StartToEnd -> DoziRed.copy(alpha = 0.15f)
                                        SwipeToDismissBoxValue.EndToStart -> DoziTurquoise.copy(alpha = 0.15f)
                                        else -> Color.Transparent
                                    }
                                    val iconColor = when (direction) {
                                        SwipeToDismissBoxValue.StartToEnd -> DoziRed
                                        SwipeToDismissBoxValue.EndToStart -> DoziTurquoise
                                        else -> Color.Transparent
                                    }
                                    val icon = when (direction) {
                                        SwipeToDismissBoxValue.StartToEnd -> Icons.Default.Delete
                                        SwipeToDismissBoxValue.EndToStart -> Icons.Default.Edit
                                        else -> null
                                    }

                                    Box(
                                        modifier = Modifier
                                            .fillMaxSize()
                                            .background(bgColor, shape = RoundedCornerShape(20.dp))
                                            .padding(horizontal = 24.dp),
                                        contentAlignment = when (direction) {
                                            SwipeToDismissBoxValue.StartToEnd -> Alignment.CenterStart
                                            SwipeToDismissBoxValue.EndToStart -> Alignment.CenterEnd
                                            else -> Alignment.Center
                                        }
                                    ) {
                                        icon?.let {
                                            Surface(
                                                modifier = Modifier.size(48.dp),
                                                color = iconColor.copy(alpha = 0.2f),
                                                shape = CircleShape
                                            ) {
                                                Box(contentAlignment = Alignment.Center) {
                                                    Icon(
                                                        imageVector = it,
                                                        contentDescription = null,
                                                        tint = iconColor,
                                                        modifier = Modifier.size(26.dp)
                                                    )
                                                }
                                            }
                                        }
                                    }
                                },
                                enableDismissFromStartToEnd = true,
                                enableDismissFromEndToStart = true,
                                content = {
                                    ModernMedicineCard(
                                        medicine = medicine,
                                        onClick = { onNavigateToDetail(medicine.id) },
                                        onEdit = { onNavigateToDetail(medicine.id) },
                                        onDelete = { showDeleteConfirm = true }
                                    )
                                }
                            )

                            if (showDeleteConfirm) {
                                DeleteConfirmDialog(
                                    medicineName = medicine.name,
                                    onConfirm = {
                                        scope.launch {
                                            try {
                                                val medicineRepository = MedicineRepository()
                                                medicineRepository.deleteMedicine(medicine.id)
                                                Toast.makeText(
                                                    context,
                                                    "${medicine.name} silindi",
                                                    Toast.LENGTH_SHORT
                                                ).show()
                                                showDeleteConfirm = false
                                                dismissed = true
                                            } catch (e: Exception) {
                                                Toast.makeText(
                                                    context,
                                                    "Silme işlemi başarısız: ${e.message}",
                                                    Toast.LENGTH_SHORT
                                                ).show()
                                            }
                                        }
                                    },
                                    onDismiss = { showDeleteConfirm = false }
                                )
                            }
                        }
                    }

                    item { Spacer(Modifier.height(100.dp)) }
                }
            }
        }
    }
}

@Composable
fun ModernMedicineCard(
    medicine: Medicine,
    onClick: () -> Unit,
    onEdit: () -> Unit = {},
    onDelete: () -> Unit = {}
) {
    var expanded by remember { mutableStateOf(false) }
    val rotationAngle by animateFloatAsState(
        targetValue = if (expanded) 180f else 0f,
        label = "rotation"
    )

    Card(
        modifier = Modifier
            .fillMaxWidth()
            .shadow(8.dp, RoundedCornerShape(24.dp)),
        shape = RoundedCornerShape(24.dp),
        colors = CardDefaults.cardColors(containerColor = Color.White),
        elevation = CardDefaults.cardElevation(defaultElevation = 0.dp),
        onClick = onClick
    ) {
        Column {
            // Üst renkli şerit
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(8.dp)
                    .background(
                        brush = Brush.horizontalGradient(
                            listOf(
                                DoziTurquoise.copy(alpha = 0.8f),
                                DoziBlue.copy(alpha = 0.6f)
                            )
                        ),
                        shape = RoundedCornerShape(topStart = 24.dp, topEnd = 24.dp)
                    )
            )

            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 20.dp, vertical = 20.dp),
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.Top
                ) {
                    Column(
                        modifier = Modifier.weight(1f),
                        verticalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        Text(
                            text = medicine.name,
                            style = MaterialTheme.typography.titleLarge,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.onSurface
                        )
                        Text(
                            text = medicine.dosage,
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            fontWeight = FontWeight.Medium
                        )
                    }

                    Surface(
                        shape = CircleShape,
                        color = DoziTurquoise.copy(alpha = 0.1f)
                    ) {
                        Box(
                            modifier = Modifier.padding(10.dp),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                Icons.Default.ChevronRight,
                                contentDescription = null,
                                tint = DoziTurquoise,
                                modifier = Modifier.size(24.dp)
                            )
                        }
                    }
                }

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(10.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    InfoTag(
                        Icons.Default.Inventory,
                        "Stok: ${medicine.stockCount}",
                        when {
                            medicine.stockCount < 5 -> DoziRed
                            medicine.stockCount < 10 -> WarningOrange
                            else -> SuccessGreen
                        }
                    )
                }

                // Hatırlatma bilgileri (varsa) - Geliştirilmiş versiyon
                HorizontalDivider(
                    modifier = Modifier.padding(vertical = 8.dp),
                    color = Gray200
                )

                val hasReminderTimes = medicine.times.any { it.isNotBlank() }

                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .background(
                            brush = Brush.horizontalGradient(
                                listOf(
                                    if (hasReminderTimes) DoziCoral.copy(alpha = 0.05f) else Gray200.copy(alpha = 0.3f),
                                    if (hasReminderTimes) DoziTurquoise.copy(alpha = 0.05f) else Gray200.copy(alpha = 0.3f)
                                )
                            ),
                            shape = RoundedCornerShape(12.dp)
                        )
                        .padding(12.dp),
                    verticalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    Row(
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Surface(
                            color = if (hasReminderTimes) DoziCoral.copy(alpha = 0.2f) else Gray400.copy(alpha = 0.2f),
                            shape = CircleShape,
                            modifier = Modifier.size(28.dp)
                        ) {
                            Box(contentAlignment = Alignment.Center) {
                                Icon(
                                    if (hasReminderTimes) Icons.Default.Notifications else Icons.Default.NotificationsOff,
                                    contentDescription = null,
                                    tint = if (hasReminderTimes) DoziCoral else Gray400,
                                    modifier = Modifier.size(16.dp)
                                )
                            }
                        }
                        Text(
                            text = "Hatırlatmalar",
                            style = MaterialTheme.typography.titleSmall,
                            fontWeight = FontWeight.Bold,
                            color = if (hasReminderTimes) DoziCoral else Gray600
                        )
                    }

                    if (hasReminderTimes) {
                        // Hatırlatma saatleri - chip formatında
                        Column(
                            verticalArrangement = Arrangement.spacedBy(6.dp)
                        ) {
                            Row(
                                horizontalArrangement = Arrangement.spacedBy(6.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Icon(
                                    Icons.Default.Schedule,
                                    contentDescription = null,
                                    tint = DoziTurquoise,
                                    modifier = Modifier.size(16.dp)
                                )
                                Text(
                                    text = "Saatler:",
                                    style = MaterialTheme.typography.labelMedium,
                                    fontWeight = FontWeight.Bold,
                                    color = Gray700
                                )
                            }

                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.spacedBy(6.dp)
                            ) {
                                medicine.times.take(4).forEach { time ->
                                    Surface(
                                        color = DoziTurquoise.copy(alpha = 0.15f),
                                        shape = RoundedCornerShape(8.dp),
                                        border = BorderStroke(1.dp, DoziTurquoise.copy(alpha = 0.3f))
                                    ) {
                                        Text(
                                            text = time,
                                            style = MaterialTheme.typography.labelMedium,
                                            fontWeight = FontWeight.Bold,
                                            color = DoziTurquoise,
                                            modifier = Modifier.padding(horizontal = 10.dp, vertical = 4.dp)
                                        )
                                    }
                                }
                                if (medicine.times.size > 4) {
                                    Surface(
                                        color = Gray200,
                                        shape = RoundedCornerShape(8.dp)
                                    ) {
                                        Text(
                                            text = "+${medicine.times.size - 4}",
                                            style = MaterialTheme.typography.labelSmall,
                                            color = Gray700,
                                            modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                                        )
                                    }
                                }
                            }
                        }

                        // Sıklık bilgisi
                        Row(
                            horizontalArrangement = Arrangement.spacedBy(8.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Icon(
                                Icons.Default.CalendarToday,
                                contentDescription = null,
                                tint = DoziCoral,
                                modifier = Modifier.size(16.dp)
                            )
                            Text(
                                text = medicine.frequency,
                                style = MaterialTheme.typography.bodyMedium,
                                fontWeight = FontWeight.Medium,
                                color = Gray900
                            )

                            if (!medicine.reminderEnabled) {
                                AssistChip(
                                    onClick = {},
                                    label = {
                                        Text(
                                            text = "Bildirim kapalı",
                                            color = DoziCoral,
                                            style = MaterialTheme.typography.labelMedium
                                        )
                                    },
                                    leadingIcon = {
                                        Icon(
                                            imageVector = Icons.Default.NotificationsOff,
                                            contentDescription = null,
                                            tint = DoziCoral,
                                            modifier = Modifier.size(14.dp)
                                        )
                                    },
                                    colors = AssistChipDefaults.assistChipColors(
                                        containerColor = DoziCoral.copy(alpha = 0.08f)
                                    ),
                                    border = BorderStroke(
                                        width = 1.dp,
                                        color = DoziCoral.copy(alpha = 0.4f)
                                    )
                                )
                            }
                        }
                    } else {
                        // Hatırlatma yoksa bilgilendirme göster
                        val reminderMessage = when {
                            !medicine.reminderEnabled && medicine.times.isEmpty() -> "Hatırlatmalar devre dışı"
                            medicine.reminderEnabled && medicine.times.isEmpty() -> "Henüz hatırlatma eklenmemiş"
                            else -> "Hatırlatma bilgisi senkronize ediliyor"
                        }
                        Text(
                            text = reminderMessage,
                            style = MaterialTheme.typography.bodyMedium,
                            color = Gray600,
                            fontWeight = FontWeight.Medium
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun ReminderListItem(
    reminder: com.bardino.dozi.core.data.model.Medicine,
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
            modifier = Modifier.padding(12.dp),
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

@Composable
private fun InfoTag(
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    text: String,
    color: Color
) {
    Surface(
        color = color.copy(alpha = 0.15f),
        shape = RoundedCornerShape(12.dp),
        border = BorderStroke(1.dp, color.copy(alpha = 0.3f)),
        modifier = Modifier.fillMaxWidth()
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 14.dp, vertical = 10.dp),
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Icon(
                icon,
                null,
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

@Composable
private fun EmptyMedicineState(
    onAddMedicine: () -> Unit
) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(32.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        Image(
            painter = painterResource(id = R.drawable.dozi_idea),
            contentDescription = null,
            modifier = Modifier.size(140.dp)
        )

        Spacer(Modifier.height(24.dp))

        Text(
            text = "Henüz İlaç Eklemediniz",
            style = MaterialTheme.typography.headlineSmall,
            fontWeight = FontWeight.Bold,
            color = MaterialTheme.colorScheme.onSurface,
            textAlign = TextAlign.Center
        )

        Spacer(Modifier.height(12.dp))

        Text(
            text = "İlaçlarınızı ekleyerek takibini kolaylaştırın ve asla unutmayın!",
            style = MaterialTheme.typography.bodyLarge,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            textAlign = TextAlign.Center
        )

        Spacer(Modifier.height(40.dp))

        Button(
            onClick = onAddMedicine,
            shape = RoundedCornerShape(20.dp),
            colors = ButtonDefaults.buttonColors(
                containerColor = DoziCoralDark
            ),
            modifier = Modifier
                .fillMaxWidth(0.8f)
                .height(56.dp),
            elevation = ButtonDefaults.buttonElevation(6.dp)
        ) {
            Icon(
                Icons.Default.Add,
                contentDescription = null,
                modifier = Modifier.size(24.dp)
            )
            Spacer(Modifier.width(12.dp))
            Text(
                "İlk İlacını Ekle",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold,
                fontSize = 16.sp
            )
        }
    }
}

@Composable
private fun DeleteConfirmDialog(
    medicineName: String,
    onConfirm: () -> Unit,
    onDismiss: () -> Unit
) {
    Dialog(onDismissRequest = onDismiss) {
        Surface(
            shape = RoundedCornerShape(28.dp),
            color = Color.White,
            shadowElevation = 8.dp
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(28.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(20.dp)
            ) {
                Surface(
                    modifier = Modifier.size(72.dp),
                    color = DoziRed.copy(alpha = 0.15f),
                    shape = CircleShape
                ) {
                    Box(contentAlignment = Alignment.Center) {
                        Icon(
                            Icons.Default.Warning,
                            contentDescription = null,
                            tint = DoziRed,
                            modifier = Modifier.size(40.dp)
                        )
                    }
                }

                Column(
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    Text(
                        text = "İlacı Sil?",
                        style = MaterialTheme.typography.headlineSmall,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onSurface
                    )

                    Text(
                        text = "$medicineName ilacını silmek istediğinize emin misiniz?",
                        style = MaterialTheme.typography.bodyLarge,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        textAlign = TextAlign.Center
                    )
                }

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    OutlinedButton(
                        onClick = onDismiss,
                        modifier = Modifier
                            .weight(1f)
                            .height(50.dp),
                        shape = RoundedCornerShape(14.dp),
                        border = BorderStroke(2.dp, VeryLightGray)
                    ) {
                        Text(
                            "İptal",
                            style = MaterialTheme.typography.titleSmall,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }

                    Button(
                        onClick = onConfirm,
                        modifier = Modifier
                            .weight(1f)
                            .height(50.dp),
                        shape = RoundedCornerShape(14.dp),
                        colors = ButtonDefaults.buttonColors(
                            containerColor = DoziRed
                        )
                    ) {
                        Icon(
                            Icons.Default.Delete,
                            contentDescription = null,
                            modifier = Modifier.size(20.dp)
                        )
                        Spacer(Modifier.width(8.dp))
                        Text(
                            "Sil",
                            style = MaterialTheme.typography.titleSmall,
                            fontWeight = FontWeight.Bold
                        )
                    }
                }
            }
        }
    }
}
