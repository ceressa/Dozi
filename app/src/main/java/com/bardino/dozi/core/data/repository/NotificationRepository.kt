package com.bardino.dozi.core.data.repository

import android.Manifest
import android.content.Context
import android.content.pm.PackageManager
import androidx.core.content.ContextCompat
import com.bardino.dozi.core.data.model.*
import com.bardino.dozi.notifications.NotificationHelper
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FieldValue
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.Query
import com.google.firebase.functions.FirebaseFunctions
import com.google.firebase.messaging.FirebaseMessaging
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow
import kotlinx.coroutines.tasks.await

/**
 * Bildirim sistemini yöneten repository
 */
class NotificationRepository(
    private val auth: FirebaseAuth = FirebaseAuth.getInstance(),
    private val db: FirebaseFirestore = FirebaseFirestore.getInstance(),
    private val functions: FirebaseFunctions = FirebaseFunctions.getInstance(),
    private val messaging: FirebaseMessaging = FirebaseMessaging.getInstance()
) {

    private val currentUserId: String?
        get() = auth.currentUser?.uid

    // ==================== FCM Token İşlemleri ====================

    /**
     * FCM token'ı al ve Firestore'a kaydet
     */
    suspend fun refreshFCMToken(): Result<String> {
        val userId = currentUserId ?: return Result.failure(Exception("User not logged in"))

        return try {
            val token = messaging.token.await()

            db.collection("users")
                .document(userId)
                .update("fcmToken", token)
                .await()

            Result.success(token)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    // ==================== Notification CRUD ====================

    /**
     * Bildirim oluştur
     */
    suspend fun createNotification(notification: DoziNotification): Result<String> {
        return try {
            val docRef = db.collection("notifications").add(notification).await()
            Result.success(docRef.id)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    /**
     * Kullanıcının bildirimlerini real-time dinle
     */
    fun getNotificationsFlow(): Flow<List<DoziNotification>> = callbackFlow {
        val userId = currentUserId ?: run {
            close()
            return@callbackFlow
        }

        val listener = db.collection("notifications")
            .whereEqualTo("userId", userId)
            .orderBy("createdAt", Query.Direction.DESCENDING)
            .limit(50)
            .addSnapshotListener { snapshot, error ->
                if (error != null) {
                    close(error)
                    return@addSnapshotListener
                }

                val notifications = snapshot?.documents?.mapNotNull { doc ->
                    doc.toObject(DoziNotification::class.java)?.copy(id = doc.id)
                } ?: emptyList()

                trySend(notifications)
            }

        awaitClose { listener.remove() }
    }

    /**
     * Okunmamış bildirim sayısını al
     */
    fun getUnreadCountFlow(): Flow<Int> = callbackFlow {
        val userId = currentUserId ?: run {
            close()
            return@callbackFlow
        }

        val listener = db.collection("notifications")
            .whereEqualTo("userId", userId)
            .whereEqualTo("isRead", false)
            .addSnapshotListener { snapshot, error ->
                if (error != null) {
                    close(error)
                    return@addSnapshotListener
                }

                trySend(snapshot?.size() ?: 0)
            }

        awaitClose { listener.remove() }
    }

    /**
     * Bildirimi okundu olarak işaretle
     */
    suspend fun markAsRead(notificationId: String): Result<Unit> {
        return try {
            db.collection("notifications")
                .document(notificationId)
                .update(
                    mapOf(
                        "isRead" to true,
                        "readAt" to FieldValue.serverTimestamp()
                    )
                )
                .await()
            Result.success(Unit)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    /**
     * Tüm bildirimleri okundu olarak işaretle
     */
    suspend fun markAllAsRead(): Result<Unit> {
        val userId = currentUserId ?: return Result.failure(Exception("User not logged in"))

        return try {
            val snapshot = db.collection("notifications")
                .whereEqualTo("userId", userId)
                .whereEqualTo("isRead", false)
                .get()
                .await()

            val batch = db.batch()
            snapshot.documents.forEach { doc ->
                batch.update(
                    doc.reference,
                    mapOf(
                        "isRead" to true,
                        "readAt" to FieldValue.serverTimestamp()
                    )
                )
            }
            batch.commit().await()

            Result.success(Unit)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    /**
     * Bildirimi sil
     */
    suspend fun deleteNotification(notificationId: String): Result<Unit> {
        return try {
            db.collection("notifications")
                .document(notificationId)
                .delete()
                .await()
            Result.success(Unit)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    // ==================== Push Notification Gönderme ====================

    /**
     * İlaç hatırlatması için buddy'lere bildirim gönder
     */
    suspend fun sendMedicationReminderToBuddies(
        medicineId: String,
        medicineName: String,
        dosage: String,
        time: String
    ): Result<Int> {
        return try {
            val data = hashMapOf(
                "reminderId" to medicineId,
                "medicineName" to medicineName,
                "dosage" to dosage,
                "time" to time
            )

            val result = functions
                .getHttpsCallable("sendMedicationReminderToBuddies")
                .call(data)
                .await()

            val sentCount = (result.data as? Map<*, *>)?.get("sentCount") as? Int ?: 0
            Result.success(sentCount)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    /**
     * Local notification göster (Android)
     */
    suspend fun showLocalNotification(
        context: Context,
        title: String,
        body: String,
        type: NotificationType = NotificationType.GENERAL
    ): Result<Unit> {
        return try {
            // Notification permission kontrolü
            if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.TIRAMISU) {
                if (ContextCompat.checkSelfPermission(
                        context,
                        Manifest.permission.POST_NOTIFICATIONS
                    ) != PackageManager.PERMISSION_GRANTED
                ) {
                    return Result.failure(Exception("Notification permission not granted"))
                }
            }

            // Bildirim göster
            when (type) {
                NotificationType.MEDICATION_REMINDER -> {
                    // Özel ilaç hatırlatma bildirimi
                    NotificationHelper.showMedicationNotification(
                        context = context,
                        medicineName = title,
                        dosage = body
                    )
                }
                else -> {
                    // Genel bildirim (NotificationHelper'a eklenebilir)
                    NotificationHelper.showMedicationNotification(
                        context = context,
                        medicineName = title,
                        dosage = body
                    )
                }
            }

            Result.success(Unit)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    // ==================== Bildirim İstatistikleri ====================

    /**
     * Bildirim istatistiklerini al
     */
    suspend fun getNotificationStats(): NotificationStats {
        val userId = currentUserId ?: return NotificationStats()

        return try {
            val snapshot = db.collection("notifications")
                .whereEqualTo("userId", userId)
                .get()
                .await()

            val notifications = snapshot.documents.mapNotNull {
                it.toObject(DoziNotification::class.java)
            }

            NotificationStats(
                totalCount = notifications.size,
                unreadCount = notifications.count { !it.isRead },
                buddyRequestCount = notifications.count { it.type == NotificationType.BUDDY_REQUEST },
                medicationReminderCount = notifications.count { it.type == NotificationType.MEDICATION_REMINDER }
            )
        } catch (e: Exception) {
            NotificationStats()
        }
    }
}

/**
 * Bildirim istatistikleri
 */
data class NotificationStats(
    val totalCount: Int = 0,
    val unreadCount: Int = 0,
    val buddyRequestCount: Int = 0,
    val medicationReminderCount: Int = 0
)
