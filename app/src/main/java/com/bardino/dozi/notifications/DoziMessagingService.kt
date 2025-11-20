package com.bardino.dozi.notifications

import android.Manifest
import android.app.PendingIntent
import android.content.Intent
import android.content.pm.PackageManager
import android.util.Log
import androidx.annotation.RequiresPermission
import androidx.core.content.ContextCompat
import com.bardino.dozi.MainActivity
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
    @RequiresPermission(Manifest.permission.POST_NOTIFICATIONS)
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
                // Badi isteği bildirimi - action butonları ile
                val requestId = data["requestId"] ?: return
                val fromUserName = data["fromUserName"] ?: "Biri"

                if (hasNotificationPermission()) {
                    NotificationHelper.showBadiRequestNotification(
                        context = this,
                        requestId = requestId,
                        fromUserName = fromUserName
                    )
                }
            }
            "buddy_medication_reminder" -> {
                // Badinin ilaç hatırlatması
                val badiName = data["buddyName"] ?: "Badiniz"
                val medicineName = data["medicineName"] ?: "ilaç"
                val time = data["time"] ?: ""
                handleNotificationMessage(
                    title = "💊 Badi İlaç Hatırlatması",
                    body = "$badiName - $medicineName alma zamanı ($time)",
                    type = "buddy_medication_reminder"
                )
            }
            "medication_taken" -> {
                // Badi ilacını aldı bildirimi
                val badiName = data["buddyName"] ?: "Badiniz"
                val medicineName = data["medicineName"] ?: "ilacını"
                handleNotificationMessage(
                    title = "✅ İlaç Alındı",
                    body = "$badiName $medicineName aldı",
                    type = "medication_taken"
                )
            }
            "medication_missed" -> {
                // Badi ilacını kaçırdı bildirimi
                val badiName = data["buddyName"] ?: "Badiniz"
                val medicineName = data["medicineName"] ?: "ilacını"
                handleNotificationMessage(
                    title = "⚠️ İlaç Kaçırıldı",
                    body = "$badiName $medicineName kaçırdı",
                    type = "medication_missed"
                )
            }
            "general_notification" -> {
                val title = data["title"] ?: "Dozi"
                val body = data["body"] ?: ""
                handleNotificationMessage(
                    title = title,
                    body = body,
                    type = "general_notification"
                )
            }
            else -> {
                Log.w(TAG, "Unknown message type: $type")
            }
        }
    }

    /**
     * Basit notification mesajlarını işle
     */
    private fun handleNotificationMessage(title: String?, body: String?, type: String = "general") {
        if (!hasNotificationPermission()) {
            Log.w(TAG, "No notification permission")
            return
        }

        Log.d(TAG, "Showing notification: $title - $body (type: $type)")

        // Notification channel'ı oluştur
        NotificationHelper.createDoziChannel(this)

        // Bildirim göster
        val notificationId = System.currentTimeMillis().toInt()

        // Type'a göre navigation route belirle
        val navigationRoute = when (type) {
            "buddy_request" -> "badi_list"
            "medication_taken", "medication_missed", "buddy_medication_reminder" -> "badi_list"
            else -> null
        }

        val contentIntent = PendingIntent.getActivity(
            this,
            0,
            Intent(this, MainActivity::class.java).apply {
                addFlags(Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP)
                // Deep link için navigation route ekle
                navigationRoute?.let { putExtra("navigation_route", it) }
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
            Log.d(TAG, "✅ Notification displayed with ID: $notificationId (route: $navigationRoute)")
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
