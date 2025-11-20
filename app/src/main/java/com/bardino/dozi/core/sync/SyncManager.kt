package com.bardino.dozi.core.sync

import android.content.Context
import android.util.Log
import com.bardino.dozi.core.data.local.DoziDatabase
import com.bardino.dozi.core.data.local.entity.SyncActionType
import com.bardino.dozi.core.data.local.entity.SyncQueueEntity
import com.bardino.dozi.core.data.model.Medicine
import com.bardino.dozi.core.utils.NetworkUtils
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import com.google.gson.Gson
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.tasks.await
import kotlinx.coroutines.withContext

/**
 * Manages offline sync queue for medicine operations
 * Implements offline-first approach with conflict resolution
 */
class SyncManager(private val context: Context) {

    private val db = DoziDatabase.getDatabase(context)
    private val syncQueueDao = db.syncQueueDao()
    private val firestore = FirebaseFirestore.getInstance()
    private val auth = FirebaseAuth.getInstance()
    private val gson = Gson()

    companion object {
        private const val TAG = "SyncManager"
        private const val MAX_RETRIES = 5
    }

    /**
     * Add a medicine add operation to the sync queue
     */
    suspend fun queueMedicineAdd(medicine: Medicine): Long {
        val userId = auth.currentUser?.uid ?: return -1

        val entity = SyncQueueEntity(
            actionType = SyncActionType.MEDICINE_ADDED,
            dataJson = gson.toJson(medicine),
            userId = userId,
            createdAt = System.currentTimeMillis()
        )

        val id = syncQueueDao.insert(entity)
        Log.d(TAG, "✅ Medicine ADD queued: ${medicine.name} (queueId: $id)")

        // Try immediate sync if online
        if (NetworkUtils.isNetworkAvailable(context)) {
            processPendingSync()
        }

        return id
    }

    /**
     * Add a medicine update operation to the sync queue
     */
    suspend fun queueMedicineUpdate(medicine: Medicine): Long {
        val userId = auth.currentUser?.uid ?: return -1

        val entity = SyncQueueEntity(
            actionType = SyncActionType.MEDICINE_UPDATED,
            dataJson = gson.toJson(medicine),
            userId = userId,
            createdAt = System.currentTimeMillis()
        )

        val id = syncQueueDao.insert(entity)
        Log.d(TAG, "✅ Medicine UPDATE queued: ${medicine.name} (queueId: $id)")

        // Try immediate sync if online
        if (NetworkUtils.isNetworkAvailable(context)) {
            processPendingSync()
        }

        return id
    }

    /**
     * Add a medicine delete operation to the sync queue
     */
    suspend fun queueMedicineDelete(medicineId: String): Long {
        val userId = auth.currentUser?.uid ?: return -1

        // Store just the ID for deletion
        val dataJson = gson.toJson(mapOf("medicineId" to medicineId))

        val entity = SyncQueueEntity(
            actionType = SyncActionType.MEDICINE_DELETED,
            dataJson = dataJson,
            userId = userId,
            createdAt = System.currentTimeMillis()
        )

        val id = syncQueueDao.insert(entity)
        Log.d(TAG, "✅ Medicine DELETE queued: $medicineId (queueId: $id)")

        // Try immediate sync if online
        if (NetworkUtils.isNetworkAvailable(context)) {
            processPendingSync()
        }

        return id
    }

    /**
     * Process all pending sync operations
     * @return Number of successfully processed items
     */
    suspend fun processPendingSync(): Int = withContext(Dispatchers.IO) {
        val userId = auth.currentUser?.uid
        if (userId == null) {
            Log.w(TAG, "⚠️ Cannot sync: user not authenticated")
            return@withContext 0
        }

        if (!NetworkUtils.isNetworkAvailable(context)) {
            Log.w(TAG, "⚠️ Cannot sync: no network available")
            return@withContext 0
        }

        val pendingItems = syncQueueDao.getPendingWithRetries(userId)

        if (pendingItems.isEmpty()) {
            Log.d(TAG, "ℹ️ No pending sync items")
            return@withContext 0
        }

        Log.d(TAG, "🔄 Processing ${pendingItems.size} pending sync items...")

        var successCount = 0

        for (item in pendingItems) {
            try {
                val success = when (item.actionType) {
                    SyncActionType.MEDICINE_ADDED -> processMedicineAdd(item)
                    SyncActionType.MEDICINE_UPDATED -> processMedicineUpdate(item)
                    SyncActionType.MEDICINE_DELETED -> processMedicineDelete(item)
                    else -> {
                        Log.w(TAG, "⚠️ Unknown action type: ${item.actionType}")
                        false
                    }
                }

                if (success) {
                    syncQueueDao.deleteById(item.id)
                    successCount++
                    Log.d(TAG, "✅ Sync successful for item ${item.id}")
                } else {
                    syncQueueDao.incrementRetryCount(
                        id = item.id,
                        attemptTime = System.currentTimeMillis(),
                        error = "Sync failed"
                    )
                    Log.w(TAG, "⚠️ Sync failed for item ${item.id}, retry count: ${item.retryCount + 1}")
                }
            } catch (e: Exception) {
                Log.e(TAG, "❌ Error processing sync item ${item.id}", e)
                syncQueueDao.incrementRetryCount(
                    id = item.id,
                    attemptTime = System.currentTimeMillis(),
                    error = e.message
                )
            }
        }

        Log.d(TAG, "🔥 Sync complete: $successCount/${pendingItems.size} items processed")
        return@withContext successCount
    }

    /**
     * Process a medicine add operation
     */
    private suspend fun processMedicineAdd(item: SyncQueueEntity): Boolean {
        return try {
            val medicine = gson.fromJson(item.dataJson, Medicine::class.java)
            val collection = getMedicinesCollection() ?: return false

            // Set timestamps
            val medicineToSave = medicine.copy(
                userId = item.userId,
                createdAt = item.createdAt,
                updatedAt = System.currentTimeMillis()
            )

            collection.document(medicine.id).set(medicineToSave).await()
            Log.d(TAG, "✅ Medicine added to Firestore: ${medicine.name}")
            true
        } catch (e: Exception) {
            Log.e(TAG, "❌ Failed to add medicine to Firestore", e)
            false
        }
    }

    /**
     * Process a medicine update operation with conflict resolution
     */
    private suspend fun processMedicineUpdate(item: SyncQueueEntity): Boolean {
        return try {
            val localMedicine = gson.fromJson(item.dataJson, Medicine::class.java)
            val collection = getMedicinesCollection() ?: return false

            // Get server version for conflict check
            val serverDoc = collection.document(localMedicine.id).get().await()

            if (serverDoc.exists()) {
                val serverMedicine = serverDoc.toObject(Medicine::class.java)

                if (serverMedicine != null) {
                    // Timestamp-based conflict resolution
                    // Local wins if it was queued after server's last update
                    val localUpdateTime = item.createdAt
                    val serverUpdateTime = serverMedicine.updatedAt

                    if (serverUpdateTime > localUpdateTime) {
                        // Server version is newer - need merge
                        Log.d(TAG, "🔀 Conflict detected: server version newer, merging...")

                        // Smart merge: keep server values for some fields, local for others
                        val mergedMedicine = mergeConflict(localMedicine, serverMedicine, item.createdAt)
                        collection.document(localMedicine.id).set(mergedMedicine).await()
                        Log.d(TAG, "✅ Medicine merged and saved: ${localMedicine.name}")
                    } else {
                        // Local version is newer - safe to overwrite
                        val updatedMedicine = localMedicine.copy(
                            updatedAt = System.currentTimeMillis()
                        )
                        collection.document(localMedicine.id).set(updatedMedicine).await()
                        Log.d(TAG, "✅ Medicine updated in Firestore: ${localMedicine.name}")
                    }
                } else {
                    // Document exists but can't parse - overwrite
                    val updatedMedicine = localMedicine.copy(
                        updatedAt = System.currentTimeMillis()
                    )
                    collection.document(localMedicine.id).set(updatedMedicine).await()
                }
            } else {
                // Document doesn't exist - create it
                val updatedMedicine = localMedicine.copy(
                    updatedAt = System.currentTimeMillis()
                )
                collection.document(localMedicine.id).set(updatedMedicine).await()
            }

            true
        } catch (e: Exception) {
            Log.e(TAG, "❌ Failed to update medicine in Firestore", e)
            false
        }
    }

    /**
     * Smart merge for conflict resolution
     * Local changes win for most fields, but server wins for certain important fields
     */
    private fun mergeConflict(local: Medicine, server: Medicine, localQueueTime: Long): Medicine {
        return local.copy(
            // Server wins for these (important not to lose server state)
            sharedWithBadis = server.sharedWithBadis,

            // Use latest stockCount (could be updated from multiple devices)
            stockCount = if (server.updatedAt > localQueueTime) server.stockCount else local.stockCount,

            // Always use latest timestamp
            updatedAt = System.currentTimeMillis(),

            // Keep server creation time
            createdAt = server.createdAt
        )
    }

    /**
     * Process a medicine delete operation
     */
    private suspend fun processMedicineDelete(item: SyncQueueEntity): Boolean {
        return try {
            val data = gson.fromJson(item.dataJson, Map::class.java) as Map<String, Any>
            val medicineId = data["medicineId"] as String

            val collection = getMedicinesCollection() ?: return false
            collection.document(medicineId).delete().await()

            Log.d(TAG, "✅ Medicine deleted from Firestore: $medicineId")
            true
        } catch (e: Exception) {
            Log.e(TAG, "❌ Failed to delete medicine from Firestore", e)
            false
        }
    }

    /**
     * Get current user's medicine collection reference
     */
    private fun getMedicinesCollection() = auth.currentUser?.let { user ->
        firestore.collection("users").document(user.uid).collection("medicines")
    }

    /**
     * Get pending sync count for the current user
     */
    suspend fun getPendingSyncCount(): Int {
        val userId = auth.currentUser?.uid ?: return 0
        return syncQueueDao.getPendingCount(userId)
    }

    /**
     * Clear all pending sync items for the current user
     * Use with caution - this will discard unsent changes
     */
    suspend fun clearPendingSync() {
        val userId = auth.currentUser?.uid ?: return
        syncQueueDao.deleteAllByUser(userId)
        Log.d(TAG, "🗑️ All pending sync items cleared for user")
    }
}
