package com.bardino.dozi.core.data.repository

import android.os.Build
import androidx.annotation.RequiresApi
import com.bardino.dozi.core.data.local.dao.MedicationLogDao
import com.bardino.dozi.core.data.model.DayCompliance
import com.bardino.dozi.core.data.model.MedicationStatus
import com.bardino.dozi.core.data.model.ProfileStats
import com.google.firebase.firestore.FirebaseFirestore
import kotlinx.coroutines.tasks.await
import java.text.SimpleDateFormat
import java.util.*
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Repository for calculating profile statistics
 */
@Singleton
class ProfileStatsRepository @Inject constructor(
    private val medicationLogDao: MedicationLogDao,
    private val db: FirebaseFirestore
) {

    /**
     * Get statistics for a specific profile
     */
    @RequiresApi(Build.VERSION_CODES.O)
    suspend fun getProfileStats(
        userId: String,
        profileId: String,
        profileName: String
    ): ProfileStats {
        // Get total medicines for this profile
        val totalMedicines = getMedicineCountForProfile(userId, profileId)

        // Get today's medicines
        val todayMedicines = getTodayMedicineCountForProfile(userId, profileId)

        // Get today's logs
        val todayLogs = getTodayLogsForProfile(profileId)
        val takenToday = todayLogs.count { it.status == MedicationStatus.TAKEN.name }
        val missedToday = todayLogs.count { it.status == MedicationStatus.MISSED.name }

        // Calculate compliance rate
        val complianceRate = if (todayMedicines > 0) {
            (takenToday.toFloat() / todayMedicines.toFloat()) * 100f
        } else {
            0f
        }

        // Get last 7 days compliance
        val last7Days = getLast7DaysCompliance(profileId)

        return ProfileStats(
            profileId = profileId,
            profileName = profileName,
            totalMedicines = totalMedicines,
            todayMedicines = todayMedicines,
            takenToday = takenToday,
            missedToday = missedToday,
            complianceRate = complianceRate,
            last7DaysCompliance = last7Days
        )
    }

    /**
     * Get medicine count for a profile from Firestore
     */
    private suspend fun getMedicineCountForProfile(userId: String, profileId: String): Int {
        return try {
            val snapshot = db.collection("users")
                .document(userId)
                .collection("medicines")
                .whereEqualTo("profileId", profileId)
                .get()
                .await()
            snapshot.size()
        } catch (e: Exception) {
            0
        }
    }

    /**
     * Get today's medicine count (medicines that should be taken today)
     */
    @RequiresApi(Build.VERSION_CODES.O)
    private suspend fun getTodayMedicineCountForProfile(userId: String, profileId: String): Int {
        return try {
            val snapshot = db.collection("users")
                .document(userId)
                .collection("medicines")
                .whereEqualTo("profileId", profileId)
                .whereEqualTo("reminderEnabled", true)
                .get()
                .await()

            // Filter medicines that are active today
            val today = System.currentTimeMillis()
            val todayMedicines = snapshot.documents.mapNotNull { doc ->
                val startDate = doc.getLong("startDate") ?: 0L
                val endDate = doc.getLong("endDate")

                val isActive = startDate <= today && (endDate == null || endDate >= today)
                if (isActive) 1 else null
            }

            todayMedicines.size
        } catch (e: Exception) {
            0
        }
    }

    /**
     * Get today's medication logs from Room
     */
    private suspend fun getTodayLogsForProfile(profileId: String): List<com.bardino.dozi.core.data.local.entity.MedicationLogEntity> {
        val calendar = Calendar.getInstance()
        calendar.set(Calendar.HOUR_OF_DAY, 0)
        calendar.set(Calendar.MINUTE, 0)
        calendar.set(Calendar.SECOND, 0)
        calendar.set(Calendar.MILLISECOND, 0)
        val startOfDay = calendar.timeInMillis

        calendar.set(Calendar.HOUR_OF_DAY, 23)
        calendar.set(Calendar.MINUTE, 59)
        calendar.set(Calendar.SECOND, 59)
        val endOfDay = calendar.timeInMillis

        return try {
            medicationLogDao.getAllLogsList()
                .filter { it.profileId == profileId }
                .filter { it.scheduledTime in startOfDay..endOfDay }
        } catch (e: Exception) {
            emptyList()
        }
    }

    /**
     * Get last 7 days compliance data
     */
    private suspend fun getLast7DaysCompliance(profileId: String): List<DayCompliance> {
        val result = mutableListOf<DayCompliance>()
        val dateFormat = SimpleDateFormat("dd/MM", Locale.getDefault())

        for (i in 6 downTo 0) {
            val calendar = Calendar.getInstance()
            calendar.add(Calendar.DAY_OF_YEAR, -i)

            // Start of day
            calendar.set(Calendar.HOUR_OF_DAY, 0)
            calendar.set(Calendar.MINUTE, 0)
            calendar.set(Calendar.SECOND, 0)
            calendar.set(Calendar.MILLISECOND, 0)
            val startOfDay = calendar.timeInMillis

            // End of day
            calendar.set(Calendar.HOUR_OF_DAY, 23)
            calendar.set(Calendar.MINUTE, 59)
            calendar.set(Calendar.SECOND, 59)
            val endOfDay = calendar.timeInMillis

            try {
                val logs = medicationLogDao.getAllLogsList()
                    .filter { it.profileId == profileId }
                    .filter { it.scheduledTime in startOfDay..endOfDay }

                val total = logs.size
                val taken = logs.count { it.status == MedicationStatus.TAKEN.name }
                val rate = if (total > 0) (taken.toFloat() / total.toFloat()) * 100f else 0f

                result.add(
                    DayCompliance(
                        date = dateFormat.format(Date(startOfDay)),
                        taken = taken,
                        total = total,
                        rate = rate
                    )
                )
            } catch (e: Exception) {
                result.add(
                    DayCompliance(
                        date = dateFormat.format(Date(startOfDay)),
                        taken = 0,
                        total = 0,
                        rate = 0f
                    )
                )
            }
        }

        return result
    }

    /**
     * Get stats for all profiles
     */
    @RequiresApi(Build.VERSION_CODES.O)
    suspend fun getAllProfilesStats(
        userId: String,
        profiles: List<Pair<String, String>>  // (profileId, profileName)
    ): List<ProfileStats> {
        return profiles.map { (profileId, profileName) ->
            getProfileStats(userId, profileId, profileName)
        }
    }
}
