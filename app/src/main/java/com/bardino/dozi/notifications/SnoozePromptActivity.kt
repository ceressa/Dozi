package com.bardino.dozi.notifications

import android.app.AlertDialog
import android.content.Context
import android.content.Intent
import android.os.Bundle
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.lifecycle.lifecycleScope
import com.bardino.dozi.MainActivity
import com.bardino.dozi.core.data.model.User
import com.bardino.dozi.core.data.repository.UserPreferencesRepository
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import kotlinx.coroutines.launch
import kotlinx.coroutines.tasks.await

class SnoozePromptActivity : ComponentActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        val medName = intent.getStringExtra("medicine") ?: "İlaç"
        val medicineId = intent.getStringExtra("medicineId") ?: ""
        val scheduledTime = intent.getLongExtra("scheduledTime", System.currentTimeMillis())

        showSmartSnoozeDialog(medName, medicineId, scheduledTime)
    }

    private fun showSmartSnoozeDialog(medicineName: String, medicineId: String, scheduledTime: Long) {
        lifecycleScope.launch {
            // 🔐 Kullanıcı ayarlarını kontrol et
            val user = getUserSettings()
            val smartReminderEnabled = user?.smartReminderEnabled ?: false

            // 🧠 Akıllı öneriler al (eğer kullanıcı aktif ettiyse)
            val suggestedTimes = if (smartReminderEnabled) {
                SmartReminderHelper.getSuggestedSnoozeTimes(this@SnoozePromptActivity, medicineId)
            } else {
                // Default seçenekler (akıllı öneri yok)
                listOf(
                    10 to "10 dakika",
                    20 to "20 dakika",
                    30 to "30 dakika",
                    60 to "1 saat"
                )
            }
            val times = suggestedTimes.map { it.second }.toTypedArray()
            val minutes = suggestedTimes.map { it.first }.toIntArray()

            // 🧠 Zamanı değiştirme önerisi al (eğer kullanıcı aktif ettiyse)
            val (newTime, timeSuggestion) = if (smartReminderEnabled) {
                SmartReminderHelper.getTimeAdjustmentSuggestion(
                    this@SnoozePromptActivity,
                    medicineId,
                    intent.getStringExtra("time") ?: "09:00"
                )
            } else {
                null to null
            }

            var selectedIndex = 0

            val builder = AlertDialog.Builder(this@SnoozePromptActivity)
            builder.setTitle("Tamam, ne kadar erteleyelim peki?")

            // 💡 Eğer zamanı değiştirme önerisi varsa mesaj olarak göster
            if (timeSuggestion != null) {
                builder.setMessage("💡 $timeSuggestion")
            }

            builder.setSingleChoiceItems(times, 0) { _, which ->
                selectedIndex = which
            }

            builder.setPositiveButton("Ertele") { dialog, _ ->
                val min = minutes[selectedIndex]
                val currentTime = System.currentTimeMillis()

                // ✅ Erteleme başlangıç zamanı: İlaç saati veya şu an (hangisi daha geç ise)
                val snoozeFromTime = maxOf(scheduledTime, currentTime)
                val snoozeUntilTime = snoozeFromTime + min * 60_000L

                // ✅ Erteleme planla (tüm parametrelerle)
                NotificationHelper.scheduleSnooze(
                    context = this@SnoozePromptActivity,
                    medicineName = medicineName,
                    medicineId = medicineId,
                    dosage = intent.getStringExtra("dosage") ?: "",
                    time = intent.getStringExtra("time") ?: "",
                    scheduledTime = scheduledTime,
                    minutes = min
                )

                // ✅ SharedPreferences'a timestamp ile kaydet
                val snoozeUntil = currentTime + min * 60_000L
                getSharedPreferences("dozi_prefs", Context.MODE_PRIVATE).edit()
                    .putString("last_action", "ERTELENDI:$medicineName:$min dk")
                    .putLong("snooze_until", snoozeUntilTime)
                    .putInt("snooze_minutes", min)
                    .putLong("snooze_timestamp", currentTime)
                    .putLong("snooze_from", snoozeFromTime)
                    .apply()

                // ✅ Firebase'e senkronize et
                lifecycleScope.launch {
                    val userPrefsRepo = UserPreferencesRepository(this@SnoozePromptActivity)
                    userPrefsRepo.syncSnoozeData(
                        snoozeMinutes = min,
                        snoozeUntil = snoozeUntil,
                        snoozeTimestamp = currentTime,
                        medicineId = medicineId
                    )
                }

                // 🧠 Pattern'i kaydet (gelecekteki öneriler için - eğer kullanıcı aktif ettiyse)
                if (smartReminderEnabled) {
                    lifecycleScope.launch {
                        SmartReminderHelper.recordSnoozePattern(
                            this@SnoozePromptActivity,
                            medicineId,
                            min,
                            scheduledTime
                        )
                    }
                }

                // Hatırlatma saatini hesapla ve göster
                val calendar = java.util.Calendar.getInstance().apply {
                    timeInMillis = snoozeUntilTime
                }
                val snoozeHour = calendar.get(java.util.Calendar.HOUR_OF_DAY)
                val snoozeMinute = calendar.get(java.util.Calendar.MINUTE)
                val formattedTime = String.format("%02d:%02d", snoozeHour, snoozeMinute)

                Toast.makeText(
                    this@SnoozePromptActivity,
                    "$medicineName saat $formattedTime'de hatırlatılacak ⏰",
                    Toast.LENGTH_LONG
                ).show()

                // ✅ Ana sayfaya dön (finish'ten ÖNCE)
                val intent = Intent(this@SnoozePromptActivity, MainActivity::class.java).apply {
                    addFlags(Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK)
                }
                startActivity(intent)

                dialog.dismiss()
                finish() // ✅ En sona taşındı
            }

            // 💡 "Zamanı Değiştir" butonu ekle (eğer öneri varsa)
            if (newTime != null && timeSuggestion != null) {
                builder.setNeutralButton("Zamanı Değiştir") { dialog, _ ->
                    // İlacın hatırlatma zamanını değiştirmek için EditReminder ekranına yönlendir
                    val editIntent = Intent(this@SnoozePromptActivity, MainActivity::class.java).apply {
                        addFlags(Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK)
                        putExtra("navigate_to", "edit_reminder")
                        putExtra("medicine_id", medicineId)
                    }
                    startActivity(editIntent)
                    dialog.dismiss()
                    finish()
                }
            }

            builder.setCancelable(false)

            val dialog = builder.create()
            dialog.show()
        }
    }

    /**
     * Kullanıcı ayarlarını Firestore'dan al
     */
    private suspend fun getUserSettings(): User? {
        return try {
            val currentUser = FirebaseAuth.getInstance().currentUser ?: return null
            val db = FirebaseFirestore.getInstance()
            val doc = db.collection("users").document(currentUser.uid).get().await()
            doc.toObject(User::class.java)
        } catch (e: Exception) {
            android.util.Log.e("SnoozePromptActivity", "Error getting user settings", e)
            null
        }
    }
}