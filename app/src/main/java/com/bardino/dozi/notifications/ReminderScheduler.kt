package com.bardino.dozi.notifications

import android.app.AlarmManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.os.Build
import android.util.Log
import com.bardino.dozi.core.data.model.Medicine
import com.bardino.dozi.core.data.repository.MedicineRepository
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import java.util.Calendar

/**
 * İlaç hatırlatma alarmlarını yöneten sınıf
 * AlarmManager kullanarak zamanlanmış bildirimleri planlar
 */
class ReminderScheduler {

    companion object {
        private const val TAG = "ReminderScheduler"
        const val ACTION_REMINDER_TRIGGER = "com.bardino.dozi.ACTION_REMINDER_TRIGGER"
        const val EXTRA_MEDICINE_ID = "medicine_id"
        const val EXTRA_MEDICINE_NAME = "medicine_name"
        const val EXTRA_TIME = "time"

        /**
         * Tek bir ilaç için tüm hatırlatmaları planla
         */
        fun scheduleReminders(context: Context, medicine: Medicine) {
            if (!medicine.reminderEnabled) {
                Log.d(TAG, "Hatırlatma devre dışı: ${medicine.name}")
                return
            }

            if (medicine.times.isEmpty()) {
                Log.d(TAG, "Hatırlatma saati yok: ${medicine.name}")
                return
            }

            val alarmManager = context.getSystemService(Context.ALARM_SERVICE) as AlarmManager

            // Her saat için bir alarm kur
            medicine.times.forEach { time ->
                scheduleReminderForTime(context, alarmManager, medicine, time)
            }

            Log.d(TAG, "✅ ${medicine.name} için ${medicine.times.size} hatırlatma planlandı")
        }

        /**
         * Belirli bir saat için hatırlatma planla
         */
        private fun scheduleReminderForTime(
            context: Context,
            alarmManager: AlarmManager,
            medicine: Medicine,
            time: String
        ) {
            try {
                val (hour, minute) = time.split(":").map { it.toInt() }

                // Intent oluştur
                val intent = Intent(context, NotificationActionReceiver::class.java).apply {
                    action = ACTION_REMINDER_TRIGGER
                    putExtra(EXTRA_MEDICINE_ID, medicine.id)
                    putExtra(EXTRA_MEDICINE_NAME, medicine.name)
                    putExtra(EXTRA_TIME, time)
                }

                // Unique request code: medicineId + time kombinasyonu
                val requestCode = getRequestCode(medicine.id, time)

                val pendingIntent = PendingIntent.getBroadcast(
                    context,
                    requestCode,
                    intent,
                    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
                        PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
                    } else {
                        PendingIntent.FLAG_UPDATE_CURRENT
                    }
                )

                // İlk tetiklenme zamanını hesapla
                val calendar = Calendar.getInstance().apply {
                    set(Calendar.HOUR_OF_DAY, hour)
                    set(Calendar.MINUTE, minute)
                    set(Calendar.SECOND, 0)
                    set(Calendar.MILLISECOND, 0)

                    // Eğer bu saat bugün geçmişse, yarına ayarla
                    if (timeInMillis <= System.currentTimeMillis()) {
                        add(Calendar.DAY_OF_MONTH, 1)
                    }
                }

                // Tekrarlama aralığını hesapla (frequency'ye göre)
                val intervalMillis = when (medicine.frequency) {
                    "Her gün" -> AlarmManager.INTERVAL_DAY
                    "Gün aşırı" -> AlarmManager.INTERVAL_DAY * 2
                    "Haftada bir" -> AlarmManager.INTERVAL_DAY * 7
                    "15 günde bir" -> AlarmManager.INTERVAL_DAY * 15
                    "Ayda bir" -> AlarmManager.INTERVAL_DAY * 30
                    "Her X günde bir" -> AlarmManager.INTERVAL_DAY * medicine.frequencyValue
                    else -> AlarmManager.INTERVAL_DAY // Default: Her gün
                }

                // Alarm kur
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
                    // Android 6.0+: Doze mode'u bypass etmek için setExactAndAllowWhileIdle kullan
                    // Ancak bu tekrarlanan alarmlar için çalışmaz, bu yüzden tek seferlik alarm kur
                    alarmManager.setExactAndAllowWhileIdle(
                        AlarmManager.RTC_WAKEUP,
                        calendar.timeInMillis,
                        pendingIntent
                    )

                    // Sonraki alarmı planlamak için bir iş kur (WorkManager kullanılabilir)
                    // Şimdilik basit yaklaşım: Her tetiklenmede bir sonrakini planla
                } else {
                    // Android 5.x: setRepeating kullan
                    alarmManager.setRepeating(
                        AlarmManager.RTC_WAKEUP,
                        calendar.timeInMillis,
                        intervalMillis,
                        pendingIntent
                    )
                }

                Log.d(TAG, "⏰ ${medicine.name} - $time için alarm kuruldu (requestCode: $requestCode)")

            } catch (e: Exception) {
                Log.e(TAG, "❌ Alarm kurulurken hata: ${medicine.name} - $time", e)
            }
        }

        /**
         * Bir ilaç için tüm alarmları iptal et
         */
        fun cancelReminders(context: Context, medicineId: String, times: List<String>) {
            val alarmManager = context.getSystemService(Context.ALARM_SERVICE) as AlarmManager

            times.forEach { time ->
                val intent = Intent(context, NotificationActionReceiver::class.java).apply {
                    action = ACTION_REMINDER_TRIGGER
                }

                val requestCode = getRequestCode(medicineId, time)

                val pendingIntent = PendingIntent.getBroadcast(
                    context,
                    requestCode,
                    intent,
                    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
                        PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
                    } else {
                        PendingIntent.FLAG_UPDATE_CURRENT
                    }
                )

                alarmManager.cancel(pendingIntent)
                pendingIntent.cancel()

                Log.d(TAG, "🚫 Alarm iptal edildi: $medicineId - $time (requestCode: $requestCode)")
            }
        }

        /**
         * Tüm ilaçlar için alarmları yeniden planla
         * BootReceiver'dan çağrılır
         */
        fun rescheduleAllReminders(context: Context) {
            Log.d(TAG, "📅 Tüm hatırlatmalar yeniden planlanıyor...")

            val medicineRepository = MedicineRepository()

            CoroutineScope(Dispatchers.IO).launch {
                try {
                    val allMedicines = medicineRepository.getAllMedicines()
                    Log.d(TAG, "📦 ${allMedicines.size} ilaç bulundu")

                    allMedicines.forEach { medicine ->
                        scheduleReminders(context, medicine)
                    }

                    Log.d(TAG, "✅ Tüm hatırlatmalar başarıyla yeniden planlandı")
                } catch (e: Exception) {
                    Log.e(TAG, "❌ Hatırlatmalar yeniden planlanırken hata", e)
                }
            }
        }

        /**
         * Medicine + time için unique request code oluştur
         */
        private fun getRequestCode(medicineId: String, time: String): Int {
            return "$medicineId-$time".hashCode()
        }
    }
}
