package com.bardino.dozi.core.domain.usecase.medicine

import com.bardino.dozi.core.data.model.Medicine
import com.bardino.dozi.notifications.ReminderEngine
import java.time.LocalDate
import javax.inject.Inject

/**
 * Belirli bir tarih için ilaçları filtreleyen UseCase
 */
class FilterMedicinesByDateUseCase @Inject constructor(
    private val reminderEngine: ReminderEngine
) {
    operator fun invoke(medicines: List<Medicine>, date: LocalDate): List<Medicine> {
        return medicines.filter { medicine ->
            medicine.reminderEnabled && reminderEngine.shouldShowOnDate(medicine, date)
        }
    }
}
