package com.bardino.dozi.core.ui.screens.medicine

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.bardino.dozi.core.data.model.Medicine
import com.bardino.dozi.core.data.repository.MedicineRepository
import com.bardino.dozi.core.ui.components.DoziTopBar
import com.bardino.dozi.core.ui.theme.*
import kotlinx.coroutines.launch

@Composable
fun MedicineDetailScreen(
    medicineId: String,
    onNavigateBack: () -> Unit,
    onEditMedicine: (String) -> Unit
) {
    val context = LocalContext.current
    val medicineRepository = remember { MedicineRepository() }
    var medicine by remember { mutableStateOf<Medicine?>(null) }

    LaunchedEffect(medicineId) {
        try {
            medicine = medicineRepository.getMedicine(medicineId)
        } catch (e: Exception) {
            // Handle error
        }
    }

    val currentMedicine = medicine ?: Medicine(
        id = medicineId,
        name = "Yükleniyor...",
        dosage = "-",
        stockCount = 0
    )

    Scaffold(
        topBar = {
            DoziTopBar(
                title = "İlaç Detayı",
                canNavigateBack = true,
                onNavigateBack = onNavigateBack,
                actions = {
                    IconButton(
                        onClick = { onEditMedicine(currentMedicine.id) },
                        modifier = Modifier
                            .size(46.dp)
                            .background(DoziTurquoise.copy(alpha = 0.1f), CircleShape)
                    ) {
                        Icon(
                            imageVector = Icons.Default.Edit,
                            contentDescription = "Düzenle",
                            tint = DoziTurquoise
                        )
                    }
                }
            )
        },
        containerColor = BackgroundLight
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .padding(20.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            Card(
                modifier = Modifier.fillMaxWidth(),
                colors = CardDefaults.cardColors(containerColor = Color.White),
                border = BorderStroke(1.dp, VeryLightGray),
                shape = RoundedCornerShape(20.dp)
            ) {
                Column(
                    Modifier.padding(20.dp),
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    DetailRow("İlaç Adı", currentMedicine.name)
                    DetailRow("Dozaj", currentMedicine.dosage)
                    DetailRow("Stok", "${currentMedicine.stockCount} adet")
                }
            }

            Text(
                text = "Bu ekran sadece görüntüleme içindir. İlaç bilgilerini düzenlemek için sağ üstteki 'Düzenle' butonuna dokun.",
                style = MaterialTheme.typography.bodyMedium,
                color = TextSecondaryLight
            )
        }
    }
}

@Composable
private fun DetailRow(label: String, value: String) {
    Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
        Text(
            text = label,
            style = MaterialTheme.typography.labelMedium,
            color = TextSecondaryLight
        )
        Text(
            text = value,
            style = MaterialTheme.typography.titleMedium,
            fontWeight = FontWeight.Bold,
            color = TextPrimaryLight
        )
    }
}