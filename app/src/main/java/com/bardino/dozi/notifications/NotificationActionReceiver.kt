package com.bardino.dozi.notifications

import android.Manifest
import android.app.AlarmManager
import android.app.AlertDialog
import android.app.PendingIntent
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.SharedPreferences
import android.content.pm.PackageManager
import android.media.RingtoneManager
import android.os.Build
import android.os.VibrationEffect
import android.os.Vibrator
import android.speech.tts.TextToSpeech
import android.view.WindowManager
import android.widget.Toast
import androidx.annotation.RequiresPermission
import androidx.core.app.ActivityCompat
import androidx.core.app.NotificationManagerCompat
import androidx.core.content.ContextCompat
import androidx.core.content.edit
import com.bardino.dozi.DoziApplication
import com.bardino.dozi.core.utils.SoundHelper
import com.bardino.dozi.core.data.repository.BadiRepository
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

class NotificationActionReceiver : BroadcastReceiver() {

    private var tts: TextToSpeech? = null

    @RequiresPermission(Manifest.permission.POST_NOTIFICATIONS)
    override fun onReceive(context: Context, intent: Intent) {
        val med = intent.getStringExtra(NotificationHelper.EXTRA_MEDICINE) ?: "İlaç"
        val medicineId = intent.getStringExtra(NotificationHelper.EXTRA_MEDICINE_ID) ?: ""
        val dosage = intent.getStringExtra(NotificationHelper.EXTRA_DOSAGE) ?: ""
        val time = intent.getStringExtra(NotificationHelper.EXTRA_TIME) ?: "Bilinmiyor"
        val scheduledTime = intent.getLongExtra(NotificationHelper.EXTRA_SCHEDULED_TIME, System.currentTimeMillis())
        val prefs = context.getSharedPreferences("dozi_prefs", Context.MODE_PRIVATE)
        val nm = NotificationManagerCompat.from(context)

        when (intent.action) {
            NotificationHelper.ACTION_TAKEN -> handleTaken(context, med, medicineId, dosage, time, scheduledTime, prefs, nm)
            NotificationHelper.ACTION_SKIP -> handleSkip(context, med, medicineId, dosage, time, scheduledTime, prefs, nm)
            NotificationHelper.ACTION_SNOOZE -> handleSnooze(context, med, medicineId, dosage, time, scheduledTime, nm)
            NotificationHelper.ACTION_BUDDY_ACCEPT -> {
                val requestId = intent.getStringExtra(NotificationHelper.EXTRA_REQUEST_ID) ?: return
                val fromUserName = intent.getStringExtra(NotificationHelper.EXTRA_FROM_USER_NAME) ?: "Kullanıcı"
                handleBuddyAccept(context, requestId, fromUserName, nm)
            }
            NotificationHelper.ACTION_BUDDY_REJECT -> {
                val requestId = intent.getStringExtra(NotificationHelper.EXTRA_REQUEST_ID) ?: return
                val fromUserName = intent.getStringExtra(NotificationHelper.EXTRA_FROM_USER_NAME) ?: "Kullanıcı"
                handleBuddyReject(context, requestId, fromUserName, nm)
            }
            "ACTION_SNOOZE_TRIGGER" -> {
                // ⏰ Erteleme süresi doldu, yeni bildirim göster + escalation/auto-missed planla
                if (hasNotificationPermission(context)) {
                    NotificationHelper.showMedicationNotification(
                        context = context,
                        medicineName = med,
                        medicineId = medicineId,
                        dosage = dosage,
                        time = time,
                        scheduledTime = scheduledTime
                    )

                    // ⏰ YENİ: 30 dakika sonra escalation (eğer hala aksiyon alınmadıysa)
                    if (medicineId.isNotEmpty()) {
                        scheduleEscalationReminder(context, medicineId, med, dosage, time, scheduledTime)
                        // ⏱️ YENİ: 1 saat sonra auto-MISSED (eğer hala aksiyon alınmadıysa)
                        scheduleAutoMissed(context, medicineId, med, dosage, time, scheduledTime)
                    }

                    android.util.Log.d("NotificationActionReceiver", "⏰ Erteleme sonrası bildirim + escalation/auto-missed planlandı: $med")
                }
            }
            ReminderScheduler.ACTION_REMINDER_TRIGGER -> {
                // 🔔 Zamanlanmış hatırlatma tetiklendi
                val medicineId = intent.getStringExtra(ReminderScheduler.EXTRA_MEDICINE_ID) ?: return
                val medicineName = intent.getStringExtra(ReminderScheduler.EXTRA_MEDICINE_NAME) ?: "İlaç"
                val reminderTime = intent.getStringExtra(ReminderScheduler.EXTRA_TIME) ?: ""

                handleReminderTrigger(context, medicineId, medicineName, reminderTime, nm)
            }
            "ACTION_ESCALATION_TRIGGER" -> {
                // ⏰ Escalation tetiklendi (30 dakika geçti, hala aksiyon alınmadı)
                if (hasNotificationPermission(context)) {
                    NotificationHelper.showMedicationNotification(
                        context = context,
                        medicineName = med,
                        medicineId = medicineId,
                        dosage = dosage,
                        time = time,
                        scheduledTime = scheduledTime
                    )
                    android.util.Log.d("NotificationActionReceiver", "⏰ Escalation bildirimi gösterildi: $med")
                }
            }
            "ACTION_AUTO_MISSED" -> {
                // ⏱️ Auto-MISSED tetiklendi (1 saat geçti, otomatik olarak MISSED işaretle)
                if (medicineId.isNotEmpty()) {
                    val app = context.applicationContext as DoziApplication
                    val medicationLogRepository = com.bardino.dozi.core.data.repository.MedicationLogRepository(
                        context,
                        com.google.firebase.auth.FirebaseAuth.getInstance(),
                        com.google.firebase.firestore.FirebaseFirestore.getInstance(),
                        app.profileManager
                    )
                    CoroutineScope(Dispatchers.IO).launch {
                        val log = com.bardino.dozi.core.data.model.MedicationLog(
                            medicineId = medicineId,
                            medicineName = med,
                            dosage = dosage,
                            scheduledTime = com.google.firebase.Timestamp(java.util.Date(scheduledTime)),
                            status = com.bardino.dozi.core.data.model.MedicationStatus.MISSED,
                            notes = "Otomatik olarak kaçırıldı olarak işaretlendi (1 saat cevapsız)"
                        )
                        medicationLogRepository.createMedicationLog(log)
                        android.util.Log.d("NotificationActionReceiver", "⏱️ Otomatik MISSED kaydı oluşturuldu: $med")
                    }

                    // Bildirimi kapat
                    nm.cancel(NotificationHelper.NOTIF_ID)
                }
            }
        }
    }

    private fun handleTaken(
        context: Context,
        medicineName: String,
        medicineId: String,
        dosage: String,
        time: String,
        scheduledTime: Long,
        prefs: SharedPreferences,
        nm: NotificationManagerCompat
    ) {
        val takenTime = System.currentTimeMillis()

        prefs.edit {
            putString("last_action", "ALINDI:$medicineName")
            putLong("last_taken_time", takenTime)
            putString("last_medicine", medicineName)
        }

        nm.cancel(NotificationHelper.NOTIF_ID)

        // ✅ MedicationLog'a kaydet
        if (medicineId.isNotEmpty()) {
            val app = context.applicationContext as DoziApplication
            val medicationLogRepository = com.bardino.dozi.core.data.repository.MedicationLogRepository(
                context,
                com.google.firebase.auth.FirebaseAuth.getInstance(),
                com.google.firebase.firestore.FirebaseFirestore.getInstance(),
                app.profileManager
            )
            CoroutineScope(Dispatchers.IO).launch {
                medicationLogRepository.logMedicationTaken(
                    medicineId = medicineId,
                    medicineName = medicineName,
                    dosage = dosage,
                    scheduledTime = scheduledTime
                )
                android.util.Log.d("NotificationActionReceiver", "✅ MedicationLog kaydedildi: TAKEN")

                // 🧠 Gecikme pattern'ini kaydet (gelecekteki öneriler için)
                SmartReminderHelper.recordDelayPattern(
                    context = context,
                    medicineId = medicineId,
                    scheduledTime = scheduledTime,
                    takenTime = takenTime
                )
            }

            // İptal: Escalation ve Auto-MISSED alarmları
            cancelEscalationAndAutoMissed(context, medicineId, time)
        }

        showToast(context, "$medicineName alındı olarak işaretlendi ✅")

        // ✅ Kullanıcının ses seçimine göre başarı sesi
        SoundHelper.playSound(context, SoundHelper.SoundType.HERSEY_TAMAM)
    }


    private fun handleSkip(
        context: Context,
        medicineName: String,
        medicineId: String,
        dosage: String,
        time: String,
        scheduledTime: Long,
        prefs: SharedPreferences,
        nm: NotificationManagerCompat
    ) {
        prefs.edit {
            putString("last_action", "ATLANDI:$medicineName")
            putLong("last_skip_time", System.currentTimeMillis())
        }

        nm.cancel(NotificationHelper.NOTIF_ID)

        // ✅ MedicationLog'a kaydet
        if (medicineId.isNotEmpty()) {
            val app = context.applicationContext as DoziApplication
            val medicationLogRepository = com.bardino.dozi.core.data.repository.MedicationLogRepository(
                context,
                com.google.firebase.auth.FirebaseAuth.getInstance(),
                com.google.firebase.firestore.FirebaseFirestore.getInstance(),
                app.profileManager
            )
            CoroutineScope(Dispatchers.IO).launch {
                medicationLogRepository.logMedicationSkipped(
                    medicineId = medicineId,
                    medicineName = medicineName,
                    dosage = dosage,
                    scheduledTime = scheduledTime,
                    reason = "Kullanıcı atladı"
                )
                android.util.Log.d("NotificationActionReceiver", "✅ MedicationLog kaydedildi: SKIPPED")
            }

            // İptal: Escalation ve Auto-MISSED alarmları
            cancelEscalationAndAutoMissed(context, medicineId, time)
        }

        showToast(context, "$medicineName atlandı 🚫")

        // ✅ Kullanıcının ses seçimine göre atla sesi
        SoundHelper.playSound(context, SoundHelper.SoundType.PEKALA)
    }


    private fun handleSnooze(
        context: Context,
        medicineName: String,
        medicineId: String,
        dosage: String,
        time: String,
        scheduledTime: Long,
        nm: NotificationManagerCompat
    ) {
        nm.cancel(NotificationHelper.NOTIF_ID)

        // ✅ MedicationLog'a kaydet (SNOOZED)
        if (medicineId.isNotEmpty()) {
            val app = context.applicationContext as DoziApplication

            val medicationLogRepository = com.bardino.dozi.core.data.repository.MedicationLogRepository(
                context,
                com.google.firebase.auth.FirebaseAuth.getInstance(),
                com.google.firebase.firestore.FirebaseFirestore.getInstance(),
                app.profileManager
            )

            CoroutineScope(Dispatchers.IO).launch {
                medicationLogRepository.logMedicationSnoozed(
                    medicineId = medicineId,
                    medicineName = medicineName,
                    dosage = dosage,
                    scheduledTime = scheduledTime,
                    snoozeMinutes = 10
                )
                android.util.Log.d("NotificationActionReceiver", "✅ MedicationLog kaydedildi: SNOOZED")
            }

            // 🔥 FIX: Erteleme seçilince escalation ve auto-MISSED iptal et
            cancelEscalationAndAutoMissed(context, medicineId, time)
        }

        // Ses çal
        SoundHelper.playSound(context, SoundHelper.SoundType.ERTELE)

        // Yeni activity aç
        val intent = Intent(context, SnoozePromptActivity::class.java).apply {
            putExtra("medicine", medicineName)
            putExtra("medicineId", medicineId)
            putExtra("dosage", dosage)
            putExtra("time", time)
            putExtra("scheduledTime", scheduledTime)
            addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        }
        context.startActivity(intent)
    }


    private fun handleBuddyAccept(
        context: Context,
        requestId: String,
        fromUserName: String,
        nm: NotificationManagerCompat
    ) {
        // Bildirimi kapat
        nm.cancel(requestId.hashCode())

        // Badi isteğini kabul et
        val buddyRepository = BadiRepository()
        CoroutineScope(Dispatchers.IO).launch {
            try {
                buddyRepository.acceptBadiRequest(requestId)
                    .onSuccess {
                        // Ana thread'de toast göster
                        CoroutineScope(Dispatchers.Main).launch {
                            showToast(context, "✅ $fromUserName buddy olarak eklendi!")
                        }
                    }
                    .onFailure { error ->
                        CoroutineScope(Dispatchers.Main).launch {
                            showToast(context, "❌ Hata: ${error.message}")
                        }
                    }
            } catch (e: Exception) {
                CoroutineScope(Dispatchers.Main).launch {
                    showToast(context, "❌ Hata: ${e.message}")
                }
            }
        }
    }

    private fun handleBuddyReject(
        context: Context,
        requestId: String,
        fromUserName: String,
        nm: NotificationManagerCompat
    ) {
        // Bildirimi kapat
        nm.cancel(requestId.hashCode())

        // Badi isteğini reddet
        val buddyRepository = BadiRepository()
        CoroutineScope(Dispatchers.IO).launch {
            try {
                buddyRepository.rejectBadiRequest(requestId)
                    .onSuccess {
                        CoroutineScope(Dispatchers.Main).launch {
                            showToast(context, "🚫 $fromUserName buddy isteği reddedildi")
                        }
                    }
                    .onFailure { error ->
                        CoroutineScope(Dispatchers.Main).launch {
                            showToast(context, "❌ Hata: ${error.message}")
                        }
                    }
            } catch (e: Exception) {
                CoroutineScope(Dispatchers.Main).launch {
                    showToast(context, "❌ Hata: ${e.message}")
                }
            }
        }
    }

    @androidx.annotation.RequiresPermission(android.Manifest.permission.POST_NOTIFICATIONS)
    private fun handleReminderTrigger(
        context: Context,
        medicineId: String,
        medicineName: String,
        time: String,
        nm: NotificationManagerCompat
    ) {
        android.util.Log.d("NotificationActionReceiver", "🔔 Hatırlatma tetiklendi: $medicineName ($time)")

        // Bildirim izni kontrolü
        if (!hasNotificationPermission(context)) {
            android.util.Log.w("NotificationActionReceiver", "⚠️ Bildirim izni yok")
            return
        }

        // Medicine bilgilerini al
        CoroutineScope(Dispatchers.IO).launch {
            try {
                val app = context.applicationContext as DoziApplication
                val medicineRepository = com.bardino.dozi.core.data.repository.MedicineRepository(app.profileManager)
                val medicine = medicineRepository.getMedicine(medicineId)

                if (medicine != null) {
                    // scheduledTime hesapla (bugünün bu saati)
                    val (hour, minute) = time.split(":").map { it.toInt() }
                    val calendar = java.util.Calendar.getInstance().apply {
                        set(java.util.Calendar.HOUR_OF_DAY, hour)
                        set(java.util.Calendar.MINUTE, minute)
                        set(java.util.Calendar.SECOND, 0)
                        set(java.util.Calendar.MILLISECOND, 0)
                    }
                    val scheduledTime = calendar.timeInMillis

                    // Bildirim göster (medicineId, dosage ve time note ile)
                    if (hasNotificationPermission(context)) {
                        val timeNote = parseTimeNoteFromMedicine(medicine.notes, time)
                        NotificationHelper.showMedicationNotification(
                            context = context,
                            medicineName = medicine.name,
                            medicineId = medicine.id,
                            dosage = "${medicine.dosage} ${medicine.unit}",
                            time = time,
                            scheduledTime = scheduledTime,
                            timeNote = timeNote
                        )
                    }

                    // 🔄 Sonraki alarmı planla (frequency'ye göre)
                    if (medicine.reminderEnabled) {
                        ReminderScheduler.scheduleReminders(context, medicine, isRescheduling = true)
                        android.util.Log.d("NotificationActionReceiver", "✅ Sonraki alarm planlandı: $medicineName (frequency: ${medicine.frequency})")
                    }

                    // ⏰ 30 dakika sonra escalation (eğer hala aksiyon alınmadıysa)
                    scheduleEscalationReminder(context, medicineId, medicineName, medicine.dosage + " " + medicine.unit, time, scheduledTime)

                    // ⏱️ 1 saat sonra auto-MISSED (eğer hala aksiyon alınmadıysa)
                    scheduleAutoMissed(context, medicineId, medicineName, medicine.dosage + " " + medicine.unit, time, scheduledTime)
                }
            } catch (e: Exception) {
                android.util.Log.e("NotificationActionReceiver", "❌ Hatırlatma işlenirken hata", e)
            }
        }
    }


    private fun hasNotificationPermission(context: Context): Boolean {
        return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            ContextCompat.checkSelfPermission(
                context,
                Manifest.permission.POST_NOTIFICATIONS
            ) == PackageManager.PERMISSION_GRANTED
        } else true
    }

    private fun showToast(context: Context, message: String) {
        Toast.makeText(context, message, Toast.LENGTH_SHORT).show()
    }

    private fun playSuccessSoundSafe(context: Context) {
        try {
            if (ActivityCompat.checkSelfPermission(
                    context,
                    Manifest.permission.POST_NOTIFICATIONS
                ) == PackageManager.PERMISSION_GRANTED || Build.VERSION.SDK_INT < Build.VERSION_CODES.TIRAMISU
            ) {
                val notification = RingtoneManager.getDefaultUri(RingtoneManager.TYPE_NOTIFICATION)
                RingtoneManager.getRingtone(context, notification)?.play()
            }
        } catch (e: SecurityException) {
            e.printStackTrace()
        }
    }

    private fun vibrateDevice(context: Context, duration: Long = 200) {
        val vibrator = ContextCompat.getSystemService(context, Vibrator::class.java)
        vibrator?.let {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                it.vibrate(VibrationEffect.createOneShot(duration, VibrationEffect.DEFAULT_AMPLITUDE))
            } else {
                @Suppress("DEPRECATION")
                it.vibrate(duration)
            }
        }
    }

    /**
     * ⏰ Escalation: 30 dakika sonra tekrar hatırlat (eğer hala aksiyon alınmadıysa)
     */
    private fun scheduleEscalationReminder(
        context: Context,
        medicineId: String,
        medicineName: String,
        dosage: String,
        time: String,
        scheduledTime: Long
    ) {
        val alarmManager = context.getSystemService(Context.ALARM_SERVICE) as AlarmManager
        val escalationTime = System.currentTimeMillis() + (30 * 60 * 1000) // 30 dakika sonra

        val intent = Intent(context, NotificationActionReceiver::class.java).apply {
            action = "ACTION_ESCALATION_TRIGGER"
            putExtra(NotificationHelper.EXTRA_MEDICINE_ID, medicineId)
            putExtra(NotificationHelper.EXTRA_MEDICINE, medicineName)
            putExtra(NotificationHelper.EXTRA_DOSAGE, dosage)
            putExtra(NotificationHelper.EXTRA_TIME, time)
            putExtra(NotificationHelper.EXTRA_SCHEDULED_TIME, scheduledTime)
        }

        val requestCode = "escalation_${medicineId}_$time".hashCode()
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
            alarmManager.setExactAndAllowWhileIdle(AlarmManager.RTC_WAKEUP, escalationTime, pendingIntent)
        } else {
            alarmManager.setExact(AlarmManager.RTC_WAKEUP, escalationTime, pendingIntent)
        }

        android.util.Log.d("NotificationActionReceiver", "⏰ Escalation planlandı: $medicineName - 30 dk sonra")
    }

    /**
     * ⏱️ Auto-MISSED: 1 saat sonra otomatik olarak MISSED olarak işaretle
     */
    private fun scheduleAutoMissed(
        context: Context,
        medicineId: String,
        medicineName: String,
        dosage: String,
        time: String,
        scheduledTime: Long
    ) {
        val alarmManager = context.getSystemService(Context.ALARM_SERVICE) as AlarmManager
        val autoMissedTime = System.currentTimeMillis() + (60 * 60 * 1000) // 1 saat sonra

        val intent = Intent(context, NotificationActionReceiver::class.java).apply {
            action = "ACTION_AUTO_MISSED"
            putExtra(NotificationHelper.EXTRA_MEDICINE_ID, medicineId)
            putExtra(NotificationHelper.EXTRA_MEDICINE, medicineName)
            putExtra(NotificationHelper.EXTRA_DOSAGE, dosage)
            putExtra(NotificationHelper.EXTRA_TIME, time)
            putExtra(NotificationHelper.EXTRA_SCHEDULED_TIME, scheduledTime)
        }

        val requestCode = "auto_missed_${medicineId}_$time".hashCode()
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
            alarmManager.setExactAndAllowWhileIdle(AlarmManager.RTC_WAKEUP, autoMissedTime, pendingIntent)
        } else {
            alarmManager.setExact(AlarmManager.RTC_WAKEUP, autoMissedTime, pendingIntent)
        }

        android.util.Log.d("NotificationActionReceiver", "⏱️ Auto-MISSED planlandı: $medicineName - 1 saat sonra")
    }

    /**
     * 🚫 Escalation ve Auto-MISSED alarmlarını iptal et
     */
    private fun cancelEscalationAndAutoMissed(context: Context, medicineId: String, time: String) {
        val alarmManager = context.getSystemService(Context.ALARM_SERVICE) as AlarmManager

        // Escalation iptal
        val escalationIntent = Intent(context, NotificationActionReceiver::class.java).apply {
            action = "ACTION_ESCALATION_TRIGGER"
        }
        val escalationRequestCode = "escalation_${medicineId}_$time".hashCode()
        val escalationPendingIntent = PendingIntent.getBroadcast(
            context,
            escalationRequestCode,
            escalationIntent,
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
                PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
            } else {
                PendingIntent.FLAG_UPDATE_CURRENT
            }
        )
        alarmManager.cancel(escalationPendingIntent)
        escalationPendingIntent.cancel()

        // Auto-MISSED iptal
        val autoMissedIntent = Intent(context, NotificationActionReceiver::class.java).apply {
            action = "ACTION_AUTO_MISSED"
        }
        val autoMissedRequestCode = "auto_missed_${medicineId}_$time".hashCode()
        val autoMissedPendingIntent = PendingIntent.getBroadcast(
            context,
            autoMissedRequestCode,
            autoMissedIntent,
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
                PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
            } else {
                PendingIntent.FLAG_UPDATE_CURRENT
            }
        )
        alarmManager.cancel(autoMissedPendingIntent)
        autoMissedPendingIntent.cancel()

        android.util.Log.d("NotificationActionReceiver", "🚫 Escalation ve Auto-MISSED iptal edildi: $medicineId - $time")
    }

    override fun toString(): String = "NotificationActionReceiver - Dozi Bildirim İşleyici"
}

private fun listAvailableVoices(tts: TextToSpeech) {
    tts.voices?.forEach { voice ->
        println("Ses adı: ${voice.name}, locale: ${voice.locale}, quality: ${voice.quality}, latency: ${voice.latency}")
    }
}

/**
 * Medicine.notes'tan belirli bir saat için notu parse et
 *
 * Format: "08:00: Tok karnına | 20:00: Yemekten sonra | Her 3 günde bir"
 *
 * @param notes Medicine.notes string
 * @param time Aranacak saat (örn: "08:00")
 * @return Bu saat için not, yoksa boş string
 */
private fun parseTimeNoteFromMedicine(notes: String, time: String): String {
    if (notes.isEmpty()) return ""

    // "|" ile ayır (birden fazla saat notu olabilir)
    val parts = notes.split("|").map { it.trim() }

    parts.forEach { part ->
        // "08:00: Tok karnına" formatında mı?
        if (part.contains(":") && part.startsWith(time)) {
            // "08:00: Tok karnına" -> "Tok karnına"
            val noteText = part.substringAfter("$time:").trim()
            if (noteText.isNotEmpty()) {
                return noteText
            }
        }
    }

    return ""
}

