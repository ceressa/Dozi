package com.bardino.dozi.core.data.model

import com.google.firebase.firestore.DocumentId

/**
 * Hatırlatma sisteminin tüm olaylarını kaydeden log modeli
 * Firebase Firestore'da users/{userId}/reminderLogs koleksiyonunda saklanır
 */
data class ReminderLog(
    @DocumentId
    val id: String = "",

    // Temel bilgiler
    val userId: String = "",
    val medicineId: String = "",
    val medicineName: String = "",
    val reminderTime: String = "",          // "08:00" formatında

    // Event bilgisi
    val eventType: String = "",             // ReminderEventType enum değeri
    val eventDescription: String = "",      // Detaylı açıklama

    // Durum
    val success: Boolean = true,
    val errorMessage: String? = null,
    val errorStackTrace: String? = null,

    // Zamanlar
    val timestamp: Long = System.currentTimeMillis(),
    val scheduledTime: Long? = null,        // Planlanan alarm zamanı
    val actualTime: Long? = null,           // Gerçekleşen zaman

    // Cihaz bilgileri
    val deviceModel: String = "",
    val androidVersion: String = "",
    val appVersion: String = "",

    // Ek metadata
    val metadata: Map<String, String> = emptyMap(),

    // Request code (alarm için)
    val requestCode: Int? = null,

    // Frequency bilgisi
    val frequency: String? = null,
    val frequencyValue: Int? = null
) {
    val eventTypeEnum: ReminderEventType
        get() = ReminderEventType.from(eventType)
}

/**
 * Hatırlatma sistemi event tipleri
 */
enum class ReminderEventType {
    // Hatırlatma oluşturma/güncelleme
    REMINDER_CREATED,           // Yeni hatırlatma oluşturuldu
    REMINDER_UPDATED,           // Hatırlatma güncellendi
    REMINDER_DELETED,           // Hatırlatma silindi

    // Alarm işlemleri
    ALARM_SCHEDULED,            // Alarm kuruldu
    ALARM_CANCELLED,            // Alarm iptal edildi
    ALARM_TRIGGERED,            // Alarm tetiklendi
    ALARM_RESCHEDULED,          // Alarm yeniden kuruldu (sonraki gün için)
    ALARM_PERMISSION_DENIED,    // Alarm izni reddedildi

    // Bildirim işlemleri
    NOTIFICATION_SENT,          // Bildirim gönderildi
    NOTIFICATION_CLICKED,       // Bildirim tıklandı
    NOTIFICATION_DISMISSED,     // Bildirim kapatıldı
    NOTIFICATION_FAILED,        // Bildirim gönderilemedi

    // Kullanıcı aksiyonları
    DOSE_TAKEN,                 // İlaç alındı
    DOSE_SKIPPED,               // İlaç atlandı
    DOSE_SNOOZED,               // İlaç ertelendi
    DOSE_MISSED,                // İlaç kaçırıldı (otomatik)

    // Snooze işlemleri
    SNOOZE_TRIGGERED,           // Erteleme süresi doldu

    // Escalation işlemleri
    ESCALATION_SCHEDULED,       // Escalation planlandı
    ESCALATION_CANCELLED,       // Escalation iptal edildi
    ESCALATION_1_TRIGGERED,     // 10 dk escalation tetiklendi
    ESCALATION_2_TRIGGERED,     // 30 dk escalation tetiklendi
    ESCALATION_3_TRIGGERED,     // 60 dk escalation tetiklendi

    // Badi (Buddy) işlemleri
    BUDDY_REQUEST_ACCEPTED,     // Badi isteği kabul edildi
    BUDDY_REQUEST_REJECTED,     // Badi isteği reddedildi
    BUDDY_NOTIFICATION_SENT,    // Badi'ye kritik ilaç bildirimi gönderildi

    // Sistem olayları
    BOOT_COMPLETED,             // Cihaz yeniden başlatıldı
    ALL_ALARMS_RESCHEDULED,     // Tüm alarmlar yeniden planlandı
    APP_OPENED,                 // Uygulama açıldı

    // Hata durumları
    ERROR,                      // Genel hata
    SCHEDULE_ERROR,             // Alarm kurma hatası
    NOTIFICATION_ERROR,         // Bildirim hatası
    FIREBASE_ERROR,             // Firebase hatası

    // Debug
    DEBUG;                      // Debug amaçlı log

    companion object {
        fun from(value: String?): ReminderEventType {
            return entries.firstOrNull { it.name == value } ?: DEBUG
        }
    }
}

/**
 * Event tipi için emoji
 */
fun ReminderEventType.toEmoji(): String = when (this) {
    ReminderEventType.REMINDER_CREATED -> "📝"
    ReminderEventType.REMINDER_UPDATED -> "✏️"
    ReminderEventType.REMINDER_DELETED -> "🗑️"
    ReminderEventType.ALARM_SCHEDULED -> "⏰"
    ReminderEventType.ALARM_CANCELLED -> "🚫"
    ReminderEventType.ALARM_TRIGGERED -> "🔔"
    ReminderEventType.ALARM_RESCHEDULED -> "🔄"
    ReminderEventType.ALARM_PERMISSION_DENIED -> "⚠️"
    ReminderEventType.NOTIFICATION_SENT -> "📤"
    ReminderEventType.NOTIFICATION_CLICKED -> "👆"
    ReminderEventType.NOTIFICATION_DISMISSED -> "❌"
    ReminderEventType.NOTIFICATION_FAILED -> "💔"
    ReminderEventType.DOSE_TAKEN -> "✅"
    ReminderEventType.DOSE_SKIPPED -> "⏭️"
    ReminderEventType.DOSE_SNOOZED -> "😴"
    ReminderEventType.DOSE_MISSED -> "❌"
    ReminderEventType.SNOOZE_TRIGGERED -> "⏰"
    ReminderEventType.ESCALATION_SCHEDULED -> "📈"
    ReminderEventType.ESCALATION_CANCELLED -> "📉"
    ReminderEventType.ESCALATION_1_TRIGGERED -> "🔔1️⃣"
    ReminderEventType.ESCALATION_2_TRIGGERED -> "🚨2️⃣"
    ReminderEventType.ESCALATION_3_TRIGGERED -> "🔴3️⃣"
    ReminderEventType.BUDDY_REQUEST_ACCEPTED -> "✅👥"
    ReminderEventType.BUDDY_REQUEST_REJECTED -> "❌👥"
    ReminderEventType.BUDDY_NOTIFICATION_SENT -> "📤👥"
    ReminderEventType.BOOT_COMPLETED -> "🔌"
    ReminderEventType.ALL_ALARMS_RESCHEDULED -> "📅"
    ReminderEventType.APP_OPENED -> "📱"
    ReminderEventType.ERROR -> "🔥"
    ReminderEventType.SCHEDULE_ERROR -> "⏰❌"
    ReminderEventType.NOTIFICATION_ERROR -> "📤❌"
    ReminderEventType.FIREBASE_ERROR -> "☁️❌"
    ReminderEventType.DEBUG -> "🐛"
}

/**
 * Event tipi için Türkçe isim
 */
fun ReminderEventType.toTurkish(): String = when (this) {
    ReminderEventType.REMINDER_CREATED -> "Hatırlatma Oluşturuldu"
    ReminderEventType.REMINDER_UPDATED -> "Hatırlatma Güncellendi"
    ReminderEventType.REMINDER_DELETED -> "Hatırlatma Silindi"
    ReminderEventType.ALARM_SCHEDULED -> "Alarm Kuruldu"
    ReminderEventType.ALARM_CANCELLED -> "Alarm İptal Edildi"
    ReminderEventType.ALARM_TRIGGERED -> "Alarm Tetiklendi"
    ReminderEventType.ALARM_RESCHEDULED -> "Alarm Yeniden Kuruldu"
    ReminderEventType.ALARM_PERMISSION_DENIED -> "Alarm İzni Reddedildi"
    ReminderEventType.NOTIFICATION_SENT -> "Bildirim Gönderildi"
    ReminderEventType.NOTIFICATION_CLICKED -> "Bildirim Tıklandı"
    ReminderEventType.NOTIFICATION_DISMISSED -> "Bildirim Kapatıldı"
    ReminderEventType.NOTIFICATION_FAILED -> "Bildirim Başarısız"
    ReminderEventType.DOSE_TAKEN -> "İlaç Alındı"
    ReminderEventType.DOSE_SKIPPED -> "İlaç Atlandı"
    ReminderEventType.DOSE_SNOOZED -> "İlaç Ertelendi"
    ReminderEventType.DOSE_MISSED -> "İlaç Kaçırıldı"
    ReminderEventType.SNOOZE_TRIGGERED -> "Erteleme Tetiklendi"
    ReminderEventType.ESCALATION_SCHEDULED -> "Escalation Planlandı"
    ReminderEventType.ESCALATION_CANCELLED -> "Escalation İptal Edildi"
    ReminderEventType.ESCALATION_1_TRIGGERED -> "Escalation 1 Tetiklendi"
    ReminderEventType.ESCALATION_2_TRIGGERED -> "Escalation 2 Tetiklendi"
    ReminderEventType.ESCALATION_3_TRIGGERED -> "Escalation 3 Tetiklendi"
    ReminderEventType.BUDDY_REQUEST_ACCEPTED -> "Badi İsteği Kabul Edildi"
    ReminderEventType.BUDDY_REQUEST_REJECTED -> "Badi İsteği Reddedildi"
    ReminderEventType.BUDDY_NOTIFICATION_SENT -> "Badi Bildirimi Gönderildi"
    ReminderEventType.BOOT_COMPLETED -> "Cihaz Başlatıldı"
    ReminderEventType.ALL_ALARMS_RESCHEDULED -> "Tüm Alarmlar Planlandı"
    ReminderEventType.APP_OPENED -> "Uygulama Açıldı"
    ReminderEventType.ERROR -> "Hata"
    ReminderEventType.SCHEDULE_ERROR -> "Alarm Kurma Hatası"
    ReminderEventType.NOTIFICATION_ERROR -> "Bildirim Hatası"
    ReminderEventType.FIREBASE_ERROR -> "Firebase Hatası"
    ReminderEventType.DEBUG -> "Debug"
}

/**
 * Günlük log özeti
 */
data class DailyReminderLogSummary(
    val date: String,                       // "2025-11-22"
    val totalEvents: Int,
    val successfulEvents: Int,
    val failedEvents: Int,
    val alarmsScheduled: Int,
    val alarmsTriggered: Int,
    val dosesTaken: Int,
    val dosesSkipped: Int,
    val dosesSnoozed: Int,
    val errors: Int
)
