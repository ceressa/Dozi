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
    const val EXTRA_MEDICINE = "EXTRA_MEDICINE"
    const val EXTRA_TIME = "EXTRA_TIME"

    @RequiresPermission(Manifest.permission.POST_NOTIFICATIONS)
    fun showMedicationNotification(context: Context, medicineName: String, time: String = getCurrentTime()) {
        createDoziChannel(context)
        val nm = NotificationManagerCompat.from(context)

        // MainActivity'e dönüş
        val contentIntent = PendingIntent.getActivity(
            context, 0,
            Intent(context, MainActivity::class.java).apply {
                addFlags(Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP)
            },
            PendingIntent.FLAG_UPDATE_CURRENT or mutableFlag()
        )

        // Aksiyonlar
        val takenPending = createActionPendingIntent(context, ACTION_TAKEN, medicineName, time, 1)
        val snoozePending = createActionPendingIntent(context, ACTION_SNOOZE, medicineName, time, 2)
        val skipPending = createActionPendingIntent(context, ACTION_SKIP, medicineName, time, 3)

        // İkon
        val largeIcon = BitmapFactory.decodeResource(context.resources, R.drawable.dozi)

        // 🎨 Özel bildirim düzenleri
        val collapsed = createCollapsedView(context, medicineName)
        val expanded = createExpandedView(context, medicineName, time, takenPending, snoozePending, skipPending)

        // Bildirim oluştur
        val notification = NotificationCompat.Builder(context, CHANNEL_ID)
            .setSmallIcon(R.drawable.ic_launcher_foreground)
            .setColor(Color.parseColor("#26C6DA"))
            .setCustomContentView(collapsed)
            .setCustomBigContentView(expanded)
            .setStyle(NotificationCompat.DecoratedCustomViewStyle())
            .setLargeIcon(largeIcon)
            .setAutoCancel(false) // Butonlarla kontrol ediyoruz
            .setPriority(NotificationCompat.PRIORITY_HIGH)
            .setCategory(NotificationCompat.CATEGORY_REMINDER)
            .setVibrate(longArrayOf(0, 300, 150, 300))
            .setLights(Color.parseColor("#26C6DA"), 1000, 1000)
            .setContentIntent(contentIntent)
            .setSound(null) // Özel ses eklemek isterseniz buraya ekleyin
            .build()

        nm.notify(NOTIF_ID, notification)
    }

    private fun createCollapsedView(context: Context, medicineName: String): RemoteViews {
        return RemoteViews(context.packageName, R.layout.notification_dozi_collapsed).apply {
            setTextViewText(R.id.dozi_title, "💧 Dozi Hatırlatıyor")
            setTextViewText(R.id.dozi_message, "$medicineName ilacını alma zamanı!")
        }
    }

    private fun createExpandedView(
        context: Context,
        medicineName: String,
        time: String,
        takenPending: PendingIntent,
        snoozePending: PendingIntent,
        skipPending: PendingIntent
    ): RemoteViews {
        return RemoteViews(context.packageName, R.layout.notification_dozi_expanded).apply {
            setTextViewText(R.id.dozi_title_big, "💧 Dozi Hatırlatıyor")
            setTextViewText(R.id.dozi_time, "Şimdi")
            setTextViewText(R.id.dozi_message_big, "İlacın zamanı geldi 💊")
            setTextViewText(R.id.dozi_medicine_name, medicineName)
            setTextViewText(R.id.dozi_details, "Saat: $time")

            // Aksiyon butonları
            setOnClickPendingIntent(R.id.btn_taken, takenPending)
            setOnClickPendingIntent(R.id.btn_snooze, snoozePending)
            setOnClickPendingIntent(R.id.btn_skip, skipPending)
        }
    }

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
}