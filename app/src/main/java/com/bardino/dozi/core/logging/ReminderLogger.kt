package com.bardino.dozi.core.logging

import android.content.Context
import android.os.Build
import android.util.Log
import com.bardino.dozi.core.data.model.Medicine
import com.bardino.dozi.core.data.model.ReminderEventType
import com.bardino.dozi.core.data.model.ReminderLog
import com.bardino.dozi.core.data.model.toEmoji
import com.bardino.dozi.core.data.repository.ReminderLogRepository
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.util.*

/**
 * Hatırlatma sisteminin tüm olaylarını loglayan ana sınıf
 *
 * Kullanım:
 * ```
 * ReminderLogger.logAlarmScheduled(context, medicine, "08:00", scheduledTime, requestCode)
 * ReminderLogger.logError(context, "Schedule failed", exception)
 * ```
 */
object ReminderLogger {

    private const val TAG = "ReminderLogger"
    private val repository = ReminderLogRepository()
    private val scope = CoroutineScope(Dispatchers.IO)

    // Cihaz bilgileri (bir kez hesaplanır)
    private val deviceModel = "${Build.MANUFACTURER} ${Build.MODEL}"
    private val androidVersion = "Android ${Build.VERSION.RELEASE} (API ${Build.VERSION.SDK_INT})"

    // App version - context ile alınacak
    private var appVersion = "unknown"

    private fun getAppVersion(context: Context): String {
        if (appVersion == "unknown") {
            try {
                val packageInfo = context.packageManager.getPackageInfo(context.packageName, 0)
                appVersion = packageInfo.versionName ?: "unknown"
            } catch (e: Exception) {
                appVersion = "unknown"
            }
        }
        return appVersion
    }

    // ============================================================
    // HATIRLATMA OLUŞTURMA/GÜNCELLEME
    // ============================================================

    /**
     * Yeni hatırlatma oluşturuldu
     */
    fun logReminderCreated(
        context: Context,
        medicine: Medicine,
        times: List<String>,
        metadata: Map<String, String> = emptyMap()
    ) {
        val description = "${medicine.name} için ${times.size} saat ile hatırlatma oluşturuldu: ${times.joinToString(", ")}"

        times.forEach { time ->
            log(
                context = context,
                eventType = ReminderEventType.REMINDER_CREATED,
                medicineId = medicine.id,
                medicineName = medicine.name,
                reminderTime = time,
                description = description,
                metadata = metadata + mapOf(
                    "frequency" to medicine.frequency,
                    "totalTimes" to times.size.toString()
                ),
                frequency = medicine.frequency,
                frequencyValue = medicine.frequencyValue
            )
        }

        logcat(ReminderEventType.REMINDER_CREATED, description)
    }

    /**
     * Hatırlatma güncellendi
     */
    fun logReminderUpdated(
        context: Context,
        medicine: Medicine,
        changes: String,
        metadata: Map<String, String> = emptyMap()
    ) {
        val description = "${medicine.name} güncellendi: $changes"

        log(
            eventType = ReminderEventType.REMINDER_UPDATED,
            medicineId = medicine.id,
            medicineName = medicine.name,
            description = description,
            metadata = metadata
        )

        logcat(ReminderEventType.REMINDER_UPDATED, description)
    }

    /**
     * Hatırlatma silindi
     */
    fun logReminderDeleted(
        context: Context,
        medicineId: String,
        medicineName: String,
        metadata: Map<String, String> = emptyMap()
    ) {
        val description = "$medicineName için hatırlatma silindi"

        log(
            eventType = ReminderEventType.REMINDER_DELETED,
            medicineId = medicineId,
            medicineName = medicineName,
            description = description,
            metadata = metadata
        )

        logcat(ReminderEventType.REMINDER_DELETED, description)
    }

    // ============================================================
    // ALARM İŞLEMLERİ
    // ============================================================

    /**
     * Alarm kuruldu
     */
    fun logAlarmScheduled(
        context: Context,
        medicine: Medicine,
        time: String,
        scheduledTimeMillis: Long,
        requestCode: Int
    ) {
        val dateFormat = SimpleDateFormat("dd/MM/yyyy HH:mm", Locale.getDefault())
        val scheduledTimeStr = dateFormat.format(Date(scheduledTimeMillis))
        val description = "${medicine.name} - $time için alarm kuruldu: $scheduledTimeStr"

        log(
            eventType = ReminderEventType.ALARM_SCHEDULED,
            medicineId = medicine.id,
            medicineName = medicine.name,
            reminderTime = time,
            description = description,
            scheduledTime = scheduledTimeMillis,
            requestCode = requestCode,
            metadata = mapOf(
                "scheduledTimeStr" to scheduledTimeStr,
                "frequency" to medicine.frequency
            ),
            frequency = medicine.frequency,
            frequencyValue = medicine.frequencyValue
        )

        logcat(ReminderEventType.ALARM_SCHEDULED, description)
    }

    /**
     * Alarm iptal edildi
     */
    fun logAlarmCancelled(
        context: Context,
        medicineId: String,
        medicineName: String,
        time: String,
        requestCode: Int
    ) {
        val description = "$medicineName - $time için alarm iptal edildi"

        log(
            eventType = ReminderEventType.ALARM_CANCELLED,
            medicineId = medicineId,
            medicineName = medicineName,
            reminderTime = time,
            description = description,
            requestCode = requestCode
        )

        logcat(ReminderEventType.ALARM_CANCELLED, description)
    }

    /**
     * Alarm tetiklendi
     */
    fun logAlarmTriggered(
        context: Context,
        medicineId: String,
        medicineName: String,
        time: String
    ) {
        val description = "$medicineName - $time alarmı tetiklendi"

        log(
            eventType = ReminderEventType.ALARM_TRIGGERED,
            medicineId = medicineId,
            medicineName = medicineName,
            reminderTime = time,
            description = description,
            actualTime = System.currentTimeMillis()
        )

        logcat(ReminderEventType.ALARM_TRIGGERED, description)
    }

    /**
     * Alarm yeniden planlandı (sonraki gün için)
     */
    fun logAlarmRescheduled(
        context: Context,
        medicine: Medicine,
        time: String,
        nextScheduledTime: Long,
        requestCode: Int
    ) {
        val dateFormat = SimpleDateFormat("dd/MM/yyyy HH:mm", Locale.getDefault())
        val nextTimeStr = dateFormat.format(Date(nextScheduledTime))
        val description = "${medicine.name} - $time bir sonraki alarm: $nextTimeStr"

        log(
            eventType = ReminderEventType.ALARM_RESCHEDULED,
            medicineId = medicine.id,
            medicineName = medicine.name,
            reminderTime = time,
            description = description,
            scheduledTime = nextScheduledTime,
            requestCode = requestCode,
            metadata = mapOf(
                "nextTimeStr" to nextTimeStr,
                "frequency" to medicine.frequency
            ),
            frequency = medicine.frequency,
            frequencyValue = medicine.frequencyValue
        )

        logcat(ReminderEventType.ALARM_RESCHEDULED, description)
    }

    /**
     * Alarm izni reddedildi
     */
    fun logAlarmPermissionDenied(context: Context) {
        val description = "SCHEDULE_EXACT_ALARM izni reddedildi"

        log(
            eventType = ReminderEventType.ALARM_PERMISSION_DENIED,
            description = description,
            success = false
        )

        logcat(ReminderEventType.ALARM_PERMISSION_DENIED, description)
    }

    // ============================================================
    // BİLDİRİM İŞLEMLERİ
    // ============================================================

    /**
     * Bildirim gönderildi
     */
    fun logNotificationSent(
        context: Context,
        medicineId: String,
        medicineName: String,
        time: String,
        notificationId: Int
    ) {
        val description = "$medicineName - $time bildirimi gönderildi (ID: $notificationId)"

        log(
            eventType = ReminderEventType.NOTIFICATION_SENT,
            medicineId = medicineId,
            medicineName = medicineName,
            reminderTime = time,
            description = description,
            metadata = mapOf("notificationId" to notificationId.toString())
        )

        logcat(ReminderEventType.NOTIFICATION_SENT, description)
    }

    /**
     * Bildirim tıklandı
     */
    fun logNotificationClicked(
        context: Context,
        medicineId: String,
        medicineName: String,
        time: String
    ) {
        val description = "$medicineName - $time bildirimi tıklandı"

        log(
            eventType = ReminderEventType.NOTIFICATION_CLICKED,
            medicineId = medicineId,
            medicineName = medicineName,
            reminderTime = time,
            description = description
        )

        logcat(ReminderEventType.NOTIFICATION_CLICKED, description)
    }

    /**
     * Bildirim gönderilemedi
     */
    fun logNotificationFailed(
        context: Context,
        medicineId: String,
        medicineName: String,
        time: String,
        error: String
    ) {
        val description = "$medicineName - $time bildirimi gönderilemedi: $error"

        log(
            eventType = ReminderEventType.NOTIFICATION_FAILED,
            medicineId = medicineId,
            medicineName = medicineName,
            reminderTime = time,
            description = description,
            success = false,
            errorMessage = error
        )

        logcat(ReminderEventType.NOTIFICATION_FAILED, description)
    }

    // ============================================================
    // KULLANICI AKSİYONLARI
    // ============================================================

    /**
     * İlaç alındı
     */
    fun logDoseTaken(
        context: Context,
        medicineId: String,
        medicineName: String,
        time: String,
        metadata: Map<String, String> = emptyMap()
    ) {
        val description = "$medicineName - $time alındı olarak işaretlendi"

        log(
            eventType = ReminderEventType.DOSE_TAKEN,
            medicineId = medicineId,
            medicineName = medicineName,
            reminderTime = time,
            description = description,
            actualTime = System.currentTimeMillis(),
            metadata = metadata
        )

        logcat(ReminderEventType.DOSE_TAKEN, description)
    }

    /**
     * İlaç atlandı
     */
    fun logDoseSkipped(
        context: Context,
        medicineId: String,
        medicineName: String,
        time: String,
        reason: String? = null
    ) {
        val description = "$medicineName - $time atlandı" + (reason?.let { " (Sebep: $it)" } ?: "")

        log(
            eventType = ReminderEventType.DOSE_SKIPPED,
            medicineId = medicineId,
            medicineName = medicineName,
            reminderTime = time,
            description = description,
            actualTime = System.currentTimeMillis(),
            metadata = reason?.let { mapOf("reason" to it) } ?: emptyMap()
        )

        logcat(ReminderEventType.DOSE_SKIPPED, description)
    }

    /**
     * İlaç ertelendi
     */
    fun logDoseSnoozed(
        context: Context,
        medicineId: String,
        medicineName: String,
        time: String,
        snoozeDuration: Int // dakika
    ) {
        val description = "$medicineName - $time $snoozeDuration dakika ertelendi"

        log(
            eventType = ReminderEventType.DOSE_SNOOZED,
            medicineId = medicineId,
            medicineName = medicineName,
            reminderTime = time,
            description = description,
            actualTime = System.currentTimeMillis(),
            metadata = mapOf("snoozeDuration" to snoozeDuration.toString())
        )

        logcat(ReminderEventType.DOSE_SNOOZED, description)
    }

    /**
     * İlaç kaçırıldı (otomatik - 1 saat cevap verilmedi)
     */
    fun logDoseMissed(
        context: Context,
        medicineId: String,
        medicineName: String,
        time: String,
        reason: String
    ) {
        val description = "$medicineName - $time kaçırıldı: $reason"

        log(
            eventType = ReminderEventType.DOSE_MISSED,
            medicineId = medicineId,
            medicineName = medicineName,
            reminderTime = time,
            description = description,
            actualTime = System.currentTimeMillis(),
            success = false,
            metadata = mapOf("reason" to reason)
        )

        logcat(ReminderEventType.DOSE_MISSED, description)
    }

    // ============================================================
    // SNOOZE İŞLEMLERİ
    // ============================================================

    /**
     * Erteleme süresi doldu, yeni bildirim gösterildi
     */
    fun logSnoozeTriggered(
        context: Context,
        medicineId: String,
        medicineName: String,
        time: String
    ) {
        val description = "$medicineName - $time erteleme süresi doldu, yeni bildirim gösteriliyor"

        log(
            eventType = ReminderEventType.SNOOZE_TRIGGERED,
            medicineId = medicineId,
            medicineName = medicineName,
            reminderTime = time,
            description = description,
            actualTime = System.currentTimeMillis()
        )

        logcat(ReminderEventType.SNOOZE_TRIGGERED, description)
    }

    // ============================================================
    // ESCALATION İŞLEMLERİ
    // ============================================================

    /**
     * Escalation planlandı
     */
    fun logEscalationScheduled(
        context: Context,
        medicineId: String,
        medicineName: String,
        time: String,
        level: Int, // 1, 2, 3
        scheduledTimeMillis: Long
    ) {
        val dateFormat = SimpleDateFormat("dd/MM/yyyy HH:mm", Locale.getDefault())
        val scheduledTimeStr = dateFormat.format(Date(scheduledTimeMillis))
        val description = "$medicineName - $time için Escalation $level planlandı: $scheduledTimeStr"

        log(
            eventType = ReminderEventType.ESCALATION_SCHEDULED,
            medicineId = medicineId,
            medicineName = medicineName,
            reminderTime = time,
            description = description,
            scheduledTime = scheduledTimeMillis,
            metadata = mapOf(
                "level" to level.toString(),
                "scheduledTimeStr" to scheduledTimeStr
            )
        )

        logcat(ReminderEventType.ESCALATION_SCHEDULED, description)
    }

    /**
     * Escalation iptal edildi
     */
    fun logEscalationCancelled(
        context: Context,
        medicineId: String,
        medicineName: String,
        time: String,
        level: Int? = null // null ise tümü iptal edildi
    ) {
        val levelStr = level?.let { "Level $it" } ?: "Tüm seviyeleri"
        val description = "$medicineName - $time için Escalation $levelStr iptal edildi"

        log(
            eventType = ReminderEventType.ESCALATION_CANCELLED,
            medicineId = medicineId,
            medicineName = medicineName,
            reminderTime = time,
            description = description,
            metadata = level?.let { mapOf("level" to it.toString()) } ?: emptyMap()
        )

        logcat(ReminderEventType.ESCALATION_CANCELLED, description)
    }

    /**
     * Escalation Level 1 tetiklendi (10 dakika)
     */
    fun logEscalation1Triggered(
        context: Context,
        medicineId: String,
        medicineName: String,
        time: String
    ) {
        val description = "$medicineName - $time Escalation Level 1 (10 dk) tetiklendi"

        log(
            eventType = ReminderEventType.ESCALATION_1_TRIGGERED,
            medicineId = medicineId,
            medicineName = medicineName,
            reminderTime = time,
            description = description,
            actualTime = System.currentTimeMillis()
        )

        logcat(ReminderEventType.ESCALATION_1_TRIGGERED, description)
    }

    /**
     * Escalation Level 2 tetiklendi (30 dakika)
     */
    fun logEscalation2Triggered(
        context: Context,
        medicineId: String,
        medicineName: String,
        time: String
    ) {
        val description = "$medicineName - $time Escalation Level 2 (30 dk) tetiklendi"

        log(
            eventType = ReminderEventType.ESCALATION_2_TRIGGERED,
            medicineId = medicineId,
            medicineName = medicineName,
            reminderTime = time,
            description = description,
            actualTime = System.currentTimeMillis()
        )

        logcat(ReminderEventType.ESCALATION_2_TRIGGERED, description)
    }

    /**
     * Escalation Level 3 tetiklendi (60 dakika - MISSED)
     */
    fun logEscalation3Triggered(
        context: Context,
        medicineId: String,
        medicineName: String,
        time: String
    ) {
        val description = "$medicineName - $time Escalation Level 3 (60 dk) tetiklendi - İlaç MISSED olarak işaretleniyor"

        log(
            eventType = ReminderEventType.ESCALATION_3_TRIGGERED,
            medicineId = medicineId,
            medicineName = medicineName,
            reminderTime = time,
            description = description,
            actualTime = System.currentTimeMillis()
        )

        logcat(ReminderEventType.ESCALATION_3_TRIGGERED, description)
    }

    // ============================================================
    // BADİ (BUDDY) İŞLEMLERİ
    // ============================================================

    /**
     * Badi isteği kabul edildi
     */
    fun logBuddyRequestAccepted(
        context: Context,
        requestId: String,
        fromUserName: String
    ) {
        val description = "$fromUserName'in badi isteği kabul edildi"

        log(
            eventType = ReminderEventType.BUDDY_REQUEST_ACCEPTED,
            description = description,
            metadata = mapOf(
                "requestId" to requestId,
                "fromUserName" to fromUserName
            )
        )

        logcat(ReminderEventType.BUDDY_REQUEST_ACCEPTED, description)
    }

    /**
     * Badi isteği reddedildi
     */
    fun logBuddyRequestRejected(
        context: Context,
        requestId: String,
        fromUserName: String
    ) {
        val description = "$fromUserName'in badi isteği reddedildi"

        log(
            eventType = ReminderEventType.BUDDY_REQUEST_REJECTED,
            description = description,
            metadata = mapOf(
                "requestId" to requestId,
                "fromUserName" to fromUserName
            )
        )

        logcat(ReminderEventType.BUDDY_REQUEST_REJECTED, description)
    }

    /**
     * Badi'ye kritik ilaç kaçırma bildirimi gönderildi
     */
    fun logBuddyNotificationSent(
        context: Context,
        medicineId: String,
        medicineName: String,
        buddyCount: Int
    ) {
        val description = "$medicineName için $buddyCount badi'ye bildirim gönderildi"

        log(
            eventType = ReminderEventType.BUDDY_NOTIFICATION_SENT,
            medicineId = medicineId,
            medicineName = medicineName,
            description = description,
            metadata = mapOf("buddyCount" to buddyCount.toString())
        )

        logcat(ReminderEventType.BUDDY_NOTIFICATION_SENT, description)
    }

    // ============================================================
    // SİSTEM OLAYLARI
    // ============================================================

    /**
     * Cihaz yeniden başlatıldı
     */
    fun logBootCompleted(context: Context) {
        val description = "Cihaz yeniden başlatıldı, alarmlar yeniden planlanacak"

        log(
            eventType = ReminderEventType.BOOT_COMPLETED,
            description = description
        )

        logcat(ReminderEventType.BOOT_COMPLETED, description)
    }

    /**
     * Tüm alarmlar yeniden planlandı
     */
    fun logAllAlarmsRescheduled(
        context: Context,
        medicineCount: Int,
        totalAlarms: Int
    ) {
        val description = "$medicineCount ilaç için toplam $totalAlarms alarm yeniden planlandı"

        log(
            eventType = ReminderEventType.ALL_ALARMS_RESCHEDULED,
            description = description,
            metadata = mapOf(
                "medicineCount" to medicineCount.toString(),
                "totalAlarms" to totalAlarms.toString()
            )
        )

        logcat(ReminderEventType.ALL_ALARMS_RESCHEDULED, description)
    }

    // ============================================================
    // HATA LOGLARI
    // ============================================================

    /**
     * Genel hata
     */
    fun logError(
        context: Context,
        message: String,
        exception: Exception? = null,
        medicineId: String = "",
        medicineName: String = "",
        time: String = ""
    ) {
        log(
            eventType = ReminderEventType.ERROR,
            medicineId = medicineId,
            medicineName = medicineName,
            reminderTime = time,
            description = message,
            success = false,
            errorMessage = exception?.message,
            errorStackTrace = exception?.stackTraceToString()
        )

        Log.e(TAG, "🔥 $message", exception)
    }

    /**
     * Alarm kurma hatası
     */
    fun logScheduleError(
        context: Context,
        medicine: Medicine,
        time: String,
        error: String,
        exception: Exception? = null
    ) {
        val description = "${medicine.name} - $time için alarm kurulamadı: $error"

        log(
            eventType = ReminderEventType.SCHEDULE_ERROR,
            medicineId = medicine.id,
            medicineName = medicine.name,
            reminderTime = time,
            description = description,
            success = false,
            errorMessage = error,
            errorStackTrace = exception?.stackTraceToString()
        )

        Log.e(TAG, "⏰❌ $description", exception)
    }

    // ============================================================
    // DEBUG
    // ============================================================

    /**
     * Debug log
     */
    fun logDebug(
        context: Context,
        message: String,
        metadata: Map<String, String> = emptyMap()
    ) {
        log(
            eventType = ReminderEventType.DEBUG,
            description = message,
            metadata = metadata
        )

        Log.d(TAG, "🐛 $message")
    }

    // ============================================================
    // PRIVATE METHODS
    // ============================================================

    private fun log(
        context: Context? = null,
        eventType: ReminderEventType,
        medicineId: String = "",
        medicineName: String = "",
        reminderTime: String = "",
        description: String,
        success: Boolean = true,
        errorMessage: String? = null,
        errorStackTrace: String? = null,
        scheduledTime: Long? = null,
        actualTime: Long? = null,
        requestCode: Int? = null,
        metadata: Map<String, String> = emptyMap(),
        frequency: String? = null,
        frequencyValue: Int? = null
    ) {
        val reminderLog = ReminderLog(
            medicineId = medicineId,
            medicineName = medicineName,
            reminderTime = reminderTime,
            eventType = eventType.name,
            eventDescription = description,
            success = success,
            errorMessage = errorMessage,
            errorStackTrace = errorStackTrace,
            timestamp = System.currentTimeMillis(),
            scheduledTime = scheduledTime,
            actualTime = actualTime,
            deviceModel = deviceModel,
            androidVersion = androidVersion,
            appVersion = context?.let { getAppVersion(it) } ?: appVersion,
            metadata = metadata,
            requestCode = requestCode,
            frequency = frequency,
            frequencyValue = frequencyValue
        )

        // Firebase'e asenkron kaydet
        scope.launch {
            repository.addLog(reminderLog)
        }
    }

    private fun logcat(eventType: ReminderEventType, message: String) {
        Log.d(TAG, "${eventType.toEmoji()} $message")
    }
}
