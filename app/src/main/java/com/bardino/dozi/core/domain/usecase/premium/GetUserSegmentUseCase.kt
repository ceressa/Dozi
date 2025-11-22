package com.bardino.dozi.core.domain.usecase.premium

import com.bardino.dozi.core.data.repository.BadiRepository
import com.bardino.dozi.core.data.repository.MedicationLogRepository
import com.bardino.dozi.core.data.repository.MedicineRepository
import javax.inject.Inject

/**
 * Kullanıcı segmentini belirleyen UseCase
 * Premium kişiselleştirme için kullanılır
 */
class GetUserSegmentUseCase @Inject constructor(
    private val medicineRepository: MedicineRepository,
    private val medicationLogRepository: MedicationLogRepository,
    private val badiRepository: BadiRepository
) {
    enum class UserSegment {
        HIGH_FREQUENCY,    // 5+ ilaç veya 10+ günlük hatırlatma
        FAMILY_USER,       // 2+ aktif badi
        CHRONIC_USER,      // Uzun süreli ilaç (90+ gün)
        VITAMIN_USER,      // Sadece takviye/vitamin
        NEW_USER           // <7 gün kullanım
    }

    suspend operator fun invoke(userId: String): UserSegment {
        return try {
            val medicines = medicineRepository.getMedicinesForUser(userId)
            val badis = badiRepository.getBadisForUser(userId)
            val daysSinceFirstLog = medicationLogRepository.getDaysSinceFirstLog(userId)

            when {
                daysSinceFirstLog < 7 -> UserSegment.NEW_USER

                badis.size >= 2 -> UserSegment.FAMILY_USER

                medicines.any { medicine ->
                    val start = medicine.startDate
                    val end = medicine.endDate
                    end == null || (end - start) > 90L * 24 * 60 * 60 * 1000
                } -> UserSegment.CHRONIC_USER

                medicines.sumOf { it.times.size } >= 10 -> UserSegment.HIGH_FREQUENCY

                medicines.isNotEmpty() && medicines.all { medicine ->
                    val n = medicine.name.lowercase()
                    n.contains("vitamin") ||
                            n.contains("takviye") ||
                            n.contains("c vitamini") ||
                            n.contains("d vitamini")
                } -> UserSegment.VITAMIN_USER

                else -> UserSegment.NEW_USER
            }
        } catch (e: Exception) {
            UserSegment.NEW_USER
        }
    }
}
