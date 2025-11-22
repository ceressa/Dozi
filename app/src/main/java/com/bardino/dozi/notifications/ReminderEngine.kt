package com.bardino.dozi.notifications

import android.app.AlarmManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.os.Build
import android.util.Log
import com.bardino.dozi.core.data.model.Medicine
import com.bardino.dozi.core.data.model.MedicineCriticality
import dagger.hilt.android.qualifiers.ApplicationContext
import java.time.LocalDate
import java.util.Calendar
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Merkezi hatırlatma motoru
 * Tüm alarm yönetimi, zamanlama hesaplaması ve escalation mantığı burada
 */
@Singleton
class ReminderEngine @Inject constructor(
    @ApplicationContext private val context: Context
) {
    companion object {
        private const val TAG = "ReminderEngine"

        const val ACTION_REMINDER_TRIGGER = "com.bardino.dozi.ACTION_REMINDER_TRIGGER"
        const val ACTION_SNOOZE_TRIGGER = "com.bardino.dozi.ACTION_SNOOZE_TRIGGER"
        const val ACTION_ESCALATION_1 = "com.bardino.dozi.ACTION_ESCALATION_1"
        const val ACTION_ESCALATION_2 = "com.bardino.dozi.ACTION_ESCALATION_2"
        const val ACTION_ESCALATION_3 = "com.bardino.dozi.ACTION_ESCALATION_3"

        const val EXTRA_MEDICINE_ID = "medicine_id"
        const val EXTRA_MEDICINE_NAME = "medicine_name"
        const val EXTRA_TIME = "time"
        const val EXTRA_DOSAGE = "dosage"
        const val EXTRA_CRITICALITY = "criticality"

        // Escalation delays in milliseconds
        const val ESCALATION_1_DELAY = 10 * 60 * 1000L  // 10 dakika
        const val ESCALATION_2_DELAY = 30 * 60 * 1000L  // 30 dakika
        const val ESCALATION_3_DELAY = 60 * 60 * 1000L  // 60 dakika
    }

    private val alarmManager: AlarmManager by lazy {
        context.getSystemService(Context.ALARM_SERVICE) as AlarmManager
    }

    // ==================== ALARM YÖNETİMİ ====================

    /**
     * Tek bir ilaç için tüm hatırlatmaları planla
     */
    fun scheduleReminders(medicine: Medicine, isRescheduling: Boolean = false) {
        if (!medicine.reminderEnabled) {
            Log.d(TAG, "Hatırlatma devre dışı: ${medicine.name}")
            return
        }

        if (medicine.times.isEmpty()) {
            Log.d(TAG, "Hatırlatma saati yok: ${medicine.name}")
            return
        }

        if (medicine.id.isEmpty()) {
            Log.e(TAG, "Medicine ID boş: ${medicine.name}")
            return
        }

        // Bitiş tarihi kontrolü
        if (medicine.endDate != null && medicine.endDate < System.currentTimeMillis()) {
            Log.d(TAG, "Bitiş tarihi geçmiş: ${medicine.name}")
            return
        }

        // İzin kontrolü
        if (!PermissionHandler.hasExactAlarmPermission(context)) {
            Log.w(TAG, "SCHEDULE_EXACT_ALARM izni yok: ${medicine.name}")
            return
        }

        medicine.times.forEach { time ->
            scheduleReminderForTime(medicine, time, isRescheduling)
        }

        Log.d(TAG, "${medicine.name} için ${medicine.times.size} hatırlatma planlandı")
    }

    /**
     * Belirli bir saat için hatırlatma planla
     */
    private fun scheduleReminderForTime(
        medicine: Medicine,
        time: String,
        isRescheduling: Boolean
    ) {
        try {
            val (hour, minute) = time.split(":").map { it.toInt() }

            val pendingIntent = createReminderIntent(medicine, time)
            val triggerTime = calculateNextAlarmTime(medicine, time, hour, minute, isRescheduling)

            // Bitiş tarihi kontrolü
            if (medicine.endDate != null && triggerTime > medicine.endDate) {
                Log.d(TAG, "${medicine.name} - $time için alarm bitiş tarihinden sonra")
                return
            }

            // Alarm kur
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
                alarmManager.setExactAndAllowWhileIdle(
                    AlarmManager.RTC_WAKEUP,
                    triggerTime,
                    pendingIntent
                )
            } else {
                alarmManager.setExact(
                    AlarmManager.RTC_WAKEUP,
                    triggerTime,
                    pendingIntent
                )
            }

            val dateFormat = java.text.SimpleDateFormat("dd/MM/yyyy HH:mm", java.util.Locale.getDefault())
            Log.d(TAG, "${medicine.name} - $time için alarm: ${dateFormat.format(triggerTime)}")

        } catch (e: Exception) {
            Log.e(TAG, "Alarm kurulurken hata: ${medicine.name} - $time", e)
        }
    }

    /**
     * Bir ilaç için tüm alarmları iptal et
     */
    fun cancelReminders(medicineId: String, times: List<String>) {
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

            // Escalation'ları da iptal et
            cancelAllEscalations(medicineId, time)

            Log.d(TAG, "Alarm iptal edildi: $medicineId - $time")
        }
    }

    // ==================== ESCALATION YÖNETİMİ ====================

    /**
     * Escalation alarmlarını planla
     */
    fun scheduleEscalations(medicine: Medicine, time: String) {
        val baseTime = System.currentTimeMillis()

        // Level 1: 10 dakika
        scheduleEscalation(medicine, time, 1, baseTime + ESCALATION_1_DELAY)

        // Level 2: 30 dakika
        scheduleEscalation(medicine, time, 2, baseTime + ESCALATION_2_DELAY)

        // Level 3: 60 dakika (kritik)
        scheduleEscalation(medicine, time, 3, baseTime + ESCALATION_3_DELAY)

        Log.d(TAG, "${medicine.name} - $time için escalation'lar planlandı")
    }

    private fun scheduleEscalation(
        medicine: Medicine,
        time: String,
        level: Int,
        triggerTime: Long
    ) {
        val action = when (level) {
            1 -> ACTION_ESCALATION_1
            2 -> ACTION_ESCALATION_2
            3 -> ACTION_ESCALATION_3
            else -> return
        }

        val intent = Intent(context, NotificationActionReceiver::class.java).apply {
            this.action = action
            putExtra(EXTRA_MEDICINE_ID, medicine.id)
            putExtra(EXTRA_MEDICINE_NAME, medicine.name)
            putExtra(EXTRA_TIME, time)
            putExtra(EXTRA_DOSAGE, medicine.dosage)
            putExtra(EXTRA_CRITICALITY, medicine.criticalityLevel.name)
        }

        val requestCode = getEscalationRequestCode(medicine.id, time, level)

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

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            alarmManager.setExactAndAllowWhileIdle(
                AlarmManager.RTC_WAKEUP,
                triggerTime,
                pendingIntent
            )
        } else {
            alarmManager.setExact(
                AlarmManager.RTC_WAKEUP,
                triggerTime,
                pendingIntent
            )
        }
    }

    /**
     * Tüm escalation'ları iptal et
     */
    fun cancelAllEscalations(medicineId: String, time: String) {
        for (level in 1..3) {
            val action = when (level) {
                1 -> ACTION_ESCALATION_1
                2 -> ACTION_ESCALATION_2
                3 -> ACTION_ESCALATION_3
                else -> continue
            }

            val intent = Intent(context, NotificationActionReceiver::class.java).apply {
                this.action = action
            }

            val requestCode = getEscalationRequestCode(medicineId, time, level)

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
        }

        Log.d(TAG, "Escalation'lar iptal edildi: $medicineId - $time")
    }

    // ==================== SNOOZE YÖNETİMİ ====================

    /**
     * Snooze alarmı planla
     */
    fun scheduleSnooze(medicine: Medicine, time: String, delayMinutes: Int) {
        val triggerTime = System.currentTimeMillis() + (delayMinutes * 60 * 1000L)

        val intent = Intent(context, NotificationActionReceiver::class.java).apply {
            action = ACTION_SNOOZE_TRIGGER
            putExtra(EXTRA_MEDICINE_ID, medicine.id)
            putExtra(EXTRA_MEDICINE_NAME, medicine.name)
            putExtra(EXTRA_TIME, time)
            putExtra(EXTRA_DOSAGE, medicine.dosage)
            putExtra(EXTRA_CRITICALITY, medicine.criticalityLevel.name)
        }

        val requestCode = getSnoozeRequestCode(medicine.id, time)

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

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            alarmManager.setExactAndAllowWhileIdle(
                AlarmManager.RTC_WAKEUP,
                triggerTime,
                pendingIntent
            )
        } else {
            alarmManager.setExact(
                AlarmManager.RTC_WAKEUP,
                triggerTime,
                pendingIntent
            )
        }

        Log.d(TAG, "${medicine.name} - $time için snooze: $delayMinutes dakika")
    }

    // ==================== FREKANS HESAPLAMA ====================

    /**
     * Bir sonraki alarm zamanını hesapla
     */
    private fun calculateNextAlarmTime(
        medicine: Medicine,
        time: String,
        hour: Int,
        minute: Int,
        isRescheduling: Boolean
    ): Long {
        val calendar = Calendar.getInstance().apply {
            set(Calendar.HOUR_OF_DAY, hour)
            set(Calendar.MINUTE, minute)
            set(Calendar.SECOND, 0)
            set(Calendar.MILLISECOND, 0)
        }

        if (isRescheduling) {
            // Alarm tetiklendi, bir sonraki zamanı hesapla
            val daysToAdd = when (medicine.frequency) {
                "Her gün" -> 1
                "Gün aşırı" -> 2
                "Haftada bir" -> 7
                "15 günde bir" -> 15
                "Ayda bir" -> 30
                "Her X günde bir" -> medicine.frequencyValue
                "İstediğim tarihlerde" -> 1
                else -> 1
            }
            calendar.add(Calendar.DAY_OF_MONTH, daysToAdd)
        } else {
            // İlk kurulum
            if (calendar.timeInMillis <= System.currentTimeMillis()) {
                val daysToAdd = when (medicine.frequency) {
                    "Her gün" -> 1
                    "Gün aşırı" -> calculateDaysUntilNextAlternateDay(medicine.startDate)
                    "Haftada bir" -> calculateDaysUntilNextWeeklyAlarm(medicine.startDate)
                    "15 günde bir" -> calculateDaysUntilNextAlarm(medicine.startDate, 15)
                    "Ayda bir" -> calculateDaysUntilNextAlarm(medicine.startDate, 30)
                    "Her X günde bir" -> calculateDaysUntilNextAlarm(medicine.startDate, medicine.frequencyValue)
                    "İstediğim tarihlerde" -> 1
                    else -> 1
                }
                calendar.add(Calendar.DAY_OF_MONTH, daysToAdd)
            }
        }

        return calendar.timeInMillis
    }

    /**
     * Bu ilacın bu tarihte gösterilmesi gerekiyor mu?
     */
    fun shouldShowOnDate(medicine: Medicine, date: LocalDate): Boolean {
        val medicineStartDate = java.time.Instant.ofEpochMilli(medicine.startDate)
            .atZone(java.time.ZoneId.systemDefault())
            .toLocalDate()

        // Başlangıç tarihinden önceyse gösterme
        if (date.isBefore(medicineStartDate)) return false

        // Bitiş tarihi varsa ve geçmişse gösterme
        medicine.endDate?.let { endDate ->
            val medicineEndDate = java.time.Instant.ofEpochMilli(endDate)
                .atZone(java.time.ZoneId.systemDefault())
                .toLocalDate()
            if (date.isAfter(medicineEndDate)) return false
        }

        val daysSinceStart = java.time.temporal.ChronoUnit.DAYS.between(medicineStartDate, date).toInt()

        return when (medicine.frequency) {
            "Her gün" -> true
            "Gün aşırı" -> daysSinceStart % 2 == 0
            "Haftada bir" -> {
                val startDayOfWeek = medicineStartDate.dayOfWeek
                date.dayOfWeek == startDayOfWeek
            }
            "15 günde bir" -> daysSinceStart % 15 == 0
            "Ayda bir" -> daysSinceStart % 30 == 0
            "Her X günde bir" -> {
                if (medicine.frequencyValue > 0) {
                    daysSinceStart % medicine.frequencyValue == 0
                } else true
            }
            "İstediğim tarihlerde" -> {
                val dateStr = date.toString() // yyyy-MM-dd format
                medicine.selectedDates.contains(dateStr)
            }
            else -> true
        }
    }

    // ==================== DND & CRITICALITY ====================

    /**
     * DND bypass gerekiyor mu?
     */
    fun shouldBypassDND(medicine: Medicine): Boolean {
        return medicine.criticalityLevel == MedicineCriticality.CRITICAL
    }

    /**
     * Hangi notification channel kullanılacak?
     */
    fun getNotificationChannel(medicine: Medicine): String {
        return when (medicine.criticalityLevel) {
            MedicineCriticality.CRITICAL -> NotificationHelper.CHANNEL_ID_IMPORTANT
            else -> NotificationHelper.CHANNEL_ID
        }
    }

    /**
     * Notification rengi
     */
    fun getNotificationColor(medicine: Medicine): Int {
        return when (medicine.criticalityLevel) {
            MedicineCriticality.CRITICAL -> android.graphics.Color.parseColor("#D32F2F")
            MedicineCriticality.IMPORTANT -> android.graphics.Color.parseColor("#FFA000")
            MedicineCriticality.ROUTINE -> android.graphics.Color.parseColor("#26C6DA")
        }
    }

    /**
     * Vibration pattern
     */
    fun getVibrationPattern(medicine: Medicine): LongArray {
        return when (medicine.criticalityLevel) {
            MedicineCriticality.CRITICAL -> longArrayOf(0, 700, 300, 700, 300, 700)
            MedicineCriticality.IMPORTANT -> longArrayOf(0, 500, 200, 500)
            MedicineCriticality.ROUTINE -> longArrayOf(0, 300, 150, 300)
        }
    }

    // ==================== INTENT FACTORY ====================

    /**
     * Reminder için PendingIntent oluştur
     */
    private fun createReminderIntent(medicine: Medicine, time: String): PendingIntent {
        val intent = Intent(context, NotificationActionReceiver::class.java).apply {
            action = ACTION_REMINDER_TRIGGER
            putExtra(EXTRA_MEDICINE_ID, medicine.id)
            putExtra(EXTRA_MEDICINE_NAME, medicine.name)
            putExtra(EXTRA_TIME, time)
            putExtra(EXTRA_DOSAGE, medicine.dosage)
            putExtra(EXTRA_CRITICALITY, medicine.criticalityLevel.name)
        }

        val requestCode = getRequestCode(medicine.id, time)

        return PendingIntent.getBroadcast(
            context,
            requestCode,
            intent,
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
                PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
            } else {
                PendingIntent.FLAG_UPDATE_CURRENT
            }
        )
    }

    // ==================== HELPER METHODS ====================

    private fun calculateDaysUntilNextAlternateDay(startDate: Long): Int {
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

        val daysSinceStart = ((today.timeInMillis - start.timeInMillis) / (24 * 60 * 60 * 1000)).toInt()

        return if (daysSinceStart % 2 == 0) 2 else 1
    }

    private fun calculateDaysUntilNextWeeklyAlarm(startDate: Long): Int {
        val startCalendar = Calendar.getInstance().apply {
            timeInMillis = startDate
        }
        val startDayOfWeek = startCalendar.get(Calendar.DAY_OF_WEEK)

        val today = Calendar.getInstance()
        val todayDayOfWeek = today.get(Calendar.DAY_OF_WEEK)

        var daysUntilNext = (startDayOfWeek - todayDayOfWeek + 7) % 7
        if (daysUntilNext == 0) daysUntilNext = 7

        return daysUntilNext
    }

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

        val daysSinceStart = ((today.timeInMillis - start.timeInMillis) / (24 * 60 * 60 * 1000)).toInt()

        val remainder = daysSinceStart % intervalDays
        return if (remainder == 0) intervalDays else intervalDays - remainder
    }

    private fun getRequestCode(medicineId: String, time: String): Int {
        return "$medicineId-$time".hashCode()
    }

    private fun getEscalationRequestCode(medicineId: String, time: String, level: Int): Int {
        return "$medicineId-$time-escalation-$level".hashCode()
    }

    private fun getSnoozeRequestCode(medicineId: String, time: String): Int {
        return "$medicineId-$time-snooze".hashCode()
    }
}
