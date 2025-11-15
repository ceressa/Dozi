package com.bardino.dozi.core.ui.screens.medicine

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material3.*
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import kotlinx.coroutines.launch
import kotlinx.coroutines.withTimeout
import com.bardino.dozi.core.data.model.Medicine
import com.bardino.dozi.core.data.repository.MedicineRepository
import com.bardino.dozi.core.ui.components.DoziTopBar
import com.bardino.dozi.core.ui.theme.*

@Composable
fun MedicineDetailScreen(
    medicineId: String,
    onNavigateBack: () -> Unit,
    onEditMedicine: (String) -> Unit
) {
    val context = LocalContext.current
    val repository = remember { MedicineRepository() }

    // Mevcut ilacı yükle
    var medicine by remember { mutableStateOf<Medicine?>(null) }
    var isLoading by remember { mutableStateOf(true) }
    var error by remember { mutableStateOf<String?>(null) }

    // Debug log
    android.util.Log.d("MedicineDetailScreen", "Screen initialized with medicineId: $medicineId")

    LaunchedEffect(medicineId) {
        android.util.Log.d("MedicineDetailScreen", "LaunchedEffect started, loading medicine: $medicineId")
        try {
            // 10 saniye timeout
            medicine = withTimeout(10000L) {
                repository.getMedicine(medicineId)
            }
        } catch (e: kotlinx.coroutines.TimeoutCancellationException) {
            error = "İlaç yükleme zaman aşımına uğradı. İnternet bağlantınızı kontrol edin."
            android.util.Log.e("MedicineDetailScreen", "Timeout loading medicine", e)
        } catch (e: Exception) {
            error = "İlaç yüklenirken hata: ${e.message}"
            android.util.Log.e("MedicineDetailScreen", "Error loading medicine", e)
        } finally {
            isLoading = false
        }
    }

    // Loading durumu
    if (isLoading) {
        Box(
            modifier = Modifier.fillMaxSize(),
            contentAlignment = androidx.compose.ui.Alignment.Center
        ) {
            CircularProgressIndicator(color = DoziTurquoise)
        }
        return
    }

    // Hata durumu
    if (error != null) {
        Box(
            modifier = Modifier.fillMaxSize(),
            contentAlignment = androidx.compose.ui.Alignment.Center
        ) {
            Column(
                horizontalAlignment = androidx.compose.ui.Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                Text(
                    text = "❌ Hata",
                    style = MaterialTheme.typography.headlineSmall,
                    color = Color.Red
                )
                Text(
                    text = error ?: "Bilinmeyen hata",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                Button(onClick = onNavigateBack) {
                    Text("Geri Dön")
                }
            }
        }
        return
    }

    // İlaç bulunamadı durumu
    if (medicine == null) {
        Box(
            modifier = Modifier.fillMaxSize(),
            contentAlignment = androidx.compose.ui.Alignment.Center
        ) {
            Column(
                horizontalAlignment = androidx.compose.ui.Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                Text(
                    text = "🔍 İlaç Bulunamadı",
                    style = MaterialTheme.typography.headlineSmall
                )
                Text(
                    text = "Bu ilaç silinmiş olabilir.",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                Button(onClick = onNavigateBack) {
                    Text("Geri Dön")
                }
            }
        }
        return
    }

    val med = medicine!! // Non-null assertion safe here

    Scaffold(
        topBar = {
            DoziTopBar(
                title = "İlaç Detayı",
                canNavigateBack = true,
                onNavigateBack = onNavigateBack,
                backgroundColor = MaterialTheme.colorScheme.surface,
                actions = {
                    IconButton(
                        onClick = { onEditMedicine(med.id) },
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
        containerColor = MaterialTheme.colorScheme.background
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .padding(20.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            // İlaç Bilgileri Kartı
            Card(
                modifier = Modifier.fillMaxWidth(),
                colors = CardDefaults.cardColors(containerColor = Color.White),
                border = BorderStroke(1.dp, VeryLightGray),
                shape = RoundedCornerShape(20.dp)
            ) {
                Column(
                    Modifier.padding(20.dp),
                    verticalArrangement = Arrangement.spacedBy(16.dp)
                ) {
                    DetailRow("İlaç Adı", med.name)
                    DetailRow("Dozaj", "${med.dosage} ${med.unit}")

                    // 📊 Stok Progress Bar
                    StockProgressIndicator(
                        currentStock = med.stockCount,
                        boxSize = med.boxSize
                    )

                    DetailRow("Form", med.form)
                    DetailRow("Kullanım Sıklığı", med.frequency)
                }
            }

            // Bilgilendirme Mesajı
            Text(
                text = "Bu ekran sadece görüntüleme içindir. İlaç bilgilerini düzenlemek için sağ üstteki 'Düzenle' butonuna dokun.",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
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
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
        Text(
            text = value,
            style = MaterialTheme.typography.titleMedium,
            fontWeight = FontWeight.Bold,
            color = MaterialTheme.colorScheme.onSurface
        )
    }
}

/**
 * 📊 Stok göstergesi (progress bar)
 */
@Composable
private fun StockProgressIndicator(
    currentStock: Int,
    boxSize: Int
) {
    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
        // Label
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = "Stok Durumu",
                style = MaterialTheme.typography.labelMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            Text(
                text = "$currentStock / $boxSize ${if (boxSize > 0) "adet" else ""}",
                style = MaterialTheme.typography.titleSmall,
                fontWeight = FontWeight.Bold,
                color = getStockColor(currentStock, boxSize)
            )
        }

        // Progress bar
        if (boxSize > 0) {
            val progress = (currentStock.toFloat() / boxSize.toFloat()).coerceIn(0f, 1f)

            LinearProgressIndicator(
                progress = progress,
                modifier = Modifier
                    .fillMaxWidth()
                    .height(12.dp),
                color = getStockColor(currentStock, boxSize),
                trackColor = VeryLightGray,
            )

            // Stok uyarı mesajı
            when {
                currentStock == 0 -> {
                    Text(
                        text = "🚨 Stok bitti! Eczaneden temin edin.",
                        style = MaterialTheme.typography.bodySmall,
                        color = Color(0xFFEF5350),
                        fontWeight = FontWeight.Medium
                    )
                }
                currentStock <= 5 -> {
                    Text(
                        text = "⚠️ Düşük stok! Eczaneden temin etmeyi unutmayın.",
                        style = MaterialTheme.typography.bodySmall,
                        color = Color(0xFFFFA726),
                        fontWeight = FontWeight.Medium
                    )
                }
            }
        } else {
            Text(
                text = "$currentStock adet",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.onSurface
            )
        }
    }
}

/**
 * Stok seviyesine göre renk döndür
 */
private fun getStockColor(currentStock: Int, boxSize: Int): Color {
    return when {
        currentStock == 0 -> Color(0xFFEF5350) // Kırmızı - Stok bitti
        currentStock <= 5 -> Color(0xFFFFA726) // Turuncu - Düşük stok
        boxSize > 0 && currentStock.toFloat() / boxSize.toFloat() < 0.25f -> Color(0xFFFFA726) // Turuncu - %25'in altında
        else -> Color(0xFF66BB6A) // Yeşil - Yeterli stok
    }
}