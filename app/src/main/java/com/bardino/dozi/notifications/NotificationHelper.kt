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
import com.bardino.dozi.core.data.model.MedicineCriticality
import com.bardino.dozi.core.data.model.User
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
    const val EXTRA_MEDICINE_ID = "EXTRA_MEDICINE_ID"
    const val EXTRA_DOSAGE = "EXTRA_DOSAGE"
    const val EXTRA_TIME = "EXTRA_TIME"
    const val EXTRA_SCHEDULED_TIME = "EXTRA_SCHEDULED_TIME"
    const val EXTRA_REQUEST_ID = "EXTRA_REQUEST_ID"
    const val EXTRA_FROM_USER_NAME = "EXTRA_FROM_USER_NAME"

    @RequiresPermission(Manifest.permission.POST_NOTIFICATIONS)
    fun showMedicationNotification(
        context: Context,
        medicineName: String,
        medicineId: String = "",
        dosage: String = "",
        time: String = getCurrentTime(),
        scheduledTime: Long = System.currentTimeMillis()
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
        val takenPending = createActionPendingIntent(context, ACTION_TAKEN, medicineName, medicineId, dosage, time, scheduledTime, 1)
        val snoozePending = createActionPendingIntent(context, ACTION_SNOOZE, medicineName, medicineId, dosage, time, scheduledTime, 2)
        val skipPending = createActionPendingIntent(context, ACTION_SKIP, medicineName, medicineId, dosage, time, scheduledTime, 3)

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
        medicineId: String,
        dosage: String,
        time: String,
        scheduledTime: Long,
        requestCode: Int
    ): PendingIntent {
        return PendingIntent.getBroadcast(
            context,
            requestCode,
            Intent(context, NotificationActionReceiver::class.java).apply {
                this.action = action
                putExtra(EXTRA_MEDICINE, medicineName)
                putExtra(EXTRA_MEDICINE_ID, medicineId)
                putExtra(EXTRA_DOSAGE, dosage)
                putExtra(EXTRA_TIME, time)
                putExtra(EXTRA_SCHEDULED_TIME, scheduledTime)
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
     * Badi request bildirimi göster (Kabul/Reddet butonları ile)
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
            .setContentTitle("🤝 Yeni Badi İsteği")
            .setContentText("$fromUserName seni buddy olarak eklemek istiyor!")
            .setStyle(
                NotificationCompat.BigTextStyle()
                    .bigText("$fromUserName seni buddy olarak eklemek istiyor!\n\nBadileriniz ilaç hatırlatmalarınızı görebilir ve sizi destekleyebilir.")
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

    /**
     * ⚠️ Düşük stok bildirimi göster (5 doz kaldı)
     */
    @RequiresPermission(Manifest.permission.POST_NOTIFICATIONS)
    fun showLowStockNotification(
        context: Context,
        medicineName: String,
        remainingStock: Int
    ) {
        createDoziChannel(context)
        val nm = NotificationManagerCompat.from(context)

        // Ana ekrana yönlendir
        val contentIntent = PendingIntent.getActivity(
            context, 0,
            Intent(context, MainActivity::class.java).apply {
                addFlags(Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP)
                putExtra("navigation_route", "medicine_list")
            },
            PendingIntent.FLAG_UPDATE_CURRENT or mutableFlag()
        )

        val notification = NotificationCompat.Builder(context, CHANNEL_ID)
            .setSmallIcon(R.drawable.ic_notification_pill)
            .setColor(Color.parseColor("#FFA726")) // Turuncu renk (uyarı)
            .setContentTitle("⚠️ Düşük Stok Uyarısı")
            .setContentText("$medicineName - $remainingStock doz kaldı!")
            .setStyle(
                NotificationCompat.BigTextStyle()
                    .bigText("📦 $medicineName ilacınızdan sadece $remainingStock doz kaldı.\n\n💊 Eczaneden temin etmeyi unutmayın!")
                    .setBigContentTitle("⚠️ Düşük Stok Uyarısı")
                    .setSummaryText("Dozi")
            )
            .setAutoCancel(true)
            .setPriority(NotificationCompat.PRIORITY_HIGH)
            .setCategory(NotificationCompat.CATEGORY_REMINDER)
            .setVisibility(NotificationCompat.VISIBILITY_PUBLIC)
            .setContentIntent(contentIntent)
            .build()

        // Her ilaç için benzersiz bildirim ID'si (medicineName hashCode)
        nm.notify(NOTIF_ID + 1000 + medicineName.hashCode(), notification)
    }

    /**
     * 🚨 Stok bitti bildirimi göster (eczane önerisi)
     */
    @RequiresPermission(Manifest.permission.POST_NOTIFICATIONS)
    fun showOutOfStockNotification(
        context: Context,
        medicineName: String
    ) {
        createDoziChannel(context)
        val nm = NotificationManagerCompat.from(context)

        // Haritalar uygulamasına yönlendir (eczane ara)
        val mapIntent = Intent(Intent.ACTION_VIEW).apply {
            data = android.net.Uri.parse("geo:0,0?q=eczane")
            addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        }

        val mapPendingIntent = PendingIntent.getActivity(
            context,
            medicineName.hashCode() + 1,
            mapIntent,
            PendingIntent.FLAG_UPDATE_CURRENT or mutableFlag()
        )

        // Ana ekrana yönlendir
        val contentIntent = PendingIntent.getActivity(
            context, 0,
            Intent(context, MainActivity::class.java).apply {
                addFlags(Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP)
                putExtra("navigation_route", "medicine_list")
            },
            PendingIntent.FLAG_UPDATE_CURRENT or mutableFlag()
        )

        val notification = NotificationCompat.Builder(context, CHANNEL_ID)
            .setSmallIcon(R.drawable.ic_notification_pill)
            .setColor(Color.parseColor("#EF5350")) // Kırmızı renk (acil)
            .setContentTitle("🚨 Stok Bitti!")
            .setContentText("$medicineName ilacınız tükendi!")
            .setStyle(
                NotificationCompat.BigTextStyle()
                    .bigText("🚨 $medicineName ilacınızın stoğu bitti!\n\n🏥 En yakın eczaneyi bulmak için dokunun.")
                    .setBigContentTitle("🚨 Stok Bitti!")
                    .setSummaryText("Dozi")
            )
            .setAutoCancel(true)
            .setPriority(NotificationCompat.PRIORITY_MAX)
            .setCategory(NotificationCompat.CATEGORY_REMINDER)
            .setVisibility(NotificationCompat.VISIBILITY_PUBLIC)
            .setContentIntent(contentIntent)
            .addAction(
                R.drawable.ic_notification_pill,
                "🏥 Eczane Bul",
                mapPendingIntent
            )
            .build()

        // Her ilaç için benzersiz bildirim ID'si
        nm.notify(NOTIF_ID + 2000 + medicineName.hashCode(), notification)
    }

    /**
     * 🔕 DND (Do Not Disturb) kontrolü
     * @return true ise bildirim gösterilebilir, false ise DND aktif
     */
    fun shouldShowNotification(
        user: User?,
        medicineCriticality: MedicineCriticality = MedicineCriticality.ROUTINE
    ): Boolean {
        // Kullanıcı yoksa veya DND kapalıysa bildirim göster
        if (user == null || !user.dndEnabled) {
            return true
        }

        // Kritik ilaçlar DND'yi bypass eder
        if (medicineCriticality == MedicineCriticality.CRITICAL) {
            return true
        }

        // Şu anki saat DND saatleri içinde mi kontrol et
        val now = Calendar.getInstance()
        val currentHour = now.get(Calendar.HOUR_OF_DAY)
        val currentMinute = now.get(Calendar.MINUTE)
        val currentTimeInMinutes = currentHour * 60 + currentMinute

        val dndStartInMinutes = user.dndStartHour * 60 + user.dndStartMinute
        val dndEndInMinutes = user.dndEndHour * 60 + user.dndEndMinute

        val isInDndPeriod = if (dndStartInMinutes <= dndEndInMinutes) {
            // Normal durum: 22:00 - 08:00
            currentTimeInMinutes >= dndStartInMinutes && currentTimeInMinutes < dndEndInMinutes
        } else {
            // Gece yarısını geçen durum: 22:00 - 02:00
            currentTimeInMinutes >= dndStartInMinutes || currentTimeInMinutes < dndEndInMinutes
        }

        // IMPORTANT ilaçlar DND'de sessiz gösterilir
        if (medicineCriticality == MedicineCriticality.IMPORTANT && isInDndPeriod) {
            // Sessiz bildirim için hala true döndür ama caller'da sessiz yapılacak
            return true
        }

        // ROUTINE ilaçlar DND'de gösterilmez
        return !isInDndPeriod
    }

    /**
     * 🔔 Adaptive timing - İlaç zamanını kullanıcı tercihine göre ayarla
     */
    fun adjustTimeWithAdaptiveTiming(
        originalTime: String,  // "08:00"
        user: User?
    ): String {
        if (user == null || !user.adaptiveTimingEnabled) {
            return originalTime
        }

        try {
            val parts = originalTime.split(":")
            val hour = parts.getOrNull(0)?.toIntOrNull() ?: return originalTime
            val minute = parts.getOrNull(1)?.toIntOrNull() ?: 0

            // Sabah ilaçları (6-11 arası) kullanıcının tercih ettiği sabah saatine kaydır
            val adjustedHour = when (hour) {
                in 6..11 -> {
                    // Kullanıcının sabah tercihi ile değiştir
                    user.preferredMorningHour
                }
                in 18..22 -> {
                    // Kullanıcının akşam tercihi ile değiştir
                    user.preferredEveningHour
                }
                else -> hour  // Diğer saatler değişmez
            }

            return String.format("%02d:%02d", adjustedHour, minute)
        } catch (e: Exception) {
            return originalTime
        }
    }

    /**
     * 🚨 Bildirim prioritesi belirle (kritiklik seviyesine göre)
     */
    fun getNotificationPriority(
        medicineCriticality: MedicineCriticality,
        isInDndPeriod: Boolean
    ): Int {
        return when {
            medicineCriticality == MedicineCriticality.CRITICAL -> NotificationCompat.PRIORITY_MAX
            medicineCriticality == MedicineCriticality.IMPORTANT && isInDndPeriod -> NotificationCompat.PRIORITY_LOW
            medicineCriticality == MedicineCriticality.IMPORTANT -> NotificationCompat.PRIORITY_HIGH
            else -> NotificationCompat.PRIORITY_DEFAULT
        }
    }
}