package com.bardino.dozi.core.data.repository

import android.content.Context
import android.os.Build
import android.util.Log
import androidx.annotation.RequiresApi
import com.bardino.dozi.core.data.model.Medicine
import com.bardino.dozi.core.sync.SyncManager
import com.bardino.dozi.core.sync.SyncWorker
import com.bardino.dozi.core.utils.NetworkUtils
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.Query
import kotlinx.coroutines.tasks.await
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow
import kotlinx.coroutines.flow.flowOf
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Repository for managing Medicine data in Firestore
 * All medicines are stored under: /users/{userId}/medicines/{medicineId}
 */
@Singleton
class MedicineRepository @Inject constructor() {
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
            android.util.Log.e("MedicineRepository", "❌ Error getting medicines: ${e.message}")
            emptyList()
        }
    }

    /**
     * Get medicines with real-time updates
     */
    fun getMedicinesFlow(): Flow<List<Medicine>> {
        val collection = getMedicinesCollection()
        if (collection == null) {
            return flowOf(emptyList())
        }

        return callbackFlow {
            val listener = collection
                .orderBy("createdAt", Query.Direction.DESCENDING)
                .addSnapshotListener { snapshot, error ->
                    if (error != null) {
                        android.util.Log.e("MedicineRepository", "Error listening to medicines: ${error.message}")
                        trySend(emptyList())
                        return@addSnapshotListener
                    }

                    val medicines = snapshot?.documents?.mapNotNull {
                        it.toObject(Medicine::class.java)
                    } ?: emptyList()

                    android.util.Log.d("MedicineRepository", "✅ Loaded ${medicines.size} medicines")
                    trySend(medicines)
                }

            awaitClose { listener.remove() }
        }
    }

    /**
     * Get medicines for today's schedule
     */
    @RequiresApi(Build.VERSION_CODES.O)
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

                "Gün aşırı" -> {
                    // Başlangıçtan bugüne kaç gün geçti?
                    val daysSinceStart = getDaysBetween(medicine.startDate, today)
                    (daysSinceStart % 2).toInt() == 0 // Çift günlerde al (0, 2, 4, ...)
                }

                "Haftada bir" -> {
                    // Başlangıç tarihinin haftanın günü ile aynı günlerde al
                    val startCal = java.util.Calendar.getInstance().apply { timeInMillis = medicine.startDate }
                    val todayCal = java.util.Calendar.getInstance().apply { timeInMillis = today }
                    startCal.get(java.util.Calendar.DAY_OF_WEEK) == todayCal.get(java.util.Calendar.DAY_OF_WEEK)
                }

                "15 günde bir" -> {
                    val daysSinceStart = getDaysBetween(medicine.startDate, today)
                    (daysSinceStart % 15).toInt() == 0
                }

                "Ayda bir" -> {
                    val daysSinceStart = getDaysBetween(medicine.startDate, today)
                    (daysSinceStart % 30).toInt() == 0
                }

                "Her X günde bir" -> {
                    val daysSinceStart = getDaysBetween(medicine.startDate, today)
                    (daysSinceStart % medicine.frequencyValue).toInt() == 0
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
        android.util.Log.d("MedicineRepository", "getMedicine called with ID: $medicineId")

        val collection = getMedicinesCollection()
        if (collection == null) {
            android.util.Log.e("MedicineRepository", "getMedicine: User not authenticated!")
            return null
        }

        return try {
            android.util.Log.d("MedicineRepository", "Fetching medicine from Firestore...")
            val doc = collection.document(medicineId).get().await()

            if (!doc.exists()) {
                android.util.Log.w("MedicineRepository", "Medicine document does not exist: $medicineId")
                return null
            }

            val medicine = doc.toObject(Medicine::class.java)
            android.util.Log.d("MedicineRepository", "Medicine fetched successfully: ${medicine?.name}")
            medicine
        } catch (e: Exception) {
            android.util.Log.e("MedicineRepository", "Error fetching medicine: $medicineId", e)
            null
        }
    }

    /**
     * Get a single medicine by ID (alias for compatibility)
     */
    suspend fun getMedicineById(medicineId: String): Medicine? {
        return getMedicine(medicineId)
    }

    /**
     * Add a new medicine
     * Uses offline-first approach: saves to Firestore if online, queues if offline
     */
    suspend fun addMedicine(medicine: Medicine, context: Context? = null): Medicine? {
        val collection = getMedicinesCollection() ?: return null
        val user = auth.currentUser ?: return null

        val medicineWithUser = medicine.copy(
            userId = user.uid,
            id = if (medicine.id.isEmpty()) collection.document().id else medicine.id,
            createdAt = System.currentTimeMillis(),
            updatedAt = System.currentTimeMillis()
        )

        // Try direct Firestore write if online
        if (context != null && NetworkUtils.isNetworkAvailable(context)) {
            return try {
                collection.document(medicineWithUser.id).set(medicineWithUser).await()
                Log.d("MedicineRepository", "✅ Medicine added directly to Firestore: ${medicineWithUser.id}")
                medicineWithUser
            } catch (e: Exception) {
                Log.w("MedicineRepository", "⚠️ Firestore write failed, queueing for sync", e)
                // Queue for later sync
                val syncManager = SyncManager(context)
                syncManager.queueMedicineAdd(medicineWithUser)
                SyncWorker.requestImmediateSync(context)
                medicineWithUser
            }
        } else if (context != null) {
            // Offline - queue for sync
            Log.d("MedicineRepository", "📥 Offline - queueing medicine add: ${medicineWithUser.id}")
            val syncManager = SyncManager(context)
            syncManager.queueMedicineAdd(medicineWithUser)
            SyncWorker.requestImmediateSync(context)
            return medicineWithUser
        } else {
            // No context - fall back to direct write (legacy behavior)
            return try {
                collection.document(medicineWithUser.id).set(medicineWithUser).await()
                Log.d("MedicineRepository", "✅ Medicine added: ${medicineWithUser.id}")
                medicineWithUser
            } catch (e: Exception) {
                Log.e("MedicineRepository", "❌ Error adding medicine", e)
                null
            }
        }
    }

    /**
     * Update an existing medicine
     * Uses offline-first approach: saves to Firestore if online, queues if offline
     */
    suspend fun updateMedicine(medicine: Medicine, context: Context? = null): Boolean {
        val collection = getMedicinesCollection() ?: return false

        val updatedMedicine = medicine.copy(
            updatedAt = System.currentTimeMillis()
        )

        // Try direct Firestore write if online
        if (context != null && NetworkUtils.isNetworkAvailable(context)) {
            return try {
                collection.document(medicine.id).set(updatedMedicine).await()
                Log.d("MedicineRepository", "✅ Medicine updated directly in Firestore: ${medicine.id}")
                true
            } catch (e: Exception) {
                Log.w("MedicineRepository", "⚠️ Firestore update failed, queueing for sync", e)
                // Queue for later sync
                val syncManager = SyncManager(context)
                syncManager.queueMedicineUpdate(updatedMedicine)
                SyncWorker.requestImmediateSync(context)
                true // Return true since we've queued the update
            }
        } else if (context != null) {
            // Offline - queue for sync
            Log.d("MedicineRepository", "📥 Offline - queueing medicine update: ${medicine.id}")
            val syncManager = SyncManager(context)
            syncManager.queueMedicineUpdate(updatedMedicine)
            SyncWorker.requestImmediateSync(context)
            return true
        } else {
            // No context - fall back to direct write (legacy behavior)
            return try {
                collection.document(medicine.id).set(updatedMedicine).await()
                true
            } catch (e: Exception) {
                false
            }
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
     * Uses offline-first approach: deletes from Firestore if online, queues if offline
     */
    suspend fun deleteMedicine(medicineId: String, context: Context? = null): Boolean {
        val collection = getMedicinesCollection() ?: return false

        // Try direct Firestore delete if online
        if (context != null && NetworkUtils.isNetworkAvailable(context)) {
            return try {
                collection.document(medicineId).delete().await()
                Log.d("MedicineRepository", "✅ Medicine deleted from Firestore: $medicineId")
                true
            } catch (e: Exception) {
                Log.w("MedicineRepository", "⚠️ Firestore delete failed, queueing for sync", e)
                // Queue for later sync
                val syncManager = SyncManager(context)
                syncManager.queueMedicineDelete(medicineId)
                SyncWorker.requestImmediateSync(context)
                true // Return true since we've queued the delete
            }
        } else if (context != null) {
            // Offline - queue for sync
            Log.d("MedicineRepository", "📥 Offline - queueing medicine delete: $medicineId")
            val syncManager = SyncManager(context)
            syncManager.queueMedicineDelete(medicineId)
            SyncWorker.requestImmediateSync(context)
            return true
        } else {
            // No context - fall back to direct write (legacy behavior)
            return try {
                collection.document(medicineId).delete().await()
                true
            } catch (e: Exception) {
                false
            }
        }
    }

    /**
     * Get medicine count for user
     */
    suspend fun getMedicineCount(): Int {
        return getAllMedicines().size
    }

    /**
     * Get upcoming medicines (rest of today) - excludes taken/skipped
     */
    @RequiresApi(Build.VERSION_CODES.O)
    suspend fun getUpcomingMedicines(context: android.content.Context): List<Pair<Medicine, String>> {
        val todaysMedicines = getTodaysMedicines()
        val currentHour = java.time.LocalTime.now().hour
        val currentMinute = java.time.LocalTime.now().minute
        val today = getCurrentDateString()

        val upcoming = mutableListOf<Pair<Medicine, String>>()

        todaysMedicines.forEach { medicine ->
            medicine.times.forEach { time ->
                val (hour, minute) = time.split(":").map { it.toInt() }
                val medicineTime = hour * 60 + minute
                val currentTime = currentHour * 60 + currentMinute

                // Bugünün geri kalan tüm ilaçları
                if (medicineTime >= currentTime) {
                    // Status kontrolü yap
                    val status = getMedicineStatus(context, medicine.id, today, time)
                    // Sadece alınmamış veya atlanmamış ilaçları ekle
                    if (status != "taken" && status != "skipped") {
                        upcoming.add(Pair(medicine, time))
                    }
                }
            }
        }

        return upcoming.sortedBy { it.second }
    }

    /**
     * Helper: Get medicine status from SharedPreferences
     */
    private fun getMedicineStatus(context: android.content.Context, medicineId: String, date: String, time: String): String? {
        val prefs = context.getSharedPreferences("medicine_status", android.content.Context.MODE_PRIVATE)
        val key = "dose_${medicineId}_${date}_${time}"
        return prefs.getString(key, null)
    }

    /**
     * Helper: Get current day name in Turkish
     */
    @RequiresApi(Build.VERSION_CODES.O)
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

    // ═══════════════════════════════════════════════════════════════════════
    // 📏 PLAN LİMİT KONTROL METODLARİ
    // ═══════════════════════════════════════════════════════════════════════

    /**
     * Toplam aktif time slot sayısını hesapla
     * Her ilacın her hatırlatma saati 1 slot olarak sayılır
     */
    suspend fun getActiveTimeSlotCount(): Int {
        val medicines = getAllMedicines()
        return medicines
            .filter { it.reminderEnabled }
            .sumOf { it.times.size }
    }

    /**
     * Belirli bir ilaç hariç time slot sayısını hesapla
     * (Düzenleme modunda mevcut ilacın slotlarını çıkarmak için)
     */
    suspend fun getActiveTimeSlotCountExcluding(medicineId: String): Int {
        val medicines = getAllMedicines()
        return medicines
            .filter { it.reminderEnabled && it.id != medicineId }
            .sumOf { it.times.size }
    }

    /**
     * Yeni time slotlar eklenebilir mi kontrol et
     * @param newSlotCount Eklenmek istenen slot sayısı
     * @param excludeMedicineId Düzenleme modunda mevcut ilacın ID'si (null ise yeni ekleme)
     * @param limit Maksimum izin verilen slot sayısı (-1 = sınırsız)
     */
    suspend fun canAddTimeSlots(newSlotCount: Int, excludeMedicineId: String? = null, limit: Int): Boolean {
        if (limit == -1) return true // Sınırsız

        val currentCount = if (excludeMedicineId != null) {
            getActiveTimeSlotCountExcluding(excludeMedicineId)
        } else {
            getActiveTimeSlotCount()
        }

        return (currentCount + newSlotCount) <= limit
    }

    /**
     * Yeni ilaç eklenebilir mi kontrol et
     * @param limit Maksimum izin verilen ilaç sayısı (-1 = sınırsız)
     */
    suspend fun canAddMedicine(limit: Int): Boolean {
        if (limit == -1) return true // Sınırsız
        return getMedicineCount() < limit
    }
}
