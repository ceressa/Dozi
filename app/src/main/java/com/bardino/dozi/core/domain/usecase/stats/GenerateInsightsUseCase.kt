package com.bardino.dozi.core.domain.usecase.stats

import android.os.Build
import androidx.annotation.RequiresApi
import com.bardino.dozi.core.data.model.MedicationStatus
import com.bardino.dozi.core.data.repository.MedicationLogRepository
import java.time.LocalDate
import javax.inject.Inject

/**
 * Kullanıcı istatistiklerinden insight üreten UseCase
 */
class GenerateInsightsUseCase @Inject constructor(
    private val medicationLogRepository: MedicationLogRepository
) {
    data class Insight(
        val type: InsightType,
        val title: String,
        val description: String,
        val severity: InsightSeverity,
        val recommendation: String?
    )

    enum class InsightType {
        TREND_IMPROVING,
        TREND_DECLINING,
        TIME_PATTERN,
        STREAK_MILESTONE,
        RISK_ALERT
    }

    enum class InsightSeverity { INFO, WARNING, CRITICAL }

    @RequiresApi(Build.VERSION_CODES.O)
    suspend operator fun invoke(userId: String): List<Insight> {
        val insights = mutableListOf<Insight>()

        try {
            val endDate = LocalDate.now()
            val startDate = endDate.minusDays(30)

            val startMillis = startDate.toEpochDay() * 86400000
            val endMillis = (endDate.toEpochDay() + 1) * 86400000

            val logs = medicationLogRepository.getLogsBetweenDates(
                userId = userId,
                startMillis = startMillis,
                endMillis = endMillis
            )

            if (logs.isEmpty()) {
                return listOf(
                    Insight(
                        type = InsightType.RISK_ALERT,
                        title = "Veri Yetersiz",
                        description = "Henüz yeterli ilaç kaydınız yok",
                        severity = InsightSeverity.INFO,
                        recommendation = "İlaçlarınızı düzenli kaydedin"
                    )
                )
            }

            // Haftalık trend
            val weeklyData = logs.groupBy { log ->
                val ts = log.scheduledTime ?: log.takenAt
                val millis = ts?.toDate()?.time ?: 0L
                (millis / (7 * 24 * 60 * 60 * 1000)).toInt()
            }

            if (weeklyData.size >= 2) {
                val weeks = weeklyData.keys.sorted()
                val lastWeek = weeklyData[weeks.last()] ?: emptyList()
                val prevWeek = weeklyData[weeks[weeks.size - 2]] ?: emptyList()

                val lastWeekCompliance = if (lastWeek.isNotEmpty()) {
                    lastWeek.count { it.status == MedicationStatus.TAKEN.name } * 100 / lastWeek.size
                } else 0

                val prevWeekCompliance = if (prevWeek.isNotEmpty()) {
                    prevWeek.count { it.status == MedicationStatus.TAKEN.name } * 100 / prevWeek.size
                } else 0

                when {
                    lastWeekCompliance < prevWeekCompliance - 10 -> {
                        insights.add(
                            Insight(
                                type = InsightType.TREND_DECLINING,
                                title = "Uyumluluk Düşüşü",
                                description = "Son hafta uyumluluğunuz %${prevWeekCompliance - lastWeekCompliance} azaldı",
                                severity = InsightSeverity.WARNING,
                                recommendation = "Hatırlatma saatlerinizi gözden geçirin"
                            )
                        )
                    }

                    lastWeekCompliance > prevWeekCompliance + 10 -> {
                        insights.add(
                            Insight(
                                type = InsightType.TREND_IMPROVING,
                                title = "Harika İlerleme!",
                                description = "Son hafta uyumluluğunuz %${lastWeekCompliance - prevWeekCompliance} arttı",
                                severity = InsightSeverity.INFO,
                                recommendation = null
                            )
                        )
                    }
                }
            }

            // Sabah / akşam pattern
            val morningLogs = logs.filter { log ->
                val ts = log.scheduledTime ?: log.takenAt
                val millis = ts?.toDate()?.time ?: return@filter false
                val cal = java.util.Calendar.getInstance().apply { timeInMillis = millis }
                val hour = cal.get(java.util.Calendar.HOUR_OF_DAY)
                hour in 6..12
            }

            val eveningLogs = logs.filter { log ->
                val ts = log.scheduledTime ?: log.takenAt
                val millis = ts?.toDate()?.time ?: return@filter false
                val cal = java.util.Calendar.getInstance().apply { timeInMillis = millis }
                val hour = cal.get(java.util.Calendar.HOUR_OF_DAY)
                hour in 18..22
            }

            val morningRate = if (morningLogs.isNotEmpty()) {
                morningLogs.count { it.status == MedicationStatus.TAKEN.name } * 100 / morningLogs.size
            } else 0

            val eveningRate = if (eveningLogs.isNotEmpty()) {
                eveningLogs.count { it.status == MedicationStatus.TAKEN.name } * 100 / eveningLogs.size
            } else 0

            if (morningLogs.isNotEmpty() && eveningLogs.isNotEmpty() && morningRate < eveningRate - 20) {
                insights.add(
                    Insight(
                        type = InsightType.TIME_PATTERN,
                        title = "Sabah Dozları Risk Altında",
                        description = "Sabah ilaçlarınızı akşama göre %${eveningRate - morningRate} daha az alıyorsunuz",
                        severity = InsightSeverity.WARNING,
                        recommendation = "Sabah rutininize ilaç almayı ekleyin"
                    )
                )
            }

            // En çok kaçırılan saat
            val missedByHour = logs
                .filter { it.status == MedicationStatus.MISSED.name }
                .groupBy { log ->
                    val ts = log.scheduledTime ?: log.takenAt
                    val millis = ts?.toDate()?.time ?: 0L
                    java.util.Calendar.getInstance().apply {
                        timeInMillis = millis
                    }.get(java.util.Calendar.HOUR_OF_DAY)
                }
                .mapValues { entry -> entry.value.size }

            val peakMissedHour = missedByHour.maxByOrNull { it.value }
            if (peakMissedHour != null && peakMissedHour.value > 3) {
                insights.add(
                    Insight(
                        type = InsightType.TIME_PATTERN,
                        title = "Kritik Saat: ${peakMissedHour.key}:00",
                        description = "Bu saatte ${peakMissedHour.value} kez ilaç kaçırdınız",
                        severity = InsightSeverity.INFO,
                        recommendation = "Bu saat için ek hatırlatma ekleyin"
                    )
                )
            }

            // Toplam TAKEN sayısına göre milestone
            val takenCount = logs.count { it.status == MedicationStatus.TAKEN.name }
            when {
                takenCount >= 100 -> {
                    insights.add(
                        Insight(
                            type = InsightType.STREAK_MILESTONE,
                            title = "100 Doz Başarısı!",
                            description = "100'den fazla dozu başarıyla aldınız",
                            severity = InsightSeverity.INFO,
                            recommendation = null
                        )
                    )
                }

                takenCount >= 50 -> {
                    insights.add(
                        Insight(
                            type = InsightType.STREAK_MILESTONE,
                            title = "50 Doz Başarısı!",
                            description = "50'den fazla dozu başarıyla aldınız",
                            severity = InsightSeverity.INFO,
                            recommendation = null
                        )
                    )
                }
            }
        } catch (e: Exception) {
            insights.add(
                Insight(
                    type = InsightType.RISK_ALERT,
                    title = "Analiz Yapılamadı",
                    description = "İstatistikler yüklenirken hata oluştu",
                    severity = InsightSeverity.INFO,
                    recommendation = null
                )
            )
        }

        return insights
    }
}
