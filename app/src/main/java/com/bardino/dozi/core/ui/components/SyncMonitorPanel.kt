package com.bardino.dozi.core.sync.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.bardino.dozi.core.data.repository.MedicationLogRepository
import com.bardino.dozi.core.data.repository.UserStatsRepository
import com.bardino.dozi.core.sync.SyncMonitor
import kotlinx.coroutines.launch

@Composable
fun SyncMonitorPanel(
    medicationLogRepository: MedicationLogRepository,
    userStatsRepository: UserStatsRepository,
    modifier: Modifier = Modifier
) {
    val scope = rememberCoroutineScope()

    var metrics by remember { mutableStateOf<SyncMonitor.SyncMetrics?>(null) }
    var health by remember { mutableStateOf(SyncMonitor.SyncHealth.EXCELLENT) }
    var loading by remember { mutableStateOf(true) }

    LaunchedEffect(Unit) {
        loading = true
        val m = SyncMonitor.getMetrics(medicationLogRepository)
        metrics = m
        health = SyncMonitor.checkHealth(m)
        loading = false
    }

    Card(
        modifier = modifier
            .fillMaxWidth()
            .padding(16.dp),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surface
        ),
        elevation = CardDefaults.cardElevation(defaultElevation = 4.dp)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp)
        ) {
            Text(
                text = "Sync Monitor",
                style = MaterialTheme.typography.titleMedium
            )

            Spacer(modifier = Modifier.height(8.dp))

            if (loading || metrics == null) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    CircularProgressIndicator(
                        modifier = Modifier.size(20.dp),
                        strokeWidth = 2.dp
                    )
                    Text("Senkronizasyon durumu yükleniyor…")
                }
            } else {
                val m = metrics!!

                val healthText: String
                val healthColor = when (health) {
                    SyncMonitor.SyncHealth.EXCELLENT -> {
                        healthText = "Mükemmel"
                        MaterialTheme.colorScheme.primary
                    }
                    SyncMonitor.SyncHealth.GOOD -> {
                        healthText = "İyi"
                        MaterialTheme.colorScheme.tertiary
                    }
                    SyncMonitor.SyncHealth.WARNING -> {
                        healthText = "Uyarı"
                        MaterialTheme.colorScheme.error
                    }
                    SyncMonitor.SyncHealth.CRITICAL -> {
                        healthText = "Kritik"
                        MaterialTheme.colorScheme.error
                    }
                }

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column {
                        Text(
                            text = "Sağlık Durumu",
                            style = MaterialTheme.typography.labelMedium
                        )
                        Text(
                            text = healthText,
                            color = healthColor,
                            style = MaterialTheme.typography.bodyLarge
                        )
                    }

                    Column(horizontalAlignment = Alignment.End) {
                        Text(
                            text = "Bekleyen log sayısı",
                            style = MaterialTheme.typography.labelMedium
                        )
                        Text(
                            text = "${m.unsyncedCount}",
                            style = MaterialTheme.typography.bodyLarge
                        )
                    }
                }

                Spacer(modifier = Modifier.height(12.dp))

                // Gerekirse manuel yeniden hesaplama butonu vs. ekleyebilirsin
                // Örneğin:
                /*
                Button(onClick = {
                    scope.launch {
                        SyncMonitor.recalculateStats(medicationLogRepository, userStatsRepository)
                        val newMetrics = SyncMonitor.getMetrics(medicationLogRepository)
                        metrics = newMetrics
                        health = SyncMonitor.checkHealth(newMetrics)
                    }
                }) {
                    Text("İstatistikleri Yeniden Hesapla")
                }
                */
            }
        }
    }
}
