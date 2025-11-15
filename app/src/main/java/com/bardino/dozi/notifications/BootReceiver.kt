package com.bardino.dozi.notifications

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.util.Log
import com.bardino.dozi.notifications.NotificationHelper

/**
 * Cihaz yeniden başladığında alarmları yeniden planlamak için
 */
class BootReceiver : BroadcastReceiver() {

    override fun onReceive(context: Context, intent: Intent) {
        if (intent.action == Intent.ACTION_BOOT_COMPLETED ||
            intent.action == "android.intent.action.QUICKBOOT_POWERON"
        ) {
            Log.d("BootReceiver", "🔄 Cihaz yeniden başladı - Alarmlar yeniden planlanıyor")

            // Notification channel'ı oluştur
            NotificationHelper.createDoziChannel(context)

            // ✅ Tüm ilaçların alarmlarını yeniden planla
            ReminderScheduler.rescheduleAllReminders(context)

            Log.d("BootReceiver", "✅ Boot receiver tamamlandı, alarmlar başarıyla yeniden planlandı")
        }
    }
}