package com.bardino.dozi.core.utils

import android.content.Context
import android.media.MediaPlayer
import com.bardino.dozi.R
import com.bardino.dozi.core.data.repository.UserRepository
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

/**
 * Ses dosyalarını kullanıcının seçimine göre çalan helper sınıfı
 *
 * Kullanım:
 * SoundHelper.playSound(context, SoundType.HARIKA)
 */
object SoundHelper {

    private val userRepository = UserRepository()
    private var currentMediaPlayer: MediaPlayer? = null

    enum class SoundType {
        ERTELE,
        HARIKA,
        HERSEY_TAMAM,
        PEKALA,
        REMINDER_1,
        REMINDER_2,
        REMINDER_3,
        REMINDER_4,
        REMINDER_5,
        SAMPLE
    }

    /**
     * Kullanıcının ses seçimine göre uygun ses dosyasını çalar
     */
    fun playSound(context: Context, soundType: SoundType) {
        CoroutineScope(Dispatchers.IO).launch {
            try {
                // Kullanıcının ses tercihini al
                val userData = userRepository.getUserData()
                val voiceGender = userData?.voiceGender ?: "erkek"

                // Ses dosyası ID'sini belirle
                val soundResId = getSoundResourceId(soundType, voiceGender)

                // Sesi çal
                withContext(Dispatchers.Main) {
                    playRawSound(context, soundResId)
                }
            } catch (e: Exception) {
                e.printStackTrace()
                // Hata durumunda default erkek sesi çal
                withContext(Dispatchers.Main) {
                    playRawSound(context, getSoundResourceId(soundType, "erkek"))
                }
            }
        }
    }

    /**
     * Belirli bir cinsiyet için örnek ses çalar (ayarlar ekranı için)
     * Önceki örnek ses çalıyorsa önce onu durdurur
     */
    fun playSampleSound(context: Context, voiceGender: String) {
        try {
            val soundResId = if (voiceGender == "erkek") {
                R.raw.dozi_erkek_sample
            } else {
                R.raw.dozi_kadin_sample
            }
            playRawSound(context, soundResId)
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

    /**
     * Ses tipi ve cinsiyete göre resource ID döndürür
     */
    private fun getSoundResourceId(soundType: SoundType, voiceGender: String): Int {
        return when (soundType) {
            SoundType.ERTELE -> {
                if (voiceGender == "erkek") R.raw.dozi_erkek_ertele
                else R.raw.dozi_kadin_ertele
            }
            SoundType.HARIKA -> {
                if (voiceGender == "erkek") R.raw.dozi_erkek_harika
                else R.raw.dozi_kadin_harika
            }
            SoundType.HERSEY_TAMAM -> {
                if (voiceGender == "erkek") R.raw.dozi_erkek_herseytamam
                else R.raw.dozi_kadin_herseytamam
            }
            SoundType.PEKALA -> {
                if (voiceGender == "erkek") R.raw.dozi_erkek_pekala
                else R.raw.dozi_kadin_pekala
            }
            SoundType.REMINDER_1 -> {
                if (voiceGender == "erkek") R.raw.dozi_erkek_reminder1
                else R.raw.dozi_kadin_reminder1
            }
            SoundType.REMINDER_2 -> {
                if (voiceGender == "erkek") R.raw.dozi_erkek_reminder2
                else R.raw.dozi_kadin_reminder2
            }
            SoundType.REMINDER_3 -> {
                if (voiceGender == "erkek") R.raw.dozi_erkek_reminder3
                else R.raw.dozi_kadin_reminder3
            }
            SoundType.REMINDER_4 -> {
                if (voiceGender == "erkek") R.raw.dozi_erkek_reminder4
                else R.raw.dozi_kadin_reminder4
            }
            SoundType.REMINDER_5 -> {
                if (voiceGender == "erkek") R.raw.dozi_erkek_reminder5
                else R.raw.dozi_kadin_reminder5
            }
            SoundType.SAMPLE -> {
                if (voiceGender == "erkek") R.raw.dozi_erkek_sample
                else R.raw.dozi_kadin_sample
            }
        }
    }

    /**
     * Raw resource'tan ses çalar
     * Önceki ses çalıyorsa önce onu durdurur
     */
    private fun playRawSound(context: Context, soundResId: Int) {
        try {
            // Önceki sesi durdur ve temizle
            stopCurrentSound()

            // Yeni sesi çal
            val player = MediaPlayer.create(context, soundResId)
            player?.setOnCompletionListener {
                it.release()
                if (currentMediaPlayer == it) {
                    currentMediaPlayer = null
                }
            }
            currentMediaPlayer = player
            player?.start()
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

    /**
     * Çalan sesi durdurur ve temizler
     */
    fun stopCurrentSound() {
        try {
            currentMediaPlayer?.apply {
                if (isPlaying) {
                    stop()
                }
                release()
            }
            currentMediaPlayer = null
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }
}
