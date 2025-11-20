package com.bardino.dozi.core.data.repository

import android.content.Context
import android.util.Log
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.SetOptions
import com.google.gson.Gson
import com.google.gson.reflect.TypeToken
import kotlinx.coroutines.tasks.await

/**
 * Kullanıcı tercihlerini ve verilerini Firebase ile senkronize eden repository
 * SharedPreferences'taki verileri Firestore'a yedekler ve çoklu cihaz desteği sağlar
 */
class UserPreferencesRepository(
    private val context: Context,
    private val auth: FirebaseAuth = FirebaseAuth.getInstance(),
    private val db: FirebaseFirestore = FirebaseFirestore.getInstance()
) {
    private val gson = Gson()

    private val currentUserId: String?
        get() = auth.currentUser?.uid

    companion object {
        private const val TAG = "UserPreferencesRepo"
        private const val COLLECTION_USERS = "users"
        private const val SUBCOLLECTION_PREFERENCES = "preferences"

        // Document IDs
        private const val DOC_SNOOZE = "snooze"
        private const val DOC_STOCK_WARNINGS = "stock_warnings"
        private const val DOC_LOCATIONS = "locations"
        private const val DOC_SMART_PATTERNS = "smart_patterns"
    }

    // ═══════════════════════════════════════════════════════════════════════════
    // 💤 SNOOZE VERİLERİ
    // ═══════════════════════════════════════════════════════════════════════════

    /**
     * Snooze verilerini Firestore'a kaydet
     */
    suspend fun syncSnoozeData(
        snoozeMinutes: Int,
        snoozeUntil: Long,
        snoozeTimestamp: Long,
        medicineId: String? = null
    ): Result<Unit> {
        val userId = currentUserId ?: return Result.failure(Exception("User not logged in"))

        return try {
            val data = hashMapOf(
                "snoozeMinutes" to snoozeMinutes,
                "snoozeUntil" to snoozeUntil,
                "snoozeTimestamp" to snoozeTimestamp,
                "medicineId" to medicineId,
                "updatedAt" to System.currentTimeMillis()
            )

            db.collection(COLLECTION_USERS)
                .document(userId)
                .collection(SUBCOLLECTION_PREFERENCES)
                .document(DOC_SNOOZE)
                .set(data, SetOptions.merge())
                .await()

            Log.d(TAG, "✅ Snooze verisi senkronize edildi: $snoozeMinutes dk")
            Result.success(Unit)
        } catch (e: Exception) {
            Log.e(TAG, "❌ Snooze verisi senkronize edilemedi", e)
            Result.failure(e)
        }
    }

    /**
     * Snooze geçmişini kaydet (SmartReminder için)
     */
    suspend fun syncSnoozeHistory(medicineId: String, history: List<Int>): Result<Unit> {
        val userId = currentUserId ?: return Result.failure(Exception("User not logged in"))

        return try {
            val data = hashMapOf(
                "history_$medicineId" to history,
                "updatedAt" to System.currentTimeMillis()
            )

            db.collection(COLLECTION_USERS)
                .document(userId)
                .collection(SUBCOLLECTION_PREFERENCES)
                .document(DOC_SNOOZE)
                .set(data, SetOptions.merge())
                .await()

            Log.d(TAG, "✅ Snooze geçmişi senkronize edildi: $medicineId")
            Result.success(Unit)
        } catch (e: Exception) {
            Log.e(TAG, "❌ Snooze geçmişi senkronize edilemedi", e)
            Result.failure(e)
        }
    }

    /**
     * Snooze verilerini Firestore'dan al
     */
    suspend fun getSnoozeData(): Map<String, Any>? {
        val userId = currentUserId ?: return null

        return try {
            val doc = db.collection(COLLECTION_USERS)
                .document(userId)
                .collection(SUBCOLLECTION_PREFERENCES)
                .document(DOC_SNOOZE)
                .get()
                .await()

            doc.data
        } catch (e: Exception) {
            Log.e(TAG, "❌ Snooze verisi alınamadı", e)
            null
        }
    }

    // ═══════════════════════════════════════════════════════════════════════════
    // ⚠️ STOCK WARNING VERİLERİ
    // ═══════════════════════════════════════════════════════════════════════════

    /**
     * Stock warning zamanını kaydet
     */
    suspend fun syncStockWarning(medicineId: String, lastWarningTime: Long): Result<Unit> {
        val userId = currentUserId ?: return Result.failure(Exception("User not logged in"))

        return try {
            val data = hashMapOf(
                "last_warning_$medicineId" to lastWarningTime,
                "updatedAt" to System.currentTimeMillis()
            )

            db.collection(COLLECTION_USERS)
                .document(userId)
                .collection(SUBCOLLECTION_PREFERENCES)
                .document(DOC_STOCK_WARNINGS)
                .set(data, SetOptions.merge())
                .await()

            Log.d(TAG, "✅ Stock warning senkronize edildi: $medicineId")
            Result.success(Unit)
        } catch (e: Exception) {
            Log.e(TAG, "❌ Stock warning senkronize edilemedi", e)
            Result.failure(e)
        }
    }

    /**
     * Stock warning verilerini al
     */
    suspend fun getStockWarnings(): Map<String, Long>? {
        val userId = currentUserId ?: return null

        return try {
            val doc = db.collection(COLLECTION_USERS)
                .document(userId)
                .collection(SUBCOLLECTION_PREFERENCES)
                .document(DOC_STOCK_WARNINGS)
                .get()
                .await()

            doc.data?.filterKeys { it.startsWith("last_warning_") }
                ?.mapValues { (it.value as? Long) ?: 0L }
        } catch (e: Exception) {
            Log.e(TAG, "❌ Stock warnings alınamadı", e)
            null
        }
    }

    // ═══════════════════════════════════════════════════════════════════════════
    // 📍 KONUM VERİLERİ
    // ═══════════════════════════════════════════════════════════════════════════

    /**
     * Kayıtlı konumları senkronize et
     */
    suspend fun syncLocations(locationsJson: String): Result<Unit> {
        val userId = currentUserId ?: return Result.failure(Exception("User not logged in"))

        return try {
            val data = hashMapOf(
                "locationsJson" to locationsJson,
                "updatedAt" to System.currentTimeMillis()
            )

            db.collection(COLLECTION_USERS)
                .document(userId)
                .collection(SUBCOLLECTION_PREFERENCES)
                .document(DOC_LOCATIONS)
                .set(data, SetOptions.merge())
                .await()

            Log.d(TAG, "✅ Konumlar senkronize edildi")
            Result.success(Unit)
        } catch (e: Exception) {
            Log.e(TAG, "❌ Konumlar senkronize edilemedi", e)
            Result.failure(e)
        }
    }

    /**
     * Kayıtlı konumları al
     */
    suspend fun getLocations(): String? {
        val userId = currentUserId ?: return null

        return try {
            val doc = db.collection(COLLECTION_USERS)
                .document(userId)
                .collection(SUBCOLLECTION_PREFERENCES)
                .document(DOC_LOCATIONS)
                .get()
                .await()

            doc.getString("locationsJson")
        } catch (e: Exception) {
            Log.e(TAG, "❌ Konumlar alınamadı", e)
            null
        }
    }

    // ═══════════════════════════════════════════════════════════════════════════
    // 🧠 SMART REMINDER PATTERNS
    // ═══════════════════════════════════════════════════════════════════════════

    /**
     * Smart reminder pattern'ını senkronize et
     */
    suspend fun syncSmartPattern(
        medicineId: String,
        modeSnoozeMinutes: Int? = null,
        avgSnoozeMinutes: Int? = null,
        avgDelayMinutes: Int? = null,
        lastSnoozeMinutes: Int? = null
    ): Result<Unit> {
        val userId = currentUserId ?: return Result.failure(Exception("User not logged in"))

        return try {
            val data = hashMapOf<String, Any>(
                "updatedAt" to System.currentTimeMillis()
            )

            modeSnoozeMinutes?.let { data["mode_snooze_$medicineId"] = it }
            avgSnoozeMinutes?.let { data["avg_snooze_$medicineId"] = it }
            avgDelayMinutes?.let { data["avg_delay_$medicineId"] = it }
            lastSnoozeMinutes?.let { data["last_snooze_$medicineId"] = it }

            db.collection(COLLECTION_USERS)
                .document(userId)
                .collection(SUBCOLLECTION_PREFERENCES)
                .document(DOC_SMART_PATTERNS)
                .set(data, SetOptions.merge())
                .await()

            Log.d(TAG, "✅ Smart pattern senkronize edildi: $medicineId")
            Result.success(Unit)
        } catch (e: Exception) {
            Log.e(TAG, "❌ Smart pattern senkronize edilemedi", e)
            Result.failure(e)
        }
    }

    /**
     * Smart reminder patterns'ı al
     */
    suspend fun getSmartPatterns(): Map<String, Any>? {
        val userId = currentUserId ?: return null

        return try {
            val doc = db.collection(COLLECTION_USERS)
                .document(userId)
                .collection(SUBCOLLECTION_PREFERENCES)
                .document(DOC_SMART_PATTERNS)
                .get()
                .await()

            doc.data
        } catch (e: Exception) {
            Log.e(TAG, "❌ Smart patterns alınamadı", e)
            null
        }
    }

    // ═══════════════════════════════════════════════════════════════════════════
    // 🔄 TOPLU SENKRONIZASYON
    // ═══════════════════════════════════════════════════════════════════════════

    /**
     * Tüm tercihleri Firestore'dan çek ve local'e kaydet
     */
    suspend fun pullAllPreferences() {
        val userId = currentUserId ?: return

        try {
            // Snooze verilerini çek
            getSnoozeData()?.let { data ->
                val prefs = context.getSharedPreferences("dozi_prefs", Context.MODE_PRIVATE)
                prefs.edit().apply {
                    (data["snoozeMinutes"] as? Long)?.toInt()?.let { putInt("snooze_minutes", it) }
                    (data["snoozeUntil"] as? Long)?.let { putLong("snooze_until", it) }
                    (data["snoozeTimestamp"] as? Long)?.let { putLong("snooze_timestamp", it) }
                    apply()
                }
            }

            // Stock warnings çek
            getStockWarnings()?.let { warnings ->
                val prefs = context.getSharedPreferences("stock_warnings", Context.MODE_PRIVATE)
                prefs.edit().apply {
                    warnings.forEach { (key, value) ->
                        putLong(key, value)
                    }
                    apply()
                }
            }

            // Locations çek
            getLocations()?.let { json ->
                val prefs = context.getSharedPreferences("location_prefs", Context.MODE_PRIVATE)
                prefs.edit().putString("saved_locations", json).apply()
            }

            Log.d(TAG, "✅ Tüm tercihler Firestore'dan çekildi")
        } catch (e: Exception) {
            Log.e(TAG, "❌ Tercihler çekilirken hata", e)
        }
    }
}
