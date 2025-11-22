package com.bardino.dozi.notifications

import android.app.AlarmManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.os.Build
import android.util.Log
import com.bardino.dozi.core.data.model.Medicine
import com.bardino.dozi.core.data.repository.MedicineRepository
import com.bardino.dozi.core.logging.ReminderLogger
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
         *
         * @param isRescheduling true ise alarm tetiklendikten sonra bir sonraki alarmı planlar (frequency'ye göre),
         *                       false ise ilk kurulum
         */
        fun scheduleReminders(context: Context, medicine: Medicine, isRescheduling: Boolean = false) {
            if (!medicine.reminderEnabled) {
                Log.d(TAG, "Hatırlatma devre dışı: ${medicine.name}")
                return
            }

            if (medicine.times.isEmpty()) {
                Log.d(TAG, "Hatırlatma saati yok: ${medicine.name}")
                return
            }

            // ✅ Medicine ID kontrolü
            if (medicine.id.isEmpty()) {
                Log.e(TAG, "❌ Medicine ID boş! ${medicine.name} için alarmlar kurulamıyor.")
                return
            }

            // 📅 Bitiş tarihi kontrolü
            if (medicine.endDate != null && medicine.endDate < System.currentTimeMillis()) {
                Log.d(TAG, "⏱️ Bitiş tarihi geçmiş: ${medicine.name}. Hatırlatma planlanmıyor.")
                return
            }

            // ✅ Exact alarm izni kontrolü (Android 12+)
            if (!PermissionHandler.hasExactAlarmPermission(context)) {
                Log.w(TAG, "⚠️ SCHEDULE_EXACT_ALARM izni yok! ${medicine.name} için alarmlar kurulamıyor.")
                Log.w(TAG, "⚠️ Kullanıcının Settings > Apps > Dozi > Alarms & reminders'dan izni vermesi gerekiyor.")
                ReminderLogger.logAlarmPermissionDenied(context)
                return
            }

            val alarmManager = context.getSystemService(Context.ALARM_SERVICE) as AlarmManager

            // Her saat için bir alarm kur
            medicine.times.forEach { time ->
                scheduleReminderForTime(context, alarmManager, medicine, time, isRescheduling)
            }

            Log.d(TAG, "✅ ${medicine.name} için ${medicine.times.size} hatırlatma planlandı")
        }

        /**
         * Belirli bir saat için hatırlatma planla
         *
         * @param isRescheduling true ise alarm tetiklendikten sonra bir sonraki alarmı planlar (frequency'ye göre),
         *                       false ise ilk kurulum (bugünden sonraki ilk uygun zamanı planlar)
         */
        private fun scheduleReminderForTime(
            context: Context,
            alarmManager: AlarmManager,
            medicine: Medicine,
            time: String,
            isRescheduling: Boolean = false
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

                // Tetiklenme zamanını hesapla
                val calendar = Calendar.getInstance().apply {
                    set(Calendar.HOUR_OF_DAY, hour)
                    set(Calendar.MINUTE, minute)
                    set(Calendar.SECOND, 0)
                    set(Calendar.MILLISECOND, 0)

                    if (isRescheduling) {
                        // Alarm tetiklendi, bir sonraki zamanı hesapla (frequency'ye göre)
                        val daysToAdd = when (medicine.frequency) {
                            "Her gün" -> 1
                            "Gün aşırı" -> 2
                            "Haftada bir" -> 7
                            "15 günde bir" -> 15
                            "Ayda bir" -> 30
                            "Her X günde bir" -> medicine.frequencyValue
                            "İstediğim tarihlerde" -> {
                                // İstediğim tarihlerde için bir sonraki tarihi bul
                                // Şimdilik 1 gün ekle (bu daha sonra düzgün handle edilecek)
                                1
                            }
                            else -> 1
                        }
                        add(Calendar.DAY_OF_MONTH, daysToAdd)
                    } else {
                        // İlk kurulum: Eğer bu saat bugün geçmişse, frequency'ye göre bir sonraki uygun zamanı bul
                        if (timeInMillis <= System.currentTimeMillis()) {
                            val daysToAdd = when (medicine.frequency) {
                                "Her gün" -> 1
                                "Gün aşırı" -> {
                                    // Başlangıç tarihinden itibaren gün aşırı mantığını uygula
                                    // Bugünden 1 veya 2 gün sonra olabilir (startDate'e göre)
                                    calculateDaysUntilNextAlternateDay(medicine.startDate, medicine.frequency)
                                }
                                "Haftada bir" -> calculateDaysUntilNextWeeklyAlarm(medicine.startDate)
                                "15 günde bir" -> calculateDaysUntilNextAlarm(medicine.startDate, 15)
                                "Ayda bir" -> calculateDaysUntilNextAlarm(medicine.startDate, 30)
                                "Her X günde bir" -> calculateDaysUntilNextAlarm(medicine.startDate, medicine.frequencyValue)
                                "İstediğim tarihlerde" -> 1 // Özel tarihler için ayrı handle edilecek
                                else -> 1
                            }
                            add(Calendar.DAY_OF_MONTH, daysToAdd)
                        }
                    }
                }

                // 📅 Bitiş tarihi kontrolü: Alarm zamanı endDate'den sonraysa kurma
                if (medicine.endDate != null && calendar.timeInMillis > medicine.endDate) {
                    Log.d(TAG, "⏱️ ${medicine.name} - $time için alarm zamanı bitiş tarihinden sonra (${calendar.time}). Alarm kurulmadı.")
                    return
                }

                // Alarm kur (her zaman tek seferlik)
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
                    // Android 6.0+: Doze mode'u bypass etmek için setExactAndAllowWhileIdle kullan
                    alarmManager.setExactAndAllowWhileIdle(
                        AlarmManager.RTC_WAKEUP,
                        calendar.timeInMillis,
                        pendingIntent
                    )
                } else {
                    // Android 5.x: setExact kullan
                    alarmManager.setExact(
                        AlarmManager.RTC_WAKEUP,
                        calendar.timeInMillis,
                        pendingIntent
                    )
                }

                val dateFormat = java.text.SimpleDateFormat("dd/MM/yyyy HH:mm", java.util.Locale.getDefault())
                Log.d(TAG, "⏰ ${medicine.name} - $time için alarm kuruldu: ${dateFormat.format(calendar.time)} (requestCode: $requestCode)")

                // 📝 Log kaydı
                if (isRescheduling) {
                    ReminderLogger.logAlarmRescheduled(context, medicine, time, calendar.timeInMillis, requestCode)
                } else {
                    ReminderLogger.logAlarmScheduled(context, medicine, time, calendar.timeInMillis, requestCode)
                }

            } catch (e: Exception) {
                Log.e(TAG, "❌ Alarm kurulurken hata: ${medicine.name} - $time", e)
                ReminderLogger.logScheduleError(context, medicine, time, e.message ?: "Bilinmeyen hata", e)
            }
        }

        /**
         * Gün aşırı için bir sonraki uygun günü hesapla
         *
         * Mantık: Başlangıç günü = gün 0 (ilaç al), gün 1 (alma), gün 2 (al), gün 3 (alma), ...
         * Yani çift günlerde ilaç alınır, tek günlerde alınmaz
         */
        private fun calculateDaysUntilNextAlternateDay(startDate: Long, frequency: String): Int {
            val today = Calendar.getInstance().apply {
                set(Calendar.HOUR_OF_DAY, 0)
                set(Calendar.MINUTE, 0)
                set(Calendar.SECOND, 0)
                set(Calendar.MILLISECOND, 0)
            }

            val start = Calendar.getInstance().apply {
                timeInMillis = startDate
                set(Calendar.HOUR_OF_DAY, 0)
                set(Calendar.MINUTE, 0)
                set(Calendar.SECOND, 0)
                set(Calendar.MILLISECOND, 0)
            }

            // Başlangıçtan bugüne kadar kaç gün geçti
            val daysSinceStart = ((today.timeInMillis - start.timeInMillis) / (24 * 60 * 60 * 1000)).toInt()

            // Gün aşırı mantığı: Başlangıç = gün 0, sonra gün 2, gün 4, gün 6, ...
            // Eğer bugün çift günse (ilaç günü) -> saat geçmiş olduğu için bir sonraki ilaç günü 2 gün sonra
            // Eğer bugün tek günse (ilaç yok) -> yarın ilaç günü
            return if (daysSinceStart % 2 == 0) {
                2 // Bugün ilaç günü (saat geçmiş), bir sonraki ilaç günü 2 gün sonra
            } else {
                1 // Bugün ilaç yok günü, yarın ilaç günü
            }
        }

        /**
         * Haftada bir için bir sonraki uygun günü hesapla
         */
        private fun calculateDaysUntilNextWeeklyAlarm(startDate: Long): Int {
            // Başlangıç gününü tespit et
            val startCalendar = Calendar.getInstance().apply {
                timeInMillis = startDate
            }
            val startDayOfWeek = startCalendar.get(Calendar.DAY_OF_WEEK)

            val today = Calendar.getInstance()
            val todayDayOfWeek = today.get(Calendar.DAY_OF_WEEK)

            // Bugünden başlangıç gününe kadar kaç gün var
            var daysUntilNext = (startDayOfWeek - todayDayOfWeek + 7) % 7
            if (daysUntilNext == 0) daysUntilNext = 7 // Bugün o gün ise, gelecek hafta

            return daysUntilNext
        }

        /**
         * X günde bir için bir sonraki uygun günü hesapla
         */
        private fun calculateDaysUntilNextAlarm(startDate: Long, intervalDays: Int): Int {
            val today = Calendar.getInstance().apply {
                set(Calendar.HOUR_OF_DAY, 0)
                set(Calendar.MINUTE, 0)
                set(Calendar.SECOND, 0)
                set(Calendar.MILLISECOND, 0)
            }

            val start = Calendar.getInstance().apply {
                timeInMillis = startDate
                set(Calendar.HOUR_OF_DAY, 0)
                set(Calendar.MINUTE, 0)
                set(Calendar.SECOND, 0)
                set(Calendar.MILLISECOND, 0)
            }

            // Başlangıçtan bugüne kadar kaç gün geçti
            val daysSinceStart = ((today.timeInMillis - start.timeInMillis) / (24 * 60 * 60 * 1000)).toInt()

            // Kaç gün sonra bir sonraki alarm günü
            val remainder = daysSinceStart % intervalDays
            return if (remainder == 0) {
                intervalDays // Bugün alarm günü ise, gelecek interval günü
            } else {
                intervalDays - remainder // Kalan günler
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

                // 📝 Log kaydı
                ReminderLogger.logAlarmCancelled(context, medicineId, "Unknown", time, requestCode)
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

                    var totalAlarms = 0
                    allMedicines.forEach { medicine ->
                        scheduleReminders(context, medicine)
                        totalAlarms += medicine.times.size
                    }

                    Log.d(TAG, "✅ Tüm hatırlatmalar başarıyla yeniden planlandı")

                    // 📝 Log kaydı
                    ReminderLogger.logAllAlarmsRescheduled(context, allMedicines.size, totalAlarms)
                } catch (e: Exception) {
                    Log.e(TAG, "❌ Hatırlatmalar yeniden planlanırken hata", e)
                    ReminderLogger.logError(context, "Tüm alarmlar yeniden planlanırken hata", e)
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
