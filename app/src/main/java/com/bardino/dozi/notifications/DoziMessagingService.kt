package com.bardino.dozi.notifications

import android.Manifest
import android.content.pm.PackageManager
import android.util.Log
import androidx.core.content.ContextCompat
import com.bardino.dozi.core.data.repository.UserRepository
import com.google.firebase.messaging.FirebaseMessagingService
import com.google.firebase.messaging.RemoteMessage
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

/**
 * Firebase Cloud Messaging Service
 * Server'dan gelen bildirimler burada işlenir
 */
class DoziMessagingService : FirebaseMessagingService() {

    private val userRepository by lazy { UserRepository() }

    /**
     * Server'dan yeni bildirim geldiğinde çağrılır
     */
    override fun onMessageReceived(remoteMessage: RemoteMessage) {
        Log.d(TAG, "FCM Message received from: ${remoteMessage.from}")

        // Data payload var mı kontrol et
        if (remoteMessage.data.isNotEmpty()) {
            Log.d(TAG, "Message data payload: ${remoteMessage.data}")
            handleDataMessage(remoteMessage.data)
        }

        // Notification payload var mı kontrol et
        remoteMessage.notification?.let {
            Log.d(TAG, "Message Notification Body: ${it.body}")
            handleNotificationMessage(it.title, it.body)
        }
    }

    /**
     * Data mesajlarını işle (server'dan özel format)
     */
    private fun handleDataMessage(data: Map<String, String>) {
        val type = data["type"]

        when (type) {
            "medicine_reminder" -> {
                val medicineName = data["medicine_name"] ?: return
                val dosage = data["dosage"] ?: ""
                val time = data["time"] ?: ""

                // Bildirim izni varsa göster
                if (hasNotificationPermission()) {
                    NotificationHelper.showMedicationNotification(
                        context = this,
                        medicineName = medicineName,
                        dosage = dosage,
                        time = time
                    )
                }
            }
            "buddy_request" -> {
                // Buddy isteği bildirimi
                val fromUserName = data["fromUserName"] ?: "Biri"
                handleNotificationMessage(
                    "🤝 Yeni Buddy İsteği",
                    "$fromUserName seni buddy olarak eklemek istiyor!"
                )
            }
            "buddy_medication_reminder" -> {
                // Buddy'nin ilaç hatırlatması
                val buddyName = data["buddyName"] ?: "Buddy'niz"
                val medicineName = data["medicineName"] ?: "ilaç"
                val time = data["time"] ?: ""
                handleNotificationMessage(
                    "💊 Buddy İlaç Hatırlatması",
                    "$buddyName - $medicineName alma zamanı ($time)"
                )
            }
            "medication_taken" -> {
                // Buddy ilacını aldı bildirimi
                val buddyName = data["buddyName"] ?: "Buddy'niz"
                val medicineName = data["medicineName"] ?: "ilacını"
                handleNotificationMessage(
                    "✅ İlaç Alındı",
                    "$buddyName $medicineName aldı"
                )
            }
            "medication_missed" -> {
                // Buddy ilacını kaçırdı bildirimi
                val buddyName = data["buddyName"] ?: "Buddy'niz"
                val medicineName = data["medicineName"] ?: "ilacını"
                handleNotificationMessage(
                    "⚠️ İlaç Kaçırıldı",
                    "$buddyName $medicineName kaçırdı"
                )
            }
            "general_notification" -> {
                val title = data["title"] ?: "Dozi"
                val body = data["body"] ?: ""
                handleNotificationMessage(title, body)
            }
            else -> {
                Log.w(TAG, "Unknown message type: $type")
            }
        }
    }

    /**
     * Basit notification mesajlarını işle
     */
    private fun handleNotificationMessage(title: String?, body: String?) {
        if (!hasNotificationPermission()) {
            Log.w(TAG, "No notification permission")
            return
        }

        Log.d(TAG, "Showing notification: $title - $body")

        // Notification channel'ı oluştur
        NotificationHelper.createDoziChannel(this)

        // Bildirim göster
        val notificationId = System.currentTimeMillis().toInt()

        val contentIntent = PendingIntent.getActivity(
            this,
            0,
            Intent(this, com.bardino.dozi.MainActivity::class.java).apply {
                addFlags(Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP)
            },
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        val notification = androidx.core.app.NotificationCompat.Builder(this, NotificationHelper.CHANNEL_ID)
            .setSmallIcon(com.bardino.dozi.R.drawable.ic_notification_pill)
            .setContentTitle(title)
            .setContentText(body)
            .setAutoCancel(true)
            .setContentIntent(contentIntent)
            .setPriority(androidx.core.app.NotificationCompat.PRIORITY_HIGH)
            .build()

        try {
            val notificationManager = androidx.core.app.NotificationManagerCompat.from(this)
            notificationManager.notify(notificationId, notification)
            Log.d(TAG, "✅ Notification displayed with ID: $notificationId")
        } catch (e: SecurityException) {
            Log.e(TAG, "❌ Notification permission denied", e)
        }
    }

    /**
     * Yeni FCM token oluşturulduğunda çağrılır
     * Token değiştiğinde Firestore'a kaydetmeliyiz
     */
    override fun onNewToken(token: String) {
        Log.d(TAG, "New FCM token: $token")

        // Token'ı Firestore'a kaydet
        CoroutineScope(Dispatchers.IO).launch {
            try {
                userRepository.updateUserField("fcmToken", token)
                Log.d(TAG, "FCM token saved to Firestore")
            } catch (e: Exception) {
                Log.e(TAG, "Failed to save FCM token", e)
            }
        }
    }

    /**
     * Bildirim izni kontrolü
     */
    private fun hasNotificationPermission(): Boolean {
        return if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.TIRAMISU) {
            ContextCompat.checkSelfPermission(
                this,
                Manifest.permission.POST_NOTIFICATIONS
            ) == PackageManager.PERMISSION_GRANTED
        } else {
            true // Android 13 altında izin gerekmiyor
        }
    }

    companion object {
        private const val TAG = "DoziMessaging"
    }
}
