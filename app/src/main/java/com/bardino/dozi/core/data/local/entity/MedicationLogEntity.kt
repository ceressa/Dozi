package com.bardino.dozi.core.data.local.entity

import androidx.room.Entity
import androidx.room.PrimaryKey
import com.bardino.dozi.core.data.model.MedicationStatus

/**
 * Room entity for local medication log caching
 * Offline-first approach: Data saved locally first, then synced to Firestore
 */
@Entity(tableName = "medication_logs")
data class MedicationLogEntity(
    @PrimaryKey
    val id: String,
    val userId: String,
    val medicineId: String,
    val medicineName: String,
    val dosage: String,
    val scheduledTime: Long,            // Epoch millis
    val takenAt: Long? = null,          // Epoch millis
    val status: String,                 // MedicationStatus enum as string
    val notes: String? = null,
    val sideEffects: String? = null,    // JSON array string
    val mood: String? = null,
    val locationLat: Double? = null,
    val locationLng: Double? = null,
    val createdAt: Long,                // Epoch millis
    val updatedAt: Long,                // Epoch millis
    val isSynced: Boolean = false       // ✅ Firestore'a sync oldu mu?
)

/**
 * Convert to Firestore-compatible format
 */
fun MedicationLogEntity.toMedicationStatus(): MedicationStatus {
    return try {
        MedicationStatus.valueOf(status)
    } catch (e: Exception) {
        MedicationStatus.PENDING
    }
}
