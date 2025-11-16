package com.bardino.dozi

import android.app.Application
import android.app.NotificationChannel
import android.app.NotificationManager
import android.graphics.Color
import android.os.Build

import androidx.compose.foundation.ExperimentalFoundationApi
import com.bardino.dozi.core.common.Constants.BUDDY_CHANNEL_ID
import com.bardino.dozi.core.common.Constants.REMINDER_CHANNEL_ID
import com.bardino.dozi.core.data.MedicineRepository
import com.bardino.dozi.core.profile.ProfileManager
import com.bardino.dozi.notifications.NotificationHelper
import com.bardino.dozi.core.data.repository.MedicineRepository as FirebaseMedicineRepository
import com.bardino.dozi.core.data.repository.UserRepository
import com.google.android.libraries.places.api.Places
import dagger.hilt.android.HiltAndroidApp
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltAndroidApp
class DoziApplication : Application() {

    @Inject
    lateinit var profileManager: ProfileManager

    @Inject
    lateinit var firebaseMedicineRepository: FirebaseMedicineRepository

    @Inject
    lateinit var userRepository: UserRepository

    private val applicationScope = CoroutineScope(SupervisorJob() + Dispatchers.Main)

    @OptIn(ExperimentalFoundationApi::class)
    override fun onCreate() {
        super.onCreate()

        if (!Places.isInitialized()) {
            Places.initialize(this, getString(R.string.google_maps_key))
        }

        // 💊 Uygulama açıldığında ilaç veritabanını belleğe yükle
        MedicineRepository.initialize(this)

        // 👥 Default profil oluştur (eğer yoksa) - kullanıcının adını kullan
        // Not: Firestore sync MainActivity'de yapılıyor (kullanıcı login olduktan sonra)
        applicationScope.launch {
            val defaultProfileId = profileManager.ensureDefaultProfile(userRepository)

            // 🔧 MIGRATION: Assign profile-specific reminders (set ownerProfileId to default profile)
            val prefs = getSharedPreferences("dozi_migrations", MODE_PRIVATE)
            val migrationDone = prefs.getBoolean("profile_reminders_migration_v4", false)

            if (!migrationDone) {
                android.util.Log.d("DoziApplication", "🔧 Starting profile-specific reminders migration v4...")
                val migratedCount = firebaseMedicineRepository.migrateOldMedicines(defaultProfileId)
                if (migratedCount >= 0) {
                    prefs.edit().putBoolean("profile_reminders_migration_v4", true).apply()
                    android.util.Log.d("DoziApplication", "✅ Profile-specific reminders migration v4 completed: $migratedCount medicines migrated")
                }
            }
        }

        // 🔔 Bildirim kanallarını oluştur
        createNotificationChannels()
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
