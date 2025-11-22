package com.bardino.dozi.utils

import android.content.Context
import android.content.Intent
import android.graphics.*
import android.net.Uri
import androidx.core.content.FileProvider
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.File
import java.io.FileOutputStream
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Paylaşılabilir başarı kartları oluşturucu
 */
@Singleton
class ShareCardGenerator @Inject constructor(
    @ApplicationContext private val context: Context
) {
    data class AchievementData(
        val title: String,
        val description: String,
        val streak: Int,
        val complianceRate: Int,
        val totalDoses: Int
    )

    /**
     * Başarı kartı oluştur
     */
    suspend fun generateAchievementCard(data: AchievementData): Bitmap {
        return withContext(Dispatchers.Default) {
            val width = 1080
            val height = 1080

            val bitmap = Bitmap.createBitmap(width, height, Bitmap.Config.ARGB_8888)
            val canvas = Canvas(bitmap)

            // Gradient arkaplan
            val gradient = LinearGradient(
                0f, 0f, width.toFloat(), height.toFloat(),
                intArrayOf(
                    Color.parseColor("#26C6DA"),
                    Color.parseColor("#7C4DFF")
                ),
                null,
                Shader.TileMode.CLAMP
            )
            val backgroundPaint = Paint().apply { shader = gradient }
            canvas.drawRect(0f, 0f, width.toFloat(), height.toFloat(), backgroundPaint)

            // Yarı saydam overlay
            val overlayPaint = Paint().apply {
                color = Color.WHITE
                alpha = 30
            }
            canvas.drawRect(0f, 0f, width.toFloat(), height.toFloat(), overlayPaint)

            // Başarı ikonu (daire)
            val iconPaint = Paint().apply {
                color = Color.WHITE
                style = Paint.Style.FILL
                isAntiAlias = true
            }
            canvas.drawCircle(width / 2f, 280f, 100f, iconPaint)

            // İkon içi emoji placeholder
            val emojiPaint = Paint().apply {
                textSize = 80f
                textAlign = Paint.Align.CENTER
                isAntiAlias = true
            }
            canvas.drawText("🏆", width / 2f, 310f, emojiPaint)

            // Başlık
            val titlePaint = Paint().apply {
                color = Color.WHITE
                textSize = 56f
                typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
                textAlign = Paint.Align.CENTER
                isAntiAlias = true
            }
            canvas.drawText(data.title, width / 2f, 480f, titlePaint)

            // Açıklama
            val descPaint = Paint().apply {
                color = Color.WHITE
                textSize = 32f
                textAlign = Paint.Align.CENTER
                isAntiAlias = true
                alpha = 230
            }
            canvas.drawText(data.description, width / 2f, 540f, descPaint)

            // İstatistikler
            val statsPaint = Paint().apply {
                color = Color.WHITE
                textSize = 42f
                typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
                textAlign = Paint.Align.CENTER
                isAntiAlias = true
            }

            val statsLabelPaint = Paint().apply {
                color = Color.WHITE
                textSize = 24f
                textAlign = Paint.Align.CENTER
                isAntiAlias = true
                alpha = 200
            }

            // Sol stat - Seri
            canvas.drawText("${data.streak}", 200f, 700f, statsPaint)
            canvas.drawText("gün seri", 200f, 740f, statsLabelPaint)

            // Orta stat - Uyumluluk
            canvas.drawText("%${data.complianceRate}", width / 2f, 700f, statsPaint)
            canvas.drawText("uyumluluk", width / 2f, 740f, statsLabelPaint)

            // Sağ stat - Toplam doz
            canvas.drawText("${data.totalDoses}", 880f, 700f, statsPaint)
            canvas.drawText("doz", 880f, 740f, statsLabelPaint)

            // Dozi branding
            val brandPaint = Paint().apply {
                color = Color.WHITE
                textSize = 28f
                textAlign = Paint.Align.CENTER
                isAntiAlias = true
                alpha = 180
            }
            canvas.drawText("Dozi ile takip ediyorum", width / 2f, 950f, brandPaint)

            // Alt hashtag
            val hashtagPaint = Paint().apply {
                color = Color.WHITE
                textSize = 22f
                textAlign = Paint.Align.CENTER
                isAntiAlias = true
                alpha = 150
            }
            canvas.drawText("#Dozi #SağlıklıYaşam", width / 2f, 1000f, hashtagPaint)

            bitmap
        }
    }

    /**
     * Milestone kartı oluştur
     */
    suspend fun generateMilestoneCard(
        milestone: String,
        value: Int,
        unit: String
    ): Bitmap {
        return withContext(Dispatchers.Default) {
            val width = 1080
            val height = 1080

            val bitmap = Bitmap.createBitmap(width, height, Bitmap.Config.ARGB_8888)
            val canvas = Canvas(bitmap)

            // Gradient arkaplan
            val gradient = LinearGradient(
                0f, 0f, width.toFloat(), height.toFloat(),
                intArrayOf(
                    Color.parseColor("#4CAF50"),
                    Color.parseColor("#26C6DA")
                ),
                null,
                Shader.TileMode.CLAMP
            )
            val backgroundPaint = Paint().apply { shader = gradient }
            canvas.drawRect(0f, 0f, width.toFloat(), height.toFloat(), backgroundPaint)

            // Büyük değer
            val valuePaint = Paint().apply {
                color = Color.WHITE
                textSize = 180f
                typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
                textAlign = Paint.Align.CENTER
                isAntiAlias = true
            }
            canvas.drawText("$value", width / 2f, 500f, valuePaint)

            // Birim
            val unitPaint = Paint().apply {
                color = Color.WHITE
                textSize = 48f
                textAlign = Paint.Align.CENTER
                isAntiAlias = true
            }
            canvas.drawText(unit, width / 2f, 580f, unitPaint)

            // Milestone başlığı
            val milestonePaint = Paint().apply {
                color = Color.WHITE
                textSize = 36f
                typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
                textAlign = Paint.Align.CENTER
                isAntiAlias = true
            }
            canvas.drawText(milestone, width / 2f, 700f, milestonePaint)

            // Branding
            val brandPaint = Paint().apply {
                color = Color.WHITE
                textSize = 28f
                textAlign = Paint.Align.CENTER
                isAntiAlias = true
                alpha = 180
            }
            canvas.drawText("Dozi ile başardım!", width / 2f, 950f, brandPaint)

            bitmap
        }
    }

    /**
     * Kartı paylaş
     */
    fun shareCard(bitmap: Bitmap) {
        val uri = saveBitmapToCache(bitmap)

        val shareIntent = Intent(Intent.ACTION_SEND).apply {
            type = "image/png"
            putExtra(Intent.EXTRA_STREAM, uri)
            putExtra(Intent.EXTRA_TEXT, "Dozi ile ilaçlarımı takip ediyorum! #Dozi #SağlıklıYaşam")
            addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
            addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        }

        context.startActivity(Intent.createChooser(shareIntent, "Paylaş").apply {
            addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        })
    }

    private fun saveBitmapToCache(bitmap: Bitmap): Uri {
        val cachePath = File(context.cacheDir, "shared_images")
        cachePath.mkdirs()

        val file = File(cachePath, "dozi_achievement_${System.currentTimeMillis()}.png")
        FileOutputStream(file).use { out ->
            bitmap.compress(Bitmap.CompressFormat.PNG, 100, out)
        }

        return FileProvider.getUriForFile(
            context,
            "${context.packageName}.fileprovider",
            file
        )
    }
}
