package com.bardino.dozi.notifications

import android.Manifest
import android.app.AlertDialog
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
import com.bardino.dozi.R
import com.bardino.dozi.notifications.NotificationHelper
import com.bardino.dozi.core.utils.SoundHelper
import com.bardino.dozi.core.data.repository.BadiRepository
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import java.util.*
import kotlin.random.Random

class NotificationActionReceiver : BroadcastReceiver() {

    private var tts: TextToSpeech? = null

    @RequiresPermission(Manifest.permission.POST_NOTIFICATIONS)
    override fun onReceive(context: Context, intent: Intent) {
        val med = intent.getStringExtra(NotificationHelper.EXTRA_MEDICINE) ?: "İlaç"
        val time = intent.getStringExtra(NotificationHelper.EXTRA_TIME) ?: "Bilinmiyor"
        val prefs = context.getSharedPreferences("dozi_prefs", Context.MODE_PRIVATE)
        val nm = NotificationManagerCompat.from(context)

        when (intent.action) {
            NotificationHelper.ACTION_TAKEN -> handleTaken(context, med, time, prefs, nm)
            NotificationHelper.ACTION_SKIP -> handleSkip(context, med, prefs, nm)
            NotificationHelper.ACTION_SNOOZE -> handleSnooze(context, med, nm)
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
                // Erteleme süresi doldu, yeni bildirim göster
                if (hasNotificationPermission(context)) {
                    NotificationHelper.showMedicationNotification(context, med)
                }
            }
        }
    }

    private fun handleTaken(
        context: Context,
        medicineName: String,
        time: String,
        prefs: SharedPreferences,
        nm: NotificationManagerCompat
    ) {
        prefs.edit {
            putString("last_action", "ALINDI:$medicineName")
            putLong("last_taken_time", System.currentTimeMillis())
            putString("last_medicine", medicineName)
        }

        nm.cancel(NotificationHelper.NOTIF_ID)

        showToast(context, "$medicineName alındı olarak işaretlendi ✅")

        // ✅ Kullanıcının ses seçimine göre başarı sesi
        SoundHelper.playSound(context, SoundHelper.SoundType.HERSEY_TAMAM)
    }


    private fun handleSkip(
        context: Context,
        medicineName: String,
        prefs: SharedPreferences,
        nm: NotificationManagerCompat
    ) {
        prefs.edit {
            putString("last_action", "ATLANDI:$medicineName")
            putLong("last_skip_time", System.currentTimeMillis())
        }

        nm.cancel(NotificationHelper.NOTIF_ID)

        showToast(context, "$medicineName atlandı 🚫")

        // ✅ Kullanıcının ses seçimine göre atla sesi
        SoundHelper.playSound(context, SoundHelper.SoundType.PEKALA)
    }


    private fun handleSnooze(
        context: Context,
        medicineName: String,
        nm: NotificationManagerCompat
    ) {
        nm.cancel(NotificationHelper.NOTIF_ID)

        // ✅ Kullanıcının ses seçimine göre erteleme sesi
        SoundHelper.playSound(context, SoundHelper.SoundType.ERTELE)

        // ✅ Yeni: Dialog yerine Activity başlat
        val intent = Intent(context, SnoozePromptActivity::class.java).apply {
            putExtra("medicine", medicineName)
            addFlags(Intent.FLAG_ACTIVITY_NEW_TASK) // 🔥 Önemli!
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



    private fun showSmartSnoozeDialog(context: Context, medicineName: String) {
        val times = arrayOf("5 dakika", "15 dakika", "30 dakika", "1 saat")
        val minutes = arrayOf(5, 15, 30, 60)

        val builder = AlertDialog.Builder(context).apply {
            setTitle("💧 Dozi - Erteleme")
            setMessage("$medicineName için ne kadar sonra hatırlatayım?")
            setItems(times) { dialog, which ->
                val min = minutes[which]

                NotificationHelper.scheduleSnooze(context, medicineName, min)

                context.getSharedPreferences("dozi_prefs", Context.MODE_PRIVATE).edit {
                    putString("last_action", "ERTELENDI:$medicineName:$min dk")
                    putLong("snooze_until", System.currentTimeMillis() + min * 60_000L)
                }

                dialog.dismiss()
            }
            setNegativeButton("İptal") { dialog, _ ->
                if (hasNotificationPermission(context)) {
                    NotificationHelper.showMedicationNotification(context, medicineName)
                }
                dialog.dismiss()
            }
        }

        val dialog = builder.create()
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            dialog.window?.setType(WindowManager.LayoutParams.TYPE_APPLICATION_OVERLAY)
        } else {
            @Suppress("DEPRECATION")
            dialog.window?.setType(WindowManager.LayoutParams.TYPE_SYSTEM_ALERT)
        }

        dialog.show()
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

    override fun toString(): String = "NotificationActionReceiver - Dozi Bildirim İşleyici"
}

private fun listAvailableVoices(tts: TextToSpeech) {
    tts.voices?.forEach { voice ->
        println("Ses adı: ${voice.name}, locale: ${voice.locale}, quality: ${voice.quality}, latency: ${voice.latency}")
    }
}

