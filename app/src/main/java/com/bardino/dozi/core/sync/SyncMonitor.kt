package com.bardino.dozi.core.sync

import android.content.Context
import android.util.Log
import com.bardino.dozi.core.data.local.dao.SyncQueueDao
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import dagger.hilt.android.qualifiers.ApplicationContext
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Senkronizasyon durumunu izleyen sınıf
 */
@Singleton
class SyncMonitor @Inject constructor(
    @ApplicationContext private val context: Context,
    private val syncQueueDao: SyncQueueDao,
    private val firestore: FirebaseFirestore,
    private val auth: FirebaseAuth
) {
    companion object {
        private const val TAG = "SyncMonitor"
    }

    data class SyncMetrics(
        val pendingCount: Int,
        val failedLast24h: Int,
        val averageDelayMs: Long,
        val oldestPendingAge: Long,
        val lastSyncTime: Long
    )

    /**
     * Mevcut senkronizasyon metriklerini al
     */
    suspend fun getMetrics(): SyncMetrics {
        return try {
            val pending = syncQueueDao.getPendingCount()
            val since24h = System.currentTimeMillis() - (24 * 60 * 60 * 1000)

            // Failed count - items with retryCount >= 5
            val allItems = syncQueueDao.getPendingWithRetries()
            val failed = allItems.count { it.retryCount >= 5 && (it.lastAttemptAt ?: 0) > since24h }

            // Average delay calculation
            val completedItems = allItems.filter { it.lastAttemptAt != null }
            val avgDelay = if (completedItems.isNotEmpty()) {
                completedItems.map { (it.lastAttemptAt ?: it.createdAt) - it.createdAt }
                    .average()
                    .toLong()
            } else 0L

            // Oldest pending age
            val oldestAge = if (allItems.isNotEmpty()) {
                System.currentTimeMillis() - allItems.minOf { it.createdAt }
            } else 0L

            // Last sync time from SharedPreferences
            val prefs = context.getSharedPreferences("sync_prefs", Context.MODE_PRIVATE)
            val lastSync = prefs.getLong("last_sync_time", 0L)

            SyncMetrics(
                pendingCount = pending,
                failedLast24h = failed,
                averageDelayMs = avgDelay,
                oldestPendingAge = oldestAge,
                lastSyncTime = lastSync
            )
        } catch (e: Exception) {
            Log.e(TAG, "Error getting metrics", e)
            SyncMetrics(0, 0, 0L, 0L, 0L)
        }
    }

    /**
     * Senkronizasyon olayını logla
     */
    suspend fun logSyncEvent(
        itemId: Long,
        success: Boolean,
        errorCode: String? = null,
        durationMs: Long
    ) {
        try {
            if (!success && errorCode != null) {
                // Increment failure count would be done via DAO update
                Log.w(TAG, "Sync failed for item $itemId: $errorCode (${durationMs}ms)")
            } else {
                Log.d(TAG, "Sync success for item $itemId (${durationMs}ms)")
            }

            // Update last sync time
            val prefs = context.getSharedPreferences("sync_prefs", Context.MODE_PRIVATE)
            prefs.edit().putLong("last_sync_time", System.currentTimeMillis()).apply()

            // Optionally log to Firestore analytics
            if (shouldLogToFirestore()) {
                logToFirestoreAnalytics(itemId, success, errorCode, durationMs)
            }
        } catch (e: Exception) {
            Log.e(TAG, "Error logging sync event", e)
        }
    }

    /**
     * Senkronizasyon sağlık durumunu kontrol et
     */
    suspend fun checkHealth(): SyncHealth {
        val metrics = getMetrics()

        return when {
            metrics.failedLast24h > 10 -> SyncHealth.CRITICAL
            metrics.pendingCount > 50 -> SyncHealth.WARNING
            metrics.oldestPendingAge > 60 * 60 * 1000 -> SyncHealth.WARNING // 1 saat
            else -> SyncHealth.HEALTHY
        }
    }

    enum class SyncHealth {
        HEALTHY,
        WARNING,
        CRITICAL
    }

    private fun shouldLogToFirestore(): Boolean {
        // Only log to Firestore occasionally to avoid excessive writes
        return (System.currentTimeMillis() % 10) == 0L
    }

    private fun logToFirestoreAnalytics(
        itemId: Long,
        success: Boolean,
        errorCode: String?,
        durationMs: Long
    ) {
        val userId = auth.currentUser?.uid ?: return

        firestore.collection("analytics")
            .document("sync_events")
            .collection(userId)
            .add(mapOf(
                "itemId" to itemId,
                "success" to success,
                "errorCode" to errorCode,
                "durationMs" to durationMs,
                "timestamp" to System.currentTimeMillis()
            ))
            .addOnFailureListener { e ->
                Log.w(TAG, "Failed to log to Firestore", e)
            }
    }

    /**
     * Kuyruk durumu özeti
     */
    suspend fun getQueueSummary(): String {
        val metrics = getMetrics()
        return buildString {
            append("Bekleyen: ${metrics.pendingCount}")
            if (metrics.failedLast24h > 0) {
                append(" | Başarısız (24s): ${metrics.failedLast24h}")
            }
            if (metrics.averageDelayMs > 0) {
                append(" | Ort. Gecikme: ${metrics.averageDelayMs / 1000}s")
            }
        }
    }
}
