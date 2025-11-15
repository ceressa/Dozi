package com.bardino.dozi.notifications

import android.app.AlertDialog
import android.content.Context
import android.content.Intent
import android.os.Bundle
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.lifecycle.lifecycleScope
import com.bardino.dozi.MainActivity
import com.bardino.dozi.notifications.NotificationHelper
import kotlinx.coroutines.launch

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
            // 🧠 Akıllı öneriler al
            val suggestedTimes = SmartReminderHelper.getSuggestedSnoozeTimes(this@SnoozePromptActivity, medicineId)
            val times = suggestedTimes.map { it.second }.toTypedArray()
            val minutes = suggestedTimes.map { it.first }.toIntArray()

            // 🧠 Zamanı değiştirme önerisi al
            val (newTime, timeSuggestion) = SmartReminderHelper.getTimeAdjustmentSuggestion(
                this@SnoozePromptActivity,
                medicineId,
                intent.getStringExtra("time") ?: "09:00"
            )

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

                // ✅ Erteleme planla
                NotificationHelper.scheduleSnooze(this@SnoozePromptActivity, medicineName, min)

                // ✅ SharedPreferences'a timestamp ile kaydet
                getSharedPreferences("dozi_prefs", Context.MODE_PRIVATE).edit()
                    .putString("last_action", "ERTELENDI:$medicineName:$min dk")
                    .putLong("snooze_until", currentTime + min * 60_000L)
                    .putInt("snooze_minutes", min)
                    .putLong("snooze_timestamp", currentTime)
                    .apply()

                // 🧠 Pattern'i kaydet (gelecekteki öneriler için)
                lifecycleScope.launch {
                    SmartReminderHelper.recordSnoozePattern(
                        this@SnoozePromptActivity,
                        medicineId,
                        min,
                        scheduledTime
                    )
                }

                Toast.makeText(
                    this@SnoozePromptActivity,
                    "$medicineName $min dakika sonra hatırlatılacak ⏰",
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
                    // TODO: İlacın hatırlatma zamanını değiştirmek için ayarlar ekranına yönlendir
                    Toast.makeText(
                        this@SnoozePromptActivity,
                        "Bu özellik yakında eklenecek! Şimdilik hatırlatmayı erteleyebilirsiniz.",
                        Toast.LENGTH_SHORT
                    ).show()
                }
            }

            builder.setCancelable(false)

            val dialog = builder.create()
            dialog.show()
        }
    }
}