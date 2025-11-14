package com.bardino.dozi.core.utils

import android.content.Context
import android.util.Log
import com.bardino.dozi.core.data.model.*
import com.bardino.dozi.core.data.repository.BuddyRepository
import com.bardino.dozi.core.data.repository.MedicationLogRepository
import com.bardino.dozi.core.data.repository.MedicineRepository
import com.bardino.dozi.core.data.repository.NotificationRepository
import com.google.firebase.auth.FirebaseAuth
import kotlinx.coroutines.flow.first
import java.util.Calendar

/**
 * 🚨 Escalation Manager
 * Kritik ilaçların kaçırılması durumunda buddy'lere bildirim gönderir
 */
class EscalationManager(
    private val context: Context
) {
    private val medicineRepository = MedicineRepository()
    private val medicationLogRepository = MedicationLogRepository(context)
    private val buddyRepository = BuddyRepository()
    private val notificationRepository = NotificationRepository()

    companion object {
        private const val TAG = "EscalationManager"
        private const val CRITICAL_MISSED_THRESHOLD = 2  // 2+ kritik ilaç kaçırıldıysa escalate et
        private const val HOURS_TO_CHECK = 24  // Son 24 saati kontrol et
    }

    /**
     * Kritik ilaçların kaçırılıp kaçırılmadığını kontrol et
     * Eğer CRITICAL_MISSED_THRESHOLD kadar kritik ilaç kaçırıldıysa buddy'lere bildir
     */
    suspend fun checkAndEscalate() {
        try {
            val userId = FirebaseAuth.getInstance().currentUser?.uid ?: return

            // Kullanıcının tüm ilaçlarını çek
            val medicines = medicineRepository.getMedicinesFlow().first()

            // Kritik ilaçları filtrele
            val criticalMedicines = medicines.filter {
                it.criticalityLevel == MedicineCriticality.CRITICAL
            }

            if (criticalMedicines.isEmpty()) {
                Log.d(TAG, "No critical medicines found")
                return
            }

            // Son 24 saatteki missed kritik ilaçları kontrol et
            val missedCriticalCount = countMissedCriticalMedicines(criticalMedicines)

            Log.d(TAG, "Missed critical medicines count: $missedCriticalCount")

            if (missedCriticalCount >= CRITICAL_MISSED_THRESHOLD) {
                // 🚨 Escalate! Buddy'lere bildirim gönder
                notifyBuddiesAboutMissedCriticalMedicines(
                    userId,
                    missedCriticalCount,
                    criticalMedicines.map { it.name }
                )
            }
        } catch (e: Exception) {
            Log.e(TAG, "Error checking escalation", e)
        }
    }

    /**
     * Son 24 saatte kaçırılan kritik ilaç sayısını hesapla
     */
    private suspend fun countMissedCriticalMedicines(criticalMedicines: List<Medicine>): Int {
        var missedCount = 0

        // Son 24 saati hesapla
        val startTime = Calendar.getInstance().apply {
            add(Calendar.HOUR_OF_DAY, -HOURS_TO_CHECK)
        }.timeInMillis

        for (medicine in criticalMedicines) {
            // İlacın loglarını kontrol et
            // Not: Bu basit implementasyon. Gerçekte MedicationLogRepository'den query yapılmalı
            // Şimdilik placeholder
            // val logs = medicationLogRepository.getLogsForMedicine(medicine.id, startTime)
            // val missedLogs = logs.filter { it.status == MedicationStatus.MISSED }
            // missedCount += missedLogs.size

            // TODO: MedicationLogRepository'ye getLogsForMedicine metodunu ekle
        }

        return missedCount
    }

    /**
     * Buddy'lere kritik ilaç kaçırılması hakkında bildirim gönder
     */
    private suspend fun notifyBuddiesAboutMissedCriticalMedicines(
        userId: String,
        missedCount: Int,
        medicineNames: List<String>
    ) {
        try {
            // Aktif buddy'leri çek
            val buddies = buddyRepository.getBuddiesFlow().first()

            // Notification almak isteyen buddy'leri filtrele
            val notifiableBuddies = buddies.filter {
                it.buddy.notificationPreferences.onMedicationMissed &&
                it.buddy.permissions.canReceiveNotifications
            }

            if (notifiableBuddies.isEmpty()) {
                Log.d(TAG, "No buddies to notify")
                return
            }

            // Her buddy için bildirim oluştur
            for (buddyWithUser in notifiableBuddies) {
                val notification = DoziNotification(
                    userId = buddyWithUser.buddy.buddyUserId,
                    type = NotificationType.CRITICAL_MEDICATION_MISSED,
                    title = "🚨 Kritik İlaç Uyarısı",
                    body = "${buddyWithUser.buddy.nickname ?: buddyWithUser.user.name} son 24 saatte $missedCount kritik ilaç kaçırdı!",
                    data = mapOf(
                        "fromUserId" to userId,
                        "missedCount" to missedCount.toString(),
                        "medicines" to medicineNames.joinToString(", ")
                    ),
                    actionUrl = "buddy_medication_tracking/$userId",
                    priority = NotificationPriority.HIGH
                )

                notificationRepository.createNotification(notification)
                Log.d(TAG, "Escalation notification sent to buddy: ${buddyWithUser.buddy.buddyUserId}")
            }

            Log.d(TAG, "🚨 Escalation notifications sent to ${notifiableBuddies.size} buddies")
        } catch (e: Exception) {
            Log.e(TAG, "Error notifying buddies", e)
        }
    }

    /**
     * Tek bir kritik ilaç kaçırıldığında hemen buddy'lere bildir
     */
    suspend fun notifyBuddiesForSingleCriticalMedicine(medicine: Medicine) {
        if (medicine.criticalityLevel != MedicineCriticality.CRITICAL) {
            return
        }

        try {
            val userId = FirebaseAuth.getInstance().currentUser?.uid ?: return

            // Aktif buddy'leri çek
            val buddies = buddyRepository.getBuddiesFlow().first()

            // Notification almak isteyen buddy'leri filtrele
            val notifiableBuddies = buddies.filter {
                it.buddy.notificationPreferences.onMedicationMissed &&
                it.buddy.permissions.canReceiveNotifications
            }

            if (notifiableBuddies.isEmpty()) {
                return
            }

            // Her buddy için bildirim oluştur
            for (buddyWithUser in notifiableBuddies) {
                val notification = DoziNotification(
                    userId = buddyWithUser.buddy.buddyUserId,
                    type = NotificationType.MEDICATION_MISSED,
                    title = "⚠️ Kritik İlaç Kaçırıldı",
                    body = "${buddyWithUser.buddy.nickname ?: buddyWithUser.user.name} ${medicine.name} ilacını kaçırdı!",
                    data = mapOf(
                        "fromUserId" to userId,
                        "medicineId" to medicine.id,
                        "medicineName" to medicine.name,
                        "criticality" to "CRITICAL"
                    ),
                    actionUrl = "buddy_medication_tracking/$userId",
                    priority = NotificationPriority.HIGH
                )

                notificationRepository.createNotification(notification)
            }

            Log.d(TAG, "✅ Critical medicine missed notification sent to ${notifiableBuddies.size} buddies")
        } catch (e: Exception) {
            Log.e(TAG, "Error notifying buddies for single critical medicine", e)
        }
    }
}
