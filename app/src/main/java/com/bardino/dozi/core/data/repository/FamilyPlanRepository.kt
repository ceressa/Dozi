package com.bardino.dozi.core.data.repository

import android.util.Log
import com.bardino.dozi.core.data.model.FamilyPlan
import com.bardino.dozi.core.data.model.FamilyPlanStatus
import com.google.firebase.Timestamp
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import kotlinx.coroutines.tasks.await
import java.util.*
import kotlin.random.Random

/**
 * Aile Planı Repository
 *
 * Dozi Ekstra Aile Paketi yönetimi için repository.
 * Ana hesap (organizer) bir plan oluşturur, davet kodu ile üye ekler.
 */
class FamilyPlanRepository(
    private val auth: FirebaseAuth = FirebaseAuth.getInstance(),
    private val db: FirebaseFirestore = FirebaseFirestore.getInstance()
) {
    companion object {
        private const val TAG = "FamilyPlanRepository"
        private const val COLLECTION_FAMILY_PLANS = "family_plans"
        private const val COLLECTION_USERS = "users"
    }

    /**
     * Yeni bir aile planı oluştur (Ana hesap için)
     *
     * @param expiryDate Premium bitiş tarihi
     * @return Oluşturulan aile planı
     */
    suspend fun createFamilyPlan(expiryDate: Timestamp): Result<FamilyPlan> {
        val userId = auth.currentUser?.uid ?: return Result.failure(Exception("Kullanıcı giriş yapmamış"))

        return try {
            // Kullanıcı bilgilerini al
            val userDoc = db.collection(COLLECTION_USERS).document(userId).get().await()
            val userEmail = userDoc.getString("email") ?: ""
            val userName = userDoc.getString("name") ?: "Kullanıcı"

            // Benzersiz davet kodu oluştur
            val invitationCode = generateUniqueInvitationCode()

            // Aile planı oluştur
            val familyPlan = FamilyPlan(
                organizerId = userId,
                organizerEmail = userEmail,
                organizerName = userName,
                planType = "FAMILY_PREMIUM",
                maxMembers = 3,
                currentMembers = emptyList(),
                invitationCode = invitationCode,
                status = FamilyPlanStatus.ACTIVE,
                createdAt = Timestamp.now(),
                expiresAt = expiryDate,
                updatedAt = Timestamp.now()
            )

            // Firestore'a kaydet
            val docRef = db.collection(COLLECTION_FAMILY_PLANS).document()
            val familyPlanWithId = familyPlan.copy(id = docRef.id)
            docRef.set(familyPlanWithId).await()

            // Kullanıcının familyPlanId ve familyRole'ünü güncelle
            db.collection(COLLECTION_USERS).document(userId).update(
                mapOf(
                    "familyPlanId" to docRef.id,
                    "familyRole" to "ORGANIZER",
                    "planType" to "family_premium"
                )
            ).await()

            Log.d(TAG, "✅ Aile planı oluşturuldu: ${docRef.id}")
            Result.success(familyPlanWithId)
        } catch (e: Exception) {
            Log.e(TAG, "❌ Aile planı oluşturma hatası", e)
            Result.failure(e)
        }
    }

    /**
     * Benzersiz davet kodu oluştur (6 haneli)
     */
    private suspend fun generateUniqueInvitationCode(): String {
        var code: String
        var isUnique: Boolean

        do {
            // 6 haneli rastgele kod oluştur (A-Z, 0-9)
            code = (1..6).map {
                val chars = "ABCDEFGHIJKLMNOPQRSTUVWXYZ0123456789"
                chars[Random.nextInt(chars.length)]
            }.joinToString("")

            // Kodun benzersiz olup olmadığını kontrol et
            val querySnapshot = db.collection(COLLECTION_FAMILY_PLANS)
                .whereEqualTo("invitationCode", code)
                .whereEqualTo("status", "ACTIVE")
                .get()
                .await()

            isUnique = querySnapshot.isEmpty
        } while (!isUnique)

        return code
    }

    /**
     * Davet kodu ile aile planına katıl
     *
     * @param invitationCode Davet kodu
     * @return Katılınan aile planı
     */
    suspend fun joinFamilyPlan(invitationCode: String): Result<FamilyPlan> {
        val userId = auth.currentUser?.uid ?: return Result.failure(Exception("Kullanıcı giriş yapmamış"))

        return try {
            // Davet kodunu kontrol et
            val querySnapshot = db.collection(COLLECTION_FAMILY_PLANS)
                .whereEqualTo("invitationCode", invitationCode.uppercase())
                .whereEqualTo("status", "ACTIVE")
                .get()
                .await()

            if (querySnapshot.isEmpty) {
                return Result.failure(Exception("Geçersiz davet kodu"))
            }

            val familyPlanDoc = querySnapshot.documents[0]
            val familyPlan = familyPlanDoc.toObject(FamilyPlan::class.java)
                ?: return Result.failure(Exception("Aile planı bulunamadı"))

            // Yer var mı kontrol et
            if (!familyPlan.hasAvailableSlots()) {
                return Result.failure(Exception("Aile planı dolu (maksimum 3 üye)"))
            }

            // Kullanıcı zaten üye mi kontrol et
            if (familyPlan.isMember(userId)) {
                return Result.failure(Exception("Zaten bu aile planının üyesisiniz"))
            }

            // Kullanıcının başka bir aile planı var mı kontrol et
            val userDoc = db.collection(COLLECTION_USERS).document(userId).get().await()
            val existingFamilyPlanId = userDoc.getString("familyPlanId")
            if (!existingFamilyPlanId.isNullOrEmpty()) {
                return Result.failure(Exception("Zaten bir aile planı üyesisiniz"))
            }

            // Üyeleri güncelle
            val updatedMembers = familyPlan.currentMembers.toMutableList()
            updatedMembers.add(userId)

            // Aile planını güncelle
            db.collection(COLLECTION_FAMILY_PLANS).document(familyPlan.id).update(
                mapOf(
                    "currentMembers" to updatedMembers,
                    "updatedAt" to Timestamp.now()
                )
            ).await()

            // Kullanıcıyı güncelle
            db.collection(COLLECTION_USERS).document(userId).update(
                mapOf(
                    "familyPlanId" to familyPlan.id,
                    "familyRole" to "MEMBER",
                    "isPremium" to true,
                    "planType" to "family_premium",
                    "premiumExpiryDate" to familyPlan.expiresAt?.toDate()?.time
                )
            ).await()

            Log.d(TAG, "✅ Kullanıcı aile planına katıldı: ${familyPlan.id}")
            Result.success(familyPlan.copy(currentMembers = updatedMembers))
        } catch (e: Exception) {
            Log.e(TAG, "❌ Aile planına katılma hatası", e)
            Result.failure(e)
        }
    }

    /**
     * Kullanıcının aile planını getir
     */
    suspend fun getUserFamilyPlan(): Result<FamilyPlan?> {
        val userId = auth.currentUser?.uid ?: return Result.failure(Exception("Kullanıcı giriş yapmamış"))

        return try {
            // Kullanıcının familyPlanId'sini al
            val userDoc = db.collection(COLLECTION_USERS).document(userId).get().await()
            val familyPlanId = userDoc.getString("familyPlanId")

            if (familyPlanId.isNullOrEmpty()) {
                return Result.success(null)
            }

            // Aile planını getir
            val familyPlanDoc = db.collection(COLLECTION_FAMILY_PLANS).document(familyPlanId).get().await()
            val familyPlan = familyPlanDoc.toObject(FamilyPlan::class.java)

            Result.success(familyPlan)
        } catch (e: Exception) {
            Log.e(TAG, "❌ Aile planı getirme hatası", e)
            Result.failure(e)
        }
    }

    /**
     * Aile üyelerini getir (organizatör için)
     */
    suspend fun getFamilyMembers(familyPlanId: String): Result<List<Map<String, Any>>> {
        return try {
            // Aile planını getir
            val familyPlanDoc = db.collection(COLLECTION_FAMILY_PLANS).document(familyPlanId).get().await()
            val familyPlan = familyPlanDoc.toObject(FamilyPlan::class.java)
                ?: return Result.failure(Exception("Aile planı bulunamadı"))

            // Üye bilgilerini getir
            val members = mutableListOf<Map<String, Any>>()

            for (memberId in familyPlan.currentMembers) {
                val memberDoc = db.collection(COLLECTION_USERS).document(memberId).get().await()
                val memberData = mutableMapOf<String, Any>(
                    "uid" to memberId,
                    "name" to (memberDoc.getString("name") ?: "İsimsiz"),
                    "email" to (memberDoc.getString("email") ?: ""),
                    "photoUrl" to (memberDoc.getString("photoUrl") ?: ""),
                    "joinedAt" to (memberDoc.getLong("createdAt") ?: 0L)
                )
                members.add(memberData)
            }

            Result.success(members)
        } catch (e: Exception) {
            Log.e(TAG, "❌ Aile üyeleri getirme hatası", e)
            Result.failure(e)
        }
    }

    /**
     * Aile üyesini çıkar (sadece organizatör yapabilir)
     */
    suspend fun removeFamilyMember(memberId: String): Result<Unit> {
        val userId = auth.currentUser?.uid ?: return Result.failure(Exception("Kullanıcı giriş yapmamış"))

        return try {
            // Kullanıcının familyPlanId'sini al
            val userDoc = db.collection(COLLECTION_USERS).document(userId).get().await()
            val familyPlanId = userDoc.getString("familyPlanId")
                ?: return Result.failure(Exception("Aile planı bulunamadı"))
            val familyRole = userDoc.getString("familyRole")

            // Organizatör kontrolü
            if (familyRole != "ORGANIZER") {
                return Result.failure(Exception("Sadece plan sahibi üye çıkarabilir"))
            }

            // Aile planını getir
            val familyPlanDoc = db.collection(COLLECTION_FAMILY_PLANS).document(familyPlanId).get().await()
            val familyPlan = familyPlanDoc.toObject(FamilyPlan::class.java)
                ?: return Result.failure(Exception("Aile planı bulunamadı"))

            // Üyeyi listeden çıkar
            val updatedMembers = familyPlan.currentMembers.toMutableList()
            if (!updatedMembers.remove(memberId)) {
                return Result.failure(Exception("Üye bulunamadı"))
            }

            // Aile planını güncelle
            db.collection(COLLECTION_FAMILY_PLANS).document(familyPlanId).update(
                mapOf(
                    "currentMembers" to updatedMembers,
                    "updatedAt" to Timestamp.now()
                )
            ).await()

            // Üyenin planını kaldır
            db.collection(COLLECTION_USERS).document(memberId).update(
                mapOf(
                    "familyPlanId" to null,
                    "familyRole" to null,
                    "isPremium" to false,
                    "planType" to "free",
                    "premiumExpiryDate" to 0L
                )
            ).await()

            Log.d(TAG, "✅ Üye aile planından çıkarıldı: $memberId")
            Result.success(Unit)
        } catch (e: Exception) {
            Log.e(TAG, "❌ Üye çıkarma hatası", e)
            Result.failure(e)
        }
    }

    /**
     * Aile planından ayrıl (üye için)
     */
    suspend fun leaveFamilyPlan(): Result<Unit> {
        val userId = auth.currentUser?.uid ?: return Result.failure(Exception("Kullanıcı giriş yapmamış"))

        return try {
            // Kullanıcının familyPlanId'sini al
            val userDoc = db.collection(COLLECTION_USERS).document(userId).get().await()
            val familyPlanId = userDoc.getString("familyPlanId")
                ?: return Result.failure(Exception("Aile planı bulunamadı"))
            val familyRole = userDoc.getString("familyRole")

            // Organizatör ayrılamaz
            if (familyRole == "ORGANIZER") {
                return Result.failure(Exception("Plan sahibi ayrılamaz. Planı iptal edebilirsiniz."))
            }

            // Aile planını getir
            val familyPlanDoc = db.collection(COLLECTION_FAMILY_PLANS).document(familyPlanId).get().await()
            val familyPlan = familyPlanDoc.toObject(FamilyPlan::class.java)
                ?: return Result.failure(Exception("Aile planı bulunamadı"))

            // Üyeyi listeden çıkar
            val updatedMembers = familyPlan.currentMembers.toMutableList()
            updatedMembers.remove(userId)

            // Aile planını güncelle
            db.collection(COLLECTION_FAMILY_PLANS).document(familyPlanId).update(
                mapOf(
                    "currentMembers" to updatedMembers,
                    "updatedAt" to Timestamp.now()
                )
            ).await()

            // Kullanıcıyı güncelle
            db.collection(COLLECTION_USERS).document(userId).update(
                mapOf(
                    "familyPlanId" to null,
                    "familyRole" to null,
                    "isPremium" to false,
                    "planType" to "free",
                    "premiumExpiryDate" to 0L
                )
            ).await()

            Log.d(TAG, "✅ Kullanıcı aile planından ayrıldı")
            Result.success(Unit)
        } catch (e: Exception) {
            Log.e(TAG, "❌ Aile planından ayrılma hatası", e)
            Result.failure(e)
        }
    }

    /**
     * Aile planını iptal et (organizatör için)
     */
    suspend fun cancelFamilyPlan(): Result<Unit> {
        val userId = auth.currentUser?.uid ?: return Result.failure(Exception("Kullanıcı giriş yapmamış"))

        return try {
            // Kullanıcının familyPlanId'sini al
            val userDoc = db.collection(COLLECTION_USERS).document(userId).get().await()
            val familyPlanId = userDoc.getString("familyPlanId")
                ?: return Result.failure(Exception("Aile planı bulunamadı"))
            val familyRole = userDoc.getString("familyRole")

            // Organizatör kontrolü
            if (familyRole != "ORGANIZER") {
                return Result.failure(Exception("Sadece plan sahibi iptal edebilir"))
            }

            // Aile planını getir
            val familyPlanDoc = db.collection(COLLECTION_FAMILY_PLANS).document(familyPlanId).get().await()
            val familyPlan = familyPlanDoc.toObject(FamilyPlan::class.java)
                ?: return Result.failure(Exception("Aile planı bulunamadı"))

            // Tüm üyelerin premium'unu kaldır
            val allMembers = familyPlan.currentMembers + familyPlan.organizerId

            for (memberId in allMembers) {
                db.collection(COLLECTION_USERS).document(memberId).update(
                    mapOf(
                        "familyPlanId" to null,
                        "familyRole" to null,
                        "isPremium" to false,
                        "planType" to "free",
                        "premiumExpiryDate" to 0L
                    )
                ).await()
            }

            // Aile planını iptal et
            db.collection(COLLECTION_FAMILY_PLANS).document(familyPlanId).update(
                mapOf(
                    "status" to "CANCELLED",
                    "updatedAt" to Timestamp.now()
                )
            ).await()

            Log.d(TAG, "✅ Aile planı iptal edildi")
            Result.success(Unit)
        } catch (e: Exception) {
            Log.e(TAG, "❌ Aile planı iptal etme hatası", e)
            Result.failure(e)
        }
    }
}
