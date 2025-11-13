package com.bardino.dozi.core.data.repository

import com.bardino.dozi.core.data.model.Medicine
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.Query
import kotlinx.coroutines.tasks.await
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow

/**
 * Repository for managing Medicine data in Firestore
 * All medicines are stored under: /users/{userId}/medicines/{medicineId}
 */
class MedicineRepository {
    private val auth = FirebaseAuth.getInstance()
    private val db = FirebaseFirestore.getInstance()

    /**
     * Get current user's medicine collection reference
     */
    private fun getMedicinesCollection() = auth.currentUser?.let { user ->
        db.collection("users").document(user.uid).collection("medicines")
    }

    /**
     * Get all medicines for current user
     */
    suspend fun getAllMedicines(): List<Medicine> {
        val collection = getMedicinesCollection() ?: return emptyList()
        return try {
            val snapshot = collection
                .orderBy("createdAt", Query.Direction.DESCENDING)
                .get()
                .await()
            snapshot.documents.mapNotNull { it.toObject(Medicine::class.java) }
        } catch (e: Exception) {
            emptyList()
        }
    }

    /**
     * Get medicines with real-time updates
     */
    fun getMedicinesFlow(): Flow<List<Medicine>> = callbackFlow {
        val collection = getMedicinesCollection()
        if (collection == null) {
            trySend(emptyList())
            close()
            return@callbackFlow
        }

        val listener = collection
            .orderBy("createdAt", Query.Direction.DESCENDING)
            .addSnapshotListener { snapshot, error ->
                if (error != null) {
                    trySend(emptyList())
                    return@addSnapshotListener
                }
                val medicines = snapshot?.documents?.mapNotNull {
                    it.toObject(Medicine::class.java)
                } ?: emptyList()
                trySend(medicines)
            }

        awaitClose { listener.remove() }
    }

    /**
     * Get medicines for today's schedule
     */
    suspend fun getTodaysMedicines(): List<Medicine> {
        val allMedicines = getAllMedicines()
        val today = System.currentTimeMillis()
        val todayDateString = getCurrentDateString() // "dd/MM/yyyy" format

        return allMedicines.filter { medicine ->
            // Check if medicine is active today
            val isActive = medicine.startDate <= today &&
                          (medicine.endDate == null || medicine.endDate >= today)

            if (!isActive || !medicine.reminderEnabled) return@filter false

            // Check frequency-based schedule
            when (medicine.frequency) {
                "Her gün" -> true

                "Gün aşırı", "Haftada bir", "15 günde bir", "Ayda bir", "Her X günde bir" -> {
                    // Başlangıçtan bugüne kaç gün geçti?
                    val daysSinceStart = getDaysBetween(medicine.startDate, today)
                    // frequencyValue'nun katı mı kontrol et (0, frequencyValue, 2*frequencyValue, ...)
                    daysSinceStart % medicine.frequencyValue == 0L
                }

                "İstediğim tarihlerde" -> {
                    // Seçilen tarihler listesinde bugün var mı?
                    medicine.days.contains(todayDateString)
                }

                else -> {
                    // Eski sistem ile uyumluluk: days listesine bak
                    medicine.days.isEmpty() || medicine.days.contains(getCurrentDayName())
                }
            }
        }
    }

    /**
     * Helper: Calculate days between two timestamps
     */
    private fun getDaysBetween(startMillis: Long, endMillis: Long): Long {
        val millisecondsPerDay = 24 * 60 * 60 * 1000
        return (endMillis - startMillis) / millisecondsPerDay
    }

    /**
     * Helper: Get current date in dd/MM/yyyy format
     */
    private fun getCurrentDateString(): String {
        val calendar = java.util.Calendar.getInstance()
        val day = calendar.get(java.util.Calendar.DAY_OF_MONTH)
        val month = calendar.get(java.util.Calendar.MONTH) + 1
        val year = calendar.get(java.util.Calendar.YEAR)
        return "%02d/%02d/%d".format(day, month, year)
    }

    /**
     * Get a single medicine by ID
     */
    suspend fun getMedicine(medicineId: String): Medicine? {
        val collection = getMedicinesCollection() ?: return null
        return try {
            val doc = collection.document(medicineId).get().await()
            doc.toObject(Medicine::class.java)
        } catch (e: Exception) {
            null
        }
    }

    /**
     * Add a new medicine
     */
    suspend fun addMedicine(medicine: Medicine): Boolean {
        val collection = getMedicinesCollection() ?: return false
        return try {
            val user = auth.currentUser ?: return false
            val medicineWithUser = medicine.copy(
                userId = user.uid,
                id = if (medicine.id.isEmpty()) collection.document().id else medicine.id,
                createdAt = System.currentTimeMillis(),
                updatedAt = System.currentTimeMillis()
            )
            collection.document(medicineWithUser.id).set(medicineWithUser).await()
            true
        } catch (e: Exception) {
            false
        }
    }

    /**
     * Update an existing medicine
     */
    suspend fun updateMedicine(medicine: Medicine): Boolean {
        val collection = getMedicinesCollection() ?: return false
        return try {
            val updatedMedicine = medicine.copy(
                updatedAt = System.currentTimeMillis()
            )
            collection.document(medicine.id).set(updatedMedicine).await()
            true
        } catch (e: Exception) {
            false
        }
    }

    /**
     * Update a specific field of a medicine
     */
    suspend fun updateMedicineField(medicineId: String, field: String, value: Any): Boolean {
        val collection = getMedicinesCollection() ?: return false
        return try {
            collection.document(medicineId).update(
                mapOf(
                    field to value,
                    "updatedAt" to System.currentTimeMillis()
                )
            ).await()
            true
        } catch (e: Exception) {
            false
        }
    }

    /**
     * Delete a medicine
     */
    suspend fun deleteMedicine(medicineId: String): Boolean {
        val collection = getMedicinesCollection() ?: return false
        return try {
            collection.document(medicineId).delete().await()
            true
        } catch (e: Exception) {
            false
        }
    }

    /**
     * Get medicine count for user
     */
    suspend fun getMedicineCount(): Int {
        return getAllMedicines().size
    }

    /**
     * Get upcoming medicines (next 3 hours)
     */
    suspend fun getUpcomingMedicines(): List<Pair<Medicine, String>> {
        val todaysMedicines = getTodaysMedicines()
        val currentHour = java.time.LocalTime.now().hour
        val currentMinute = java.time.LocalTime.now().minute

        val upcoming = mutableListOf<Pair<Medicine, String>>()

        todaysMedicines.forEach { medicine ->
            medicine.times.forEach { time ->
                val (hour, minute) = time.split(":").map { it.toInt() }
                val medicineTime = hour * 60 + minute
                val currentTime = currentHour * 60 + currentMinute

                // If medicine time is within next 3 hours
                if (medicineTime > currentTime && medicineTime <= currentTime + 180) {
                    upcoming.add(Pair(medicine, time))
                }
            }
        }

        return upcoming.sortedBy { it.second }
    }

    /**
     * Helper: Get current day name in Turkish
     */
    private fun getCurrentDayName(): String {
        val dayOfWeek = java.time.LocalDate.now().dayOfWeek.value
        return when (dayOfWeek) {
            1 -> "Pazartesi"
            2 -> "Salı"
            3 -> "Çarşamba"
            4 -> "Perşembe"
            5 -> "Cuma"
            6 -> "Cumartesi"
            7 -> "Pazar"
            else -> ""
        }
    }
}
