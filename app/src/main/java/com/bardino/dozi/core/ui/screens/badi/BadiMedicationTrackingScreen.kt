package com.bardino.dozi.core.ui.screens.badi

import androidx.compose.foundation.background
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
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import com.bardino.dozi.core.data.model.*
import com.bardino.dozi.core.ui.viewmodel.BadiViewModel
import coil.compose.AsyncImage
import java.text.SimpleDateFormat
import java.util.*

/**
 * Badi İlaç Takibi Ekranı
 * Badinin ilaç alma geçmişini ve istatistiklerini gösterir
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun BadiMedicationTrackingScreen(
    badiId: String,
    onNavigateBack: () -> Unit,
    viewModel: BadiViewModel = hiltViewModel()
) {
    val uiState by viewModel.uiState.collectAsState()
    val medicationLogs by viewModel.selectedBadiLogs.collectAsState()

    // Badiyi bul
    val badi = uiState.badis.find { it.badi.id == badiId }

    LaunchedEffect(badiId) {
        badi?.let {
            viewModel.loadBadiMedicationLogs(it.badi.buddyUserId)
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Text(badi?.badi?.nickname ?: buddy?.user?.name ?: "Badi Takibi")
                },
                navigationIcon = {
                    IconButton(onClick = onNavigateBack) {
                        Icon(Icons.Default.ArrowBack, "Geri")
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.primaryContainer
                )
            )
        }
    ) { padding ->
        if (badi == null) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(padding),
                contentAlignment = Alignment.Center
            ) {
                Text("Badi bulunamadı")
            }
            return@Scaffold
        }

        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding),
            contentPadding = PaddingValues(16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            // Badi bilgisi
            item {
                BadiInfoCard(badi = badi)
            }

            // İstatistikler
            item {
                MedicationStatsCard(logs = medicationLogs)
            }

            // Geçmiş başlık
            item {
                Text(
                    "📋 İlaç Geçmişi",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold
                )
            }

            // İlaç geçmişi
            if (medicationLogs.isEmpty()) {
                item {
                    EmptyMedicationLogsCard()
                }
            } else {
                items(medicationLogs, key = { it.id }) { log ->
                    MedicationLogCard(log = log)
                }
            }

            // Loading
            if (uiState.isLoading) {
                item {
                    Box(
                        modifier = Modifier.fillMaxWidth(),
                        contentAlignment = Alignment.Center
                    ) {
                        CircularProgressIndicator()
                    }
                }
            }
        }
    }
}

@Composable
fun BadiInfoCard(badi: BadiWithUser) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.primaryContainer
        )
    ) {
        Row(
            modifier = Modifier.padding(16.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            AsyncImage(
                model = badi.user.photoUrl,
                contentDescription = "Profil",
                modifier = Modifier
                    .size(64.dp)
                    .clip(CircleShape)
                    .background(MaterialTheme.colorScheme.primary.copy(alpha = 0.2f))
            )

            Column {
                Text(
                    buddy.badi.nickname ?: badi.user.name,
                    style = MaterialTheme.typography.titleLarge,
                    fontWeight = FontWeight.Bold
                )
                Text(
                    badi.user.email,
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f)
                )
            }
        }
    }
}

@Composable
fun MedicationStatsCard(logs: List<MedicationLog>) {
    val takenCount = logs.count { it.status == MedicationStatus.TAKEN }
    val missedCount = logs.count { it.status == MedicationStatus.MISSED }
    val skippedCount = logs.count { it.status == MedicationStatus.SKIPPED }
    val totalCount = logs.size

    val adherenceRate = if (totalCount > 0) {
        (takenCount.toFloat() / totalCount * 100).toInt()
    } else 0

    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(16.dp)
    ) {
        Column(
            modifier = Modifier.padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Text(
                "📊 İstatistikler",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold
            )

            // Uyum oranı
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    "Uyum Oranı",
                    style = MaterialTheme.typography.bodyLarge
                )
                Text(
                    "%$adherenceRate",
                    style = MaterialTheme.typography.headlineMedium,
                    fontWeight = FontWeight.Bold,
                    color = when {
                        adherenceRate >= 80 -> MaterialTheme.colorScheme.primary
                        adherenceRate >= 60 -> MaterialTheme.colorScheme.tertiary
                        else -> MaterialTheme.colorScheme.error
                    }
                )
            }

            LinearProgressIndicator(
                progress = adherenceRate / 100f,
                modifier = Modifier
                    .fillMaxWidth()
                    .height(8.dp)
                    .clip(RoundedCornerShape(4.dp))
            )

            Divider()

            // Detaylı istatistikler
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceEvenly
            ) {
                StatItem(
                    icon = "✅",
                    label = "Alınan",
                    value = takenCount.toString()
                )
                StatItem(
                    icon = "❌",
                    label = "Kaçırılan",
                    value = missedCount.toString()
                )
                StatItem(
                    icon = "⏭️",
                    label = "Atlanan",
                    value = skippedCount.toString()
                )
            }
        }
    }
}

@Composable
fun StatItem(
    icon: String,
    label: String,
    value: String
) {
    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(4.dp)
    ) {
        Text(
            icon,
            style = MaterialTheme.typography.headlineMedium
        )
        Text(
            value,
            style = MaterialTheme.typography.titleLarge,
            fontWeight = FontWeight.Bold
        )
        Text(
            label,
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f)
        )
    }
}

@Composable
fun MedicationLogCard(log: MedicationLog) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(12.dp)
    ) {
        Row(
            modifier = Modifier.padding(16.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            // Durum ikonu
            Surface(
                shape = CircleShape,
                color = when (log.status) {
                    MedicationStatus.TAKEN -> MaterialTheme.colorScheme.primaryContainer
                    MedicationStatus.MISSED -> MaterialTheme.colorScheme.errorContainer
                    MedicationStatus.SKIPPED -> MaterialTheme.colorScheme.tertiaryContainer
                    else -> MaterialTheme.colorScheme.surfaceVariant
                }
            ) {
                Text(
                    log.status.toEmoji(),
                    modifier = Modifier.padding(12.dp),
                    style = MaterialTheme.typography.headlineSmall
                )
            }

            // İlaç bilgisi
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    log.medicineName,
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold
                )
                Text(
                    log.dosage,
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f)
                )
                log.scheduledTime?.let { time ->
                    Text(
                        formatTimestamp(time),
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.5f)
                    )
                }
            }

            // Durum metni
            Surface(
                shape = RoundedCornerShape(8.dp),
                color = when (log.status) {
                    MedicationStatus.TAKEN -> MaterialTheme.colorScheme.primaryContainer
                    MedicationStatus.MISSED -> MaterialTheme.colorScheme.errorContainer
                    MedicationStatus.SKIPPED -> MaterialTheme.colorScheme.tertiaryContainer
                    else -> MaterialTheme.colorScheme.surfaceVariant
                }
            ) {
                Text(
                    log.status.toTurkish(),
                    modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp),
                    style = MaterialTheme.typography.labelMedium,
                    fontWeight = FontWeight.Bold
                )
            }
        }
    }
}

@Composable
fun EmptyMedicationLogsCard() {
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f)
        )
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(32.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            Text(
                "📭",
                style = MaterialTheme.typography.displayMedium
            )
            Text(
                "Henüz kayıt yok",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold
            )
            Text(
                "Badi henüz ilaç kaydı oluşturmamış",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f)
            )
        }
    }
}

private fun formatTimestamp(timestamp: com.google.firebase.Timestamp): String {
    val date = timestamp.toDate()
    val dateFormat = SimpleDateFormat("dd MMM yyyy, HH:mm", Locale("tr"))
    return dateFormat.format(date)
}
