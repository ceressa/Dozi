package com.bardino.dozi.notifications

import android.app.AlertDialog
import android.content.Context
import android.content.Intent
import android.os.Bundle
import android.widget.Toast
import androidx.activity.ComponentActivity
import com.bardino.dozi.MainActivity
import com.bardino.dozi.notifications.NotificationHelper

class SnoozePromptActivity : ComponentActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        val medName = intent.getStringExtra("medicine") ?: "İlaç"
        showSmartSnoozeDialog(medName)
    }

    private fun showSmartSnoozeDialog(medicineName: String) {
        val times = arrayOf("10 dakika", "20 dakika", "30 dakika", "1 saat")
        val minutes = arrayOf(10, 20, 30, 60)
        var selectedIndex = 0

        val builder = AlertDialog.Builder(this)
        builder.setTitle("Tamam, ne kadar erteleyelim peki?")

        builder.setSingleChoiceItems(times, 0) { _, which ->
            selectedIndex = which
        }

        builder.setPositiveButton("Ertele") { dialog, _ ->
            val min = minutes[selectedIndex]
            val currentTime = System.currentTimeMillis()

            // ✅ Erteleme planla
            NotificationHelper.scheduleSnooze(this, medicineName, min)

            // ✅ SharedPreferences'a timestamp ile kaydet
            getSharedPreferences("dozi_prefs", Context.MODE_PRIVATE).edit()
                .putString("last_action", "ERTELENDI:$medicineName:$min dk")
                .putLong("snooze_until", currentTime + min * 60_000L)
                .putInt("snooze_minutes", min)
                .putLong("snooze_timestamp", currentTime)
                .apply()

            Toast.makeText(
                this,
                "$medicineName $min dakika sonra hatırlatılacak ⏰",
                Toast.LENGTH_LONG
            ).show()

            // ✅ Ana sayfaya dön (finish'ten ÖNCE)
            val intent = Intent(this, MainActivity::class.java).apply {
                addFlags(Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK)
            }
            startActivity(intent)

            dialog.dismiss()
            finish() // ✅ En sona taşındı
        }

        builder.setCancelable(false)

        val dialog = builder.create()
        dialog.show()
    }
}