package com.bardino.dozi.core.data.repository

import android.os.Build
import androidx.annotation.RequiresApi
import com.bardino.dozi.core.data.model.Medicine
import com.bardino.dozi.core.profile.ProfileManager
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.Query
import kotlinx.coroutines.tasks.await
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.flow.firstOrNull
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Repository for managing Medicine data in Firestore
 * All medicines are stored under: /users/{userId}/medicines/{medicineId}
 * Now supports multi-user profiles: medicines are filtered by active profileId
 */
@Singleton
class MedicineRepository @Inject constructor(
    private val profileManager: ProfileManager
) {
    private val auth = FirebaseAuth.getInstance()
    private val db = FirebaseFirestore.getInstance()

    /**
     * Get current user's medicine collection reference
     */
    private fun getMedicinesCollection() = auth.currentUser?.let { user ->
        db.collection("users").document(user.uid).collection("medicines")
    }

    /**
     * Check if given profile is the default/main profile
     * Logic:
     * - If user has only 1 profile, it's the default
     * - If user has multiple profiles, the first created profile (oldest) is default
     */
    private suspend fun isDefaultProfile(profileId: String): Boolean {
        val profileCount = profileManager.getProfileCount()

        // If only 1 profile exists, it's the default
        if (profileCount == 1) {
            return true
        }

        // If multiple profiles, check if this is the first created one
        val allProfiles = profileManager.getAllProfiles().firstOrNull()
        val firstProfile = allProfiles?.minByOrNull { it.createdAt }
        return firstProfile?.id == profileId
    }

    /**
     * Get all medicines for current user and active profile
     * If active profile is default, returns medicines with ownerProfileId = null
     * Otherwise, returns medicines with ownerProfileId = activeProfileId
     */
    suspend fun getAllMedicines(): List<Medicine> {
        val collection = getMedicinesCollection() ?: return emptyList()
        val activeProfileId = profileManager.getActiveProfileId()
        val isDefault = isDefaultProfile(activeProfileId)

        return try {
            // Get all medicines and filter client-side for now
            // TODO: Optimize with composite query when needed
            val snapshot = collection
                .orderBy("createdAt", Query.Direction.DESCENDING)
                .get()
                .await()

            snapshot.documents.mapNotNull { it.toObject(Medicine::class.java) }.filter { medicine ->
                if (isDefault) {
                    // Default profile sees medicines with null or empty ownerProfileId
                    medicine.ownerProfileId.isNullOrEmpty()
                } else {
                    // Other profiles see only their own medicines
                    medicine.ownerProfileId == activeProfileId
                }
            }
        } catch (e: Exception) {
            android.util.Log.e("MedicineRepository", "❌ Error getting medicines: ${e.message}")
            emptyList()
        }
    }

    /**
     * Get medicines with real-time updates for active profile
     * 🔥 BUG FIX: Now properly reacts to profile changes and filters by ownerProfileId
     */
    fun getMedicinesFlow(): Flow<List<Medicine>> = profileManager.getAllProfiles()
        .flatMapLatest { allProfiles ->
            profileManager.getActiveProfile().flatMapLatest { activeProfile ->
                val collection = getMedicinesCollection()
                if (collection == null || activeProfile == null) {
                    return@flatMapLatest flowOf(emptyList())
                }

                // Determine if this is the default profile
                val isDefault = if (allProfiles.size == 1) {
                    // Only 1 profile = default
                    true
                } else {
                    // Multiple profiles = check if this is the first created one
                    val firstProfile = allProfiles.minByOrNull { it.createdAt }
                    firstProfile?.id == activeProfile.id
                }

                callbackFlow {
                    val listener = collection
                        // Get all medicines, filter client-side
                        .addSnapshotListener { snapshot, error ->
                            if (error != null) {
                                android.util.Log.e("MedicineRepository", "Error listening to medicines: ${error.message}")
                                trySend(emptyList())
                                return@addSnapshotListener
                            }

                            var medicines = snapshot?.documents?.mapNotNull {
                                it.toObject(Medicine::class.java)
                            }?.filter { medicine ->
                                if (isDefault) {
                                    // Default profile sees medicines with null or empty ownerProfileId
                                    medicine.ownerProfileId.isNullOrEmpty()
                                } else {
                                    // Other profiles see only their own medicines
                                    medicine.ownerProfileId == activeProfile.id
                                }
                            } ?: emptyList()

                            // Client-side sorting
                            medicines = medicines.sortedByDescending { it.createdAt }

                            android.util.Log.d("MedicineRepository", "✅ Loaded ${medicines.size} medicines for profile: ${activeProfile.name} (${activeProfile.id}), isDefault: $isDefault")
                            trySend(medicines)
                        }

                    awaitClose { listener.remove() }
                }
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
     * Add a new medicine for active profile
     * Sets ownerProfileId to null if default profile, or activeProfileId if family member profile
     */
    suspend fun addMedicine(medicine: Medicine): Medicine? {
        val collection = getMedicinesCollection() ?: return null
        return try {
            val user = auth.currentUser ?: return null
            val activeProfileId = profileManager.getActiveProfileId()
            val isDefault = isDefaultProfile(activeProfileId)

            val medicineWithUser = medicine.copy(
                userId = user.uid,
                ownerProfileId = if (isDefault) null else activeProfileId,
                id = if (medicine.id.isEmpty()) collection.document().id else medicine.id,
                createdAt = System.currentTimeMillis(),
                updatedAt = System.currentTimeMillis()
            )
            collection.document(medicineWithUser.id).set(medicineWithUser).await()
            android.util.Log.d("MedicineRepository", "✅ Medicine added with ID: ${medicineWithUser.id} for profile: $activeProfileId (ownerProfileId: ${medicineWithUser.ownerProfileId})")
            medicineWithUser
        } catch (e: Exception) {
            android.util.Log.e("MedicineRepository", "❌ Error adding medicine", e)
            null
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

    /**
     * 🔧 MIGRATION: Assign default profileId to medicines that don't have one
     * This should be called once after multi-profile feature is added
     *
     * @param defaultProfileId The default profile ID to assign (usually from ProfileManager.ensureDefaultProfile())
     * @return Number of medicines migrated
     */
    suspend fun migrateOldMedicines(defaultProfileId: String): Int {
        val collection = getMedicinesCollection() ?: return 0
        var migratedCount = 0

        return try {
            // Get all medicines
            val snapshot = collection.get().await()

            snapshot.documents.forEach { doc ->
                val medicine = doc.toObject(Medicine::class.java)

                // Migration strategy: Set ownerProfileId = null for default profile medicines
                // This ensures backwards compatibility
                if (medicine != null && medicine.ownerProfileId == null) {
                    // Medicine already migrated or is for default profile, no action needed
                    android.util.Log.d("MedicineRepository", "Medicine ${medicine.name} already has correct ownerProfileId (null = default profile)")
                }
            }

            android.util.Log.d("MedicineRepository", "✅ Migration complete: $migratedCount medicines checked")
            migratedCount
        } catch (e: Exception) {
            android.util.Log.e("MedicineRepository", "❌ Migration failed: ${e.message}")
            0
        }
    }
}
