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
 * Badi sistemini yöneten repository
 */
class BadiRepository(
    private val auth: FirebaseAuth = FirebaseAuth.getInstance(),
    private val db: FirebaseFirestore = FirebaseFirestore.getInstance()
) {

    private val currentUserId: String?
        get() = auth.currentUser?.uid

    // ==================== Badi İşlemleri ====================

    /**
     * Kullanıcının badilerini real-time olarak dinle
     * ACTIVE ve PAUSED badileri gösterir, REMOVED olanları hariç tutar
     */
    fun getBadisFlow(): Flow<List<BadiWithUser>> = callbackFlow {
        val userId = currentUserId ?: run {
            android.util.Log.w("BadiRepository", "getBadisFlow: No user logged in")
            close()
            return@callbackFlow
        }

        android.util.Log.d("BadiRepository", "getBadisFlow: Listening for badis of userId=$userId")

        val scope = this

        val listener = db.collection("buddies")
            .whereEqualTo("userId", userId)
            .addSnapshotListener { snapshot, error ->
                if (error != null) {
                    android.util.Log.e("BadiRepository", "getBadisFlow: Error", error)
                    close(error)
                    return@addSnapshotListener
                }

                // Client-side filtreleme: REMOVED olanları hariç tut
                val badis = snapshot?.documents?.mapNotNull { doc ->
                    val badi = doc.toObject(Badi::class.java)?.copy(id = doc.id)
                    android.util.Log.d("BadiRepository", "getBadisFlow: Found badi record - id=${doc.id}, userId=${badi?.userId}, buddyUserId=${badi?.buddyUserId}, status=${badi?.status}")

                    // REMOVED olanları ve self-buddy'leri filtrele
                    when {
                        badi?.status == BadiStatus.REMOVED -> {
                            android.util.Log.d("BadiRepository", "getBadisFlow: Filtering out REMOVED badi ${doc.id}")
                            null
                        }
                        badi?.userId == badi?.buddyUserId -> {
                            android.util.Log.w("BadiRepository", "getBadisFlow: Filtering out self-buddy ${doc.id}")
                            null
                        }
                        else -> badi
                    }
                } ?: emptyList()

                android.util.Log.d("BadiRepository", "getBadisFlow: Total ${badis.size} badi records found")

                // ❗ Suspend fonksiyon kullanacağımız için coroutine açıyoruz
                scope.launch {
                    val list = badis.map { badi ->
                        android.util.Log.d("BadiRepository", "getBadisFlow: Fetching user info for buddyUserId=${badi.buddyUserId}")
                        val user = getUserById(badi.buddyUserId)
                        android.util.Log.d("BadiRepository", "getBadisFlow: Got user - uid=${user.uid}, name=${user.name}, email=${user.email}")
                        BadiWithUser(badi, user)
                    }
                    android.util.Log.d("BadiRepository", "getBadisFlow: Sending ${list.size} badis to UI")
                    trySend(list).isSuccess
                }
            }

        awaitClose { listener.remove() }
    }


    /**
     * Belirli bir badiyi ID ile getir
     */
    suspend fun getBadiById(badiId: String): Badi? {
        return try {
            db.collection("buddies")
                .document(badiId)
                .get()
                .await()
                .toObject(Badi::class.java)
        } catch (e: Exception) {
            null
        }
    }

    suspend fun getBadisForUser(userId: String): List<Badi> {
        return try {
            val snapshot = db.collection("buddies")
                .whereEqualTo("userId", userId)
                .get()
                .await()

            snapshot.documents.mapNotNull { doc ->
                doc.toObject(Badi::class.java)?.copy(id = doc.id)
            }
        } catch (e: Exception) {
            emptyList()
        }
    }


    /**
     * İki kullanıcı arasında badi ilişkisi var mı kontrol et
     */
    suspend fun isBadi(otherUserId: String): Boolean {
        val userId = currentUserId ?: return false

        return try {
            val snapshot = db.collection("buddies")
                .whereEqualTo("userId", userId)
                .whereEqualTo("buddyUserId", otherUserId)
                .whereEqualTo("status", BadiStatus.ACTIVE.name)
                .get()
                .await()

            !snapshot.isEmpty
        } catch (e: Exception) {
            false
        }
    }

    /**
     * Badi izinlerini güncelle
     */
    suspend fun updateBadiPermissions(
        badiId: String,
        permissions: BadiPermissions
    ): Result<Unit> {
        return try {
            db.collection("buddies")
                .document(badiId)
                .update("permissions", permissions)
                .await()
            Result.success(Unit)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    /**
     * Badi bildirim tercihlerini güncelle
     */
    suspend fun updateBadiNotificationPreferences(
        badiId: String,
        preferences: BadiNotificationPreferences
    ): Result<Unit> {
        return try {
            db.collection("buddies")
                .document(badiId)
                .update("notificationPreferences", preferences)
                .await()
            Result.success(Unit)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    /**
     * Badiye nickname (takma isim) ekle
     */
    suspend fun updateBadiNickname(
        badiId: String,
        nickname: String?
    ): Result<Unit> {
        return try {
            db.collection("buddies")
                .document(badiId)
                .update("nickname", nickname)
                .await()
            Result.success(Unit)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    /**
     * Badi durumunu güncelle (ACTIVE, PAUSED, REMOVED)
     */
    suspend fun updateBadiStatus(
        badiId: String,
        status: BadiStatus
    ): Result<Unit> {
        return try {
            db.collection("buddies")
                .document(badiId)
                .update("status", status.name)
                .await()
            Result.success(Unit)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    /**
     * Badi ilişkisini kaldır
     */
    suspend fun removeBadi(badiId: String): Result<Unit> {
        return try {
            db.collection("buddies")
                .document(badiId)
                .update("status", BadiStatus.REMOVED.name)
                .await()
            Result.success(Unit)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    // ==================== Badi İstekleri ====================

    /**
     * Badi kodu oluştur (6 haneli)
     */
    suspend fun generateBadiCode(): String {
        val userId = currentUserId ?: return ""

        val code = Random.nextInt(100000, 999999).toString()

        // User'a badi kodu kaydet
        db.collection("users")
            .document(userId)
            .update("buddyCode", code)
            .await()

        return code
    }

    /**
     * Badi kodu ile kullanıcı bul
     */
    suspend fun findUserByBadiCode(code: String): User? {
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
     * Badi isteği gönder
     */
    suspend fun sendBadiRequest(
        toUserId: String,
        message: String? = null
    ): Result<String> {
        val userId = currentUserId ?: return Result.failure(Exception("User not logged in"))

        // Kendine istek göndermeyi engelle
        if (userId == toUserId) {
            return Result.failure(Exception("Kendinize badi isteği gönderemezsiniz"))
        }

        return try {
            // Kullanıcı bilgilerini al
            val currentUser = db.collection("users").document(userId).get().await()
                .toObject(User::class.java) ?: return Result.failure(Exception("User not found"))

            // Daha önce istek gönderilmiş mi kontrol et
            val existingRequest = db.collection("buddy_requests")
                .whereEqualTo("fromUserId", userId)
                .whereEqualTo("toUserId", toUserId)
                .whereEqualTo("status", BadiRequestStatus.PENDING.name)
                .get()
                .await()

            if (!existingRequest.isEmpty) {
                return Result.failure(Exception("Zaten bekleyen bir istek var"))
            }

            // Yeni istek oluştur
            val request = BadiRequest(
                fromUserId = userId,
                toUserId = toUserId,
                fromUserName = currentUser.name,
                fromUserPhoto = currentUser.photoUrl,
                message = message,
                status = BadiRequestStatus.PENDING,
                expiresAt = Timestamp(Date(System.currentTimeMillis() + 7 * 24 * 60 * 60 * 1000)) // 7 gün
            )

            val docRef = db.collection("buddy_requests").add(request).await()
            Result.success(docRef.id)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    /**
     * Bekleyen badi isteklerini real-time dinle
     * Not: respondedAt NULL olan (henüz yanıt verilmemiş) istekleri getir
     * Firestore null değerleri index'lemediği için client-side filtreleme yapıyoruz
     */
    fun getPendingBadiRequestsFlow(): Flow<List<BadiRequestWithUser>> = callbackFlow {
        val userId = currentUserId ?: run {
            android.util.Log.w("BadiRepository", "getPendingBadiRequestsFlow: No user logged in")
            close()
            return@callbackFlow
        }

        android.util.Log.d("BadiRepository", "getPendingBadiRequestsFlow: Listening for requests to userId=$userId")

        val scope = this

        val listener = db.collection("buddy_requests")
            .whereEqualTo("toUserId", userId)
            .whereEqualTo("status", BadiRequestStatus.PENDING.name)
            .orderBy("createdAt", Query.Direction.DESCENDING)
            .addSnapshotListener { snapshot, error ->
                if (error != null) {
                    android.util.Log.e("BadiRepository", "getPendingBadiRequestsFlow error", error)
                    close(error)
                    return@addSnapshotListener
                }

                // Client-side filtreleme: SADECE respondedAt NULL olanları al
                val requests = snapshot?.documents?.mapNotNull { doc ->
                    val request = doc.toObject(BadiRequest::class.java)?.copy(id = doc.id)
                    if (request?.respondedAt != null) {
                        android.util.Log.w("BadiRepository", "Found request with respondedAt but still PENDING: ${doc.id}")
                        null // Filtrele
                    } else {
                        android.util.Log.d("BadiRepository", "Found valid pending request: ${doc.id}")
                        request
                    }
                } ?: emptyList()

                android.util.Log.d("BadiRepository", "Total valid pending requests: ${requests.size}")

                scope.launch {
                    val list = requests.map { request ->
                        val user = getUserById(request.fromUserId)
                        BadiRequestWithUser(request, user)
                    }
                    android.util.Log.d("BadiRepository", "Sending ${list.size} pending requests to UI")
                    trySend(list).isSuccess
                }
            }

        awaitClose { listener.remove() }
    }


    /**
     * Badi isteğini kabul et
     */
    suspend fun acceptBadiRequest(requestId: String): Result<Unit> {
        val userId = currentUserId ?: run {
            android.util.Log.e("BadiRepository", "acceptBadiRequest: User not logged in")
            return Result.failure(Exception("User not logged in"))
        }

        return try {
            android.util.Log.d("BadiRepository", "acceptBadiRequest: Getting request $requestId")

            // İsteği al
            val requestDoc = db.collection("buddy_requests").document(requestId).get().await()
            val request = requestDoc.toObject(BadiRequest::class.java)
                ?: run {
                    android.util.Log.e("BadiRepository", "acceptBadiRequest: Request not found")
                    return Result.failure(Exception("Request not found"))
                }

            // İstek zaten kabul edilmiş mi kontrol et
            if (request.status != BadiRequestStatus.PENDING) {
                android.util.Log.w("BadiRepository", "acceptBadiRequest: Request already processed (status=${request.status})")
                return Result.failure(Exception("Bu istek zaten işleme alınmış"))
            }

            android.util.Log.d("BadiRepository", "acceptBadiRequest: From ${request.fromUserId} to $userId")

            // 🔥 FIX: Self-buddy kontrolü - kendine badi isteği kabul edilemez
            if (request.fromUserId == userId) {
                android.util.Log.e("BadiRepository", "acceptBadiRequest: Cannot add yourself as badi (self-buddy)")
                return Result.failure(Exception("Kendinizi badi olarak ekleyemezsiniz"))
            }

            // Bu kullanıcılar arasında zaten badi ilişkisi var mı kontrol et
            val existingBadi = db.collection("buddies")
                .whereEqualTo("userId", userId)
                .whereEqualTo("buddyUserId", request.fromUserId)
                .whereEqualTo("status", BadiStatus.ACTIVE.name)
                .get()
                .await()

            if (!existingBadi.isEmpty) {
                android.util.Log.w("BadiRepository", "acceptBadiRequest: Badi relationship already exists")
                // İsteği kabul edildi olarak işaretle
                db.collection("buddy_requests").document(requestId)
                    .update(
                        mapOf(
                            "status" to BadiRequestStatus.ACCEPTED.name,
                            "respondedAt" to FieldValue.serverTimestamp()
                        )
                    ).await()
                return Result.success(Unit)
            }

            // İki yönlü badi ilişkisi oluştur
            val badi1 = Badi(
                userId = request.fromUserId,
                buddyUserId = userId,
                status = BadiStatus.ACTIVE
            )

            val badi2 = Badi(
                userId = userId,
                buddyUserId = request.fromUserId,
                status = BadiStatus.ACTIVE
            )

            android.util.Log.d("BadiRepository", "acceptBadiRequest: Creating badis - badi1(userId=${badi1.userId}, buddyUserId=${badi1.buddyUserId}), badi2(userId=${badi2.userId}, buddyUserId=${badi2.buddyUserId})")

            // Firestore batch işlemi
            val batch = db.batch()

            // Badi ilişkilerini ekle
            val badi1Ref = db.collection("buddies").document()
            val badi2Ref = db.collection("buddies").document()

            batch.set(badi1Ref, badi1)
            batch.set(badi2Ref, badi2)

            // İstek durumunu güncelle
            batch.update(
                db.collection("buddy_requests").document(requestId),
                mapOf(
                    "status" to BadiRequestStatus.ACCEPTED.name,
                    "respondedAt" to FieldValue.serverTimestamp()
                )
            )

            android.util.Log.d("BadiRepository", "acceptBadiRequest: Committing batch...")
            batch.commit().await()

            android.util.Log.d("BadiRepository", "acceptBadiRequest: ✅ Success - Badi relationship created")
            Result.success(Unit)
        } catch (e: Exception) {
            android.util.Log.e("BadiRepository", "acceptBadiRequest: ❌ Error", e)
            Result.failure(e)
        }
    }

    /**
     * Kirli badi request kayıtlarını temizle
     * (respondedAt varsa ama status hala PENDING olanları düzelt)
     */
    suspend fun cleanupStaleRequests(): Result<Int> {
        val userId = currentUserId ?: return Result.failure(Exception("User not logged in"))

        return try {
            android.util.Log.d("BadiRepository", "cleanupStaleRequests: Starting cleanup")

            // Status=PENDING ama respondedAt olan kayıtları bul
            val staleRequests = db.collection("buddy_requests")
                .whereEqualTo("toUserId", userId)
                .whereEqualTo("status", BadiRequestStatus.PENDING.name)
                .get()
                .await()

            val requestsToUpdate = staleRequests.documents.filter { doc ->
                val respondedAt = doc.get("respondedAt")
                respondedAt != null
            }

            android.util.Log.d("BadiRepository", "cleanupStaleRequests: Found ${requestsToUpdate.size} stale requests")

            if (requestsToUpdate.isEmpty()) {
                return Result.success(0)
            }

            // Bu istekleri EXPIRED olarak işaretle (veya sil)
            val batch = db.batch()
            requestsToUpdate.forEach { doc ->
                android.util.Log.d("BadiRepository", "cleanupStaleRequests: Deleting stale request ${doc.id}")
                batch.delete(doc.reference)
            }

            batch.commit().await()
            android.util.Log.d("BadiRepository", "cleanupStaleRequests: ✅ Cleaned up ${requestsToUpdate.size} stale requests")

            Result.success(requestsToUpdate.size)
        } catch (e: Exception) {
            android.util.Log.e("BadiRepository", "cleanupStaleRequests: ❌ Error", e)
            Result.failure(e)
        }
    }

    /**
     * Duplicate badi kayıtlarını temizle
     * (Her userId-buddyUserId kombinasyonundan sadece birini bırak)
     */
    suspend fun cleanupDuplicateBadis(): Result<Int> {
        val userId = currentUserId ?: return Result.failure(Exception("User not logged in"))

        return try {
            android.util.Log.d("BadiRepository", "cleanupDuplicateBadis: Starting cleanup for user $userId")

            // Kullanıcının tüm aktif badi kayıtlarını al (ACTIVE ve PAUSED)
            val badisSnapshot = db.collection("buddies")
                .whereEqualTo("userId", userId)
                .get()
                .await()

            // REMOVED olanları filtrele
            val activeDocs = badisSnapshot.documents.filter { doc ->
                val status = doc.toObject(Badi::class.java)?.status
                status != BadiStatus.REMOVED
            }

            // buddyUserId'ye göre grupla
            val grouped = activeDocs.groupBy { doc ->
                doc.toObject(Badi::class.java)?.buddyUserId ?: ""
            }

            var deletedCount = 0
            val batch = db.batch()

            // Her grup için sadece ilkini tut, diğerlerini sil
            grouped.forEach { (buddyUserId, docs) ->
                if (docs.size > 1) {
                    android.util.Log.d("BadiRepository", "cleanupDuplicateBadis: Found ${docs.size} duplicates for badi $buddyUserId")
                    // İlkini hariç tut, diğerlerini sil
                    docs.drop(1).forEach { doc ->
                        batch.delete(doc.reference)
                        deletedCount++
                    }
                }
            }

            if (deletedCount > 0) {
                batch.commit().await()
                android.util.Log.d("BadiRepository", "cleanupDuplicateBadis: ✅ Deleted $deletedCount duplicate records")
            } else {
                android.util.Log.d("BadiRepository", "cleanupDuplicateBadis: No duplicates found")
            }

            Result.success(deletedCount)
        } catch (e: Exception) {
            android.util.Log.e("BadiRepository", "cleanupDuplicateBadis: ❌ Error", e)
            Result.failure(e)
        }
    }

    /**
     * Self-buddy kayıtlarını temizle
     * (userId == buddyUserId olan hatalı kayıtları sil)
     */
    suspend fun cleanupSelfBuddies(): Result<Int> {
        val userId = currentUserId ?: return Result.failure(Exception("User not logged in"))

        return try {
            android.util.Log.d("BadiRepository", "cleanupSelfBuddies: Starting cleanup for user $userId")

            // Kullanıcının tüm badi kayıtlarını al
            val badisSnapshot = db.collection("buddies")
                .whereEqualTo("userId", userId)
                .get()
                .await()

            var deletedCount = 0
            val batch = db.batch()

            // Self-buddy kayıtlarını bul ve sil
            badisSnapshot.documents.forEach { doc ->
                val badi = doc.toObject(Badi::class.java)
                if (badi != null && badi.userId == badi.buddyUserId) {
                    android.util.Log.w("BadiRepository", "cleanupSelfBuddies: Found self-buddy record ${doc.id}")
                    batch.delete(doc.reference)
                    deletedCount++
                }
            }

            if (deletedCount > 0) {
                batch.commit().await()
                android.util.Log.d("BadiRepository", "cleanupSelfBuddies: ✅ Deleted $deletedCount self-buddy records")
            } else {
                android.util.Log.d("BadiRepository", "cleanupSelfBuddies: No self-buddy records found")
            }

            Result.success(deletedCount)
        } catch (e: Exception) {
            android.util.Log.e("BadiRepository", "cleanupSelfBuddies: ❌ Error", e)
            Result.failure(e)
        }
    }

    /**
     * Badi isteğini reddet
     */
    suspend fun rejectBadiRequest(requestId: String): Result<Unit> {
        return try {
            db.collection("buddy_requests")
                .document(requestId)
                .update(
                    mapOf(
                        "status" to BadiRequestStatus.REJECTED.name,
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
            android.util.Log.d("BadiRepository", "getUserById: Fetching user with userId=$userId")
            val userDoc = db.collection("users")
                .document(userId)
                .get()
                .await()

            val user = userDoc.toObject(User::class.java)
            if (user != null) {
                android.util.Log.d("BadiRepository", "getUserById: Found user - uid=${user.uid}, name=${user.name}, email=${user.email}")
                user
            } else {
                android.util.Log.w("BadiRepository", "getUserById: User not found in Firestore, using placeholder")
                User(uid = userId, name = "Bilinmeyen")
            }
        } catch (e: Exception) {
            android.util.Log.e("BadiRepository", "getUserById: Error fetching user $userId", e)
            User(uid = userId, name = "Bilinmeyen")
        }
    }

    // ═══════════════════════════════════════════════════════════════════════
    // 📏 PLAN LİMİT KONTROL METODLARİ
    // ═══════════════════════════════════════════════════════════════════════

    /**
     * Kullanıcının aktif badi sayısını döndür
     * ACTIVE ve PAUSED badileri sayar
     */
    suspend fun getActiveBadiCount(): Int {
        val userId = currentUserId ?: return 0

        return try {
            val snapshot = db.collection("buddies")
                .whereEqualTo("userId", userId)
                .get()
                .await()

            snapshot.documents.count { doc ->
                val status = doc.getString("status")
                status == BadiStatus.ACTIVE.name || status == BadiStatus.PAUSED.name
            }
        } catch (e: Exception) {
            android.util.Log.e("BadiRepository", "getActiveBadiCount: Error", e)
            0
        }
    }

    /**
     * Daha fazla badi eklenebilir mi kontrol et
     * @param limit Maksimum izin verilen badi sayısı (-1 = sınırsız)
     */
    suspend fun canAddMoreBadis(limit: Int): Boolean {
        if (limit == -1) return true // Sınırsız
        if (limit == 0) return false // Badi yok (Free plan)
        return getActiveBadiCount() < limit
    }

    /**
     * Badi isteği kabul edilebilir mi kontrol et
     * Her iki kullanıcının da limitini kontrol eder
     */
    suspend fun canAcceptBadiRequest(requestId: String, userLimit: Int, requesterLimit: Int): Result<Boolean> {
        val userId = currentUserId ?: return Result.failure(Exception("User not logged in"))

        return try {
            // İsteği al
            val request = db.collection("buddy_requests")
                .document(requestId)
                .get()
                .await()
                .toObject(BadiRequest::class.java)
                ?: return Result.failure(Exception("Request not found"))

            // Kabul eden kullanıcının limitini kontrol et
            if (!canAddMoreBadis(userLimit)) {
                return Result.success(false)
            }

            // İsteği gönderen kullanıcının limitini kontrol et
            val requesterBadiCount = try {
                val snapshot = db.collection("buddies")
                    .whereEqualTo("userId", request.fromUserId)
                    .get()
                    .await()

                snapshot.documents.count { doc ->
                    val status = doc.getString("status")
                    status == BadiStatus.ACTIVE.name || status == BadiStatus.PAUSED.name
                }
            } catch (e: Exception) {
                0
            }

            if (requesterLimit != -1 && requesterBadiCount >= requesterLimit) {
                return Result.success(false)
            }

            Result.success(true)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }
}
