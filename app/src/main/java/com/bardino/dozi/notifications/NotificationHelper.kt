package com.bardino.dozi.notifications

import android.Manifest
import android.app.*
import android.content.Context
import android.content.Intent
import android.graphics.BitmapFactory
import android.graphics.Color
import android.os.Build
import android.widget.RemoteViews
import androidx.annotation.RequiresPermission
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import com.bardino.dozi.MainActivity
import com.bardino.dozi.R
import java.text.SimpleDateFormat
import java.util.*

object NotificationHelper {

    const val CHANNEL_ID = "dozi_med_channel"
    const val NOTIF_ID = 2025

    // Action keys
    const val ACTION_TAKEN = "ACTION_TAKEN"
    const val ACTION_SNOOZE = "ACTION_SNOOZE"
    const val ACTION_SKIP = "ACTION_SKIP"
    const val ACTION_BUDDY_ACCEPT = "ACTION_BUDDY_ACCEPT"
    const val ACTION_BUDDY_REJECT = "ACTION_BUDDY_REJECT"
    const val EXTRA_MEDICINE = "EXTRA_MEDICINE"
    const val EXTRA_TIME = "EXTRA_TIME"
    const val EXTRA_REQUEST_ID = "EXTRA_REQUEST_ID"
    const val EXTRA_FROM_USER_NAME = "EXTRA_FROM_USER_NAME"

    @RequiresPermission(Manifest.permission.POST_NOTIFICATIONS)
    fun showMedicationNotification(
        context: Context,
        medicineName: String,
        dosage: String = "",
        time: String = getCurrentTime()
    ) {
        createDoziChannel(context)
        val nm = NotificationManagerCompat.from(context)

        // MedicationActionScreen'e yönlendir (zamanı parametre olarak gönder)
        val contentIntent = PendingIntent.getActivity(
            context, 0,
            Intent(context, MainActivity::class.java).apply {
                addFlags(Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP)
                // Deep link için navigation route ekle
                putExtra("navigation_route", "medication_action/$time")
            },
            PendingIntent.FLAG_UPDATE_CURRENT or mutableFlag()
        )

        // Aksiyonlar
        val takenPending = createActionPendingIntent(context, ACTION_TAKEN, medicineName, time, 1)
        val snoozePending = createActionPendingIntent(context, ACTION_SNOOZE, medicineName, time, 2)
        val skipPending = createActionPendingIntent(context, ACTION_SKIP, medicineName, time, 3)

        // Dozi large icon
        val largeIcon = BitmapFactory.decodeResource(context.resources, R.drawable.dozi)

        // 🎨 Kısa ve öz bildirim metni
        val notification = NotificationCompat.Builder(context, CHANNEL_ID)
            .setSmallIcon(R.drawable.ic_notification_pill)
            .setColor(Color.parseColor("#26C6DA"))
            .setLargeIcon(largeIcon)
            .setContentTitle("💊 İlaç Hatırlatması")
            .setContentText("$time • Hatırlatmalarınızı görüntüleyin")
            .setStyle(
                NotificationCompat.BigTextStyle()
                    .bigText("⏰ Saat: $time\n\n📋 İlaçlarınızı almayı unutmayın. Detayları görmek için dokunun.")
                    .setBigContentTitle("💊 İlaç Hatırlatması")
                    .setSummaryText("Dozi")
            )
            .setAutoCancel(false) // Butonlarla kontrol ediyoruz
            .setPriority(NotificationCompat.PRIORITY_HIGH)
            .setCategory(NotificationCompat.CATEGORY_REMINDER)
            .setVisibility(NotificationCompat.VISIBILITY_PUBLIC)
            .setVibrate(longArrayOf(0, 300, 150, 300))
            .setLights(Color.parseColor("#26C6DA"), 1000, 1000)
            .setContentIntent(contentIntent)
            .setSound(null) // Özel ses eklemek isterseniz buraya ekleyin
            // Action buttons
            .addAction(
                R.drawable.ic_notification_pill,
                "Aldım ✓",
                takenPending
            )
            .addAction(
                R.drawable.ic_notification_pill,
                "Ertele ⏰",
                snoozePending
            )
            .addAction(
                R.drawable.ic_notification_pill,
                "Atla ✕",
                skipPending
            )
            .build()

        nm.notify(NOTIF_ID, notification)
    }

    // ✅ RemoteViews kaldırıldı - Modern BigTextStyle kullanıyoruz

    private fun createActionPendingIntent(
        context: Context,
        action: String,
        medicineName: String,
        time: String,
        requestCode: Int
    ): PendingIntent {
        return PendingIntent.getBroadcast(
            context,
            requestCode,
            Intent(context, NotificationActionReceiver::class.java).apply {
                this.action = action
                putExtra(EXTRA_MEDICINE, medicineName)
                putExtra(EXTRA_TIME, time)
            },
            PendingIntent.FLAG_UPDATE_CURRENT or mutableFlag()
        )
    }

    fun createDoziChannel(context: Context) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channel = NotificationChannel(
                CHANNEL_ID,
                "💧 Dozi Hatırlatmalar",
                NotificationManager.IMPORTANCE_HIGH
            ).apply {
                description = "Dozi tarafından ilaç hatırlatmaları ve bildirimler"
                enableLights(true)
                enableVibration(true)
                lightColor = Color.parseColor("#26C6DA")
                vibrationPattern = longArrayOf(0, 300, 150, 300)
                setShowBadge(true)
            }
            val nm = context.getSystemService(NotificationManager::class.java)
            nm.createNotificationChannel(channel)
        }
    }

    fun scheduleSnooze(context: Context, medicineName: String, minutes: Int = 10) {
        val alarmMgr = context.getSystemService(Context.ALARM_SERVICE) as AlarmManager
        val triggerAt = System.currentTimeMillis() + minutes * 60_000L

        val pi = PendingIntent.getBroadcast(
            context,
            NOTIF_ID + 100, // Farklı ID kullan
            Intent(context, NotificationActionReceiver::class.java).apply {
                action = "ACTION_SNOOZE_TRIGGER"
                putExtra(EXTRA_MEDICINE, medicineName)
            },
            PendingIntent.FLAG_UPDATE_CURRENT or mutableFlag()
        )

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            alarmMgr.setExactAndAllowWhileIdle(AlarmManager.RTC_WAKEUP, triggerAt, pi)
        } else {
            alarmMgr.setExact(AlarmManager.RTC_WAKEUP, triggerAt, pi)
        }
    }

    private fun getCurrentTime(): String {
        return SimpleDateFormat("HH:mm", Locale.getDefault()).format(Date())
    }

    private fun mutableFlag(): Int =
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) PendingIntent.FLAG_MUTABLE else 0

    /**
     * Buddy request bildirimi göster (Kabul/Reddet butonları ile)
     */
    @RequiresPermission(Manifest.permission.POST_NOTIFICATIONS)
    fun showBuddyRequestNotification(
        context: Context,
        requestId: String,
        fromUserName: String
    ) {
        createDoziChannel(context)
        val nm = NotificationManagerCompat.from(context)

        // Bildirime tıklanınca buddy_list ekranına yönlendir
        val contentIntent = PendingIntent.getActivity(
            context, 0,
            Intent(context, MainActivity::class.java).apply {
                addFlags(Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP)
                putExtra("navigation_route", "buddy_list")
            },
            PendingIntent.FLAG_UPDATE_CURRENT or mutableFlag()
        )

        // Kabul et butonu
        val acceptPending = PendingIntent.getBroadcast(
            context,
            requestId.hashCode() + 1,
            Intent(context, NotificationActionReceiver::class.java).apply {
                action = ACTION_BUDDY_ACCEPT
                putExtra(EXTRA_REQUEST_ID, requestId)
                putExtra(EXTRA_FROM_USER_NAME, fromUserName)
            },
            PendingIntent.FLAG_UPDATE_CURRENT or mutableFlag()
        )

        // Reddet butonu
        val rejectPending = PendingIntent.getBroadcast(
            context,
            requestId.hashCode() + 2,
            Intent(context, NotificationActionReceiver::class.java).apply {
                action = ACTION_BUDDY_REJECT
                putExtra(EXTRA_REQUEST_ID, requestId)
                putExtra(EXTRA_FROM_USER_NAME, fromUserName)
            },
            PendingIntent.FLAG_UPDATE_CURRENT or mutableFlag()
        )

        val notification = NotificationCompat.Builder(context, CHANNEL_ID)
            .setSmallIcon(R.drawable.ic_notification_pill)
            .setColor(Color.parseColor("#26C6DA"))
            .setContentTitle("🤝 Yeni Buddy İsteği")
            .setContentText("$fromUserName seni buddy olarak eklemek istiyor!")
            .setStyle(
                NotificationCompat.BigTextStyle()
                    .bigText("$fromUserName seni buddy olarak eklemek istiyor!\n\nBuddy'leriniz ilaç hatırlatmalarınızı görebilir ve sizi destekleyebilir.")
            )
            .setAutoCancel(true)
            .setPriority(NotificationCompat.PRIORITY_HIGH)
            .setCategory(NotificationCompat.CATEGORY_SOCIAL)
            .setVisibility(NotificationCompat.VISIBILITY_PUBLIC)
            .setContentIntent(contentIntent)
            .addAction(
                R.drawable.ic_notification_pill,
                "Kabul Et ✓",
                acceptPending
            )
            .addAction(
                R.drawable.ic_notification_pill,
                "Reddet ✕",
                rejectPending
            )
            .build()

        nm.notify(requestId.hashCode(), notification)
    }
}