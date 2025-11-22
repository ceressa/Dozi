package com.bardino.dozi.utils.reporting

import android.content.Context
import android.graphics.*
import android.graphics.pdf.PdfDocument
import android.util.Log
import com.bardino.dozi.core.data.repository.MedicationLogRepository
import com.bardino.dozi.core.data.repository.MedicineRepository
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.File
import java.io.FileOutputStream
import java.time.LocalDate
import java.time.format.DateTimeFormatter
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Haftalık sağlık raporu oluşturucu
 */
@Singleton
class WeeklyReportGenerator @Inject constructor(
    @ApplicationContext private val context: Context,
    private val medicationLogRepository: MedicationLogRepository,
    private val medicineRepository: MedicineRepository
) {
    companion object {
        private const val TAG = "WeeklyReportGenerator"
    }

    data class WeeklyReport(
        val startDate: LocalDate,
        val endDate: LocalDate,
        val overallCompliance: Int,
        val totalDoses: Int,
        val takenDoses: Int,
        val missedDoses: Int,
        val skippedDoses: Int,
        val bestDay: String,
        val worstDay: String,
        val mostMissedTime: String?,
        val recommendations: List<String>
    )

    /**
     * Haftalık rapor oluştur
     */
    suspend fun generateReport(userId: String): WeeklyReport {
        return withContext(Dispatchers.IO) {
            try {
                val endDate = LocalDate.now()
                val startDate = endDate.minusDays(6)

                val startTimestamp = startDate.toEpochDay() * 86400000
                val endTimestamp = (endDate.toEpochDay() + 1) * 86400000

                val logs = medicationLogRepository.getLogsBetweenDates(userId, startTimestamp, endTimestamp)
                val medicines = medicineRepository.getMedicinesForUser(userId)

                // Toplam beklenen dozları hesapla
                val totalDoses = logs.size.coerceAtLeast(1)
                val takenDoses = logs.count { it.status == "TAKEN" }
                val missedDoses = logs.count { it.status == "MISSED" }
                val skippedDoses = logs.count { it.status == "SKIPPED" }

                val overallCompliance = if (totalDoses > 0) {
                    (takenDoses * 100) / totalDoses
                } else 0

                // Günlere göre grupla
                val dayFormatter = DateTimeFormatter.ofPattern("EEEE", java.util.Locale("tr"))
                val logsByDay = logs.groupBy { log ->
                    java.time.Instant.ofEpochMilli(log.timestamp)
                        .atZone(java.time.ZoneId.systemDefault())
                        .toLocalDate()
                        .format(dayFormatter)
                }

                val complianceByDay = logsByDay.mapValues { (_, dayLogs) ->
                    if (dayLogs.isNotEmpty()) {
                        dayLogs.count { it.status == "TAKEN" } * 100 / dayLogs.size
                    } else 0
                }

                val bestDay = complianceByDay.maxByOrNull { it.value }?.key ?: "N/A"
                val worstDay = complianceByDay.minByOrNull { it.value }?.key ?: "N/A"

                // En çok kaçırılan saat
                val missedByHour = logs
                    .filter { it.status == "MISSED" }
                    .groupBy { log ->
                        java.util.Calendar.getInstance().apply {
                            timeInMillis = log.timestamp
                        }.get(java.util.Calendar.HOUR_OF_DAY)
                    }
                    .mapValues { it.value.size }

                val mostMissedTime = missedByHour.maxByOrNull { it.value }?.let { "${it.key}:00" }

                // Öneriler oluştur
                val recommendations = generateRecommendations(
                    overallCompliance,
                    mostMissedTime,
                    worstDay,
                    missedDoses
                )

                WeeklyReport(
                    startDate = startDate,
                    endDate = endDate,
                    overallCompliance = overallCompliance,
                    totalDoses = totalDoses,
                    takenDoses = takenDoses,
                    missedDoses = missedDoses,
                    skippedDoses = skippedDoses,
                    bestDay = bestDay,
                    worstDay = worstDay,
                    mostMissedTime = mostMissedTime,
                    recommendations = recommendations
                )
            } catch (e: Exception) {
                Log.e(TAG, "Error generating report", e)
                WeeklyReport(
                    startDate = LocalDate.now().minusDays(6),
                    endDate = LocalDate.now(),
                    overallCompliance = 0,
                    totalDoses = 0,
                    takenDoses = 0,
                    missedDoses = 0,
                    skippedDoses = 0,
                    bestDay = "N/A",
                    worstDay = "N/A",
                    mostMissedTime = null,
                    recommendations = listOf("Veri yüklenemedi")
                )
            }
        }
    }

    /**
     * PDF raporu oluştur
     */
    suspend fun generatePDF(report: WeeklyReport): File {
        return withContext(Dispatchers.IO) {
            val document = PdfDocument()
            val pageInfo = PdfDocument.PageInfo.Builder(595, 842, 1).create() // A4
            val page = document.startPage(pageInfo)
            val canvas = page.canvas

            // Arkaplan
            canvas.drawColor(Color.WHITE)

            // Başlık
            val titlePaint = Paint().apply {
                color = Color.parseColor("#26C6DA")
                textSize = 32f
                typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
                isAntiAlias = true
            }
            canvas.drawText("Dozi Haftalık Rapor", 50f, 80f, titlePaint)

            // Tarih aralığı
            val datePaint = Paint().apply {
                color = Color.GRAY
                textSize = 14f
                isAntiAlias = true
            }
            val dateFormatter = DateTimeFormatter.ofPattern("dd MMM yyyy", java.util.Locale("tr"))
            canvas.drawText(
                "${report.startDate.format(dateFormatter)} - ${report.endDate.format(dateFormatter)}",
                50f, 110f, datePaint
            )

            // Ana metrikler
            val metricPaint = Paint().apply {
                color = Color.BLACK
                textSize = 18f
                isAntiAlias = true
            }

            val boldPaint = Paint().apply {
                color = Color.parseColor("#26C6DA")
                textSize = 48f
                typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
                isAntiAlias = true
            }

            // Uyumluluk yüzdesi
            canvas.drawText("%${report.overallCompliance}", 50f, 200f, boldPaint)
            canvas.drawText("Genel Uyumluluk", 50f, 230f, metricPaint)

            // Doz bilgileri
            canvas.drawText("${report.takenDoses}", 250f, 200f, boldPaint)
            canvas.drawText("Alınan Doz", 250f, 230f, metricPaint)

            canvas.drawText("${report.missedDoses}", 400f, 200f, boldPaint)
            canvas.drawText("Kaçırılan Doz", 400f, 230f, metricPaint)

            // Detaylar
            var yPos = 300f
            val detailPaint = Paint().apply {
                color = Color.BLACK
                textSize = 14f
                isAntiAlias = true
            }

            canvas.drawText("En iyi gün: ${report.bestDay}", 50f, yPos, detailPaint)
            yPos += 25f
            canvas.drawText("En kötü gün: ${report.worstDay}", 50f, yPos, detailPaint)
            yPos += 25f

            report.mostMissedTime?.let {
                canvas.drawText("En çok kaçırılan saat: $it", 50f, yPos, detailPaint)
                yPos += 25f
            }

            // Öneriler
            yPos += 30f
            val sectionPaint = Paint().apply {
                color = Color.parseColor("#26C6DA")
                textSize = 20f
                typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
                isAntiAlias = true
            }
            canvas.drawText("Öneriler", 50f, yPos, sectionPaint)
            yPos += 30f

            report.recommendations.forEach { rec ->
                canvas.drawText("• $rec", 60f, yPos, detailPaint)
                yPos += 25f
            }

            // Footer
            val footerPaint = Paint().apply {
                color = Color.GRAY
                textSize = 10f
                isAntiAlias = true
            }
            canvas.drawText("Dozi - İlaç Hatırlatma Uygulaması", 50f, 800f, footerPaint)

            document.finishPage(page)

            // Dosyaya yaz
            val file = File(context.cacheDir, "dozi_weekly_report_${report.endDate}.pdf")
            FileOutputStream(file).use { out ->
                document.writeTo(out)
            }
            document.close()

            file
        }
    }

    private fun generateRecommendations(
        compliance: Int,
        mostMissedTime: String?,
        worstDay: String,
        missedCount: Int
    ): List<String> {
        val recommendations = mutableListOf<String>()

        when {
            compliance < 50 -> {
                recommendations.add("Uyumluluğunuz düşük. Hatırlatma ayarlarınızı gözden geçirin.")
            }
            compliance < 80 -> {
                recommendations.add("İyi ilerleme! Biraz daha dikkat ile mükemmel olabilirsiniz.")
            }
            else -> {
                recommendations.add("Harika! Uyumluluğunuz çok iyi durumda.")
            }
        }

        mostMissedTime?.let {
            recommendations.add("$it saatinde daha dikkatli olun.")
        }

        if (worstDay != "N/A" && missedCount > 2) {
            recommendations.add("$worstDay günleri için ek hatırlatma ekleyin.")
        }

        return recommendations
    }
}
