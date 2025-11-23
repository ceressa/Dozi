package com.bardino.dozi.notifications

import android.Manifest
import android.app.AlarmManager
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
import android.widget.Toast
import androidx.annotation.RequiresPermission
import androidx.core.app.ActivityCompat
import androidx.core.app.NotificationManagerCompat
import androidx.core.content.ContextCompat
import androidx.core.content.edit
import com.bardino.dozi.DoziApplication
import com.bardino.dozi.core.utils.SoundHelper
import com.bardino.dozi.core.utils.EscalationManager
import com.bardino.dozi.core.data.repository.BadiRepository
import com.bardino.dozi.core.data.repository.MedicationLogRepository
import com.bardino.dozi.core.data.repository.MedicineRepository
import com.bardino.dozi.core.logging.ReminderLogger
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
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
                // 📝 Log: Erteleme tetiklendi
                ReminderLogger.logSnoozeTriggered(context, medicineId, med, time)

                if (hasNotificationPermission(context)) {
                    // 📅 Medicine bilgisini al ve endDate kontrolü yap
                    CoroutineScope(Dispatchers.IO).launch {
                        try {
                            val medicineRepository = com.bardino.dozi.core.data.repository.MedicineRepository()
                            val medicine = medicineRepository.getMedicineById(medicineId)

                            // Bitiş tarihi kontrolü
                            if (medicine?.endDate != null && medicine.endDate < System.currentTimeMillis()) {
                                android.util.Log.d("NotificationActionReceiver", "⏱️ Bitiş tarihi geçmiş: $med. Erteleme bildirimi gösterilmiyor.")
                                return@launch
                            }

                            // 🔥 Kritik ilaç kontrolü
                            val isCritical = medicine?.criticalityLevel == com.bardino.dozi.core.data.model.MedicineCriticality.CRITICAL
                            NotificationHelper.showMedicationNotification(
                                context = context,
                                medicineName = med,
                                medicineId = medicineId,
                                dosage = dosage,
                                time = time,
                                scheduledTime = scheduledTime,
                                isCritical = isCritical
                            )

                            // 📝 Log: Bildirim gönderildi
                            val notificationId = "${medicineId}_$time".hashCode()
                            ReminderLogger.logNotificationSent(context, medicineId, med, time, notificationId)

                            // ⏰ Escalation sistemi: 10dk, 30dk, 60dk (endDate geçmemişse)
                            if (medicineId.isNotEmpty() && (medicine?.endDate == null || medicine.endDate > System.currentTimeMillis())) {
                                scheduleEscalation1(context, medicineId, med, dosage, time, scheduledTime) // 10 dk
                                scheduleEscalation2(context, medicineId, med, dosage, time, scheduledTime) // 30 dk
                                scheduleEscalation3(context, medicineId, med, dosage, time, scheduledTime) // 60 dk
                            }

                            android.util.Log.d("NotificationActionReceiver", "⏰ Erteleme sonrası bildirim + escalation/auto-missed planlandı: $med")
                        } catch (e: Exception) {
                            android.util.Log.e("NotificationActionReceiver", "❌ Erteleme tetiklemesi işlenirken hata", e)
                            ReminderLogger.logError(context, "Erteleme tetiklemesi hatası", e, medicineId, med, time)
                        }
                    }
                }
            }
            ReminderScheduler.ACTION_REMINDER_TRIGGER -> {
                // 🔔 Zamanlanmış hatırlatma tetiklendi
                val medicineId = intent.getStringExtra(ReminderScheduler.EXTRA_MEDICINE_ID) ?: return
                val medicineName = intent.getStringExtra(ReminderScheduler.EXTRA_MEDICINE_NAME) ?: "İlaç"
                val reminderTime = intent.getStringExtra(ReminderScheduler.EXTRA_TIME) ?: ""

                handleReminderTrigger(context, medicineId, medicineName, reminderTime, nm)
            }
            "ACTION_ESCALATION_1" -> {
                // ⏰ Escalation Level 1 (10 dakika sonra)
                if (hasNotificationPermission(context) && medicineId.isNotEmpty()) {
                    // 🔥 FIX: İlaç zaten alındı mı kontrol et
                    CoroutineScope(Dispatchers.IO).launch {
                        try {
                            val medicationLogRepository = MedicationLogRepository(
                                context,
                                FirebaseAuth.getInstance(),
                                FirebaseFirestore.getInstance()
                            )

                            val alreadyLogged = medicationLogRepository.isMedicationLoggedForTime(
                                medicineId = medicineId,
                                scheduledTime = scheduledTime
                            )

                            if (alreadyLogged) {
                                android.util.Log.d("NotificationActionReceiver", "⏭️ Escalation 1 atlandı - ilaç zaten loglandı: $med")
                                return@launch
                            }

                            // 📝 Log: Escalation 1 tetiklendi
                            ReminderLogger.logEscalation1Triggered(context, medicineId, med, time)

                            NotificationHelper.showEscalationLevel1Notification(
                                context = context,
                                medicineName = med,
                                medicineId = medicineId,
                                dosage = dosage,
                                time = time,
                                scheduledTime = scheduledTime
                            )

                            // 📝 Log: Bildirim gönderildi
                            val notificationId = "esc1_${medicineId}_$time".hashCode()
                            ReminderLogger.logNotificationSent(context, medicineId, med, time, notificationId)

                            android.util.Log.d("NotificationActionReceiver", "⏰ Escalation Level 1 bildirimi gösterildi: $med")
                        } catch (e: Exception) {
                            android.util.Log.e("NotificationActionReceiver", "❌ Escalation 1 hatası", e)
                            ReminderLogger.logError(context, "Escalation 1 hatası", e, medicineId, med, time)
                        }
                    }
                }
            }
            "ACTION_ESCALATION_2" -> {
                // 🚨 Escalation Level 2 (30 dakika sonra - kırmızı, urgent)
                if (hasNotificationPermission(context) && medicineId.isNotEmpty()) {
                    // 🔥 FIX: İlaç zaten alındı mı kontrol et
                    CoroutineScope(Dispatchers.IO).launch {
                        try {
                            val medicationLogRepository = MedicationLogRepository(
                                context,
                                FirebaseAuth.getInstance(),
                                FirebaseFirestore.getInstance()
                            )

                            val alreadyLogged = medicationLogRepository.isMedicationLoggedForTime(
                                medicineId = medicineId,
                                scheduledTime = scheduledTime
                            )

                            if (alreadyLogged) {
                                android.util.Log.d("NotificationActionReceiver", "⏭️ Escalation 2 atlandı - ilaç zaten loglandı: $med")
                                return@launch
                            }

                            // 📝 Log: Escalation 2 tetiklendi
                            ReminderLogger.logEscalation2Triggered(context, medicineId, med, time)

                            NotificationHelper.showEscalationLevel2Notification(
                                context = context,
                                medicineName = med,
                                medicineId = medicineId,
                                dosage = dosage,
                                time = time,
                                scheduledTime = scheduledTime
                            )

                            // 📝 Log: Bildirim gönderildi
                            val notificationId = "esc2_${medicineId}_$time".hashCode()
                            ReminderLogger.logNotificationSent(context, medicineId, med, time, notificationId)

                            android.util.Log.d("NotificationActionReceiver", "🚨 Escalation Level 2 bildirimi gösterildi: $med")
                        } catch (e: Exception) {
                            android.util.Log.e("NotificationActionReceiver", "❌ Escalation 2 hatası", e)
                            ReminderLogger.logError(context, "Escalation 2 hatası", e, medicineId, med, time)
                        }
                    }
                }
            }
            "ACTION_ESCALATION_3" -> {
                // 🔴 Escalation Level 3 (60 dakika sonra - IMPORTANT)
                if (hasNotificationPermission(context) && medicineId.isNotEmpty()) {
                    // 🔥 FIX: İlaç zaten alındı mı kontrol et
                    CoroutineScope(Dispatchers.IO).launch {
                        try {
                            val medicationLogRepository = MedicationLogRepository(
                                context,
                                FirebaseAuth.getInstance(),
                                FirebaseFirestore.getInstance()
                            )

                            val alreadyLogged = medicationLogRepository.isMedicationLoggedForTime(
                                medicineId = medicineId,
                                scheduledTime = scheduledTime
                            )

                            if (alreadyLogged) {
                                android.util.Log.d("NotificationActionReceiver", "⏭️ Escalation 3 atlandı - ilaç zaten loglandı: $med")
                                return@launch
                            }

                            // 📝 Log: Escalation 3 tetiklendi
                            ReminderLogger.logEscalation3Triggered(context, medicineId, med, time)

                            NotificationHelper.showEscalationLevel3Notification(
                                context = context,
                                medicineName = med,
                                medicineId = medicineId,
                                dosage = dosage,
                                time = time,
                                scheduledTime = scheduledTime
                            )

                            // 📝 Log: Bildirim gönderildi
                            val notificationId = "esc3_${medicineId}_$time".hashCode()
                            ReminderLogger.logNotificationSent(context, medicineId, med, time, notificationId)

                            android.util.Log.d("NotificationActionReceiver", "🔴 Escalation Level 3 bildirimi gösterildi: $med")

                            // İlacı MISSED olarak logla
                            medicationLogRepository.logMedicationMissed(
                                medicineId = medicineId,
                                medicineName = med,
                                dosage = dosage,
                                scheduledTime = scheduledTime,
                                reason = "1 saat boyunca cevap verilmedi"
                            )

                            // 📝 Log: İlaç kaçırıldı
                            ReminderLogger.logDoseMissed(context, medicineId, med, time, "1 saat boyunca cevap verilmedi")

                            android.util.Log.d("NotificationActionReceiver", "❌ İlaç MISSED olarak loglandı: $med")

                            // Kritik ilaç ise buddy'lere bildir
                            val medicineRepository = MedicineRepository()
                            val medicine = medicineRepository.getMedicineById(medicineId)
                            if (medicine != null && medicine.criticalityLevel == com.bardino.dozi.core.data.model.MedicineCriticality.CRITICAL) {
                                val escalationManager = EscalationManager(context)
                                escalationManager.notifyBuddiesForSingleCriticalMedicine(medicine)

                                // 📝 Log: Buddy bildirimi gönderildi (kritik ilaç için)
                                ReminderLogger.logBuddyNotificationSent(context, medicineId, med, 1)

                                android.util.Log.d("NotificationActionReceiver", "🚨 Buddy bildirimi gönderildi: $med")
                            }
                        } catch (e: Exception) {
                            android.util.Log.e("NotificationActionReceiver", "❌ Escalation 3 hatası: ${e.message}", e)
                            ReminderLogger.logError(context, "Escalation 3 hatası", e, medicineId, med, time)
                        }
                    }
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

        // 🚫 Aynı ilaca ait TÜM bildirimleri iptal et
        NotificationHelper.cancelAllNotificationsForMedicine(context, medicineId, time)

        // ✅ MedicationLog'a kaydet (WorkManager ile garantili)
        if (medicineId.isNotEmpty()) {
            MedicationSyncWorker.enqueueLogTaken(
                context = context,
                medicineId = medicineId,
                medicineName = medicineName,
                dosage = dosage,
                scheduledTime = scheduledTime,
                takenTime = takenTime
            )
            android.util.Log.d("NotificationActionReceiver", "✅ MedicationLog WorkManager'a enqueue edildi: TAKEN")

            // İptal: Tüm escalation alarmları
            cancelAllEscalations(context, medicineId, time)

            // 🔥 FIX: Bu saat için planlanmış reminder alarmını iptal et
            ReminderScheduler.cancelReminders(context, medicineId, listOf(time))
            android.util.Log.d("NotificationActionReceiver", "🔕 Reminder alarm iptal edildi: $medicineId @ $time")

            // 📦 Stok azalt (tutarlılık için - Home screen ve Widget ile aynı davranış)
            CoroutineScope(Dispatchers.IO).launch {
                try {
                    val medicineRepository = MedicineRepository()
                    val medicine = medicineRepository.getMedicineById(medicineId)
                    if (medicine != null && medicine.autoDecrementEnabled && medicine.stockCount > 0) {
                        val newStockCount = medicine.stockCount - 1
                        medicineRepository.updateMedicineField(medicineId, "stockCount", newStockCount)
                        android.util.Log.d("NotificationActionReceiver", "📦 Stok azaltıldı: $medicineName -> $newStockCount")
                    }
                } catch (e: Exception) {
                    android.util.Log.e("NotificationActionReceiver", "❌ Stok azaltma hatası", e)
                }
            }
        }

        showToast(context, "$medicineName alındı olarak işaretlendi ✅")

        // ✅ Kullanıcının ses seçimine göre başarı sesi
        SoundHelper.playSound(context, SoundHelper.SoundType.HERSEY_TAMAM)

        // 📝 Log kaydı
        ReminderLogger.logDoseTaken(context, medicineId, medicineName, time)
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

        // 🚫 Aynı ilaca ait TÜM bildirimleri iptal et
        NotificationHelper.cancelAllNotificationsForMedicine(context, medicineId, time)

        // ✅ MedicationLog'a kaydet (WorkManager ile garantili)
        if (medicineId.isNotEmpty()) {
            MedicationSyncWorker.enqueueLogSkipped(
                context = context,
                medicineId = medicineId,
                medicineName = medicineName,
                dosage = dosage,
                scheduledTime = scheduledTime,
                reason = "Kullanıcı atladı"
            )
            android.util.Log.d("NotificationActionReceiver", "✅ MedicationLog WorkManager'a enqueue edildi: SKIPPED")

            // İptal: Tüm escalation alarmları
            cancelAllEscalations(context, medicineId, time)

            // 🔥 FIX: Bu saat için planlanmış reminder alarmını iptal et
            ReminderScheduler.cancelReminders(context, medicineId, listOf(time))
            android.util.Log.d("NotificationActionReceiver", "🔕 Reminder alarm iptal edildi: $medicineId @ $time")
        }

        showToast(context, "$medicineName atlandı 🚫")

        // ✅ Kullanıcının ses seçimine göre atla sesi
        SoundHelper.playSound(context, SoundHelper.SoundType.PEKALA)

        // 📝 Log kaydı
        ReminderLogger.logDoseSkipped(context, medicineId, medicineName, time, "Kullanıcı atladı")
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
        // 🚫 Aynı ilaca ait TÜM bildirimleri iptal et
        NotificationHelper.cancelAllNotificationsForMedicine(context, medicineId, time)

        // ✅ MedicationLog'a kaydet (WorkManager ile garantili)
        if (medicineId.isNotEmpty()) {
            MedicationSyncWorker.enqueueLogSnoozed(
                context = context,
                medicineId = medicineId,
                medicineName = medicineName,
                dosage = dosage,
                scheduledTime = scheduledTime,
                snoozeMinutes = 10
            )
            android.util.Log.d("NotificationActionReceiver", "✅ MedicationLog WorkManager'a enqueue edildi: SNOOZED")

            // 🔥 FIX: Erteleme seçilince tüm escalation alarmlarını iptal et
            cancelAllEscalations(context, medicineId, time)
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

        // 📝 Log kaydı
        ReminderLogger.logDoseSnoozed(context, medicineId, medicineName, time, 10)
    }


    private fun handleBuddyAccept(
        context: Context,
        requestId: String,
        fromUserName: String,
        nm: NotificationManagerCompat
    ) {
        // Bildirimi kapat
        nm.cancel(requestId.hashCode())

        // 📝 Log: Badi isteği kabul edildi
        ReminderLogger.logBuddyRequestAccepted(context, requestId, fromUserName)

        // Badi isteğini kabul et (WorkManager ile garantili)
        MedicationSyncWorker.enqueueBuddyAccept(context, requestId)
        showToast(context, "✅ $fromUserName buddy isteği işleniyor...")
        android.util.Log.d("NotificationActionReceiver", "✅ Buddy accept WorkManager'a enqueue edildi: $requestId")
    }

    private fun handleBuddyReject(
        context: Context,
        requestId: String,
        fromUserName: String,
        nm: NotificationManagerCompat
    ) {
        // Bildirimi kapat
        nm.cancel(requestId.hashCode())

        // 📝 Log: Badi isteği reddedildi
        ReminderLogger.logBuddyRequestRejected(context, requestId, fromUserName)

        // Badi isteğini reddet (WorkManager ile garantili)
        MedicationSyncWorker.enqueueBuddyReject(context, requestId)
        showToast(context, "🚫 $fromUserName buddy isteği reddediliyor...")
        android.util.Log.d("NotificationActionReceiver", "✅ Buddy reject WorkManager'a enqueue edildi: $requestId")
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

        // 📝 Alarm tetiklenme logu
        ReminderLogger.logAlarmTriggered(context, medicineId, medicineName, time)

        // Bildirim izni kontrolü
        if (!hasNotificationPermission(context)) {
            android.util.Log.w("NotificationActionReceiver", "⚠️ Bildirim izni yok")
            ReminderLogger.logNotificationFailed(context, medicineId, medicineName, time, "Bildirim izni yok")
            return
        }

        // Medicine bilgilerini al
        CoroutineScope(Dispatchers.IO).launch {
            try {
                val medicineRepository = com.bardino.dozi.core.data.repository.MedicineRepository()
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

                    // 🔥 FIX: İlaç zaten alındı/atlandı/ertelendi mi kontrol et
                    // Bu kontrol, kullanıcı uygulamadan "Aldım" işaretlediyse bildirimi engeller
                    val medicationLogRepository = MedicationLogRepository(context)
                    val alreadyLogged = medicationLogRepository.isMedicationLoggedForTime(medicineId, scheduledTime)

                    if (alreadyLogged) {
                        android.util.Log.d("NotificationActionReceiver", "✅ İlaç zaten işlendi, bildirim gösterilmiyor: $medicineName ($time)")
                        // Sonraki alarmı yine de planla
                        if (medicine.reminderEnabled) {
                            if (medicine.endDate == null || medicine.endDate > System.currentTimeMillis()) {
                                ReminderScheduler.scheduleReminders(context, medicine, isRescheduling = true)
                                android.util.Log.d("NotificationActionReceiver", "✅ Sonraki alarm planlandı (ilaç zaten alındı): $medicineName")
                            }
                        }
                        return@launch
                    }

                    // Bildirim göster (medicineId, dosage, time note ve reminderName ile)
                    if (hasNotificationPermission(context)) {
                        val timeNote = parseTimeNoteFromMedicine(medicine.notes, time)
                        // 🔥 Kritik ilaç kontrolü
                        val isCritical = medicine.criticalityLevel == com.bardino.dozi.core.data.model.MedicineCriticality.CRITICAL
                        NotificationHelper.showMedicationNotification(
                            context = context,
                            medicineName = medicine.name,
                            medicineId = medicine.id,
                            dosage = "${medicine.dosage} ${medicine.unit}",
                            time = time,
                            scheduledTime = scheduledTime,
                            timeNote = timeNote,
                            reminderName = medicine.reminderName,
                            isCritical = isCritical
                        )

                        // 📝 Bildirim gönderildi logu
                        val notificationId = "${medicine.id}_$time".hashCode()
                        ReminderLogger.logNotificationSent(context, medicine.id, medicine.name, time, notificationId)
                    }

                    // 🔄 Sonraki alarmı planla (frequency'ye göre)
                    // 📅 Bitiş tarihi kontrolü: endDate geçmişse sonraki alarmı planlama
                    if (medicine.reminderEnabled) {
                        if (medicine.endDate != null && medicine.endDate < System.currentTimeMillis()) {
                            android.util.Log.d("NotificationActionReceiver", "⏱️ Bitiş tarihi geçmiş: $medicineName. Sonraki alarm planlanmıyor.")
                        } else {
                            ReminderScheduler.scheduleReminders(context, medicine, isRescheduling = true)
                            android.util.Log.d("NotificationActionReceiver", "✅ Sonraki alarm planlandı: $medicineName (frequency: ${medicine.frequency})")
                        }
                    }

                    // ⏰ Escalation sistemi: 10dk, 30dk, 60dk
                    // 📅 Bitiş tarihi kontrolü: endDate geçmişse escalation da planlanmasın
                    if (medicine.endDate == null || medicine.endDate > System.currentTimeMillis()) {
                        scheduleEscalation1(context, medicineId, medicineName, medicine.dosage + " " + medicine.unit, time, scheduledTime)
                        scheduleEscalation2(context, medicineId, medicineName, medicine.dosage + " " + medicine.unit, time, scheduledTime)
                        scheduleEscalation3(context, medicineId, medicineName, medicine.dosage + " " + medicine.unit, time, scheduledTime)
                    }
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
     * ⏰ Escalation Level 1: 10 dakika sonra hatırlat
     */
    private fun scheduleEscalation1(
        context: Context,
        medicineId: String,
        medicineName: String,
        dosage: String,
        time: String,
        scheduledTime: Long
    ) {
        val alarmManager = context.getSystemService(Context.ALARM_SERVICE) as AlarmManager
        val escalationTime = System.currentTimeMillis() + (10 * 60 * 1000) // 10 dakika sonra

        val intent = Intent(context, NotificationActionReceiver::class.java).apply {
            action = "ACTION_ESCALATION_1"
            putExtra(NotificationHelper.EXTRA_MEDICINE_ID, medicineId)
            putExtra(NotificationHelper.EXTRA_MEDICINE, medicineName)
            putExtra(NotificationHelper.EXTRA_DOSAGE, dosage)
            putExtra(NotificationHelper.EXTRA_TIME, time)
            putExtra(NotificationHelper.EXTRA_SCHEDULED_TIME, scheduledTime)
        }

        val requestCode = "escalation1_${medicineId}_$time".hashCode()
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

        // 📝 Log: Escalation 1 planlandı
        ReminderLogger.logEscalationScheduled(context, medicineId, medicineName, time, 1, escalationTime)

        android.util.Log.d("NotificationActionReceiver", "⏰ Escalation Level 1 planlandı: $medicineName - 10 dk sonra")
    }

    /**
     * 🚨 Escalation Level 2: 30 dakika sonra hatırlat (kırmızı, urgent)
     */
    private fun scheduleEscalation2(
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
            action = "ACTION_ESCALATION_2"
            putExtra(NotificationHelper.EXTRA_MEDICINE_ID, medicineId)
            putExtra(NotificationHelper.EXTRA_MEDICINE, medicineName)
            putExtra(NotificationHelper.EXTRA_DOSAGE, dosage)
            putExtra(NotificationHelper.EXTRA_TIME, time)
            putExtra(NotificationHelper.EXTRA_SCHEDULED_TIME, scheduledTime)
        }

        val requestCode = "escalation2_${medicineId}_$time".hashCode()
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

        // 📝 Log: Escalation 2 planlandı
        ReminderLogger.logEscalationScheduled(context, medicineId, medicineName, time, 2, escalationTime)

        android.util.Log.d("NotificationActionReceiver", "🚨 Escalation Level 2 planlandı: $medicineName - 30 dk sonra")
    }

    /**
     * 🔴 Escalation Level 3: 60 dakika sonra hatırlat (IMPORTANT)
     * Sadece kullanıcı ayarı açıksa planlanır
     */
    private fun scheduleEscalation3(
        context: Context,
        medicineId: String,
        medicineName: String,
        dosage: String,
        time: String,
        scheduledTime: Long
    ) {
        // Kullanıcı ayarını kontrol et
        CoroutineScope(Dispatchers.IO).launch {
            try {
                val userRepository = com.bardino.dozi.core.data.repository.UserRepository()
                val user = userRepository.getUserData()

                // Önemli bildirimler kapalıysa Level 3'ü planlama
                if (user?.importantNotificationsEnabled == false) {
                    android.util.Log.d("NotificationActionReceiver", "⚙️ Önemli bildirimler kapalı, Level 3 planlanmadı: $medicineName")
                    return@launch
                }

                val alarmManager = context.getSystemService(Context.ALARM_SERVICE) as AlarmManager
                val escalationTime = System.currentTimeMillis() + (60 * 60 * 1000) // 60 dakika sonra

                val intent = Intent(context, NotificationActionReceiver::class.java).apply {
                    action = "ACTION_ESCALATION_3"
                    putExtra(NotificationHelper.EXTRA_MEDICINE_ID, medicineId)
                    putExtra(NotificationHelper.EXTRA_MEDICINE, medicineName)
                    putExtra(NotificationHelper.EXTRA_DOSAGE, dosage)
                    putExtra(NotificationHelper.EXTRA_TIME, time)
                    putExtra(NotificationHelper.EXTRA_SCHEDULED_TIME, scheduledTime)
                }

                val requestCode = "escalation3_${medicineId}_$time".hashCode()
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

                // 📝 Log: Escalation 3 planlandı
                ReminderLogger.logEscalationScheduled(context, medicineId, medicineName, time, 3, escalationTime)

                android.util.Log.d("NotificationActionReceiver", "🔴 Escalation Level 3 planlandı: $medicineName - 60 dk sonra")
            } catch (e: Exception) {
                android.util.Log.e("NotificationActionReceiver", "❌ Level 3 planlanırken hata", e)
                ReminderLogger.logError(context, "Escalation 3 planlama hatası", e, medicineId, medicineName, time)
            }
        }
    }

    /**
     * 🚫 Tüm escalation alarmlarını iptal et
     * FLAG_UPDATE_CURRENT kullanarak aynı PendingIntent'i alıp iptal ediyoruz
     */
    private fun cancelAllEscalations(context: Context, medicineId: String, time: String) {
        val alarmManager = context.getSystemService(Context.ALARM_SERVICE) as AlarmManager

        android.util.Log.d("NotificationActionReceiver", "🔕 Escalation iptal ediliyor: $medicineId @ $time")

        // Escalation 1 iptal - Aynı intent ve requestCode ile
        val esc1Intent = Intent(context, NotificationActionReceiver::class.java).apply {
            action = "ACTION_ESCALATION_1"
        }
        val esc1RequestCode = "escalation1_${medicineId}_$time".hashCode()
        val esc1PendingIntent = PendingIntent.getBroadcast(
            context,
            esc1RequestCode,
            esc1Intent,
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
                PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
            } else {
                PendingIntent.FLAG_UPDATE_CURRENT
            }
        )
        alarmManager.cancel(esc1PendingIntent)
        esc1PendingIntent.cancel()
        android.util.Log.d("NotificationActionReceiver", "✅ Escalation 1 iptal edildi: $medicineId (requestCode: $esc1RequestCode)")

        // Escalation 2 iptal
        val esc2Intent = Intent(context, NotificationActionReceiver::class.java).apply {
            action = "ACTION_ESCALATION_2"
        }
        val esc2RequestCode = "escalation2_${medicineId}_$time".hashCode()
        val esc2PendingIntent = PendingIntent.getBroadcast(
            context,
            esc2RequestCode,
            esc2Intent,
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
                PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
            } else {
                PendingIntent.FLAG_UPDATE_CURRENT
            }
        )
        alarmManager.cancel(esc2PendingIntent)
        esc2PendingIntent.cancel()
        android.util.Log.d("NotificationActionReceiver", "✅ Escalation 2 iptal edildi: $medicineId (requestCode: $esc2RequestCode)")

        // Escalation 3 iptal
        val esc3Intent = Intent(context, NotificationActionReceiver::class.java).apply {
            action = "ACTION_ESCALATION_3"
        }
        val esc3RequestCode = "escalation3_${medicineId}_$time".hashCode()
        val esc3PendingIntent = PendingIntent.getBroadcast(
            context,
            esc3RequestCode,
            esc3Intent,
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
                PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
            } else {
                PendingIntent.FLAG_UPDATE_CURRENT
            }
        )
        alarmManager.cancel(esc3PendingIntent)
        esc3PendingIntent.cancel()
        android.util.Log.d("NotificationActionReceiver", "✅ Escalation 3 iptal edildi: $medicineId (requestCode: $esc3RequestCode)")

        // 📝 Log: Tüm escalation'lar iptal edildi
        ReminderLogger.logEscalationCancelled(context, medicineId, "", time, null)

        android.util.Log.d("NotificationActionReceiver", "🚫 Tüm escalation alarmları iptal edildi: $medicineId - $time")
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

