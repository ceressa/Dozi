package com.bardino.dozi.core.data.local.dao

import androidx.room.*
import com.bardino.dozi.core.data.local.entity.MedicationLogEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface MedicationLogDao {

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(log: MedicationLogEntity)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertAll(logs: List<MedicationLogEntity>)

    @Update
    suspend fun update(log: MedicationLogEntity)

    @Delete
    suspend fun delete(log: MedicationLogEntity)

    @Query("SELECT * FROM medication_logs WHERE id = :logId")
    suspend fun getById(logId: String): MedicationLogEntity?

    @Query("SELECT * FROM medication_logs WHERE userId = :userId ORDER BY scheduledTime DESC")
    fun getAllByUserFlow(userId: String): Flow<List<MedicationLogEntity>>

    @Query("SELECT * FROM medication_logs WHERE userId = :userId ORDER BY scheduledTime DESC")
    suspend fun getAllByUser(userId: String): List<MedicationLogEntity>

    @Query("SELECT * FROM medication_logs ORDER BY scheduledTime DESC")
    suspend fun getAllLogsList(): List<MedicationLogEntity>

    @Query("SELECT * FROM medication_logs WHERE userId = :userId AND scheduledTime >= :startTime AND scheduledTime < :endTime ORDER BY scheduledTime ASC")
    suspend fun getByDateRange(userId: String, startTime: Long, endTime: Long): List<MedicationLogEntity>

    @Query("SELECT * FROM medication_logs WHERE userId = :userId AND isSynced = 0")
    suspend fun getUnsyncedLogs(userId: String): List<MedicationLogEntity>

    @Query("UPDATE medication_logs SET isSynced = 1 WHERE id = :logId")
    suspend fun markAsSynced(logId: String)

    @Query("UPDATE medication_logs SET isSynced = 1 WHERE id IN (:logIds)")
    suspend fun markAllAsSynced(logIds: List<String>)

    @Query("DELETE FROM medication_logs WHERE userId = :userId")
    suspend fun deleteAllByUser(userId: String)

    @Query("SELECT COUNT(*) FROM medication_logs WHERE userId = :userId AND status = 'TAKEN' AND scheduledTime >= :startTime AND scheduledTime < :endTime")
    suspend fun getTakenCount(userId: String, startTime: Long, endTime: Long): Int

    @Query("SELECT COUNT(*) FROM medication_logs WHERE userId = :userId AND status = 'MISSED' AND scheduledTime >= :startTime AND scheduledTime < :endTime")
    suspend fun getMissedCount(userId: String, startTime: Long, endTime: Long): Int

    @Query("SELECT COUNT(*) FROM medication_logs WHERE userId = :userId AND status = 'SKIPPED' AND scheduledTime >= :startTime AND scheduledTime < :endTime")
    suspend fun getSkippedCount(userId: String, startTime: Long, endTime: Long): Int

    /**
     * 🔥 Belirli bir ilaç için belirli bir zamanda log var mı kontrol et
     * Escalation handler'ları için (duplicate bildirim önleme)
     * ±5 dakika toleransla kontrol eder
     */
    @Query("""
        SELECT * FROM medication_logs
        WHERE medicineId = :medicineId
        AND userId = :userId
        AND scheduledTime >= :scheduledTime - 300000
        AND scheduledTime <= :scheduledTime + 300000
        LIMIT 1
    """)
    suspend fun getLogForMedicineAndTime(
        medicineId: String,
        scheduledTime: Long,
        userId: String
    ): MedicationLogEntity?
}
