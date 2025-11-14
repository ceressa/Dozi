package com.bardino.dozi.core.data.local.dao

import androidx.room.*
import com.bardino.dozi.core.data.local.entity.SyncQueueEntity

@Dao
interface SyncQueueDao {

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(action: SyncQueueEntity): Long

    @Update
    suspend fun update(action: SyncQueueEntity)

    @Delete
    suspend fun delete(action: SyncQueueEntity)

    @Query("SELECT * FROM sync_queue WHERE userId = :userId ORDER BY createdAt ASC")
    suspend fun getAllPending(userId: String): List<SyncQueueEntity>

    @Query("SELECT * FROM sync_queue WHERE userId = :userId AND retryCount < 5 ORDER BY createdAt ASC")
    suspend fun getPendingWithRetries(userId: String): List<SyncQueueEntity>

    @Query("DELETE FROM sync_queue WHERE id = :id")
    suspend fun deleteById(id: Long)

    @Query("DELETE FROM sync_queue WHERE userId = :userId")
    suspend fun deleteAllByUser(userId: String)

    @Query("SELECT COUNT(*) FROM sync_queue WHERE userId = :userId")
    suspend fun getPendingCount(userId: String): Int

    @Query("UPDATE sync_queue SET retryCount = retryCount + 1, lastAttemptAt = :attemptTime, errorMessage = :error WHERE id = :id")
    suspend fun incrementRetryCount(id: Long, attemptTime: Long, error: String?)
}
