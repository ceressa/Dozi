package com.bardino.dozi

import android.app.Application
import android.app.NotificationChannel
import android.app.NotificationManager
import android.graphics.Color
import android.os.Build
import android.os.Handler
import android.os.Looper

import androidx.compose.foundation.ExperimentalFoundationApi
import com.bardino.dozi.core.common.Constants.BUDDY_CHANNEL_ID
import com.bardino.dozi.core.common.Constants.REMINDER_CHANNEL_ID
import com.bardino.dozi.core.data.MedicineLookupRepository
import com.bardino.dozi.core.sync.SyncWorker
import com.bardino.dozi.notifications.NotificationHelper
import com.google.android.libraries.places.api.Places
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.FirebaseFirestoreSettings
import dagger.hilt.android.HiltAndroidApp
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

@HiltAndroidApp
class DoziApplication : Application() {

    companion object {
        var instance: DoziApplication? = null
            private set
    }

    @OptIn(ExperimentalFoundationApi::class)
    override fun onCreate() {
        super.onCreate()

        // Instance'ı kaydet
        instance = this

        if (!Places.isInitialized()) {
            Places.initialize(this, getString(R.string.google_maps_key))
        }

        // 🔥 Firestore offline persistence'ı aktif et
        FirebaseFirestore.getInstance().firestoreSettings = FirebaseFirestoreSettings.Builder()
            .setPersistenceEnabled(true)
            .build()
        android.util.Log.d("DoziApplication", "✅ Firestore offline persistence enabled")

        // 🔔 Bildirim kanallarını oluştur (kritik - hemen yapılmalı)
        createNotificationChannels()

        // 🔄 Periyodik sync worker'ı başlat (offline-first support)
        SyncWorker.schedulePeriodicSync(this)
        android.util.Log.d("DoziApplication", "✅ Periodic sync worker scheduled")

        // 💊 Lazy initialization - ilaç veritabanını 3 saniye sonra yükle
        // Bu sayede uygulama açılış süresi kısalır
        CoroutineScope(Dispatchers.IO).launch {
            delay(3000) // 3 saniye bekle
            MedicineLookupRepository.initialize(this@DoziApplication)
            android.util.Log.d("DoziApplication", "✅ Medicine lookup database loaded (lazy)")
        }
    }

    private fun createNotificationChannels() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val manager = getSystemService(NotificationManager::class.java)

            // 💧 Dozi Ana Kanal (Yeni - En öncelikli)
            NotificationHelper.createDoziChannel(this)

            // 📅 İlaç Hatırlatmaları Kanalı (Eski - Geri uyumluluk için)
            val reminderChannel = NotificationChannel(
                REMINDER_CHANNEL_ID,
                "İlaç Hatırlatmaları",
                NotificationManager.IMPORTANCE_HIGH
            ).apply {
                description = "İlaç alma zamanı hatırlatmaları"
                enableVibration(true)
                vibrationPattern = longArrayOf(0, 300, 150, 300)
                enableLights(true)
                lightColor = Color.parseColor("#4DD0E1")
                setShowBadge(true)
            }

            // 👨‍👩‍👧‍👦 Aile Bildirimleri Kanalı
            val buddyChannel = NotificationChannel(
                BUDDY_CHANNEL_ID,
                "Aile Bildirimleri",
                NotificationManager.IMPORTANCE_DEFAULT
            ).apply {
                description = "Aile üyesi bildirimleri ve güncellemeleri"
                enableVibration(true)
                setShowBadge(true)
            }

            // Tüm kanalları oluştur
            manager.createNotificationChannels(
                listOf(
                    reminderChannel,
                    buddyChannel
                )
            )
        }
    }
}
