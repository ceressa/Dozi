package com.bardino.dozi.core.data.local.entity

import androidx.room.Entity
import androidx.room.PrimaryKey

/**
 * Queue for pending Firestore sync operations
 * Used for offline-first approach
 */
@Entity(tableName = "sync_queue")
data class SyncQueueEntity(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,
    val actionType: String,             // "MEDICATION_TAKEN", "MEDICATION_SKIPPED", "MEDICINE_ADDED", etc.
    val dataJson: String,               // JSON payload for the action
    val userId: String,
    val retryCount: Int = 0,
    val createdAt: Long,                // Epoch millis
    val lastAttemptAt: Long? = null,    // Epoch millis
    val errorMessage: String? = null
)

/**
 * Sync action types
 */
object SyncActionType {
    const val MEDICATION_TAKEN = "MEDICATION_TAKEN"
    const val MEDICATION_SKIPPED = "MEDICATION_SKIPPED"
    const val MEDICATION_SNOOZED = "MEDICATION_SNOOZED"
    const val MEDICINE_ADDED = "MEDICINE_ADDED"
    const val MEDICINE_UPDATED = "MEDICINE_UPDATED"
    const val MEDICINE_DELETED = "MEDICINE_DELETED"
}
