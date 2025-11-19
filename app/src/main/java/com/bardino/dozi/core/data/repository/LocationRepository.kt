package com.bardino.dozi.core.data.repository

import android.content.Context
import android.util.Log
import com.bardino.dozi.core.data.LocationPreferences
import com.bardino.dozi.core.data.SavedLocation
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FieldValue
import com.google.firebase.firestore.FirebaseFirestore
import kotlinx.coroutines.tasks.await

/**
 * 📍 LocationRepository
 *
 * Konumları Firestore'da saklar ve senkronize eder.
 * - Uygulama silinse bile konumlar kaybolmaz
 * - Kullanıcı farklı cihazlarda giriş yapsa aynı konumları görür
 */
class LocationRepository(
    private val auth: FirebaseAuth = FirebaseAuth.getInstance(),
    private val db: FirebaseFirestore = FirebaseFirestore.getInstance()
) {
    companion object {
        private const val TAG = "LocationRepository"
        private const val USERS_COLLECTION = "users"
        private const val LOCATIONS_FIELD = "locations"
        const val MAX_LOCATIONS = 5
    }

    /**
     * 📥 Firestore'dan konumları getir
     */
    suspend fun getLocations(): List<SavedLocation> {
        return try {
            val user = auth.currentUser ?: return emptyList()
            val doc = db.collection(USERS_COLLECTION)
                .document(user.uid)
                .get()
                .await()

            val locationsList = doc.get(LOCATIONS_FIELD) as? List<Map<String, Any>> ?: emptyList()

            locationsList.mapNotNull { locationMap ->
                try {
                    SavedLocation(
                        id = locationMap["id"] as? String ?: return@mapNotNull null,
                        name = locationMap["name"] as? String ?: "",
                        lat = (locationMap["lat"] as? Number)?.toDouble() ?: 0.0,
                        lng = (locationMap["lng"] as? Number)?.toDouble() ?: 0.0,
                        address = locationMap["address"] as? String ?: ""
                    )
                } catch (e: Exception) {
                    Log.e(TAG, "❌ Error parsing location: ${e.message}")
                    null
                }
            }
        } catch (e: Exception) {
            Log.e(TAG, "❌ Error getting locations from Firestore: ${e.message}")
            emptyList()
        }
    }

    /**
     * 💾 Firestore'a konum ekle
     */
    suspend fun addLocation(location: SavedLocation): Result<Unit> {
        return try {
            val user = auth.currentUser ?: return Result.failure(Exception("Kullanıcı giriş yapmamış"))

            // Önce mevcut konumları al
            val currentLocations = getLocations()

            // Max limit kontrolü
            if (currentLocations.size >= MAX_LOCATIONS) {
                return Result.failure(Exception("En fazla $MAX_LOCATIONS konum kaydedebilirsiniz"))
            }

            val locationMap = mapOf(
                "id" to location.id,
                "name" to location.name,
                "lat" to location.lat,
                "lng" to location.lng,
                "address" to location.address
            )

            db.collection(USERS_COLLECTION)
                .document(user.uid)
                .update(LOCATIONS_FIELD, FieldValue.arrayUnion(locationMap))
                .await()

            Log.d(TAG, "✅ Location added to Firestore: ${location.name}")
            Result.success(Unit)
        } catch (e: Exception) {
            Log.e(TAG, "❌ Error adding location to Firestore: ${e.message}")
            Result.failure(e)
        }
    }

    /**
     * 🗑️ Firestore'dan konum sil
     */
    suspend fun removeLocation(locationId: String): Result<Unit> {
        return try {
            val user = auth.currentUser ?: return Result.failure(Exception("Kullanıcı giriş yapmamış"))

            // Önce tüm konumları al
            val allLocations = getLocations()

            // Silinecek konumu bul
            val locationToRemove = allLocations.find { it.id == locationId }
                ?: return Result.failure(Exception("Konum bulunamadı"))

            val locationMap = mapOf(
                "id" to locationToRemove.id,
                "name" to locationToRemove.name,
                "lat" to locationToRemove.lat,
                "lng" to locationToRemove.lng,
                "address" to locationToRemove.address
            )

            db.collection(USERS_COLLECTION)
                .document(user.uid)
                .update(LOCATIONS_FIELD, FieldValue.arrayRemove(locationMap))
                .await()

            Log.d(TAG, "✅ Location removed from Firestore: ${locationToRemove.name}")
            Result.success(Unit)
        } catch (e: Exception) {
            Log.e(TAG, "❌ Error removing location from Firestore: ${e.message}")
            Result.failure(e)
        }
    }

    /**
     * 🧹 Tüm konumları sil
     */
    suspend fun clearAllLocations(): Result<Unit> {
        return try {
            val user = auth.currentUser ?: return Result.failure(Exception("Kullanıcı giriş yapmamış"))

            db.collection(USERS_COLLECTION)
                .document(user.uid)
                .update(LOCATIONS_FIELD, emptyList<Map<String, Any>>())
                .await()

            Log.d(TAG, "✅ All locations cleared from Firestore")
            Result.success(Unit)
        } catch (e: Exception) {
            Log.e(TAG, "❌ Error clearing locations from Firestore: ${e.message}")
            Result.failure(e)
        }
    }

    /**
     * 🔄 Local SharedPreferences'dan Firestore'a migrate et
     *
     * İlk kullanımda eski local konumları Firestore'a taşır
     */
    suspend fun migrateLocalLocationsToFirestore(context: Context): Result<Unit> {
        return try {
            val user = auth.currentUser ?: return Result.failure(Exception("Kullanıcı giriş yapmamış"))

            // Local konumları al
            val localLocations = LocationPreferences.getLocations(context)

            if (localLocations.isEmpty()) {
                Log.d(TAG, "ℹ️ No local locations to migrate")
                return Result.success(Unit)
            }

            // Firestore'daki mevcut konumları kontrol et
            val firestoreLocations = getLocations()

            if (firestoreLocations.isNotEmpty()) {
                Log.d(TAG, "ℹ️ Firestore already has locations, skipping migration")
                return Result.success(Unit)
            }

            // Local konumları Firestore'a aktar
            val locationMaps = localLocations.map { location ->
                mapOf(
                    "id" to location.id,
                    "name" to location.name,
                    "lat" to location.lat,
                    "lng" to location.lng,
                    "address" to location.address
                )
            }

            db.collection(USERS_COLLECTION)
                .document(user.uid)
                .update(LOCATIONS_FIELD, locationMaps)
                .await()

            Log.d(TAG, "✅ Successfully migrated ${localLocations.size} locations to Firestore")

            // Migration başarılı olursa local konumları temizle
            LocationPreferences.clearAllLocations(context)
            Log.d(TAG, "✅ Local locations cleared after successful migration")

            Result.success(Unit)
        } catch (e: Exception) {
            Log.e(TAG, "❌ Error migrating locations to Firestore: ${e.message}")
            Result.failure(e)
        }
    }
}
