package com.bardino.dozi.core.sync

import android.os.Build
import androidx.annotation.RequiresApi
import com.bardino.dozi.core.data.repository.MedicationLogRepository
import com.bardino.dozi.core.data.repository.UserStatsRepository

/**
 * Senkronizasyon sağlık durumu izleyici
 * Debug / admin panellerinde kullanılmak üzere basit metrikler sağlar.
 */
object SyncMonitor {

    enum class SyncHealth {
        EXCELLENT,
        GOOD,
        WARNING,
        CRITICAL
    }

    data class SyncMetrics(
        val unsyncedCount: Int,
        val lastSyncTime: Long?,
        val lastErrorMessage: String?
    )

    /**
     * Temel metrikleri hesapla
     */
    suspend fun getMetrics(
        medicationLogRepository: MedicationLogRepository
    ): SyncMetrics {
        val unsynced = medicationLogRepository.getUnsyncedCount()
        // Şimdilik lastSyncTime / lastErrorMessage yok, ileride genişletilebilir
        return SyncMetrics(
            unsyncedCount = unsynced,
            lastSyncTime = null,
            lastErrorMessage = null
        )
    }

    /**
     * Metriklere göre sağlık durumunu belirle
     */
    fun checkHealth(metrics: SyncMetrics): SyncHealth {
        return when {
            metrics.unsyncedCount == 0 -> SyncHealth.EXCELLENT
            metrics.unsyncedCount in 1..9 -> SyncHealth.GOOD
            metrics.unsyncedCount in 10..49 -> SyncHealth.WARNING
            else -> SyncHealth.CRITICAL
        }
    }

    /**
     * İleride istatistikleri yeniden hesaplamak istersen kullanırsın.
     * Şu an sadece UserStats tarafını tetiklemek için placeholder.
     */
    @RequiresApi(Build.VERSION_CODES.O)
    suspend fun recalculateStats(
        medicationLogRepository: MedicationLogRepository,
        userStatsRepository: UserStatsRepository
    ) {
        // Basit yeniden hesaplama: uyumluluk oranını tekrar hesapla
        userStatsRepository.updateComplianceRate(medicationLogRepository)
    }
}
