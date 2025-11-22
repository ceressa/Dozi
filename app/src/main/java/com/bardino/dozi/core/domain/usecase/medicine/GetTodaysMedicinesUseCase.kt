package com.bardino.dozi.core.domain.usecase.medicine

import com.bardino.dozi.core.data.model.Medicine
import com.bardino.dozi.core.data.repository.MedicineRepository
import com.bardino.dozi.notifications.ReminderEngine
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import java.time.LocalDate
import javax.inject.Inject

/**
 * Bugünkü ilaçları getiren UseCase
 */
class GetTodaysMedicinesUseCase @Inject constructor(
    private val medicineRepository: MedicineRepository,
    private val reminderEngine: ReminderEngine
) {
    operator fun invoke(): Flow<List<Medicine>> {
        return medicineRepository.getMedicinesFlow()
            .map { medicines ->
                val today = LocalDate.now()
                medicines.filter { medicine ->
                    medicine.reminderEnabled &&
                    isInDateRange(medicine, today) &&
                    reminderEngine.shouldShowOnDate(medicine, today)
                }
            }
    }

    private fun isInDateRange(medicine: Medicine, date: LocalDate): Boolean {
        val startDate = java.time.Instant.ofEpochMilli(medicine.startDate)
            .atZone(java.time.ZoneId.systemDefault())
            .toLocalDate()

        if (date.isBefore(startDate)) return false

        medicine.endDate?.let { endDate ->
            val end = java.time.Instant.ofEpochMilli(endDate)
                .atZone(java.time.ZoneId.systemDefault())
                .toLocalDate()
            if (date.isAfter(end)) return false
        }

        return true
    }
}
