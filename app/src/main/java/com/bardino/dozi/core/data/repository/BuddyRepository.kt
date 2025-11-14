package com.bardino.dozi.core.data.repository

import com.bardino.dozi.core.data.model.*
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FieldValue
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.Query
import com.google.firebase.Timestamp
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.tasks.await
import java.util.*
import kotlin.random.Random

/**
 * Buddy sistemini yöneten repository
 */
class BuddyRepository(
    private val auth: FirebaseAuth = FirebaseAuth.getInstance(),
    private val db: FirebaseFirestore = FirebaseFirestore.getInstance()
) {

    private val currentUserId: String?
        get() = auth.currentUser?.uid

    // ==================== Buddy İşlemleri ====================

    /**
     * Kullanıcının buddy'lerini real-time olarak dinle
     */
    fun getBuddiesFlow(): Flow<List<BuddyWithUser>> = callbackFlow {
        val userId = currentUserId ?: run {
            android.util.Log.w("BuddyRepository", "getBuddiesFlow: No user logged in")
            close()
            return@callbackFlow
        }

        android.util.Log.d("BuddyRepository", "getBuddiesFlow: Listening for buddies of userId=$userId")

        val scope = this

        val listener = db.collection("buddies")
            .whereEqualTo("userId", userId)
            .whereEqualTo("status", BuddyStatus.ACTIVE.name)
            .addSnapshotListener { snapshot, error ->
                if (error != null) {
                    android.util.Log.e("BuddyRepository", "getBuddiesFlow: Error", error)
                    close(error)
                    return@addSnapshotListener
                }

                val buddies = snapshot?.documents?.mapNotNull { doc ->
                    val buddy = doc.toObject(Buddy::class.java)?.copy(id = doc.id)
                    android.util.Log.d("BuddyRepository", "getBuddiesFlow: Found buddy record - id=${doc.id}, userId=${buddy?.userId}, buddyUserId=${buddy?.buddyUserId}")
                    buddy
                } ?: emptyList()

                android.util.Log.d("BuddyRepository", "getBuddiesFlow: Total ${buddies.size} buddy records found")

                // ❗ Suspend fonksiyon kullanacağımız için coroutine açıyoruz
                scope.launch {
                    val list = buddies.map { buddy ->
                        android.util.Log.d("BuddyRepository", "getBuddiesFlow: Fetching user info for buddyUserId=${buddy.buddyUserId}")
                        val user = getUserById(buddy.buddyUserId)
                        android.util.Log.d("BuddyRepository", "getBuddiesFlow: Got user - uid=${user.uid}, name=${user.name}, email=${user.email}")
                        BuddyWithUser(buddy, user)
                    }
                    android.util.Log.d("BuddyRepository", "getBuddiesFlow: Sending ${list.size} buddies to UI")
                    trySend(list).isSuccess
                }
            }

        awaitClose { listener.remove() }
    }


    /**
     * Belirli bir buddy'yi ID ile getir
     */
    suspend fun getBuddyById(buddyId: String): Buddy? {
        return try {
            db.collection("buddies")
                .document(buddyId)
                .get()
                .await()
                .toObject(Buddy::class.java)
        } catch (e: Exception) {
            null
        }
    }

    /**
     * İki kullanıcı arasında buddy ilişkisi var mı kontrol et
     */
    suspend fun isBuddy(otherUserId: String): Boolean {
        val userId = currentUserId ?: return false

        return try {
            val snapshot = db.collection("buddies")
                .whereEqualTo("userId", userId)
                .whereEqualTo("buddyUserId", otherUserId)
                .whereEqualTo("status", BuddyStatus.ACTIVE.name)
                .get()
                .await()

            !snapshot.isEmpty
        } catch (e: Exception) {
            false
        }
    }

    /**
     * Buddy izinlerini güncelle
     */
    suspend fun updateBuddyPermissions(
        buddyId: String,
        permissions: BuddyPermissions
    ): Result<Unit> {
        return try {
            db.collection("buddies")
                .document(buddyId)
                .update("permissions", permissions)
                .await()
            Result.success(Unit)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    /**
     * Buddy bildirim tercihlerini güncelle
     */
    suspend fun updateBuddyNotificationPreferences(
        buddyId: String,
        preferences: BuddyNotificationPreferences
    ): Result<Unit> {
        return try {
            db.collection("buddies")
                .document(buddyId)
                .update("notificationPreferences", preferences)
                .await()
            Result.success(Unit)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    /**
     * Buddy'ye nickname (takma isim) ekle
     */
    suspend fun updateBuddyNickname(
        buddyId: String,
        nickname: String?
    ): Result<Unit> {
        return try {
            db.collection("buddies")
                .document(buddyId)
                .update("nickname", nickname)
                .await()
            Result.success(Unit)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    /**
     * Buddy ilişkisini kaldır
     */
    suspend fun removeBuddy(buddyId: String): Result<Unit> {
        return try {
            db.collection("buddies")
                .document(buddyId)
                .update("status", BuddyStatus.REMOVED.name)
                .await()
            Result.success(Unit)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    // ==================== Buddy İstekleri ====================

    /**
     * Buddy kodu oluştur (6 haneli)
     */
    suspend fun generateBuddyCode(): String {
        val userId = currentUserId ?: return ""

        val code = Random.nextInt(100000, 999999).toString()

        // User'a buddy kodu kaydet
        db.collection("users")
            .document(userId)
            .update("buddyCode", code)
            .await()

        return code
    }

    /**
     * Buddy kodu ile kullanıcı bul
     */
    suspend fun findUserByBuddyCode(code: String): User? {
        return try {
            val snapshot = db.collection("users")
                .whereEqualTo("buddyCode", code)
                .limit(1)
                .get()
                .await()

            snapshot.documents.firstOrNull()?.toObject(User::class.java)
        } catch (e: Exception) {
            null
        }
    }

    /**
     * Email ile kullanıcı bul
     */
    suspend fun findUserByEmail(email: String): User? {
        return try {
            val snapshot = db.collection("users")
                .whereEqualTo("email", email)
                .limit(1)
                .get()
                .await()

            snapshot.documents.firstOrNull()?.toObject(User::class.java)
        } catch (e: Exception) {
            null
        }
    }

    /**
     * Buddy isteği gönder
     */
    suspend fun sendBuddyRequest(
        toUserId: String,
        message: String? = null
    ): Result<String> {
        val userId = currentUserId ?: return Result.failure(Exception("User not logged in"))

        return try {
            // Kullanıcı bilgilerini al
            val currentUser = db.collection("users").document(userId).get().await()
                .toObject(User::class.java) ?: return Result.failure(Exception("User not found"))

            // Daha önce istek gönderilmiş mi kontrol et
            val existingRequest = db.collection("buddy_requests")
                .whereEqualTo("fromUserId", userId)
                .whereEqualTo("toUserId", toUserId)
                .whereEqualTo("status", BuddyRequestStatus.PENDING.name)
                .get()
                .await()

            if (!existingRequest.isEmpty) {
                return Result.failure(Exception("Zaten bekleyen bir istek var"))
            }

            // Yeni istek oluştur
            val request = BuddyRequest(
                fromUserId = userId,
                toUserId = toUserId,
                fromUserName = currentUser.name,
                fromUserPhoto = currentUser.photoUrl,
                message = message,
                status = BuddyRequestStatus.PENDING,
                expiresAt = Timestamp(Date(System.currentTimeMillis() + 7 * 24 * 60 * 60 * 1000)) // 7 gün
            )

            val docRef = db.collection("buddy_requests").add(request).await()
            Result.success(docRef.id)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    /**
     * Bekleyen buddy isteklerini real-time dinle
     */
    fun getPendingBuddyRequestsFlow(): Flow<List<BuddyRequestWithUser>> = callbackFlow {
        val userId = currentUserId ?: run {
            android.util.Log.w("BuddyRepository", "getPendingBuddyRequestsFlow: No user logged in")
            close()
            return@callbackFlow
        }

        android.util.Log.d("BuddyRepository", "getPendingBuddyRequestsFlow: Listening for requests to userId=$userId")

        val scope = this

        val listener = db.collection("buddy_requests")
            .whereEqualTo("toUserId", userId)
            .whereEqualTo("status", BuddyRequestStatus.PENDING.name)
            .orderBy("createdAt", Query.Direction.DESCENDING)
            .addSnapshotListener { snapshot, error ->
                if (error != null) {
                    android.util.Log.e("BuddyRepository", "getPendingBuddyRequestsFlow error", error)
                    close(error)
                    return@addSnapshotListener
                }

                val requests = snapshot?.documents?.mapNotNull { doc ->
                    android.util.Log.d("BuddyRepository", "Found pending request: ${doc.id} -> ${doc.data}")
                    doc.toObject(BuddyRequest::class.java)?.copy(id = doc.id)
                } ?: emptyList()

                android.util.Log.d("BuddyRepository", "Total pending requests: ${requests.size}")

                scope.launch {
                    val list = requests.map { request ->
                        val user = getUserById(request.fromUserId)
                        BuddyRequestWithUser(request, user)
                    }
                    android.util.Log.d("BuddyRepository", "Sending ${list.size} pending requests to UI")
                    trySend(list).isSuccess
                }
            }

        awaitClose { listener.remove() }
    }


    /**
     * Buddy isteğini kabul et
     */
    suspend fun acceptBuddyRequest(requestId: String): Result<Unit> {
        val userId = currentUserId ?: run {
            android.util.Log.e("BuddyRepository", "acceptBuddyRequest: User not logged in")
            return Result.failure(Exception("User not logged in"))
        }

        return try {
            android.util.Log.d("BuddyRepository", "acceptBuddyRequest: Getting request $requestId")

            // İsteği al
            val requestDoc = db.collection("buddy_requests").document(requestId).get().await()
            val request = requestDoc.toObject(BuddyRequest::class.java)
                ?: run {
                    android.util.Log.e("BuddyRepository", "acceptBuddyRequest: Request not found")
                    return Result.failure(Exception("Request not found"))
                }

            // İstek zaten kabul edilmiş mi kontrol et
            if (request.status != BuddyRequestStatus.PENDING) {
                android.util.Log.w("BuddyRepository", "acceptBuddyRequest: Request already processed (status=${request.status})")
                return Result.failure(Exception("Bu istek zaten işleme alınmış"))
            }

            android.util.Log.d("BuddyRepository", "acceptBuddyRequest: From ${request.fromUserId} to $userId")

            // Bu kullanıcılar arasında zaten buddy ilişkisi var mı kontrol et
            val existingBuddy = db.collection("buddies")
                .whereEqualTo("userId", userId)
                .whereEqualTo("buddyUserId", request.fromUserId)
                .whereEqualTo("status", BuddyStatus.ACTIVE.name)
                .get()
                .await()

            if (!existingBuddy.isEmpty) {
                android.util.Log.w("BuddyRepository", "acceptBuddyRequest: Buddy relationship already exists")
                // İsteği kabul edildi olarak işaretle
                db.collection("buddy_requests").document(requestId)
                    .update(
                        mapOf(
                            "status" to BuddyRequestStatus.ACCEPTED.name,
                            "respondedAt" to FieldValue.serverTimestamp()
                        )
                    ).await()
                return Result.success(Unit)
            }

            // İki yönlü buddy ilişkisi oluştur
            val buddy1 = Buddy(
                userId = request.fromUserId,
                buddyUserId = userId,
                status = BuddyStatus.ACTIVE
            )

            val buddy2 = Buddy(
                userId = userId,
                buddyUserId = request.fromUserId,
                status = BuddyStatus.ACTIVE
            )

            android.util.Log.d("BuddyRepository", "acceptBuddyRequest: Creating buddies - buddy1(userId=${buddy1.userId}, buddyUserId=${buddy1.buddyUserId}), buddy2(userId=${buddy2.userId}, buddyUserId=${buddy2.buddyUserId})")

            // Firestore batch işlemi
            val batch = db.batch()

            // Buddy ilişkilerini ekle
            val buddy1Ref = db.collection("buddies").document()
            val buddy2Ref = db.collection("buddies").document()

            batch.set(buddy1Ref, buddy1)
            batch.set(buddy2Ref, buddy2)

            // İstek durumunu güncelle
            batch.update(
                db.collection("buddy_requests").document(requestId),
                mapOf(
                    "status" to BuddyRequestStatus.ACCEPTED.name,
                    "respondedAt" to FieldValue.serverTimestamp()
                )
            )

            android.util.Log.d("BuddyRepository", "acceptBuddyRequest: Committing batch...")
            batch.commit().await()

            android.util.Log.d("BuddyRepository", "acceptBuddyRequest: ✅ Success - Buddy relationship created")
            Result.success(Unit)
        } catch (e: Exception) {
            android.util.Log.e("BuddyRepository", "acceptBuddyRequest: ❌ Error", e)
            Result.failure(e)
        }
    }

    /**
     * Duplicate buddy kayıtlarını temizle
     * (Her userId-buddyUserId kombinasyonundan sadece birini bırak)
     */
    suspend fun cleanupDuplicateBuddies(): Result<Int> {
        val userId = currentUserId ?: return Result.failure(Exception("User not logged in"))

        return try {
            android.util.Log.d("BuddyRepository", "cleanupDuplicateBuddies: Starting cleanup for user $userId")

            // Kullanıcının tüm buddy kayıtlarını al
            val buddiesSnapshot = db.collection("buddies")
                .whereEqualTo("userId", userId)
                .whereEqualTo("status", BuddyStatus.ACTIVE.name)
                .get()
                .await()

            // buddyUserId'ye göre grupla
            val grouped = buddiesSnapshot.documents.groupBy { doc ->
                doc.toObject(Buddy::class.java)?.buddyUserId ?: ""
            }

            var deletedCount = 0
            val batch = db.batch()

            // Her grup için sadece ilkini tut, diğerlerini sil
            grouped.forEach { (buddyUserId, docs) ->
                if (docs.size > 1) {
                    android.util.Log.d("BuddyRepository", "cleanupDuplicateBuddies: Found ${docs.size} duplicates for buddy $buddyUserId")
                    // İlkini hariç tut, diğerlerini sil
                    docs.drop(1).forEach { doc ->
                        batch.delete(doc.reference)
                        deletedCount++
                    }
                }
            }

            if (deletedCount > 0) {
                batch.commit().await()
                android.util.Log.d("BuddyRepository", "cleanupDuplicateBuddies: ✅ Deleted $deletedCount duplicate records")
            } else {
                android.util.Log.d("BuddyRepository", "cleanupDuplicateBuddies: No duplicates found")
            }

            Result.success(deletedCount)
        } catch (e: Exception) {
            android.util.Log.e("BuddyRepository", "cleanupDuplicateBuddies: ❌ Error", e)
            Result.failure(e)
        }
    }

    /**
     * Buddy isteğini reddet
     */
    suspend fun rejectBuddyRequest(requestId: String): Result<Unit> {
        return try {
            db.collection("buddy_requests")
                .document(requestId)
                .update(
                    mapOf(
                        "status" to BuddyRequestStatus.REJECTED.name,
                        "respondedAt" to FieldValue.serverTimestamp()
                    )
                )
                .await()
            Result.success(Unit)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    // ==================== Yardımcı Fonksiyonlar ====================

    private suspend fun getUserById(userId: String): User {
        return try {
            android.util.Log.d("BuddyRepository", "getUserById: Fetching user with userId=$userId")
            val userDoc = db.collection("users")
                .document(userId)
                .get()
                .await()

            val user = userDoc.toObject(User::class.java)
            if (user != null) {
                android.util.Log.d("BuddyRepository", "getUserById: Found user - uid=${user.uid}, name=${user.name}, email=${user.email}")
                user
            } else {
                android.util.Log.w("BuddyRepository", "getUserById: User not found in Firestore, using placeholder")
                User(uid = userId, name = "Bilinmeyen")
            }
        } catch (e: Exception) {
            android.util.Log.e("BuddyRepository", "getUserById: Error fetching user $userId", e)
            User(uid = userId, name = "Bilinmeyen")
        }
    }
}
