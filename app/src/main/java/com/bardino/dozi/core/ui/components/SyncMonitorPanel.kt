package com.bardino.dozi.core.ui.components

import androidx.compose.foundation.layout.*
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import com.bardino.dozi.core.sync.SyncMonitor
import com.bardino.dozi.core.ui.components.base.*
import kotlinx.coroutines.launch

/**
 * Admin Dashboard için senkronizasyon durumu paneli
 */
@Composable
fun SyncMonitorPanel(
    syncMonitor: SyncMonitor,
    modifier: Modifier = Modifier
) {
    var metrics by remember { mutableStateOf<SyncMonitor.SyncMetrics?>(null) }
    var health by remember { mutableStateOf(SyncMonitor.SyncHealth.HEALTHY) }
    val scope = rememberCoroutineScope()

    LaunchedEffect(Unit) {
        scope.launch {
            metrics = syncMonitor.getMetrics()
            health = syncMonitor.checkHealth()
        }
    }

    DoziCard(modifier = modifier.fillMaxWidth()) {
        Column {
            // Başlık
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Text(
                    text = "Senkronizasyon Durumu",
                    style = DoziTypography.subtitle1,
                    color = DoziColors.OnSurface
                )

                // Sağlık göstergesi
                Text(
                    text = when (health) {
                        SyncMonitor.SyncHealth.HEALTHY -> "✅ Sağlıklı"
                        SyncMonitor.SyncHealth.WARNING -> "⚠️ Uyarı"
                        SyncMonitor.SyncHealth.CRITICAL -> "🔴 Kritik"
                    },
                    style = DoziTypography.caption,
                    color = when (health) {
                        SyncMonitor.SyncHealth.HEALTHY -> DoziColors.Success
                        SyncMonitor.SyncHealth.WARNING -> DoziColors.Warning
                        SyncMonitor.SyncHealth.CRITICAL -> DoziColors.Error
                    }
                )
            }

            Spacer(modifier = Modifier.height(DoziSpacing.md))

            metrics?.let { m ->
                // Metrikler
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    MetricItem(
                        label = "Bekleyen",
                        value = m.pendingCount.toString()
                    )
                    MetricItem(
                        label = "Başarısız (24s)",
                        value = m.failedLast24h.toString(),
                        valueColor = if (m.failedLast24h > 5) DoziColors.Error else DoziColors.OnSurface
                    )
                    MetricItem(
                        label = "Ort. Gecikme",
                        value = "${m.averageDelayMs / 1000}s"
                    )
                }

                // Uyarılar
                if (m.failedLast24h > 10) {
                    Spacer(modifier = Modifier.height(DoziSpacing.sm))
                    Text(
                        text = "⚠️ Yüksek hata oranı tespit edildi",
                        style = DoziTypography.caption,
                        color = DoziColors.Error
                    )
                }

                if (m.oldestPendingAge > 60 * 60 * 1000) { // 1 saat
                    Spacer(modifier = Modifier.height(DoziSpacing.xs))
                    Text(
                        text = "⏱️ En eski bekleyen ${m.oldestPendingAge / 60000} dakikadır kuyrukta",
                        style = DoziTypography.caption,
                        color = DoziColors.Warning
                    )
                }

                // Son senkronizasyon
                if (m.lastSyncTime > 0) {
                    Spacer(modifier = Modifier.height(DoziSpacing.sm))
                    val minutesAgo = (System.currentTimeMillis() - m.lastSyncTime) / 60000
                    Text(
                        text = "Son senkronizasyon: $minutesAgo dakika önce",
                        style = DoziTypography.caption,
                        color = DoziColors.OnSurface.copy(alpha = 0.7f)
                    )
                }
            } ?: run {
                Text(
                    text = "Yükleniyor...",
                    style = DoziTypography.body2,
                    color = DoziColors.OnSurface.copy(alpha = 0.5f)
                )
            }
        }
    }
}

@Composable
private fun MetricItem(
    label: String,
    value: String,
    valueColor: androidx.compose.ui.graphics.Color = DoziColors.OnSurface
) {
    Column {
        Text(
            text = value,
            style = DoziTypography.h3,
            color = valueColor
        )
        Text(
            text = label,
            style = DoziTypography.caption,
            color = DoziColors.OnSurface.copy(alpha = 0.7f)
        )
    }
}
